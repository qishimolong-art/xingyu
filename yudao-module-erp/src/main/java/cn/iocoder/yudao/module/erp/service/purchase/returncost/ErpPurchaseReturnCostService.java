package cn.iocoder.yudao.module.erp.service.purchase.returncost;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.*;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.*;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseReturnModeEnum;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeSnapshotService.PurchaseReturnOrigin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 来源先于本单锁；按实际历史单据校验数量，金额只能由当时库存余额计算。 */
@Service
public class ErpPurchaseReturnCostService {
    private static final Object CONTEXT = new Object();
    private static final String RULE = "PURCHASE_RETURN_CURRENT_AVERAGE_V1";
    @Value("${erp.reporting.dual-cost-enabled:false}") private boolean enabled;
    @Resource private ErpPurchaseReturnMapper returnMapper;
    @Resource private ErpPurchaseReturnItemMapper returnItemMapper;
    @Resource private ErpPurchaseInMapper inMapper;
    @Resource private ErpPurchaseInItemMapper inItemMapper;
    @Resource private ErpSaleReturnMapper saleReturnMapper;
    @Resource private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource private ErpStockMapper stockMapper;
    @Resource private ErpWarehouseService warehouseService;
    @Resource private ErpPurchaseReturnCostRepository repository;

    public boolean isEnabled() { return enabled; }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void lockBeforeMutation(Long id,List<ErpPurchaseReturnItemDO> incoming) {
        lockBeforeMutations(id==null?Collections.emptyList():Collections.singletonList(id),incoming);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void lockBeforeMutations(Collection<Long> ids,List<ErpPurchaseReturnItemDO> incoming) {
        if (!enabled) return;
        List<ErpPurchaseReturnItemDO> combined=new ArrayList<>();
        for (Long id:new TreeSet<>(ids)) {
            if (returnMapper.selectById(id)==null) throw error("采购退货不存在或无数据权限");
            combined.addAll(returnItemMapper.selectListByReturnId(id));
        }
        if (incoming!=null) combined.addAll(incoming);
        lockReferences(combined);
    }

    private void lockReferences(List<ErpPurchaseReturnItemDO> items) {
        Context context=context();
        Set<Long> inIds=new TreeSet<>(),saleIds=new TreeSet<>();
        Set<Long> inItemIds=items.stream().map(ErpPurchaseReturnItemDO::getSourceInItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> saleItemIds=items.stream().map(ErpPurchaseReturnItemDO::getSourceSaleReturnItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!inItemIds.isEmpty()) {
            List<ErpPurchaseInItemDO> found=DataPermissionUtils.executeIgnore(()->inItemMapper.selectBatchIds(inItemIds));
            if (found.size()!=inItemIds.size()) throw error("原采购入库明细缺失，不能推测来源");
            found.forEach(i->inIds.add(i.getInId()));
        }
        if (!saleItemIds.isEmpty()) {
            List<ErpSaleReturnItemDO> found=DataPermissionUtils.executeIgnore(()->saleReturnItemMapper.selectBatchIds(saleItemIds));
            if (found.size()!=saleItemIds.size()) throw error("来源销售退货明细缺失");
            found.forEach(i->saleIds.add(i.getReturnId()));
        }
        items.forEach(i->{if(i.getSourceInId()!=null) inIds.add(i.getSourceInId());
            if(i.getSourceSaleReturnId()!=null) saleIds.add(i.getSourceSaleReturnId());});
        // 来源类型顺序固定：销售退货、采购入库；各类再按ID。重复调用只复用既有锁。
        for(Long id:saleIds) {
            String key=sourceKey(true,id);
            if(context.keys.contains(key)) continue;
            validateNextLock(context,key);
            ErpSaleReturnDO header=saleReturnMapper.selectByIdForUpdate(id);
            if(header==null || !Integer.valueOf(20).equals(header.getStatus())) throw error("来源销售退货不存在、无权限或未审核");
            context.saleHeaders.put(id,header);
            saleReturnItemMapper.selectListByReturnIdForUpdate(id).forEach(i->context.saleItems.put(i.getId(),i));
            context.keys.add(key);
        }
        for(Long id:inIds) {
            String key=sourceKey(false,id);
            if(context.keys.contains(key)) continue;
            validateNextLock(context,key);
            ErpPurchaseInDO header=inMapper.selectByIdForUpdate(id);
            if(header==null || !Integer.valueOf(20).equals(header.getStatus())) throw error("原采购入库不存在、无权限或未审核");
            context.inHeaders.put(id,header);
            inItemMapper.selectListByInIdForUpdate(id).forEach(i->context.inItems.put(i.getId(),i));
            context.keys.add(key);
        }
    }

    private void validateNextLock(Context context,String key) {
        if(context.parentLocked || (!context.keys.isEmpty() && context.keys.last().compareTo(key)>0)) {
            throw error("本事务来源集合已变化，请整体回滚后重新提交，不能逆序追加来源锁");
        }
    }

    /** 本单父锁取得后调用；旧+新来源若不在预先完整锁集合，立即回滚。 */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void validateLockedMutationSources(List<ErpPurchaseReturnItemDO> items) {
        if(!enabled) return;
        Context context=context();
        for(ErpPurchaseReturnItemDO item:items) {
            if(item.getSourceInItemId()!=null && !context.inItems.containsKey(item.getSourceInItemId()))
                throw error("锁定期间采购来源已变更，请重新提交");
            if(item.getSourceInId()!=null && !context.inHeaders.containsKey(item.getSourceInId()))
                throw error("锁定期间采购来源父单已变更，请重新提交");
            if(item.getSourceSaleReturnItemId()!=null && !context.saleItems.containsKey(item.getSourceSaleReturnItemId()))
                throw error("锁定期间销售退货来源已变更，请重新提交");
            if(item.getSourceSaleReturnId()!=null && !context.saleHeaders.containsKey(item.getSourceSaleReturnId()))
                throw error("锁定期间销售退货来源父单已变更，请重新提交");
        }
        context.parentLocked=true;
    }

    /** 归一化可推导来源ID/No；调用方必须把归一化完整行写入后再准备交易快照。 */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Map<Long,String> normalizeAndValidateSources(ErpPurchaseReturnDO header,List<ErpPurchaseReturnItemDO> items,
                                                       boolean includeProcessing) {
        if(!enabled) return Collections.emptyMap();
        Context context=context();
        validateLockedMutationSources(items);
        boolean byOrder=ErpPurchaseReturnModeEnum.isByOrder(header.getReturnMode());
        if(!byOrder && !ErpPurchaseReturnModeEnum.isByStock(header.getReturnMode())) throw error("采购退货模式无效");
        Map<String,List<ErpPurchaseReturnItemDO>> groups=new TreeMap<>();
        for(ErpPurchaseReturnItemDO item:items) {
            if(item.getCount()==null || item.getCount().signum()<=0) throw error("采购退货数量必须为正");
            if(byOrder) {
                ErpPurchaseInItemDO source=context.inItems.get(item.getSourceInItemId());
                if(source==null) throw error("按原单退货缺少真实入库明细");
                ErpPurchaseInDO in=context.inHeaders.get(source.getInId());
                if(in==null || !Objects.equals(source.getProductId(),item.getProductId())
                        || (item.getSourceInId()!=null && !Objects.equals(item.getSourceInId(),source.getInId()))
                        || (header.getSupplierId()!=null && !Objects.equals(header.getSupplierId(),in.getSupplierId())))
                    throw error("原采购主从、商品或供应商与退货不一致");
                item.setSourceInId(in.getId()).setSourceInNo(in.getNo());
                groups.computeIfAbsent("P:"+source.getId(),k->new ArrayList<>()).add(item);
            } else {
                item.setSourceInId(null).setSourceInItemId(null).setSourceInNo(null);
            }
            if(item.getSourceSaleReturnItemId()!=null) {
                ErpSaleReturnItemDO source=context.saleItems.get(item.getSourceSaleReturnItemId());
                if(source==null || !Objects.equals(source.getProductId(),item.getProductId())
                        || (item.getSourceSaleReturnId()!=null && !Objects.equals(item.getSourceSaleReturnId(),source.getReturnId())))
                    throw error("销售退货追溯主从或商品不一致");
                ErpSaleReturnDO sale=context.saleHeaders.get(source.getReturnId());
                item.setSourceSaleReturnId(sale.getId()).setSourceSaleReturnNo(sale.getNo());
                groups.computeIfAbsent("S:"+source.getId(),k->new ArrayList<>()).add(item);
            } else if(item.getSourceSaleReturnId()!=null) {
                throw error("销售退货追溯必须指定真实来源明细");
            } else {
                item.setSourceSaleReturnNo(null);
            }
        }
        Map<Long,Map<String,Object>> evidence=new HashMap<>();
        for(Map.Entry<String,List<ErpPurchaseReturnItemDO>> entry:groups.entrySet()) {
            boolean sale=entry.getKey().startsWith("S:");
            long sourceId=Long.parseLong(entry.getKey().substring(2));
            BigDecimal sourceQuantity=sale?context.saleItems.get(sourceId).getCount():context.inItems.get(sourceId).getCount();
            Long product=sale?context.saleItems.get(sourceId).getProductId():context.inItems.get(sourceId).getProductId();
            Long supplier=sale?null:context.inHeaders.get(context.inItems.get(sourceId).getInId()).getSupplierId();
            if(sourceQuantity==null || sourceQuantity.signum()<=0) throw error("真实来源数量缺失或无效");
            List<Map<String,Object>> previous=repository.relatedReturns(tenant(),sale,sourceId,header.getId(),includeProcessing);
            BigDecimal used=BigDecimal.ZERO;
            for(Map<String,Object> row:previous) {
                BigDecimal quantity=decimal(row,"count");
                if(quantity.signum()<=0 || !Objects.equals(number(row,"product_id"),product)
                        || (!sale && !Objects.equals(number(row,"supplier_id"),supplier)))
                    throw error("历史关联退货数量、商品或往来不一致，请先核对");
                used=used.add(quantity);
            }
            BigDecimal requested=entry.getValue().stream().map(ErpPurchaseReturnItemDO::getCount).reduce(BigDecimal.ZERO,BigDecimal::add);
            if(used.add(requested).compareTo(sourceQuantity)>0) throw error("采购退货超过真实来源的剩余可退/可转数量");
            Map<String,Object> data=new TreeMap<>();
            data.put("sourceQuantity",sourceQuantity);data.put("previousQuantity",used);data.put("requestedQuantity",requested);
            data.put("sourceItemId",sourceId);data.put("previousRows",previous);data.put("scope",sale?"ALL_LIVE_TRANSFERS":includeProcessing?"PROCESS_AND_APPROVE":"APPROVE");
            for(ErpPurchaseReturnItemDO item:entry.getValue()) evidence.computeIfAbsent(item.getId(),k->new TreeMap<>()).put(entry.getKey(),data);
        }
        Map<Long,String> result=new HashMap<>();
        evidence.forEach((id,value)->result.put(id,JsonUtils.toJsonString(value)));
        return result;
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Map<Long,ErpStockRecordCreateReqBO> prepareApproval(ErpPurchaseReturnDO header,List<ErpPurchaseReturnItemDO> items) {
        if(!enabled) return Collections.emptyMap();
        Context context=context();
        ErpPurchaseReturnDO stored=returnMapper.selectByIdForUpdate(header.getId());
        List<ErpPurchaseReturnItemDO> actual=returnItemMapper.selectListByReturnIdForUpdate(header.getId());
        if(stored==null || !Integer.valueOf(20).equals(stored.getStatus()) || items.isEmpty()
                || !sourceSignature(stored,actual).equals(sourceSignature(header,items))) throw error("采购退货过账来源与本次锁定审核不一致");
        if(stored.getSupplierId()==null || stored.getSupplierId()<=0) throw error("采购退货正式过账必须有真实供应商归属");
        Map<Long,String> evidence=normalizeAndValidateSources(stored,actual,false);
        if(!sourceSignature(stored,actual).equals(sourceSignature(header,items))) throw error("采购退货来源尚未归一化保存");
        validateStocks(items);
        Map<Long,ErpStockRecordCreateReqBO> result=new LinkedHashMap<>();
        String signature=sourceSignature(stored,actual);
        for(ErpPurchaseReturnItemDO item:items) {
            if (item.getProductPrice() == null || item.getProductPrice().signum() < 0) {
                throw error("采购退货正式过账必须有明确的非负退款单价");
            }
            ErpStockRecordCreateReqBO bo=new ErpStockRecordCreateReqBO(item.getProductId(),item.getWarehouseId(),item.getBatchNo(),
                    item.getCount().negate(),80,stored.getId(),item.getId(),stored.getNo(),item.getProductPrice(),stored.getReturnTime())
                    .setAccountingDeptId(stored.getDeptId()).setSourcePriceBasis("INCLUSIVE_UNCONFIRMED");
            if(ErpPurchaseReturnModeEnum.isByOrder(stored.getReturnMode())) bo.setSourceBizType(70).setSourceBizId(item.getSourceInId()).setSourceBizItemId(item.getSourceInItemId());
            Prepared prepared=new Prepared(tenant(),signature,postingSignature(bo),evidence.getOrDefault(item.getId(),"{}"),item);
            if(context.prepared.putIfAbsent(key(bo),prepared)!=null) throw error("本事务采购退货行已经准备");
            result.put(item.getId(),bo);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void beforeCommit(boolean readOnly) {
                if(repository.linkCount(tenant(),stored.getId())!=items.size()
                        || result.values().stream().anyMatch(bo->!context.prepared.get(key(bo)).posted))
                    throw error("采购退货尚未完成全部实际库存及来源证据，整单回滚");
            }
        });
        return result;
    }

    /** 只使用本次已锁定、已核对的原入库主从；不查订单，也不修改退货单自身订单引用。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public Map<Long, PurchaseReturnOrigin> preparedOrigins(Long returnId) {
        if (!enabled) {
            return Collections.emptyMap();
        }
        Context context = context();
        Map<Long, PurchaseReturnOrigin> origins = new LinkedHashMap<>();
        String prefix = returnId + ":";
        for (Map.Entry<String, Prepared> entry : context.prepared.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            Prepared prepared = entry.getValue();
            if (prepared.sourceInItemId == null) {
                continue;
            }
            ErpPurchaseInItemDO item = context.inItems.get(prepared.sourceInItemId);
            ErpPurchaseInDO header = context.inHeaders.get(prepared.sourceInId);
            if (item == null || header == null) {
                throw error("本次采购退货缺少已锁定的上游来源");
            }
            Long returnItemId = Long.valueOf(entry.getKey().substring(prefix.length()));
            origins.put(returnItemId, new PurchaseReturnOrigin(header, item));
        }
        return Collections.unmodifiableMap(origins);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void validatePosting(ErpStockRecordCreateReqBO request) { prepared(request); }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void appendAllocation(long postingId,LocalDateTime postedAt,ErpStockRecordCreateReqBO request) {
        Prepared prepared=prepared(request);
        Map<String,Object> event=repository.posting(tenant(),postingId);
        if(prepared.posted || event==null || !Objects.equals(number(event,"biz_type"),80L)
                || !Objects.equals(number(event,"biz_id"),request.getBizId()) || !Objects.equals(number(event,"biz_item_id"),request.getBizItemId())
                || !Objects.equals(number(event,"product_id"),request.getProductId()) || !Objects.equals(number(event,"warehouse_id"),request.getWarehouseId())
                || !Objects.equals(number(event,"accounting_dept_id"),request.getAccountingDeptId())
                || !Objects.equals(number(event,"source_biz_type"),request.getSourceBizType() == null ? null : request.getSourceBizType().longValue())
                || !Objects.equals(number(event,"source_biz_id"),request.getSourceBizId()) || !Objects.equals(number(event,"source_biz_item_id"),request.getSourceBizItemId())
                || !equal(decimal(event,"quantity"),request.getCount()) || decimal(event,"financial_movement").signum()>0
                || decimal(event,"settlement_movement").signum()>0 || !Objects.equals(dateTime(event.get("posted_at")),postedAt))
            throw error("采购退货追加证据必须绑定本次真实均价扣库事件");
        int inserted=repository.execute("INSERT INTO erp_purchase_return_posting_link (tenant_id,return_id,return_item_id,posting_id,"
                        +"source_in_id,source_in_item_id,trace_sale_return_id,trace_sale_return_item_id,quantity,financial_cost_amount,"
                        +"settlement_cost_amount,source_signature,quantity_evidence,posted_at,rule_version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                tenant(),request.getBizId(),request.getBizItemId(),postingId,prepared.sourceInId,prepared.sourceInItemId,
                prepared.saleReturnId,prepared.saleReturnItemId,request.getCount().negate(),decimal(event,"financial_movement").negate(),
                decimal(event,"settlement_movement").negate(),prepared.sourceSignature,prepared.evidence,postedAt,RULE);
        if(inserted!=1) throw error("采购退货来源证据追加失败");
        prepared.posted=true;
    }

    private void validateStocks(List<ErpPurchaseReturnItemDO> items) {
        Set<Long> products=items.stream().map(ErpPurchaseReturnItemDO::getProductId).collect(Collectors.toSet());
        Set<Long> warehouses=items.stream().map(ErpPurchaseReturnItemDO::getWarehouseId).collect(Collectors.toSet());
        Set<String> required=items.stream().map(i->i.getProductId()+":"+i.getWarehouseId()).collect(Collectors.toSet());
        Map<String,ErpStockDO> stocks=new HashMap<>();
        for(ErpStockDO stock:DataPermissionUtils.executeIgnore(()->stockMapper.selectListByProductIdsAndWarehouseIds(products,warehouses))) {
            String key=stock.getProductId()+":"+stock.getWarehouseId();
            if(required.contains(key) && stocks.put(key,stock)!=null) throw error("商品仓库存在重复库存维度");
        }
        if(!stocks.keySet().equals(required)) throw error("采购退货库存尚无可靠核算起点");
        warehouseService.validateCurrentUserStockPermission(stocks.values());
    }

    private Prepared prepared(ErpStockRecordCreateReqBO request) {
        Context context=(Context)TransactionSynchronizationManager.getResource(CONTEXT);
        Prepared value=context==null?null:context.prepared.get(key(request));
        if(value==null || value.tenant!=tenant() || !value.signature.equals(postingSignature(request))
                || (request.getPostingActionKey()!=null && !"APPROVE".equals(request.getPostingActionKey())))
            throw error("采购退货没有当前审核事务的完整真实来源授权");
        return value;
    }

    private Context context() {
        if(!TransactionSynchronizationManager.isActualTransactionActive()) throw error("采购退货来源操作必须在业务事务内");
        Context existing=(Context)TransactionSynchronizationManager.getResource(CONTEXT);
        if(existing!=null) {
            if(existing.tenant!=tenant()) throw error("采购退货来源上下文不能跨租户使用");
            return existing;
        }
        Context created=new Context();
        TransactionSynchronizationManager.bindResource(CONTEXT,created);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void suspend() {TransactionSynchronizationManager.unbindResourceIfPossible(CONTEXT);}
            @Override public void resume() {TransactionSynchronizationManager.bindResource(CONTEXT,created);}
            @Override public void afterCompletion(int status) {TransactionSynchronizationManager.unbindResourceIfPossible(CONTEXT);}
        });
        return created;
    }

    private static String sourceSignature(ErpPurchaseReturnDO h,List<ErpPurchaseReturnItemDO> items) {
        List<Object> values=new ArrayList<>(Arrays.asList(h.getId(),h.getNo(),h.getSupplierId(),h.getDeptId(),h.getReturnMode(),h.getReturnTime(),h.getTotalPrice(),h.getDiscountPrice(),h.getFeeAmount(),h.getFreightAmount()));
        List<ErpPurchaseReturnItemDO> sorted=new ArrayList<>(items);sorted.sort(Comparator.comparing(ErpPurchaseReturnItemDO::getId));
        for(ErpPurchaseReturnItemDO i:sorted) Collections.addAll(values,i.getId(),i.getReturnId(),i.getProductId(),i.getWarehouseId(),i.getCount(),i.getProductPrice(),i.getTotalPrice(),i.getBatchNo(),i.getSourceInId(),i.getSourceInItemId(),i.getSourceInNo(),i.getSourceSaleReturnId(),i.getSourceSaleReturnItemId(),i.getSourceSaleReturnNo());
        return hash(values);
    }
    private static String postingSignature(ErpStockRecordCreateReqBO r) {
        return hash(Arrays.asList(r.getBizType(),r.getBizId(),r.getBizItemId(),r.getProductId(),r.getWarehouseId(),r.getCount(),r.getUnitPrice(),r.getAccountingDeptId(),r.getBizDate(),r.getBatchNo(),r.getSourceBizType(),r.getSourceBizId(),r.getSourceBizItemId()));
    }
    private static String hash(List<Object> values) {
        StringBuilder text=new StringBuilder();
        for(Object value:values) {String v=value==null?"":value instanceof BigDecimal?((BigDecimal)value).stripTrailingZeros().toPlainString():value.toString();text.append(value==null?-1:v.length()).append(':').append(v);}
        try {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:bytes)hex.append(String.format("%02x",b&255));return hex.toString();}
        catch(java.security.NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    private static long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private static String sourceKey(boolean sale,Long id){return (sale?"0:":"1:")+String.format("%020d",id);}
    private static String key(ErpStockRecordCreateReqBO r){return r.getBizId()+":"+r.getBizItemId();}
    private static ServiceException error(String message){return new ServiceException(409,message);}
    private static Long number(Map<String,Object> row,String key){return row.get(key)==null?null:((Number)row.get(key)).longValue();}
    private static BigDecimal decimal(Map<String,Object> row,String key){if(row.get(key)==null)throw error("库存或来源数量字段缺失");return new BigDecimal(row.get(key).toString());}
    private static boolean equal(BigDecimal a,BigDecimal b){return a!=null && b!=null && a.compareTo(b)==0;}
    private static LocalDateTime dateTime(Object value){return value instanceof java.sql.Timestamp?((java.sql.Timestamp)value).toLocalDateTime():(LocalDateTime)value;}
    private static final class Context {
        private final long tenant=tenant();
        private final TreeSet<String> keys=new TreeSet<>();
        private final Map<Long,ErpPurchaseInDO> inHeaders=new HashMap<>();
        private final Map<Long,ErpPurchaseInItemDO> inItems=new HashMap<>();
        private final Map<Long,ErpSaleReturnDO> saleHeaders=new HashMap<>();
        private final Map<Long,ErpSaleReturnItemDO> saleItems=new HashMap<>();
        private final Map<String,Prepared> prepared=new HashMap<>();
        private boolean parentLocked;
    }
    private static final class Prepared {
        private final long tenant;
        private final String sourceSignature,signature,evidence;
        private final Long sourceInId,sourceInItemId,saleReturnId,saleReturnItemId;
        private boolean posted;
        private Prepared(long tenant,String sourceSignature,String signature,String evidence,ErpPurchaseReturnItemDO item) {
            this.tenant=tenant;this.sourceSignature=sourceSignature;this.signature=signature;this.evidence=evidence;
            sourceInId=item.getSourceInId();sourceInItemId=item.getSourceInItemId();saleReturnId=item.getSourceSaleReturnId();saleReturnItemId=item.getSourceSaleReturnItemId();
        }
    }
}
