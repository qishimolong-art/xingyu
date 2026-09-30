package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

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
        value.getArguments().put("sql","SELECT p.id FROM purchase_orders p LIMIT 20");
        AssistantExecutionEnvelope restored=codec.read(codec.write(value),null);
        assertEquals(2,restored.getSchemaVersion());
        assertEquals(value.getRouteType(),restored.getRouteType());
        assertEquals(value.getToolName(),restored.getToolName());
        assertEquals(value.getArguments(),restored.getArguments());
    }

    @Test
    void malformedHistoryFailsClosed() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->codec.read("{bad",null));
        assertEquals("INVALID_HISTORY",failure.getCode());
    }
}
