package cn.iocoder.yudao.module.erp.service.purchase.cost;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpPurchaseCostConfirmationModels.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.*;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** 明确未税总金额确认；不会把来源临时含税价或未知税率转成未税成本。 */
@Service
public class ErpPurchaseCostConfirmationService {
    private static final Object APPROVAL_CONTEXT=new Object();
    @Resource private ErpPurchaseInMapper purchaseInMapper;
    @Resource private ErpPurchaseInItemMapper itemMapper;
    @Resource private ErpProductMapper productMapper;
    @Resource private ErpPurchaseCostConfirmationRepository repository;
    @Resource private PermissionApi permissionApi;
    @Resource @Lazy private ErpWarehouseService warehouseService;
    @Resource private ErpStockMapper stockMapper;
    @Value("${erp.reporting.dual-cost-enabled:false}") private boolean enabled;

    @Transactional(readOnly=true)
    public Header preview(Long id) {
        Source source=load(id,false,true);
        Header header=header(source,null);
        try { repository.checkSchema(); return header(source,repository.latest(tenant(),id,false)); }
        catch(BadSqlGrammarException e) { header.setStatus("SCHEMA_MISSING"); header.setReason("采购成本确认表未迁移，请先部署增量脚本");header.setCanConfirm(false);header.setConfirmBlockedReason(header.getReason());return header; }
    }

    @Transactional(readOnly=true)
    public PageResult<Item> itemPage(Long id,String expectedSignature,int pageNo,int pageSize) {
        if(pageNo<1||pageSize<1||pageSize>500) throw error(400,"分页大小必须为1到500");
        Source source=load(id,false,true);
        if(!Objects.equals(source.signature,expectedSignature)) throw error(409,"采购来源已变化，请重新加载完整预览");
        requireSchema();
        Map<String,Object> confirmation=repository.latest(tenant(),id,false);
        int from=(int)Math.min((long)(pageNo-1)*pageSize,source.items.size());
        int to=Math.min(from+pageSize,source.items.size());
        List<ErpPurchaseInItemDO> page=source.items.subList(from,to);
        Map<Long,Map<String,Object>> confirmed=new HashMap<>();
        if(confirmation!=null && Objects.equals(source.signature,confirmation.get("source_signature")))
            for(Map<String,Object> row:repository.linesForItems(tenant(),number(confirmation,"id"),page.stream().map(ErpPurchaseInItemDO::getId).collect(Collectors.toList()))) confirmed.put(number(row,"source_item_id"),row);
        Map<Long,ErpProductDO> products=new HashMap<>();
        Set<Long> productIds=page.stream().map(ErpPurchaseInItemDO::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        if(!productIds.isEmpty()) for(ErpProductDO product:DataPermissionUtils.executeIgnore(()->productMapper.selectByIds(productIds)))products.put(product.getId(),product);
        List<Item> result=new ArrayList<>();
        for(ErpPurchaseInItemDO row:page) {
            Item item=new Item();item.setSourceSignature(source.signature);item.setSourceItemId(row.getId());item.setProductId(row.getProductId());
            item.setWarehouseId(row.getWarehouseId());item.setQuantity(row.getCount());item.setGift(row.getGift());
            ErpProductDO product=products.get(row.getProductId());
            if(product!=null) {item.setProductCode(product.getCode());item.setProductName(product.getName());}
            if(!source.masked) {
                item.setRawUnitPrice(row.getProductPrice());item.setRawLineAmount(row.getTotalPrice());
                Map<String,Object> cost=confirmed.get(row.getId());
                if(cost!=null) {item.setConfirmedNetTotalAmount(decimal(cost,"confirmed_net_total_amount"));item.setEvidence((String)cost.get("evidence"));}
            }
            result.add(item);
        }
        return new PageResult<>(result,(long)source.items.size());
    }

    @Transactional(rollbackFor=Exception.class)
    public Header confirm(ConfirmRequest request) {
        validateRequest(request);
        Source source=load(request.getPurchaseInId(),true,true);
        if(source.masked) throw error(403,"当前字段权限不允许查看并确认采购核算成本");
        requireSchema();
        String requestHash=ErpPurchaseCostSignature.requestHash(request);
        Map<String,Object> retry=repository.byRequest(tenant(),request.getRequestKey());
        if(retry!=null) {
            if(!Objects.equals(requestHash,retry.get("request_hash"))||number(retry,"purchase_in_id")!=request.getPurchaseInId())
                throw error(409,"相同确认请求标识的内容不同");
            return header(source,retry);
        }
        if(!confirmableStatus(source.header.getStatus())) throw error(409,"仅草稿或未审核采购可确认成本；已审核历史单不能补确认或覆盖");
        if(!Objects.equals(source.signature,request.getExpectedSignature())) throw error(409,"来源数量、金额或归属已变化，请重新预览确认");
        Map<String,Object> latest=repository.latest(tenant(),request.getPurchaseInId(),true);
        int revision=latest==null?0:((Number)latest.get("revision")).intValue();
        if(revision!=request.getExpectedRevision()) throw error(409,"成本确认版本已变化，请重新加载");
        if(latest!=null && latest.get("consumed_at")!=null) throw error(409,"已消费成本快照不可覆盖");
        Map<Long,ConfirmItem> requested=new HashMap<>();
        for(ConfirmItem item:request.getItems()) if(requested.put(item.getSourceItemId(),item)!=null) throw error(400,"确认明细重复");
        Set<Long> ids=source.items.stream().map(ErpPurchaseInItemDO::getId).collect(Collectors.toSet());
        if(!ids.equals(requested.keySet())) throw error(409,"必须确认当前采购单全部来源明细，不能遗漏未加载页");
        for(ErpPurchaseInItemDO item:source.items) {
            if(item.getCount()==null||item.getCount().signum()<=0||item.getProductId()==null||item.getWarehouseId()==null)
                throw error(409,"请先补齐采购来源商品、仓库和正数量");
            precision(item.getCount());
        }
        LocalDateTime now=LocalDateTime.now();
        long id=repository.insert("INSERT INTO erp_purchase_cost_confirmation (tenant_id,purchase_in_id,revision,request_key,request_hash,"
                +"source_signature,source_snapshot,rule_version,price_basis,tax_status,evidence,fee_treatment,confirmed_by,confirmed_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                tenant(),request.getPurchaseInId(),revision+1,request.getRequestKey(),requestHash,source.signature,source.snapshot,
                "MANUAL_LINE_AMOUNT_V1","INCLUSIVE_UNCONFIRMED","UNKNOWN",request.getEvidence(),request.getFeeTreatment(),operator(),now);
        for(ErpPurchaseInItemDO item:source.items) {
            ConfirmItem cost=requested.get(item.getId());
            repository.execute("INSERT INTO erp_purchase_cost_confirmation_line (tenant_id,confirmation_id,source_item_id,product_id,warehouse_id,"
                    +"quantity,confirmed_net_total_amount,raw_unit_price,raw_line_amount,evidence) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    tenant(),id,item.getId(),item.getProductId(),item.getWarehouseId(),item.getCount(),cost.getConfirmedNetTotalAmount(),
                    item.getProductPrice(),item.getTotalPrice(),cost.getEvidence());
        }
        return header(source,repository.byId(tenant(),id));
    }

    /** 已锁定并重新校验状态的采购审核调用；false开关直接返回，不访问新表。 */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Map<Long,ErpStockRecordCreateReqBO> prepareApproval(ErpPurchaseInDO header,List<ErpPurchaseInItemDO> items) {
        if(!enabled) return Collections.emptyMap();
        ErpPurchaseInDO stored=purchaseInMapper.selectByIdForUpdate(header.getId());
        if(stored==null||!ErpAuditStatus.APPROVE.getStatus().equals(stored.getStatus())) throw error(409,"采购成本只能在已认领的审核事务中消费");
        List<ErpPurchaseInItemDO> storedItems=itemMapper.selectListByInIdForUpdate(header.getId());
        if(!ErpPurchaseCostSignature.snapshot(stored,storedItems).equals(ErpPurchaseCostSignature.snapshot(header,items)))
            throw error(409,"审核来源快照与锁定数据库记录不一致");
        requireSchema();
        Map<String,Object> confirmation=repository.latest(tenant(),header.getId(),true);
        String signature=ErpPurchaseCostSignature.hash(ErpPurchaseCostSignature.snapshot(header,items));
        if(confirmation==null || !Objects.equals(signature,confirmation.get("source_signature")))
            throw error(409,"采购未完成当前来源的未税成本确认，不能进入新账");
        if(confirmation.get("consumed_at")!=null) throw error(409,"该采购确认已消费，不允许再次入库");
        Map<Long,Map<String,Object>> costs=new HashMap<>();
        for(Map<String,Object> line:repository.lines(tenant(),number(confirmation,"id"))) costs.put(number(line,"source_item_id"),line);
        if(costs.size()!=items.size()) throw error(409,"采购确认未覆盖全部明细");
        Map<Long,ErpStockRecordCreateReqBO> result=new HashMap<>();
        for(ErpPurchaseInItemDO item:items) {
            Map<String,Object> line=costs.get(item.getId());
            if(line==null) throw error(409,"采购确认来源行缺失");
            BigDecimal amount=decimal(line,"confirmed_net_total_amount");
            ErpStockRecordCreateReqBO bo=new ErpStockRecordCreateReqBO(item.getProductId(),item.getWarehouseId(),item.getBatchNo(),item.getCount(),
                    70,header.getId(),item.getId(),header.getNo(),item.getProductPrice(),header.getInTime());
            bo.setAccountingDeptId(header.getDeptId()).setSourcePriceBasis("INCLUSIVE_UNCONFIRMED").setCostBasisConfirmed(true)
                    .setFinancialMovementAmount(amount).setSettlementMovementAmount(amount)
                    .setCostConfirmationId(number(confirmation,"id")).setCostConfirmationRevision(((Number)confirmation.get("revision")).intValue());
            result.put(item.getId(),bo);
        }
        if(repository.execute("UPDATE erp_purchase_cost_confirmation SET consumed_at=? WHERE tenant_id=? AND id=? AND consumed_at IS NULL",
                LocalDateTime.now(),tenant(),number(confirmation,"id"))!=1) throw error(409,"采购确认消费冲突");
        final long confirmationId=number(confirmation,"id"), currentTenant=tenant();
        final int expectedLines=items.size();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void beforeCommit(boolean readOnly) {
                if(repository.postedLineCount(currentTenant,confirmationId)!=expectedLines)
                    throw error(409,"采购确认消费未完成全部库存过账，整单回滚");
            }
        });
        @SuppressWarnings("unchecked") Map<String,String> context=(Map<String,String>)TransactionSynchronizationManager.getResource(APPROVAL_CONTEXT);
        if(context==null) {
            context=new HashMap<>();TransactionSynchronizationManager.bindResource(APPROVAL_CONTEXT,context);
            final Map<String,String> createdContext=context;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void suspend() {TransactionSynchronizationManager.unbindResourceIfPossible(APPROVAL_CONTEXT);}
                @Override public void resume() {TransactionSynchronizationManager.bindResource(APPROVAL_CONTEXT,createdContext);}
                @Override public void afterCompletion(int status) {TransactionSynchronizationManager.unbindResourceIfPossible(APPROVAL_CONTEXT);}
            });
        }
        for(ErpStockRecordCreateReqBO bo:result.values())context.put(contextKey(bo),postingSignature(bo));
        return result;
    }

    /** 统一过账验证真实持久化引用，禁止仅靠调用方布尔标记声明成本已确认。 */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void validatePosting(ErpStockRecordCreateReqBO request) {
        @SuppressWarnings("unchecked") Map<String,String> context=(Map<String,String>)TransactionSynchronizationManager.getResource(APPROVAL_CONTEXT);
        if(context==null||!Objects.equals(context.get(contextKey(request)),postingSignature(request)))
            throw error(409,"采购成本引用未由当前审核事务授权，不可凭历史确认或布尔值直接过账");
        if(request.getCostConfirmationId()==null||request.getCostConfirmationRevision()==null) throw error(409,"采购过账缺少真实成本确认引用");
        Map<String,Object> confirmation=repository.byId(tenant(),request.getCostConfirmationId());
        if(confirmation==null || number(confirmation,"purchase_in_id")!=request.getBizId()
                || ((Number)confirmation.get("revision")).intValue()!=request.getCostConfirmationRevision()
                || confirmation.get("consumed_at")==null || (StringUtils.hasText(request.getPostingActionKey())&&!"APPROVE".equals(request.getPostingActionKey())))
            throw error(409,"采购成本确认尚未在当前审核事务消费或动作不合法");
        Map<String,Object> line=repository.line(tenant(),request.getCostConfirmationId(),request.getBizItemId());
        if(line==null || number(line,"product_id")!=request.getProductId() || number(line,"warehouse_id")!=request.getWarehouseId()
                || !equal(decimal(line,"quantity"),request.getCount())
                || !equal(decimal(line,"confirmed_net_total_amount"),request.getFinancialMovementAmount())
                || !equal(request.getFinancialMovementAmount(),request.getSettlementMovementAmount())) throw error(409,"采购过账金额或来源与确认快照不一致");
    }

    private Source load(Long id,boolean lock,boolean checkPermission) {
        operator();
        ErpPurchaseInDO header=lock?purchaseInMapper.selectByIdForUpdate(id):purchaseInMapper.selectById(id);
        if(header==null) throw error(403,"采购单不存在或无单据数据权限");
        List<ErpPurchaseInItemDO> items=lock?itemMapper.selectListByInIdForUpdate(id):itemMapper.selectListByInId(id);
        items=new ArrayList<>(items);items.sort(Comparator.comparing(ErpPurchaseInItemDO::getId));
        Set<Long> depts=new HashSet<>();if(header.getDeptId()!=null) depts.add(header.getDeptId());
        Set<Long> productIds=new HashSet<>(),warehouseIds=new HashSet<>();Set<String> pairs=new HashSet<>();
        for(ErpPurchaseInItemDO item:items) {
            if(item.getDeptId()!=null)depts.add(item.getDeptId());
            if(item.getProductId()!=null&&item.getWarehouseId()!=null) {
                productIds.add(item.getProductId());warehouseIds.add(item.getWarehouseId());pairs.add(pair(item.getProductId(),item.getWarehouseId()));
            }
        }
        Map<String,ErpStockDO> stocks=new HashMap<>();
        if(!pairs.isEmpty()) for(ErpStockDO stock:DataPermissionUtils.executeIgnore(()->stockMapper.selectListByProductIdsAndWarehouseIds(productIds,warehouseIds))) {
            String key=pair(stock.getProductId(),stock.getWarehouseId());if(!pairs.contains(key))continue;
            if(stocks.put(key,stock)!=null)throw error(409,"来源商品仓库存在重复库存维度，请先核对，不能任取一条");
        }
        if(!stocks.keySet().equals(pairs))throw error(409,"来源明细尚无可核对库存，请先补齐商品仓库库存记录");
        if(!stocks.isEmpty())warehouseService.validateCurrentUserStockPermission(stocks.values());
        for(ErpStockDO stock:stocks.values())if(stock.getDeptId()!=null)depts.add(stock.getDeptId());
        Source source=new Source();source.header=header;source.items=items;
        source.snapshot=ErpPurchaseCostSignature.snapshot(header,items);source.signature=ErpPurchaseCostSignature.hash(source.snapshot);
        Set<String> fields=new HashSet<>();
        for(String module:Arrays.asList("erp_product","erp_purchase_in")) {
            add(fields,permissionApi.getCurrentUserHiddenFields(module));
            for(Long dept:depts) add(fields,permissionApi.getCurrentUserHiddenFields(module,dept));
        }
        source.masked=fields.stream().map(f->f.replaceFirst("^(select_)?(item_)?(col_)?","")).anyMatch(f ->
                Arrays.asList("purchasePrice","lastPurchasePrice","productPrice","totalPrice","costPrice","costAmount",
                        "financialAmount","settlementAmount","financialUnitCost","settlementUnitCost","financialMovement",
                        "settlementMovement","financialBalance","settlementBalance","confirmedNetTotalAmount","feeAmount","discountPrice","otherPrice",
                        "totalProductPrice","rawProductAmount","rawUnitPrice","rawLineAmount","discountAmount","totalFreight1","totalFreight2").contains(f));
        return source;
    }
    private Header header(Source source,Map<String,Object> confirmation) {
        Header result=new Header();result.setPurchaseInId(source.header.getId());result.setPurchaseNo(source.header.getNo());
        result.setSourceSignature(source.signature);result.setSourceItemCount(source.items.size());result.setCostMasked(source.masked);
        result.setLatestRevision(confirmation==null?0:((Number)confirmation.get("revision")).intValue());
        result.setSourceStatus(source.header.getStatus());
        result.setCanConfirm(confirmableStatus(source.header.getStatus())&&!source.masked&&!source.items.isEmpty()
                && (confirmation==null||confirmation.get("consumed_at")==null));
        if(!result.isCanConfirm())result.setConfirmBlockedReason(!confirmableStatus(source.header.getStatus())?"仅草稿或未审核单允许成本确认；历史已审核单不得补确认":
                source.masked?"成本字段无查看权限":source.items.isEmpty()?"采购明细为空":"确认版本已消费，不可覆盖");
        result.setStatus(confirmation==null?"NONE":!Objects.equals(source.signature,confirmation.get("source_signature"))?"STALE":
                confirmation.get("consumed_at")==null?"CONFIRMED":"CONSUMED");
        if(!source.masked) {
            result.setRawProductAmount(source.header.getTotalProductPrice());result.setDiscountAmount(source.header.getDiscountPrice());
            result.setFeeAmount(source.header.getFeeAmount()!=null?source.header.getFeeAmount():source.header.getOtherPrice());
            result.setFreight1(source.header.getTotalFreight1());result.setFreight2(source.header.getTotalFreight2());
        }
        if(confirmation!=null) {
            result.setConfirmationId(number(confirmation,"id"));result.setConfirmedBy(number(confirmation,"confirmed_by"));
            Object time=confirmation.get("confirmed_at");result.setConfirmedAt(time instanceof Timestamp?((Timestamp)time).toLocalDateTime():(LocalDateTime)time);
            if(!source.masked) {result.setEvidence((String)confirmation.get("evidence"));result.setFeeTreatment((String)confirmation.get("fee_treatment"));}
        }
        return result;
    }
    private void requireSchema() { try {repository.checkSchema();} catch(BadSqlGrammarException e){throw error(409,"采购成本确认表未迁移，请先部署增量脚本");} }
    private static void add(Set<String> target,List<String> values) {if(values!=null)target.addAll(values);}
    private static long tenant() {return TenantContextHolder.getRequiredTenantId();}
    private static long operator() {Long id=getLoginUserId();if(id==null)throw error(403,"请先登录");return id;}
    private static long number(Map<String,Object> row,String key) {return ((Number)row.get(key)).longValue();}
    private static BigDecimal decimal(Map<String,Object> row,String key) {Object v=row.get(key);return v==null?null:new BigDecimal(v.toString());}
    private static boolean equal(BigDecimal a,BigDecimal b) {return a!=null&&b!=null&&a.compareTo(b)==0;}
    private static ServiceException error(int code,String message) {return new ServiceException(code,message);}
    private static void precision(BigDecimal n) {BigDecimal value=n.stripTrailingZeros();if(value.scale()>6||value.precision()-value.scale()>18)throw error(400,"数量及成本最多18位整数、6位小数");}
    static void validateRequest(ConfirmRequest r) {
        if(r==null||r.getPurchaseInId()==null||r.getExpectedRevision()==null||r.getExpectedRevision()<0||!StringUtils.hasText(r.getExpectedSignature())
                ||r.getRequestKey()==null||!r.getRequestKey().matches("[A-Za-z0-9_-]{1,80}")||!text(r.getEvidence())||!text(r.getFeeTreatment())||r.getItems()==null||r.getItems().isEmpty())throw error(400,"请提供完整成本确认信息和费用处理说明");
        for(ConfirmItem item:r.getItems()) {
            if(item==null||item.getSourceItemId()==null||item.getConfirmedNetTotalAmount()==null||item.getConfirmedNetTotalAmount().signum()<0||!text(item.getEvidence()))throw error(400,"每行必须明确未税总成本及依据，未知不能填零");
            precision(item.getConfirmedNetTotalAmount());
        }
    }
    private static boolean text(String value) {return StringUtils.hasText(value)&&value.length()<=1000;}
    private static String pair(Long product,Long warehouse) {return product+":"+warehouse;}
    private static boolean confirmableStatus(Integer status) {return Integer.valueOf(0).equals(status)||ErpAuditStatus.PROCESS.getStatus().equals(status);}
    private static String contextKey(ErpStockRecordCreateReqBO bo) {return tenant()+":"+bo.getCostConfirmationId()+":"+bo.getBizItemId();}
    private static String postingSignature(ErpStockRecordCreateReqBO bo) {
        return ErpPurchaseCostSignature.hash(String.valueOf(Arrays.asList(bo.getProductId(),bo.getWarehouseId(),bo.getBatchNo(),
                bo.getCount(),bo.getBizType(),bo.getBizId(),bo.getBizItemId(),bo.getBizNo(),bo.getUnitPrice(),bo.getBizDate(),
                bo.getAccountingDeptId(),bo.getSourcePriceBasis(),bo.getCostBasisConfirmed(),bo.getFinancialMovementAmount(),
                bo.getSettlementMovementAmount(),bo.getCostConfirmationId(),bo.getCostConfirmationRevision(),bo.getPostingActionKey())));
    }
    private static class Source {ErpPurchaseInDO header;List<ErpPurchaseInItemDO> items;String signature;String snapshot;boolean masked;}
}
