package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssistantExecutionCodecTest {
    private final AssistantExecutionCodec codec=new AssistantExecutionCodec();

    @Test
    void legacyPlanIsReadAsVerifiedMetricEnvelope() {
        AssistantExecutionEnvelope envelope=codec.read(
                "{\"metric\":\"STOCK\",\"group\":\"WAREHOUSE\",\"period\":\"CURRENT\",\"stockMode\":\"ALL\"}",
                "各仓库存");
        assertEquals(AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC,envelope.getRouteType());
        assertEquals("query_verified_metric",envelope.getToolName());
        assertEquals(AssistantPlan.Metric.STOCK,envelope.getLegacyPlan().getMetric());
        assertEquals(AssistantPlan.RankOrder.DESC,envelope.getLegacyPlan().getRankOrder());
        assertEquals("各仓库存",envelope.getQuestion());
    }

    @Test
    void v2EnvelopeRoundTripsWithoutLosingRouteOrTool() {
        AssistantExecutionEnvelope value=new AssistantExecutionEnvelope();
        value.setQuestion("今天做了几个采购订单");
        value.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
        value.setToolName("query_purchase_documents");
        value.setEngine("V2");
        AssistantSemanticRequest request=new AssistantSemanticRequest();
        request.setSource(AssistantSemanticRequest.Source.WEB_DRAWER);
        request.setCapabilityId("query_purchase_documents");
        request.setIntentId("DOCUMENT_QUERY");
        request.setMetric("PURCHASE");
        request.getBusinessObjects().add("SUPPLIER");
        request.getContext().setMode("NEW_TOPIC");
        value.setSemanticRequest(request);
        value.getArguments().put("sql","SELECT p.id FROM purchase_orders p LIMIT 20");
        Map<String,Object> organizationContext=new LinkedHashMap<>();
        organizationContext.put("unit","传媒运营部");organizationContext.put("people",Arrays.asList("王宇"));
        organizationContext.put("role","负责人");organizationContext.put("sourceIds",Arrays.asList("reviewed-source"));
        value.getArguments().put(AssistantKnowledgeContext.ORGANIZATION_CONTEXT_ARGUMENT,organizationContext);
        AssistantExecutionEnvelope restored=codec.read(codec.write(value),null);
        assertEquals(4,restored.getSchemaVersion());
        assertEquals(value.getRouteType(),restored.getRouteType());
        assertEquals(value.getToolName(),restored.getToolName());
        assertEquals("V2",restored.getEngine());
        assertEquals(AssistantSemanticRequest.Source.WEB_DRAWER,restored.getSemanticRequest().getSource());
        assertEquals("DOCUMENT_QUERY",restored.getSemanticRequest().getIntentId());
        assertEquals("PURCHASE",restored.getSemanticRequest().getMetric());
        assertEquals(java.util.Collections.singletonList("SUPPLIER"),restored.getSemanticRequest().getBusinessObjects());
        assertEquals(value.getArguments(),restored.getArguments());
    }

    @Test
    void malformedHistoryFailsClosed() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->codec.read("{bad",null));
        assertEquals("INVALID_HISTORY",failure.getCode());
    }
}
