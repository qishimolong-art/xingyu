package cn.iocoder.yudao.module.erp.service.assistant;

import org.springframework.stereotype.Component;
import java.util.*;

/** Model-visible tools. Execution is resolved by name in AssistantOrchestrator, never by reflection. */
@Component
public class AssistantToolRegistry {
    private static final List<String> NAMES=Arrays.asList(
            "query_verified_metric","query_master_data_stats","resolve_business_entity","query_product_stock","query_inventory_details","query_stock_movements",
            "query_purchase_documents","query_purchase_fulfillment","query_sale_documents","query_sale_fulfillment",
            "query_cash_documents","query_account_balance","query_semantic_schema","execute_semantic_query");

    public boolean contains(String name){return NAMES.contains(name);}
    public List<String> names(){return Collections.unmodifiableList(NAMES);}

    public ExecutionContract executionContract(String name,AssistantPlan.Metric metric) {
        switch(name) {
            case "query_master_data_stats":
                return contract(false,false,false,false);
            case "query_product_stock": case "query_inventory_details": case "query_stock_movements":
                return contract(true,true,true,true,"PRODUCT","WAREHOUSE","DEPARTMENT");
            case "query_purchase_documents": case "query_purchase_fulfillment":
                return contract(true,true,true,true,"SUPPLIER","DEPARTMENT");
            case "query_sale_documents": case "query_sale_fulfillment":
                return contract(true,true,true,true,"CUSTOMER","PRODUCT","WAREHOUSE","DEPARTMENT","SALESPERSON");
            case "query_cash_documents": case "query_account_balance": case "query_verified_metric":
                if(AssistantPlan.Metric.SALE==metric)
                    return contract(true,true,true,true,"PRODUCT","CUSTOMER","DEPARTMENT");
                if(Arrays.asList(AssistantPlan.Metric.RECEIPT,AssistantPlan.Metric.RECEIVABLE).contains(metric))
                    return contract(true,true,true,true,"CUSTOMER","DEPARTMENT");
                if(Arrays.asList(AssistantPlan.Metric.PURCHASE,AssistantPlan.Metric.PAYMENT,AssistantPlan.Metric.PAYABLE).contains(metric))
                    return contract(true,true,true,true,"SUPPLIER","DEPARTMENT");
                if(Arrays.asList(AssistantPlan.Metric.STOCK,AssistantPlan.Metric.STOCK_SKU).contains(metric))
                    return contract(false,true,true,true,"PRODUCT","WAREHOUSE","DEPARTMENT");
                return contract(true,true,true,true);
            default: return contract(false,false,false,false);
        }
    }

    public ExecutionContract datasetContract(String dataset) {
        if(Arrays.asList("inventory_current","stock_movements").contains(dataset))
            return contract(false,true,true,true,"PRODUCT","WAREHOUSE","DEPARTMENT");
        if("purchase_orders".equals(dataset)) return contract(true,true,true,true,"SUPPLIER","DEPARTMENT");
        if("sale_orders".equals(dataset)) return contract(true,true,true,true,"CUSTOMER","DEPARTMENT","SALESPERSON");
        if("sale_order_items".equals(dataset)) return contract(true,true,true,true,"CUSTOMER","PRODUCT","WAREHOUSE","DEPARTMENT","SALESPERSON");
        return contract(false,false,false,false);
    }

    private static ExecutionContract contract(boolean time,boolean grouping,boolean trend,boolean ranking,String... types) {
        return new ExecutionContract(new LinkedHashSet<>(Arrays.asList(types)),time,grouping,trend,ranking);
    }

    public static final class ExecutionContract {
        private final Set<String> supportedEntityTypes;
        private final Set<Set<String>> supportedCombinations;
        private final boolean time,grouping,trend,ranking;

        private ExecutionContract(Set<String> supportedEntityTypes,boolean time,boolean grouping,boolean trend,boolean ranking) {
            this.supportedEntityTypes=Collections.unmodifiableSet(new LinkedHashSet<>(supportedEntityTypes));
            this.supportedCombinations=allCombinations(supportedEntityTypes);
            this.time=time;this.grouping=grouping;this.trend=trend;this.ranking=ranking;
        }

        public Set<String> getSupportedEntityTypes(){return supportedEntityTypes;}
        public Set<Set<String>> getSupportedCombinations(){return supportedCombinations;}
        public boolean isTime(){return time;}public boolean isGrouping(){return grouping;}
        public boolean isTrend(){return trend;}public boolean isRanking(){return ranking;}
        public boolean supportsEntities(Collection<String> types){return supportedCombinations.contains(new LinkedHashSet<>(types));}

        private static Set<Set<String>> allCombinations(Set<String> types) {
            List<String> values=new ArrayList<>(types);Set<Set<String>> result=new LinkedHashSet<>();
            int maximum=1<<values.size();for(int mask=0;mask<maximum;mask++) {
                Set<String> combination=new LinkedHashSet<>();for(int i=0;i<values.size();i++) if((mask&(1<<i))!=0) combination.add(values.get(i));
                result.add(Collections.unmodifiableSet(combination));
            }
            return Collections.unmodifiableSet(result);
        }
    }
    public List<Map<String,Object>> definitions(Collection<String> allowed) {
        Set<String> names=new HashSet<>(allowed);List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> tool:definitions()) {
            Map<?,?> function=(Map<?,?>)tool.get("function");
            if(function!=null && names.contains(function.get("name"))) result.add(tool);
        }
        return result;
    }
    public void validate(AssistantToolCall call) {
        if(call==null || !contains(call.getName())) throw new AssistantFailure("MODEL_INVALID","模型选择了未注册的查询工具");
        Map<String,Object> args=call.getArguments();
        if(args==null) throw new AssistantFailure("MODEL_INVALID","模型工具参数为空");
        Set<String> allowed=new HashSet<>();
        switch(call.getName()) {
            case "query_master_data_stats": allowed.addAll(Arrays.asList("question","archiveType","statusMode"));break;
            case "query_product_stock": allowed.addAll(Arrays.asList("question","product","warehouse","groupByWarehouse","stockMode"));break;
            case "query_inventory_details": allowed.add("question");break;
            case "resolve_business_entity": allowed.addAll(Arrays.asList("entityType","keyword"));break;
            case "query_verified_metric": allowed.addAll(Arrays.asList("metric","question"));break;
            case "query_semantic_schema": allowed.add("subject");break;
            case "execute_semantic_query": allowed.addAll(Arrays.asList("sql","parameters"));break;
            default: allowed.add("question");break;
        }
        if(!allowed.containsAll(args.keySet())) throw new AssistantFailure("MODEL_INVALID","模型工具包含未允许参数");
        for(Object value:args.values()) if(!(value==null || value instanceof String || value instanceof Number
                || value instanceof Boolean || value instanceof Map) || value instanceof String && ((String)value).length()>2000)
            throw new AssistantFailure("MODEL_INVALID","模型工具参数类型无效");
    }
    public List<Map<String,Object>> definitions() {
        List<Map<String,Object>> result=new ArrayList<>();
        result.add(tool("query_verified_metric","查询已核验的销售、采购入库、库存、SKU、收款、付款、应收或应付指标",props(
                "metric",enumProperty("SALE","PURCHASE","STOCK","STOCK_SKU","RECEIPT","PAYMENT","RECEIVABLE","PAYABLE"),"question",string()),"question"));
        result.add(tool("query_master_data_stats","查询当前账号可见的客户、供应商或配件档案数量及启停用状态统计",props(
                "question",string(),"archiveType",enumProperty("CUSTOMER","SUPPLIER","PRODUCT"),
                "statusMode",enumProperty("ENABLED","DISABLED","ALL","SPLIT")),"question","archiveType","statusMode"));
        result.add(tool("resolve_business_entity","解析产品、仓库、部门、客户、供应商或业务员候选对象",props("entityType",enumProperty("product","warehouse","department","customer","supplier","salesperson"),"keyword",string()),"entityType","keyword"));
        result.add(tool("query_product_stock","查询指定产品价格、股份价、零售价、当前库存、有无货或逐仓库存数量；不用于销售额、采购额或客户欠款统计",props("product",string(),"warehouse",string(),"groupByWarehouse",bool(),"stockMode",enumProperty("ALL","POSITIVE","NEGATIVE","AVAILABLE")),"product"));
        result.add(tool("query_inventory_details","查询当前库存明细、负库存明细、库存产品或按仓库/产品汇总的账面库存数量；不回答历史库存或库存金额",questionProps(),"question"));
        result.add(tool("query_stock_movements","查询库存流水、出入库、调拨或盘点变化",questionProps(),"question"));
        result.add(tool("query_purchase_documents","查询采购订单数量、金额、状态或分布；不是采购入库指标",questionProps(),"question"));
        result.add(tool("query_purchase_fulfillment","查询采购订单到货、未到货、退货和剩余数量",questionProps(),"question"));
        result.add(tool("query_sale_documents","查询销售订单数量、金额、状态或分布；不是销售出库指标",questionProps(),"question"));
        result.add(tool("query_sale_fulfillment","查询销售订单发货、未发货、退货和剩余数量",questionProps(),"question"));
        result.add(tool("query_cash_documents","查询收款或付款单；明确发生额时仍优先已核验指标",questionProps(),"question"));
        result.add(tool("query_account_balance","查询当前应收或应付余额",questionProps(),"question"));
        result.add(tool("query_semantic_schema","固定指标和专用工具均无法表达时，查询可用逻辑数据集",props("subject",string()),"subject"));
        result.add(tool("execute_semantic_query","执行已经获取schema的受控语义SQL",props("sql",string(),"parameters",object()),"sql","parameters"));
        return result;
    }
    private static Map<String,Object> questionProps(){return props("question",string());}
    private static Map<String,Object> tool(String name,String description,Map<String,Object> properties,String... required) {
        Map<String,Object> function=new LinkedHashMap<>();function.put("name",name);function.put("description",description);
        Map<String,Object> parameters=new LinkedHashMap<>();parameters.put("type","object");parameters.put("properties",properties);parameters.put("required",Arrays.asList(required));parameters.put("additionalProperties",false);
        function.put("parameters",parameters);Map<String,Object> tool=new LinkedHashMap<>();tool.put("type","function");tool.put("function",function);return tool;
    }
    private static Map<String,Object> props(Object... values){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2) result.put(String.valueOf(values[i]),values[i+1]);return result;}
    private static Map<String,Object> string(){return props("type","string","maxLength",2000);}
    private static Map<String,Object> bool(){return props("type","boolean");}
    private static Map<String,Object> object(){return props("type","object","additionalProperties",true);}
    private static Map<String,Object> enumProperty(String... values){return props("type","string","enum",Arrays.asList(values));}
}
