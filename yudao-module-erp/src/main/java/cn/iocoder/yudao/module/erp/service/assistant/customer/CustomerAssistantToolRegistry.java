package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.module.erp.service.assistant.AssistantProperties;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomerAssistantToolRegistry {
    public static final String PRICE = "query_customer_price";
    public static final String STOCK = "query_mall_stock";
    public static final String ORDER = "query_customer_orders";
    public static final String CLARIFY = "clarify_customer_intent";

    private final AssistantProperties properties;

    public CustomerAssistantToolRegistry(AssistantProperties properties) {
        this.properties = properties;
    }

    public List<Map<String,Object>> definitions() {
        List<Map<String,Object>> result=new ArrayList<>();
        if (isAllowed(PRICE)) {
            result.add(tool(PRICE,"查询当前绑定客户适用的商城商品客户价",productParameters()));
        }
        if (isAllowed(STOCK)) {
            result.add(tool(STOCK,"查询商城商品精确可售总量，不查询分仓或内部库存",productParameters()));
        }
        Map<String,Object> order=object();
        Map<String,Object> props=cast(order.get("properties"));
        props.put("orderNo",property("string","用户明确给出的订单号"));
        props.put("unfinishedOnly",property("boolean","是否只查询未发货或未完成订单"));
        props.put("page",property("integer","分页页码，从1开始"));
        if (isAllowed(ORDER)) {
            result.add(tool(ORDER,"查询当前绑定客户在授权销售部门内的订单及履约进度",order));
        }
        Map<String,Object> clarify=object();
        cast(clarify.get("properties")).put("question",property("string","需要客户补充的信息"));
        if (isAllowed(CLARIFY)) {
            result.add(tool(CLARIFY,"仅当价格、库存、订单意图或商品对象不明确时请求澄清",clarify));
        }
        return result;
    }

    /** Model registration and execution both use this check, so configuration cannot be bypassed by model output. */
    public boolean isAllowed(String toolName) {
        String capability;
        if (PRICE.equals(toolName)) {
            capability = "CUSTOMER_PRICE";
        } else if (STOCK.equals(toolName)) {
            capability = "MALL_STOCK";
        } else if (ORDER.equals(toolName)) {
            capability = "CUSTOMER_ORDER";
        } else if (CLARIFY.equals(toolName)) {
            capability = "CLARIFICATION";
        } else {
            return false;
        }
        return properties.getCustomerApp().getAllowedCapabilities().stream()
                .anyMatch(capability::equalsIgnoreCase);
    }

    private Map<String,Object> productParameters() {
        Map<String,Object> params=object();
        Map<String,Object> props=cast(params.get("properties"));
        props.put("keyword",property("string","商品名称、配件编码、厂商编码或规格关键词"));
        props.put("productId",property("integer","只能来自服务端上一轮返回的候选商品编号"));
        return params;
    }
    private Map<String,Object> tool(String name,String description,Map<String,Object> parameters) {
        Map<String,Object> fn=new LinkedHashMap<>();fn.put("name",name);fn.put("description",description);fn.put("parameters",parameters);
        Map<String,Object> tool=new LinkedHashMap<>();tool.put("type","function");tool.put("function",fn);return tool;
    }
    private Map<String,Object> object(){Map<String,Object> v=new LinkedHashMap<>();v.put("type","object");v.put("properties",new LinkedHashMap<String,Object>());v.put("additionalProperties",false);return v;}
    private Map<String,Object> property(String type,String description){Map<String,Object> v=new LinkedHashMap<>();v.put("type",type);v.put("description",description);return v;}
    @SuppressWarnings("unchecked") private Map<String,Object> cast(Object value){return (Map<String,Object>)value;}
}
