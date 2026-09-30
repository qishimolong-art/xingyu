package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssistantStoreTest {

    @Test
    void previousContextUsesOnlyImmediateSuccessfulOrUsefulClarificationMessage() {
        AssistantStore store=new AssistantStore();
        String success="{\"metric\":\"SALE\"}";
        assertEquals(success,store.previousContext(Collections.singletonList(row("SUCCESS",success))));
        assertEquals(success,store.previousContext(Collections.singletonList(row("EMPTY",success))));

        String selected="{\"arguments\":{\"resolvedEntity\":{\"id\":7,\"entityType\":\"PRODUCT\"}}}";
        String choices="{\"arguments\":{\"pendingEntities\":[{\"id\":7,\"entityType\":\"PRODUCT\"}]}}";
        assertEquals(selected,store.previousContext(Collections.singletonList(row("CLARIFY",selected))));
        assertEquals(choices,store.previousContext(Collections.singletonList(row("CLARIFY",choices))));
    }

    @Test
    void failedOrUnresolvedImmediateMessageIsAContextBarrier() {
        AssistantStore store=new AssistantStore();
        String stale="{\"arguments\":{\"resolvedEntity\":{\"id\":759,\"name\":\"黑霸王\",\"entityType\":\"PRODUCT\"}}}";
        assertNull(store.previousContext(Arrays.asList(row("MODEL_INVALID",null),row("SUCCESS",stale))));
        assertNull(store.previousContext(Arrays.asList(row("ENTITY_NOT_FOUND",null),row("SUCCESS",stale))));
        assertNull(store.previousContext(Arrays.asList(row("CLARIFY","{\"legacyPlan\":{\"clarification\":\"请补充对象\"}}"),row("SUCCESS",stale))));
    }

    private static Map<String,Object> row(String status,String plan) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("status",status);row.put("plan_json",plan);return row;
    }

    @Test
    void auditSummaryTreatsResolvedIdsAsActiveFilters() throws Exception {
        String envelope="{\"routeType\":\"VERIFIED_METRIC\",\"toolName\":\"query_verified_metric\","
                +"\"legacyPlan\":{\"metric\":\"RECEIPT\",\"group\":\"NONE\",\"period\":\"THIS_MONTH\","
                +"\"warehouseId\":11,\"partyId\":12,\"productId\":13,\"departmentId\":10}}";

        JsonNode summary=new ObjectMapper().readTree(new AssistantStore().auditSummary(envelope));

        assertTrue(summary.path("warehouseFiltered").asBoolean());
        assertTrue(summary.path("partyFiltered").asBoolean());
        assertTrue(summary.path("productFiltered").asBoolean());
        assertTrue(summary.path("departmentFiltered").asBoolean());
        assertEquals("RECEIPT",summary.path("metric").asText());
    }

    @Test
    void auditSummaryKeepsUnfilteredObjectsFalse() throws Exception {
        String plan="{\"metric\":\"RECEIPT\",\"group\":\"NONE\",\"period\":\"THIS_MONTH\"}";

        JsonNode summary=new ObjectMapper().readTree(new AssistantStore().auditSummary(plan));

        assertFalse(summary.path("warehouseFiltered").asBoolean());
        assertFalse(summary.path("partyFiltered").asBoolean());
        assertFalse(summary.path("productFiltered").asBoolean());
        assertFalse(summary.path("departmentFiltered").asBoolean());
    }
}
