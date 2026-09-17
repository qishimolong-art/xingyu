package cn.iocoder.yudao.module.erp.service.report.trade;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.trade.ErpTradeReportModels.*;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Service
public class ErpTradeReportService {
    @Resource private ErpTradeReportRepository repository;
    @Resource private PermissionApi permissionApi;
    @Value("${erp.reporting.dual-cost-enabled:false}") private boolean enabled;
    @Value("${erp.reporting.dual-cost-cutover:}") private String cutover;
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON=new com.fasterxml.jackson.databind.ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    public Map<String,Object> status(boolean sale) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","DISABLED");result.put("reason","新核算尚未启用，请先完成核对和业务接入");
        result.put("availableBusinessTypes",sale?Arrays.asList("SALE_OUT","SALE_RETURN"):Arrays.asList("PURCHASE_IN","PURCHASE_RETURN"));
        result.put("coverageWarnings",Arrays.asList("仅包含真实过账；未过账历史请查历史口径","税率未确认；未税收入、毛利率暂不可计算","费用分配及商品收付款归集待接入","新销售审核直接扣库存；旧出仓单仅供历史查询，待领记录请核对","部分反向业务尚未完整接入；新核算自动调拨仍需后续接入","销售调价尚未接入新核算；新核算启用时暂不允许审核，草稿及历史查询保留","采购退货按退货时两套当前均价扣库；退款与成本税基未确认，差额损益不可计算","无单销售退货按当时两套当前均价或人工确认成本入库；无原销售记录，历史缺成本不自动补齐"));
        if(!enabled)return result;
        LocalDateTime start;
        try{start=LocalDateTime.parse(cutover);if(start.isAfter(LocalDateTime.now()))throw new IllegalArgumentException();}
        catch(RuntimeException e){result.put("status","INVALID_CONFIG");result.put("reason","切换时间配置无效或尚未到达");return result;}
        result.put("cutoverAt",start.toString());
        try{repository.checkSchema();}catch(BadSqlGrammarException e){result.put("status","SCHEMA_MISSING");result.put("reason","交易快照或过账表未迁移");return result;}
        result.put("status","READY");result.put("reason","可查询真实已过账数据；指标完整性以各列状态为准");return result;
    }

    /** 本页、总条数和全部合计在同一次REPEATABLE_READ只读事务中计算；不承诺跨请求快照。 */
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Bundle page(boolean sale,Filter filter,boolean grouped) {
        Context c=prepare(sale,filter);String group=grouped?filter.getGroupBy():null;
        long total=repository.count(c.query,group);
        List<Map<String,Object>> rows=repository.rows(c.query,filter,group,(long)(filter.getPageNo()-1)*filter.getPageSize(),filter.getPageSize());
        List<Map<String,Object>> result=rows.stream().map(r->present(r,c,grouped)).collect(Collectors.toList());
        Bundle bundle=new Bundle();bundle.setPage(new PageResult<>(result,total));
        bundle.setSummary(present(repository.summary(c.query),c,true));bundle.setQueryContext(queryContext(filter));return bundle;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> summary(boolean sale,Filter filter){Context c=prepare(sale,filter);return present(repository.summary(c.query),c,true);}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public PageResult<Map<String,Object>> options(boolean sale,Filter filter,String dimension,String keyword) {
        Context c=prepare(sale,filter);validateGroup(sale,dimension);
        if(keyword!=null&&keyword.length()>100)throw error(400,"搜索词过长");
        List<Map<String,Object>> values=repository.options(c.query,dimension,keyword,filter.getPageSize(),(filter.getPageNo()-1)*filter.getPageSize());
        List<Map<String,Object>> result=new ArrayList<>();for(Map<String,Object> v:values){Map<String,Object> item=new LinkedHashMap<>();item.put("value",v.get("value"));item.put("label",v.get("value")==null?"未记录":v.get("label")==null?String.valueOf(v.get("value")):v.get("label").toString()+" ["+v.get("value")+"]");result.add(item);}
        return new PageResult<>(result,repository.optionCount(c.query,dimension,keyword));
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class)
    public void export(boolean sale,Filter filter,String view,HttpServletResponse response) throws IOException {
        if(!"DETAIL".equals(view)&&!"GROUP".equals(view))throw error(400,"导出模式不支持");
        Context c=prepare(sale,filter);String group="GROUP".equals(view)?filter.getGroupBy():null;
        long count=repository.count(c.query,group);if(count>1_000_000)throw error(400,"单次导出最多100万行，请缩小日期范围");
        Map<String,Object> total=present(repository.summary(c.query),c,true);
        LinkedHashMap<String,String> columns=columns();if(group!=null){columns.clear();columns.put("groupKey","分组值");columns.put("groupLabel","历史名称标签");summaryColumns(columns);}
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition","attachment;filename*=UTF-8''"+URLEncoder.encode(sale?"销售商品明细-新核算.xlsx":"采购商品明细-新核算.xlsx","UTF-8").replace("+","%20"));
        try(ExcelWriter writer=FastExcelFactory.write(response.getOutputStream()).autoCloseStream(false).build()) {
            WriteSheet sheet=FastExcelFactory.writerSheet(0,group==null?"商品明细":"分组汇总").head(columns.values().stream().map(Collections::singletonList).collect(Collectors.toList())).build();
            writer.write(Collections.emptyList(),sheet);
            for(long offset=0;offset<count;offset+=1000){List<List<String>> data=new ArrayList<>();for(Map<String,Object> raw:repository.rows(c.query,filter,group,offset,1000)){Map<String,Object> row=present(raw,c,group!=null);List<String> cells=new ArrayList<>();for(String key:columns.keySet())cells.add(display(row,key));data.add(cells);}writer.write(data,sheet);}
            LinkedHashMap<String,String> totals=new LinkedHashMap<>();summaryColumns(totals);
            List<List<String>> notes=new ArrayList<>();notes.add(Arrays.asList("取数区间",filter.getPostedFrom()+" ≤ postedAt < "+filter.getPostedTo()));
            notes.add(Arrays.asList("一致性","本文件采用独立只读事务快照，可能与之前页面查询有新业务差异"));
            notes.add(Arrays.asList("税价口径","销售采购原价暂含税，税率待确认；不可用不等于0"));
            for(Map.Entry<String,String> e:totals.entrySet())notes.add(Arrays.asList(e.getValue(),display(total,e.getKey())));
            writer.write(notes,FastExcelFactory.writerSheet(1,"全部合计与说明").head(Arrays.asList(Collections.singletonList("项目"),Collections.singletonList("值及状态"))).build());
        }
    }

    private Context prepare(boolean sale,Filter f) {
        validate(sale,f);Map<String,Object> status=status(sale);if(!"READY".equals(status.get("status")))throw error(409,(String)status.get("reason"));
        if(f.getPostedFrom().isBefore(LocalDateTime.parse(cutover)))throw error(409,"开始时间早于切换时间，请显式修改查询区间；历史业务使用历史口径");
        Long user=getLoginUserId();if(user==null)throw error(401,"请先登录");
        ErpFinanceVisibleScope doc=ErpFinanceVisibleScope.from(permissionApi.getDeptDataPermission(user,sale?"erp_sale_report":"erp_purchase_report"),user);
        ErpFinanceVisibleScope party=ErpFinanceVisibleScope.from(permissionApi.getDeptDataPermission(user,sale?"erp_customer":"erp_supplier"),user);
        Context c=new Context(sale,repository.prepare(sale,TenantContextHolder.getRequiredTenantId(),f,doc,party));
        Set<String> fields=new HashSet<>();add(fields,permissionApi.getCurrentUserHiddenFields("erp_product"));
        List<Long> departments=repository.departments(c.query);
        for(Long dept:departments)if(dept!=null)add(fields,permissionApi.getCurrentUserHiddenFields("erp_product",dept));
        c.priceMasked=hidden(fields,sale?new String[]{"salePrice","saleAmount","grossAmount","netAmount"}:new String[]{"purchasePrice","lastPurchasePrice","purchaseAmount","grossAmount","netAmount"});
        c.marginMasked=hidden(fields,"grossProfitRate","financialMargin","settlementMargin");
        Set<String> sourceFields=new HashSet<>();
        List<String> types=f.getBusinessTypes()==null||f.getBusinessTypes().isEmpty()?(sale?Arrays.asList("SALE_OUT","SALE_RETURN"):Arrays.asList("PURCHASE_IN","PURCHASE_RETURN")):f.getBusinessTypes();
        for(String type:types){String module="erp_"+type.toLowerCase(Locale.ROOT);add(sourceFields,permissionApi.getCurrentUserHiddenFields(module));for(Long dept:departments)if(dept!=null)add(sourceFields,permissionApi.getCurrentUserHiddenFields(module,dept));}
        c.priceMasked=c.priceMasked||hidden(sourceFields,"totalPrice","item_totalPrice","productPrice","item_productPrice","originalProductPrice","item_originalProductPrice","actualSaleAmount","afterReductionAmount","grossAmount","netAmount");
        c.marginMasked=c.marginMasked||hidden(sourceFields,"grossProfitRate","financialMargin","settlementMargin");
        // 成本同时受商品、来源单据及实际库存归属的字段权限限制；独立集合避免把库存成本权限扩成交易价格权限。
        Set<String> costFields=new HashSet<>();addCostFields(costFields,fields);addCostFields(costFields,sourceFields);
        addCostFields(costFields,permissionApi.getCurrentUserHiddenFields("erp_stock"));
        for(Long dept:departments)if(dept!=null)addCostFields(costFields,permissionApi.getCurrentUserHiddenFields("erp_stock",dept));
        boolean legacy=hidden(costFields,"purchasePrice","lastPurchasePrice","costPrice","costAmount");
        c.financialMasked=legacy||hidden(costFields,"financialUnitCost","financialAmount","financialMovement","financialBalance","financialCostPrice","financialCost");
        c.settlementMasked=legacy||hidden(costFields,"settlementUnitCost","settlementAmount","settlementMovement","settlementBalance","settlementCostPrice","settlementCost");return c;
    }
    private static void validate(boolean sale,Filter f){
        if(f==null||f.getPostedFrom()==null||f.getPostedTo()==null||!f.getPostedFrom().isBefore(f.getPostedTo()))throw error(400,"请填写有效左闭右开过账时间范围");
        if(f.getPageNo()<1||f.getPageSize()<1||f.getPageSize()>200)throw error(400,"分页大小应为1到200");
        if(!Arrays.asList("postedAt","postingId").contains(f.getOrderField())||!Arrays.asList("asc","desc").contains(f.getOrderDirection()))throw error(400,"排序字段或方向不支持");
        validateGroup(sale,f.getGroupBy());
        if(sale&&(f.getSupplierId()!=null||f.getPurchaser()!=null)||!sale&&(f.getCustomerId()!=null||f.getSaleUserId()!=null||f.getNoOriginalSale()!=null))throw error(400,"筛选参数不适用于当前报表");
        if(f.getBusinessTypes()!=null)for(String t:f.getBusinessTypes())if(!ErpTradeReportRepository.TYPES.containsKey(t)||sale!=t.startsWith("SALE_"))throw error(400,"业务类型不支持");
        for(List<Long> ids:Arrays.asList(f.getAccountingDeptIds(),f.getStockDeptIds(),f.getWarehouseIds()))if(ids!=null&&(ids.size()>200||ids.stream().anyMatch(id->id==null||id<=0)))throw error(400,"部门/仓库选择不合法");
    }
    private static void validateGroup(boolean sale,String group){if(!ErpTradeReportRepository.GROUPS.containsKey(group)||sale&&Arrays.asList("SUPPLIER","PURCHASER").contains(group)||!sale&&Arrays.asList("CUSTOMER","SALE_USER").contains(group))throw error(400,"分组维度不支持");}

    private Map<String,Object> present(Map<String,Object> raw,Context c,boolean summary) {
        Map<String,Object> row=new LinkedHashMap<>();for(Map.Entry<String,Object> e:raw.entrySet())if(!"snapshot_json".equalsIgnoreCase(e.getKey()))row.put(camel(e.getKey().toLowerCase(Locale.ROOT)),jsonValue(e.getValue()));
        for(String key:Arrays.asList("rowCount","documentCount","missingSnapshotCount","missingGrossCount"))if(row.get(key)!=null)row.put(key,new BigDecimal(row.get(key).toString()).longValueExact());
        Map<String,String> states=new LinkedHashMap<>();
        boolean missing=!summary&&raw.get("snapshot_id")==null;
        if(!summary){
            int type=((Number)raw.get("biz_type")).intValue();boolean positive=type==50||type==70;int direction=positive?1:-1;
            row.put("businessType",type==50?"SALE_OUT":type==60?"SALE_RETURN":type==70?"PURCHASE_IN":"PURCHASE_RETURN");
            BigDecimal qty=decimal(raw.get("quantity")).abs();row.put("positiveQuantity",positive?qty.toPlainString():"0");row.put("returnQuantity",positive?"0":qty.toPlainString());row.put("netQuantity",qty.multiply(BigDecimal.valueOf(direction)).toPlainString());
            for(String pair:Arrays.asList("financial","settlement")){BigDecimal cost=decimal(raw.get(pair+"_movement")).multiply(BigDecimal.valueOf(c.sale?-1:1));row.put(pair+"Cost",cost.toPlainString());row.put("positive"+upper(pair)+"Cost",positive?cost.toPlainString():"0");row.put("return"+upper(pair)+"Cost",positive?"0":cost.negate().toPlainString());row.put(pair+"UnitCost",qty.signum()==0?null:cost.abs().divide(qty,6,RoundingMode.HALF_UP).toPlainString());}
            for(String amount:Arrays.asList("rawGrossAmount","grossAmount","netAmount")){BigDecimal value=decimal(row.get(amount));row.put(amount,value==null?null:value.multiply(BigDecimal.valueOf(direction)).toPlainString());}
            row.put("positiveGrossAmount",positive?row.get("grossAmount"):"0");
            row.put("returnGrossAmount",positive?"0":row.get("grossAmount")==null?null:decimal(row.get("grossAmount")).negate().toPlainString());
            Map<String,Object> detail=parseSnapshot(raw.get("snapshot_json"));
            for(String key:Arrays.asList("rawUnitPrice","remark","returnReason","sourceRole","attributeSource","allocationVersion","originalPostingId",
                    "returnMode","sourcePurchaseInId","sourcePurchaseInItemId","sourcePurchaseInNo","sourceOrderId","sourceOrderItemId","sourceOrderNo",
                    "traceSaleReturnId","traceSaleReturnItemId","traceSaleReturnNo","costBasis",
                    "returnDocumentOrderId","returnDocumentOrderItemId","returnDocumentOrderNo",
                    "currentHandlerId","currentHandlerName","sourceCreator","originalSaleQuantity"))row.put(key,jsonValue(detail.get(key)));
            if("NO_ORIGINAL_SALE".equals(detail.get("sourceRole"))) {
                states.put("saleUserId","NO_ORIGINAL_SALE");states.put("saleUserName","NO_ORIGINAL_SALE");states.put("originalSaleQuantity","NO_ORIGINAL_SALE");
                row.put("costBasis",raw.get("current_cost_source"));
                if(row.get("costBasis")==null)states.put("costBasis","COST_BASIS_NOT_RECORDED");
            }
            row.remove("currentCostSource");
            row.put("dataStatus",missing?"SOURCE_SNAPSHOT_MISSING":"POSTED");
        }
        for(String key:Arrays.asList("rawGrossAmount","grossAmount","positiveGrossAmount","returnGrossAmount","rawUnitPrice")) {
            if(c.priceMasked){row.put(key,null);states.put(key,"MASKED");}
            else if(row.get(key)==null)states.put(key,missing?"SOURCE_SNAPSHOT_MISSING":"HEADER_ALLOCATION_PENDING");
        }
        for(String prefix:Arrays.asList("financial","settlement")) {
            boolean mask="financial".equals(prefix)?c.financialMasked:c.settlementMasked;
            if(mask)for(String key:Arrays.asList(prefix+"Cost",prefix+"UnitCost",prefix+"Movement","positive"+upper(prefix)+"Cost","return"+upper(prefix)+"Cost")){row.put(key,null);states.put(key,"MASKED");}
        }
        unavailable(row,states,"netAmount",c.priceMasked?"MASKED":"TAX_BASIS_UNCONFIRMED");
        for(String key:Arrays.asList("financialGrossProfit","financialMargin"))unavailable(row,states,key,c.priceMasked||c.financialMasked?"MASKED":"TAX_BASIS_UNCONFIRMED");
        for(String key:Arrays.asList("settlementGrossProfit","settlementMargin"))unavailable(row,states,key,c.priceMasked||c.settlementMasked?"MASKED":"TAX_BASIS_UNCONFIRMED");
        if(c.marginMasked){unavailable(row,states,"financialMargin","MASKED");unavailable(row,states,"settlementMargin","MASKED");}
        for(String key:Arrays.asList("receivedAmount","paidAmount","refundedAmount","receivableBalance","payableBalance"))unavailable(row,states,key,"SETTLEMENT_ALLOCATION_PENDING");
        for(String key:Arrays.asList("freightAllocatedAmount","otherFeeAllocatedAmount"))unavailable(row,states,key,c.priceMasked?"MASKED":"FEE_ALLOCATION_PENDING");
        boolean purchaseReturn=!c.sale&&(summary?decimal(raw.get("return_quantity"))!=null&&decimal(raw.get("return_quantity")).signum()>0:((Number)raw.get("biz_type")).intValue()==80);
        unavailable(row,states,"returnDifferenceProfit",!purchaseReturn?"NOT_APPLICABLE":c.priceMasked||c.financialMasked?"MASKED":"TAX_BASIS_UNCONFIRMED");
        row.put("returnDifferenceStatus",states.get("returnDifferenceProfit"));
        row.put("financialMasked",c.financialMasked);row.put("settlementMasked",c.settlementMasked);row.put("priceMasked",c.priceMasked);
        row.put("priceBasis","INCLUSIVE_UNCONFIRMED");row.put("taxStatus","UNKNOWN");row.put("metricStates",states);return row;
    }
    private static Map<String,Object> queryContext(Filter f){Map<String,Object> m=new LinkedHashMap<>();m.put("postedFrom",f.getPostedFrom().toString());m.put("postedTo",f.getPostedTo().toString());m.put("consistency","本页与全部合计来自同一次查询；翻页、重新查询或导出可能包含新提交业务");return m;}
    private static void unavailable(Map<String,Object> row,Map<String,String> states,String key,String reason){row.put(key,null);states.put(key,reason);}
    @SuppressWarnings("unchecked") private static Map<String,Object> parseSnapshot(Object value){if(value==null)return Collections.emptyMap();try{return JSON.readValue(value.toString(),Map.class);}catch(IOException e){throw error(409,"交易快照数据损坏，请核对");}}
    private static Object jsonValue(Object v){if(v instanceof BigDecimal)return ((BigDecimal)v).toPlainString();if(v instanceof Timestamp)return ((Timestamp)v).toLocalDateTime().toString();if(v instanceof LocalDateTime)return v.toString();return v;}
    private static String camel(String key){StringBuilder s=new StringBuilder();boolean up=false;for(char c:key.toCharArray()){if(c=='_'){up=true;continue;}s.append(up?Character.toUpperCase(c):c);up=false;}return s.toString();}
    private static String upper(String s){return Character.toUpperCase(s.charAt(0))+s.substring(1);}
    private static BigDecimal decimal(Object v){return v==null?null:new BigDecimal(v.toString());}
    private static void add(Set<String> set,List<String> fields){if(fields!=null)for(String field:fields)if(field!=null)set.add(field.startsWith("col_")?field.substring(4):field);}
    private static void addCostFields(Set<String> set,Collection<String> fields){
        if(fields==null)return;
        for(String field:fields)if(field!=null){
            String name=field;boolean stripped;
            do{stripped=false;for(String prefix:Arrays.asList("select_","item_","col_"))if(name.startsWith(prefix)){name=name.substring(prefix.length());stripped=true;break;}}while(stripped);
            set.add(name);
        }
    }
    private static boolean hidden(Set<String> set,String...names){for(String n:names)if(set.contains(n))return true;return false;}
    private static ServiceException error(int code,String message){return new ServiceException(code,message);}
    private static final class Context {final boolean sale;final ErpTradeReportRepository.Query query;boolean priceMasked,financialMasked,settlementMasked,marginMasked;Context(boolean sale,ErpTradeReportRepository.Query query){this.sale=sale;this.query=query;}}
    private static String display(Map<String,Object> row,String key){return ErpTradeReportValueFormatter.display(row,key);}
    public static LinkedHashMap<String,String> columns(){LinkedHashMap<String,String> c=new LinkedHashMap<>();String[][] a={{"postedAt","过账时间"},{"bizDate","业务日期"},{"businessType","业务类型"},{"bizNo","单据编号"},{"originalBizNo","原单编号"},{"sourceRole","来源关系"},{"returnMode","退货模式"},{"sourcePurchaseInNo","来源采购入库单"},{"sourcePurchaseInId","来源采购入库ID"},{"sourcePurchaseInItemId","来源采购入库明细ID"},{"sourceOrderNo","原入库上游采购订单"},{"sourceOrderId","原入库上游采购订单ID"},{"sourceOrderItemId","原入库上游采购订单明细ID"},{"returnDocumentOrderNo","退货单自身关联订单"},{"returnDocumentOrderId","退货单自身关联订单ID"},{"returnDocumentOrderItemId","退货单自身关联订单明细ID"},{"traceSaleReturnNo","销售退货追溯单"},{"traceSaleReturnId","销售退货追溯ID"},{"traceSaleReturnItemId","销售退货追溯明细ID"},{"costBasis","库存成本依据"},{"returnDifferenceStatus","退货差额状态"},{"noOriginalSale","是否无原单退货"},{"partyName","客户/供应商"},{"accountingDeptName","业务核算部门"},{"accountingDeptId","业务核算部门ID"},{"stockDeptName","库存归属部门"},{"stockDeptId","库存部门ID"},{"warehouseName","实际出入库仓库"},{"warehouseId","实际仓库ID"},{"saleUserName","原销售人员"},{"currentHandlerName","无单退货经办人"},{"currentHandlerId","无单退货经办人ID"},{"sourceCreator","来源制单人ID"},{"saleUserId","原销售人员ID"},{"purchaser","采购员"},{"productCode","商品编码"},{"oeNumber","OE号"},{"productName","商品名称"},{"brand","品牌"},{"specification","规格"},{"unitName","单位"},{"positiveQuantity","正向销售/采购数量"},{"returnQuantity","退货数量"},{"netQuantity","净数量"},{"rawUnitPrice","原含税单价"},{"rawGrossAmount","原含税行额（带方向）"},{"positiveGrossAmount","正向含税交易额"},{"returnGrossAmount","退货含税交易额"},{"grossAmount","含税净交易额"},{"netAmount","未税净交易额"},{"financialUnitCost","财务成本单价"},{"settlementUnitCost","部门结算成本单价"},{"positiveFinancialCost","正向财务成本"},{"returnFinancialCost","退货财务成本"},{"financialCost","财务净成本"},{"positiveSettlementCost","正向结算成本"},{"returnSettlementCost","退货结算成本"},{"settlementCost","部门结算净成本"},{"financialGrossProfit","财务毛利"},{"financialMargin","财务毛利率"},{"settlementGrossProfit","部门毛利"},{"settlementMargin","部门毛利率"},{"freightAllocatedAmount","分摊运费"},{"otherFeeAllocatedAmount","分摊其他费"},{"receivedAmount","已收"},{"paidAmount","已付"},{"refundedAmount","退款"},{"receivableBalance","应收余额"},{"payableBalance","应付余额"},{"returnDifferenceProfit","退货差额损益"},{"returnReason","退货原因"},{"remark","备注"},{"dataStatus","数据状态"}};for(String[] entry:a)c.put(entry[0],entry[1]);return c;}
    private static void summaryColumns(LinkedHashMap<String,String> c){for(Map.Entry<String,String> e:columns().entrySet())if(Arrays.asList("positiveQuantity","returnQuantity","netQuantity","rawGrossAmount","positiveGrossAmount","returnGrossAmount","grossAmount","netAmount","positiveFinancialCost","returnFinancialCost","financialCost","positiveSettlementCost","returnSettlementCost","settlementCost","financialGrossProfit","financialMargin","settlementGrossProfit","settlementMargin","returnDifferenceProfit","receivedAmount","paidAmount","refundedAmount","receivableBalance","payableBalance").contains(e.getKey()))c.put(e.getKey(),e.getValue());c.put("rowCount","事件行数");c.put("missingSnapshotCount","缺交易快照行数");c.put("missingGrossCount","缺净交易金额行数");}
}
