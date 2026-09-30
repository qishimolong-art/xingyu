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
    }

    @Test
    void exposesOnlyRegisteredTools() {
        assertTrue(registry.contains("query_verified_metric"));
        assertTrue(registry.contains("query_master_data_stats"));
        assertTrue(registry.contains("execute_semantic_query"));
        assertFalse(registry.contains("execute_sql"));
        assertEquals(14,registry.definitions().size());
    }

    @Test
    void executionContractsDeclareSupportedObjectCombinationsAndCapabilities() {
        AssistantToolRegistry.ExecutionContract stock=registry.executionContract("query_stock_movements",null);
        assertTrue(stock.supportsEntities(Arrays.asList("PRODUCT","WAREHOUSE")));
        assertTrue(stock.isTime());assertTrue(stock.isGrouping());assertTrue(stock.isTrend());assertTrue(stock.isRanking());

        AssistantToolRegistry.ExecutionContract purchase=registry.executionContract("query_purchase_documents",null);
        assertTrue(purchase.supportsEntities(Arrays.asList("SUPPLIER","DEPARTMENT")));
        assertFalse(purchase.supportsEntities(Arrays.asList("SUPPLIER","PRODUCT")));
        assertEquals(new LinkedHashSet<>(Arrays.asList("SUPPLIER","DEPARTMENT")),purchase.getSupportedEntityTypes());

        AssistantToolRegistry.ExecutionContract saleOrder=registry.executionContract("query_sale_documents",null);
        assertTrue(saleOrder.supportsEntities(Arrays.asList("CUSTOMER","PRODUCT")));
        assertTrue(saleOrder.supportsEntities(Collections.singleton("SALESPERSON")));

        AssistantToolRegistry.ExecutionContract receivable=registry.executionContract("query_verified_metric",AssistantPlan.Metric.RECEIVABLE);
        assertTrue(receivable.supportsEntities(Collections.singleton("CUSTOMER")));
        assertFalse(receivable.supportsEntities(Collections.singleton("SUPPLIER")));

        AssistantToolRegistry.ExecutionContract sale=registry.executionContract("query_verified_metric",AssistantPlan.Metric.SALE);
        assertTrue(sale.supportsEntities(Collections.singleton("PRODUCT")));
    }

    @Test
    void datasetContractsMatchPublishedSemanticObjectScopes() {
        assertTrue(registry.datasetContract("stock_movements").supportsEntities(Arrays.asList("PRODUCT","WAREHOUSE")));
        assertTrue(registry.datasetContract("sale_orders").supportsEntities(Collections.singleton("CUSTOMER")));
        assertFalse(registry.datasetContract("sale_orders").supportsEntities(Collections.singleton("PRODUCT")));
        assertTrue(registry.datasetContract("sale_order_items").supportsEntities(Arrays.asList("CUSTOMER","PRODUCT")));
        assertFalse(registry.datasetContract("unknown").supportsEntities(Collections.singleton("CUSTOMER")));
    }
}
