package cn.iocoder.yudao.module.erp.service.report.trade;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.*;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.*;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.*;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 业务来源在审批父锁内冻结；库存writer持真实posting后同事务追加，查询不得补写历史。 */
@Service
public class ErpTradeSnapshotService {
    private static final Object CONTEXT_KEY = new Object();
    private static final com.fasterxml.jackson.databind.ObjectMapper EXACT_JSON = new com.fasterxml.jackson.databind.ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    @Value("${erp.reporting.dual-cost-enabled:false}") private boolean enabled;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private ErpSaleOutMapper saleOutMapper;
    @Resource private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource private ErpPurchaseInMapper purchaseInMapper;
    @Resource private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;

    /** 不可变且只能由本服务构造；不能由HTTP来源构造过账上下文。 */
    public static final class PreparedTradeContext {
        private final long tenant;
        private final int stockBizType;
        private final Long bizId, itemId, productId, warehouseId, accountingDeptId;
        private final BigDecimal quantity;
        private final String signature, json;
        private PreparedTradeContext(long tenant, int stockBizType, Map<String,Object> data) {
            this.tenant=tenant; this.stockBizType=stockBizType;
            this.bizId=number(data.get("bizId")); this.itemId=number(data.get("bizItemId"));
            this.productId=number(data.get("productId")); this.warehouseId=number(data.get("warehouseId"));
            this.accountingDeptId=number(data.get("accountingDeptId"));
            this.quantity=decimal(data.get("stockQuantity"));
            this.json=JsonUtils.toJsonString(new TreeMap<>(data));
            this.signature=hash(tenant+":"+stockBizType+":"+json);
        }
        public String getSignature() { return signature; }
    }

    /** 仅由采购退货来源锁服务传入已经锁定的真实入库主明细；构造后不随DO再变化。 */
    public static final class PurchaseReturnOrigin {
        private final Long inId,inItemId,productId,supplierId,orderId,orderItemId;
        private final String inNo,orderNo;
        public PurchaseReturnOrigin(ErpPurchaseInDO header,ErpPurchaseInItemDO item) {
            if(header==null||item==null||header.getId()==null||!Objects.equals(header.getId(),item.getInId()))
                throw new IllegalArgumentException("采购退货上游必须来自同一真实入库主明细");
            inId=header.getId();inItemId=item.getId();productId=item.getProductId();supplierId=header.getSupplierId();inNo=header.getNo();
            orderId=header.getOrderId();orderNo=header.getOrderNo();orderItemId=item.getOrderItemId();
        }
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Map<Long,PreparedTradeContext> prepareSaleOut(ErpSaleOutDO header,List<ErpSaleOutItemDO> items) {
        if(!enabled) return Collections.emptyMap();
        requireTransaction();
        ErpSaleOutDO stored=DataPermissionUtils.executeIgnore(()->saleOutMapper.selectByIdForUpdate(header.getId()));
        List<ErpSaleOutItemDO> locked=DataPermissionUtils.executeIgnore(()->saleOutItemMapper.selectListByOutIdForUpdate(header.getId()));
        verifySource(header,items,stored,locked);
        return prepare(50,bean(header),beans(items),Collections.emptyMap());
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Map<Long,PreparedTradeContext> preparePurchaseIn(ErpPurchaseInDO header,List<ErpPurchaseInItemDO> items) {
        if(!enabled) return Collections.emptyMap();
        requireTransaction();
        ErpPurchaseInDO stored=DataPermissionUtils.executeIgnore(()->purchaseInMapper.selectByIdForUpdate(header.getId()));
        List<ErpPurchaseInItemDO> locked=DataPermissionUtils.executeIgnore(()->purchaseInItemMapper.selectListByInIdForUpdate(header.getId()));
        verifySource(header,items,stored,locked);
        return prepare(70,bean(header),beans(items),Collections.emptyMap());
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Map<Long,PreparedTradeContext> prepareSaleReturn(ErpSaleReturnDO header,List<ErpSaleReturnItemDO> items,
                                                           Map<Long,Long> sourcePostingByReturnItem) {
        if(!enabled) return Collections.emptyMap();
        requireTransaction();
        // Return来源父锁/全量原行校验由退货审批服务持有；当前读确认实际已认领状态。
        List<Map<String,Object>> locked=jdbcTemplate.queryForList("SELECT id,status FROM erp_sale_return WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE",tenant(),header.getId());
        if(locked.size()!=1 || !Integer.valueOf(20).equals(((Number)locked.get(0).get("status")).intValue())
                || !Integer.valueOf(20).equals(header.getStatus())) throw new IllegalStateException("退货交易快照必须来自已认领审核的锁定来源");
        if(Integer.valueOf(20).equals(header.getReturnMode())) {
            if(sourcePostingByReturnItem==null||!sourcePostingByReturnItem.isEmpty()||header.getSourceOutId()!=null
                    ||header.getSourceOutNo()!=null&&!header.getSourceOutNo().trim().isEmpty()
                    ||items.stream().anyMatch(i->i.getSourceOutItemId()!=null))throw new IllegalStateException("无单销售退货不能带入虚构原销售来源");
        } else if(!Integer.valueOf(10).equals(header.getReturnMode()))throw new IllegalStateException("旧订单退货不可伪装为无单销售退货");
        return prepare(60,bean(header),beans(items),sourcePostingByReturnItem);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public Map<Long,PreparedTradeContext> preparePurchaseReturn(ErpPurchaseReturnDO header,List<ErpPurchaseReturnItemDO> items,
                                                              Map<Long,PurchaseReturnOrigin> originsByReturnItem) {
        if(!enabled) return Collections.emptyMap();
        requireTransaction();
        ErpPurchaseReturnDO stored=DataPermissionUtils.executeIgnore(()->purchaseReturnMapper.selectByIdForUpdate(header.getId()));
        List<ErpPurchaseReturnItemDO> locked=DataPermissionUtils.executeIgnore(()->purchaseReturnItemMapper.selectListByReturnIdForUpdate(header.getId()));
        verifySource(header,items,stored,locked);
        boolean byOrder=Integer.valueOf(10).equals(header.getReturnMode());
        if(originsByReturnItem==null||byOrder&&(!originsByReturnItem.keySet().equals(items.stream().map(ErpPurchaseReturnItemDO::getId).collect(Collectors.toSet())))
                || !byOrder&&!originsByReturnItem.isEmpty()) throw new IllegalStateException("采购退货上游引用必须与完整退货行逐项对应");
        return prepare(80,bean(header),beans(items),Collections.emptyMap(),originsByReturnItem);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void validatePreparedForPosting(ErpStockRecordCreateReqBO request) {
        if(!enabled) return;
        requireTransaction();
        PreparedTradeContext c=request.getTradeContext();
        if(c==null || !registered().contains(c)) throw new IllegalStateException("业务缺少本次审批事务内的交易来源快照");
        if(c.tenant!=tenant() || c.stockBizType!=request.getBizType()
                || !Objects.equals(c.bizId,request.getBizId()) || !Objects.equals(c.itemId,request.getBizItemId())
                || !Objects.equals(c.productId,request.getProductId()) || !Objects.equals(c.warehouseId,request.getWarehouseId())
                || c.accountingDeptId==null || c.accountingDeptId<=0 || !Objects.equals(c.accountingDeptId,request.getAccountingDeptId())
                || request.getCount()==null || c.quantity.compareTo(request.getCount())!=0)
            throw new IllegalStateException("交易快照与实际过账来源行或数量不一致");
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void appendForPosting(long postingId,LocalDateTime postedAt,PreparedTradeContext c) {
        requireTransaction();
        if(c==null || c.tenant!=tenant() || !registered().contains(c)) throw new IllegalStateException("交易快照不属于当前审批事务");
        Map<String,Object> posting=one("SELECT * FROM erp_stock_dual_cost_posting WHERE tenant_id=? AND id=?",tenant(),postingId);
        if(posting==null || !Objects.equals(number(posting.get("biz_id")),c.bizId)
                || !Objects.equals(number(posting.get("biz_item_id")),c.itemId)
                || !Objects.equals(number(posting.get("product_id")),c.productId)
                || !Objects.equals(number(posting.get("warehouse_id")),c.warehouseId)
                || c.accountingDeptId==null || c.accountingDeptId<=0 || !Objects.equals(number(posting.get("accounting_dept_id")),c.accountingDeptId)
                || ((Number)posting.get("biz_type")).intValue()!=c.stockBizType
                || decimal(posting.get("quantity")).compareTo(c.quantity)!=0
                || !asDate(posting.get("posted_at")).equals(postedAt))
            throw new IllegalStateException("交易快照必须绑定真实过账事件及实际时间");
        Map<String,Object> existing=one("SELECT signature FROM erp_business_report_item_snapshot WHERE tenant_id=? AND posting_id=?",tenant(),postingId);
        if(existing!=null) {
            if(!Objects.equals(existing.get("signature"),c.signature)) throw new IllegalStateException("已存在交易快照内容不同，禁止覆盖");
            return;
        }
        Map<String,Object> d=beanJson(c.json);
        jdbcTemplate.update("INSERT INTO erp_business_report_item_snapshot (tenant_id,posting_id,posted_at,signature,business_type,biz_id,biz_item_id,"
                +"party_id,party_name,sale_user_id,sale_user_name,accounting_dept_name,stock_dept_name,warehouse_name,purchaser,source_creator,product_code,product_name,oe_number,brand,category_id,unit_name,specification,"
                +"raw_gross_amount,gross_amount,net_amount,gross_status,original_biz_id,original_biz_item_id,original_biz_no,no_original_sale,snapshot_json) "
                +"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                tenant(),postingId,Timestamp.valueOf(postedAt),c.signature,d.get("businessType"),c.bizId,c.itemId,
                d.get("partyId"),d.get("partyName"),d.get("saleUserId"),d.get("saleUserName"),d.get("accountingDeptName"),d.get("stockDeptName"),d.get("warehouseName"),d.get("purchaser"),d.get("sourceCreator"),d.get("productCode"),d.get("productName"),
                d.get("oeNumber"),d.get("brand"),d.get("categoryId"),d.get("unitName"),d.get("specification"),d.get("rawGrossAmount"),d.get("grossAmount"),
                null,d.get("grossStatus"),d.get("originalBizId"),d.get("originalBizItemId"),d.get("originalBizNo"),d.get("noOriginalSale"),c.json);
    }

    private Map<Long,PreparedTradeContext> prepare(int type,Map<String,Object> header,List<Map<String,Object>> items,Map<Long,Long> sources) {
        return prepare(type,header,items,sources,Collections.emptyMap());
    }
    private Map<Long,PreparedTradeContext> prepare(int type,Map<String,Object> header,List<Map<String,Object>> items,Map<Long,Long> sources,
                                                 Map<Long,PurchaseReturnOrigin> purchaseOrigins) {
        if(items.isEmpty()) throw new IllegalStateException("交易来源没有完整商品行");
        boolean purchase=type==70||type==80;
        String kind=type==80?"PURCHASE_RETURN":type==70?"PURCHASE_IN":type==60?"SALE_RETURN":"SALE_OUT";
        Long partyId=number(header.get(purchase?"supplierId":"customerId"));
        Map<String,Object> party=partyId==null?null:one("SELECT name FROM "+(purchase?"erp_supplier":"erp_customer")+" WHERE tenant_id=? AND id=?",tenant(),partyId);
        Set<Long> productIds=items.stream().map(i->number(i.get("productId"))).collect(Collectors.toCollection(TreeSet::new));
        Map<Long,Map<String,Object>> products=products(productIds);
        Map<String,Map<String,Object>> stocks=stockNames(productIds);
        Map<String,Object> department=header.get("deptId")==null?null:one("SELECT name FROM system_dept WHERE tenant_id=? AND id=?",tenant(),header.get("deptId"));
        Map<String,Object> user=header.get("saleUserId")==null?null:one("SELECT nickname FROM system_users WHERE tenant_id=? AND id=?",tenant(),header.get("saleUserId"));
        Map<String,Object> handler=type!=60||header.get("handler")==null?null:one("SELECT nickname FROM system_users WHERE tenant_id=? AND id=?",tenant(),header.get("handler"));
        BigDecimal rowSum=items.stream().map(i->decimal(i.get("totalPrice"))).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        boolean direct=items.stream().allMatch(i->i.get("totalPrice")!=null) && equal(rowSum,decimal(header.get("totalPrice")))
                && zero(header,"discountPrice","feeAmount","otherPrice","extraFee","reductionAmount","freightAmount","totalFreight1","totalFreight2");
        Map<Long,PreparedTradeContext> result=new LinkedHashMap<>();
        for(Map<String,Object> item:items) {
            Long id=number(item.get("id"));
            String parent=type==70?"inId":type==60||type==80?"returnId":"outId";
            if(id==null || !Objects.equals(number(item.get(parent)),number(header.get("id"))) || result.containsKey(id))
                throw new IllegalStateException("交易来源行重复或不属于当前单据");
            BigDecimal count=decimal(item.get("count"));
            if(count==null || count.signum()<=0) throw new IllegalStateException("交易来源数量必须为正且已明确");
            Map<String,Object> d=new TreeMap<>();
            d.put("businessType",kind);d.put("bizId",number(header.get("id")));d.put("bizItemId",id);
            d.put("bizNo",header.get("no"));d.put("productId",number(item.get("productId")));d.put("warehouseId",number(item.get("warehouseId")));
            d.put("stockQuantity",(type==50||type==80?count.negate():count).toPlainString());
            d.put("accountingDeptId",number(header.get("deptId")));d.put("partyId",partyId);d.put("partyName",party==null?null:party.get("name"));
            d.put("saleUserId",header.get("saleUserId"));d.put("purchaser",header.get("purchaser"));d.put("sourceCreator",header.get("creator"));
            Map<String,Object> stock=stocks.get(number(item.get("productId"))+":"+number(item.get("warehouseId")));
            d.put("saleUserName",get(user,"nickname"));d.put("accountingDeptName",get(department,"name"));
            d.put("warehouseName",get(stock,"warehouse_name"));d.put("stockDeptName",get(stock,"stock_dept_name"));
            d.put("rawUnitPrice",plain(item.get("productPrice")));d.put("rawGrossAmount",plain(item.get("totalPrice")));
            d.put("grossAmount",direct?plain(item.get("totalPrice")):null);d.put("grossStatus",direct?"AVAILABLE":"HEADER_ALLOCATION_PENDING");
            d.put("priceBasis","INCLUSIVE_UNCONFIRMED");d.put("taxStatus","UNKNOWN");d.put("netAmount",null);
            d.put("headerOriginalAmount",plain(header.get("totalProductPrice")));d.put("headerGrossAmount",plain(header.get("totalPrice")));
            for(String key:Arrays.asList("discountPrice","feeAmount","otherPrice","extraFee","reductionAmount","freightAmount","totalFreight1","totalFreight2")) d.put(key,plain(header.get(key)));
            d.put("allocationVersion",direct?"NO_HEADER_ALLOCATION_V1":null);d.put("freightAllocatedAmount",null);d.put("otherFeeAllocatedAmount",null);
            d.put("remark",item.get("remark"));d.put("headerRemark",header.get("remark"));d.put("returnReason",item.get("returnReason"));
            d.put("originalBizId",type==60?header.get("sourceOutId"):header.get("orderId"));
            d.put("originalBizItemId",type==60?item.get("sourceOutItemId"):item.get("orderItemId"));
            d.put("originalBizNo",type==60?header.get("sourceOutNo"):header.get("orderNo"));
            d.put("sourceRole",type==60?"ORIGINAL_SALE":"SOURCE_ORDER");
            d.put("noOriginalSale",type==60?item.get("sourceOutItemId")==null:null);
            Map<String,Object> product=products.get(number(item.get("productId")));
            d.put("productCode",get(product,"code"));d.put("productName",get(product,"name"));d.put("oeNumber",get(product,"oe_number"));
            d.put("brand",item.get("brand")!=null?item.get("brand"):get(product,"brand"));d.put("categoryId",get(product,"category_id"));
            d.put("specification",item.get("standard")!=null?item.get("standard"):get(product,"standard"));
            d.put("unitName",get(product,"unit_name"));d.put("attributeSource","POSTING_SOURCE_SNAPSHOT");
            if(type==60&&Long.valueOf(20).equals(number(header.get("returnMode")))) {
                d.put("returnMode",20);d.put("sourceRole","NO_ORIGINAL_SALE");d.put("noOriginalSale",true);
                d.put("originalBizId",null);d.put("originalBizItemId",null);d.put("originalBizNo",null);d.put("originalPostingId",null);
                d.put("saleUserId",null);d.put("saleUserName",null);d.put("originalSaleQuantity",null);
                d.put("currentHandlerId",number(header.get("handler")));
                d.put("currentHandlerName",get(handler,"nickname"));
                d.put("attributeSource","NO_ORIGINAL_SALE_CURRENT_SOURCE");
            } else if(type==60) inheritOriginal(d,sources.get(id));
            if(type==80) purchaseReturnReferences(d,header,item,purchaseOrigins.get(id));
            d.put("sourceSignature",hash(JsonUtils.toJsonString(new TreeMap<>(header))+JsonUtils.toJsonString(item)));
            PreparedTradeContext context=new PreparedTradeContext(tenant(),type,d);registered().add(context);result.put(id,context);
        }
        return result;
    }

    private void purchaseReturnReferences(Map<String,Object> d,Map<String,Object> header,Map<String,Object> item,PurchaseReturnOrigin origin) {
        Long mode=number(header.get("returnMode"));
        if(!Long.valueOf(10).equals(mode)&&!Long.valueOf(20).equals(mode))throw new IllegalStateException("采购退货模式必须明确");
        boolean byOrder=Long.valueOf(10).equals(mode);
        Long sourceId=number(item.get("sourceInId")),sourceItemId=number(item.get("sourceInItemId"));
        if(byOrder&&(sourceId==null||sourceItemId==null))throw new IllegalStateException("按单采购退货缺少已核实的原入库主明细");
        if(byOrder&&(origin==null||!Objects.equals(origin.inId,sourceId)||!Objects.equals(origin.inItemId,sourceItemId)
                ||!Objects.equals(origin.productId,number(item.get("productId")))||!Objects.equals(origin.supplierId,number(header.get("supplierId")))
                ||!Objects.equals(origin.inNo,item.get("sourceInNo"))))throw new IllegalStateException("采购退货上游引用与真实入库来源不一致");
        if(!byOrder&&(sourceId!=null||sourceItemId!=null||item.get("sourceInNo")!=null))throw new IllegalStateException("按库存退货不能伪造原采购入库来源");
        d.put("returnMode",mode);d.put("sourceRole",byOrder?"SOURCE_PURCHASE_IN":"NO_ORIGINAL_PURCHASE");
        d.put("originalBizId",sourceId);d.put("originalBizItemId",sourceItemId);d.put("originalBizNo",item.get("sourceInNo"));
        d.put("sourcePurchaseInId",sourceId);d.put("sourcePurchaseInItemId",sourceItemId);d.put("sourcePurchaseInNo",item.get("sourceInNo"));
        d.put("sourceOrderId",origin==null?null:origin.orderId);d.put("sourceOrderItemId",origin==null?null:origin.orderItemId);d.put("sourceOrderNo",origin==null?null:origin.orderNo);
        d.put("returnDocumentOrderId",header.get("orderId"));d.put("returnDocumentOrderItemId",item.get("orderItemId"));d.put("returnDocumentOrderNo",header.get("orderNo"));
        d.put("traceSaleReturnId",item.get("sourceSaleReturnId"));d.put("traceSaleReturnItemId",item.get("sourceSaleReturnItemId"));d.put("traceSaleReturnNo",item.get("sourceSaleReturnNo"));
        d.put("costBasis","CURRENT_AVERAGE");d.put("returnDifferenceProfit",null);d.put("returnDifferenceStatus","TAX_BASIS_UNCONFIRMED");
        // 核销累计值只保留单头当时证据，不能作为每个商品的实际退款重复汇总。
        d.put("headerSettledRefundAmountAtPosting",plain(header.get("refundPrice")));
    }

    private void inheritOriginal(Map<String,Object> d,Long postingId) {
        Map<String,Object> source=postingId==null?null:one("SELECT biz_type,biz_id,biz_item_id,product_id,accounting_dept_id FROM erp_stock_dual_cost_posting WHERE tenant_id=? AND id=?",tenant(),postingId);
        if(source==null || ((Number)source.get("biz_type")).intValue()!=50
                || !Objects.equals(number(source.get("product_id")),number(d.get("productId")))
                || !Objects.equals(number(source.get("biz_id")),number(d.get("originalBizId")))
                || !Objects.equals(number(source.get("biz_item_id")),number(d.get("originalBizItemId"))))
            throw new IllegalStateException("退货原销售过账来源主明细不一致");
        d.put("accountingDeptId",number(source.get("accounting_dept_id")));
        Map<String,Object> original=postingId==null?null:one("SELECT snapshot_json FROM erp_business_report_item_snapshot WHERE tenant_id=? AND posting_id=?",tenant(),postingId);
        Map<String,Object> data=original==null?Collections.emptyMap():beanJson((String)original.get("snapshot_json"));
        for(String key:Arrays.asList("saleUserId","saleUserName","accountingDeptName","productCode","productName","oeNumber","brand","categoryId","specification","unitName")) d.put(key,data.get(key));
        d.put("attributeSource",original==null?"ORIGINAL_SNAPSHOT_MISSING":"ORIGINAL_SALE_SNAPSHOT");
        d.put("originalPostingId",postingId);
    }
    private Map<Long,Map<String,Object>> products(Set<Long> ids) {
        List<Object> args=new ArrayList<>();args.add(tenant());args.addAll(ids);
        List<Map<String,Object>> rows=jdbcTemplate.queryForList("SELECT p.id,p.code,p.name,p.oe_number,p.brand,p.category_id,p.standard,u.name AS unit_name "
                +"FROM erp_product p LEFT JOIN erp_product_unit u ON u.id=p.unit_id AND u.tenant_id=p.tenant_id WHERE p.tenant_id=? AND p.id IN ("
                +String.join(",",Collections.nCopies(ids.size(),"?"))+")",args.toArray());
        Map<Long,Map<String,Object>> map=new HashMap<>();for(Map<String,Object> row:rows)map.put(number(row.get("id")),row);return map;
    }
    private Map<String,Map<String,Object>> stockNames(Set<Long> products) {
        List<Object> args=new ArrayList<>();args.add(tenant());args.addAll(products);
        List<Map<String,Object>> rows=jdbcTemplate.queryForList("SELECT st.product_id,st.warehouse_id,w.name AS warehouse_name,d.name AS stock_dept_name "
                +"FROM erp_stock st LEFT JOIN erp_warehouse w ON w.tenant_id=st.tenant_id AND w.id=st.warehouse_id "
                +"LEFT JOIN system_dept d ON d.tenant_id=st.tenant_id AND d.id=st.dept_id WHERE st.tenant_id=? AND st.deleted=0 AND st.product_id IN ("
                +String.join(",",Collections.nCopies(products.size(),"?"))+")",args.toArray());
        Map<String,Map<String,Object>> result=new HashMap<>();for(Map<String,Object> row:rows)result.put(number(row.get("product_id"))+":"+number(row.get("warehouse_id")),row);return result;
    }
    private static void verifySource(Object header,List<?> items,Object stored,List<?> locked) {
        if(stored==null || !Integer.valueOf(20).equals(number(bean(stored).get("status")).intValue())) throw new IllegalStateException("交易快照必须在审核认领后生成");
        if(!JsonUtils.toJsonString(bean(header)).equals(JsonUtils.toJsonString(bean(stored)))
                || !JsonUtils.toJsonString(beans(items)).equals(JsonUtils.toJsonString(beans(locked)))) throw new IllegalStateException("交易来源与锁定完整主明细不一致");
    }
    @SuppressWarnings("unchecked") private Set<PreparedTradeContext> registered() {
        requireTransaction();
        Set<PreparedTradeContext> set=(Set<PreparedTradeContext>)TransactionSynchronizationManager.getResource(CONTEXT_KEY);
        if(set==null) {
            set=Collections.newSetFromMap(new IdentityHashMap<PreparedTradeContext,Boolean>());
            TransactionSynchronizationManager.bindResource(CONTEXT_KEY,set);
            final Set<PreparedTradeContext> transactionContexts=set;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void suspend(){TransactionSynchronizationManager.unbindResourceIfPossible(CONTEXT_KEY);}
                @Override public void resume(){TransactionSynchronizationManager.bindResource(CONTEXT_KEY,transactionContexts);}
                @Override public void afterCompletion(int status){TransactionSynchronizationManager.unbindResourceIfPossible(CONTEXT_KEY);}
            });
        }
        return set;
    }
    private static void requireTransaction(){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("交易快照必须处于业务审批事务");}
    private static long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private Map<String,Object> one(String sql,Object...args){List<Map<String,Object>> rows=jdbcTemplate.queryForList(sql,args);return rows.isEmpty()?null:rows.get(0);}
    private static Object get(Map<String,Object> map,String key){return map==null?null:map.get(key);}
    private static Map<String,Object> bean(Object value){return new TreeMap<>(beanJson(JsonUtils.toJsonString(value)));}
    @SuppressWarnings("unchecked") private static Map<String,Object> beanJson(String json){try{return EXACT_JSON.readValue(json,Map.class);}catch(java.io.IOException e){throw new IllegalStateException("交易快照JSON无效",e);}}
    private static List<Map<String,Object>> beans(List<?> items){return items.stream().map(ErpTradeSnapshotService::bean).sorted(Comparator.comparing(i->number(i.get("id")))).collect(Collectors.toList());}
    private static Long number(Object value){return value==null?null:value instanceof Number?((Number)value).longValue():Long.valueOf(value.toString());}
    private static BigDecimal decimal(Object value){return value==null?null:new BigDecimal(value.toString());}
    private static String plain(Object value){BigDecimal d=decimal(value);return d==null?null:d.toPlainString();}
    private static boolean equal(BigDecimal a,BigDecimal b){return a!=null&&b!=null&&a.compareTo(b)==0;}
    private static boolean zero(Map<String,Object> data,String...keys){for(String key:keys){BigDecimal v=decimal(data.get(key));if(v!=null&&v.signum()!=0)return false;}return true;}
    private static LocalDateTime asDate(Object value){return value instanceof Timestamp?((Timestamp)value).toLocalDateTime():(LocalDateTime)value;}
    private static String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
