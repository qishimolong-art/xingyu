package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

class AssistantToolRegistryTest {
    private final AssistantToolRegistry registry=new AssistantToolRegistry();

    @Test
    void acceptsDeclaredArgumentsAndRejectsUnknownOnes() {
        AssistantToolCall valid=new AssistantToolCall();valid.setName("query_purchase_documents");
        valid.setArguments(Collections.<String,Object>singletonMap("question","今天做了几个采购订单"));
        assertDoesNotThrow(()->registry.validate(valid));

        AssistantToolCall invalid=new AssistantToolCall();invalid.setName("query_purchase_documents");
        invalid.setArguments(Collections.<String,Object>singletonMap("sql","DROP TABLE x"));
        assertEquals("MODEL_INVALID",assertThrows(AssistantFailure.class,()->registry.validate(invalid)).getCode());

        AssistantToolCall filteredProductCount=new AssistantToolCall();
        filteredProductCount.setName("query_master_data_stats");
        java.util.Map<String,Object> productArguments=new java.util.LinkedHashMap<>();
        productArguments.put("question","ERP中有多少个美孚产品");
        productArguments.put("archiveType","PRODUCT");productArguments.put("statusMode","ENABLED");
        productArguments.put("brand","美孚");filteredProductCount.setArguments(productArguments);
        assertDoesNotThrow(()->registry.validate(filteredProductCount));
    }

    @Test
    void exposesOnlyRegisteredTools() {
        assertTrue(registry.contains("query_verified_metric"));
        assertTrue(registry.contains("query_master_data_stats"));
        assertTrue(registry.contains(AssistantCompanyKnowledge.TOOL_NAME));
        assertTrue(registry.contains(AssistantToolRegistry.CLARIFY_INTENT_TOOL));
        assertTrue(registry.contains("execute_semantic_query"));
        assertFalse(registry.contains("execute_sql"));
        assertEquals(22,registry.definitions().size());
    }

    @Test
    void clarificationToolAcceptsOnlyBoundedIntentChoices() {
        AssistantToolCall valid=new AssistantToolCall();valid.setName(AssistantToolRegistry.CLARIFY_INTENT_TOOL);
        java.util.Map<String,Object> arguments=new java.util.LinkedHashMap<>();
        arguments.put("question","帮我看看这个情况");arguments.put("choice1","查询实时业务数据");
        arguments.put("choice2","查询公司知识资料");
        valid.setArguments(arguments);
        assertDoesNotThrow(()->registry.validate(valid));

        arguments.put("sql","select * from x");
        assertEquals("MODEL_INVALID",assertThrows(AssistantFailure.class,()->registry.validate(valid)).getCode());
    }

    @Test
    void executionContractsDeclareSupportedObjectCombinationsAndCapabilities() {
        AssistantToolRegistry.ExecutionContract stock=registry.executionContract("query_stock_movements",null);
        assertTrue(stock.supportsEntities(Arrays.asList("PRODUCT","WAREHOUSE")));
        assertTrue(stock.isTime());assertTrue(stock.isGrouping());assertTrue(stock.isTrend());assertTrue(stock.isRanking());

        AssistantToolRegistry.ExecutionContract purchase=registry.executionContract("query_purchase_documents",null);
        assertTrue(purchase.supportsEntities(Arrays.asList("SUPPLIER","DEPARTMENT")));
        assertFalse(purchase.supportsEntities(Arrays.asList("SUPPLIER","PRODUCT")));
        assertEquals(new LinkedHashSet<>(Arrays.asList("SUPPLIER","DEPARTMENT","PURCHASER")),purchase.getSupportedEntityTypes());
        assertTrue(purchase.supportsEntities(Collections.singleton("PURCHASER")));

        AssistantToolRegistry.ExecutionContract saleOrder=registry.executionContract("query_sale_documents",null);
        assertTrue(saleOrder.supportsEntities(Arrays.asList("CUSTOMER","PRODUCT")));
        assertTrue(saleOrder.supportsEntities(Collections.singleton("SALESPERSON")));

        AssistantToolRegistry.ExecutionContract receivable=registry.executionContract("query_verified_metric",AssistantPlan.Metric.RECEIVABLE);
        assertTrue(receivable.supportsEntities(Collections.singleton("CUSTOMER")));
        assertTrue(receivable.supportsEntities(Collections.singleton("SALESPERSON")));
        assertFalse(receivable.supportsEntities(Collections.singleton("SUPPLIER")));

        AssistantToolRegistry.ExecutionContract sale=registry.executionContract("query_verified_metric",AssistantPlan.Metric.SALE);
        assertTrue(sale.supportsEntities(Collections.singleton("PRODUCT")));
        assertTrue(sale.supportsEntities(Collections.singleton("SALESPERSON")));
    }

    @Test
    void datasetContractsMatchPublishedSemanticObjectScopes() {
        assertTrue(registry.datasetContract("stock_movements").supportsEntities(Arrays.asList("PRODUCT","WAREHOUSE")));
        assertTrue(registry.datasetContract("sale_orders").supportsEntities(Collections.singleton("CUSTOMER")));
        assertFalse(registry.datasetContract("sale_orders").supportsEntities(Collections.singleton("PRODUCT")));
        assertTrue(registry.datasetContract("sale_order_items").supportsEntities(Arrays.asList("CUSTOMER","PRODUCT")));
        assertTrue(registry.datasetContract("purchase_orders").supportsEntities(Collections.singleton("PURCHASER")));
        assertFalse(registry.datasetContract("unknown").supportsEntities(Collections.singleton("CUSTOMER")));
    }
}
