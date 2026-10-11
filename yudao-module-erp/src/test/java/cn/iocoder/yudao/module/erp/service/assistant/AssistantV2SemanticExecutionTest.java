package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantV2SemanticExecutionTest {

    private AssistantOrchestrator orchestrator;
    private AssistantV2Engine engine;
    private AssistantSemanticAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        orchestrator = mock(AssistantOrchestrator.class);
        analyzer = new AssistantSemanticAnalyzer();
        AssistantCapabilityRegistry capabilities = new AssistantCapabilityRegistry();
        ReflectionTestUtils.setField(capabilities, "tools", new AssistantToolRegistry());
        engine = new AssistantV2Engine();
        ReflectionTestUtils.setField(engine, "orchestrator", orchestrator);
        ReflectionTestUtils.setField(engine, "capabilities", capabilities);
        ReflectionTestUtils.setField(engine, "semantics", analyzer);
    }

    @Test
    void newTopicCutsOffPreviousEntityBeforeCallingOrchestrator() {
        AssistantExecutionEnvelope previous = previous("CUSTOMER");
        AssistantSemanticRequest request = semantic("query_product_stock", "那当前库存呢", previous);
        when(orchestrator.execute(eq("那当前库存呢"), isNull(), anyList()))
                .thenReturn(outcome("query_product_stock"));

        AssistantOrchestrator.Outcome outcome = engine.execute(request, "那当前库存呢", previous,
                Collections.emptyList());

        verify(orchestrator).execute(eq("那当前库存呢"), isNull(), anyList());
        assertEquals("NEW_TOPIC", outcome.getEnvelope().getSemanticRequest().getContext().getMode());
        assertEquals("STOCK", outcome.getEnvelope().getSemanticRequest().getMetric());
    }

    @Test
    void explicitFollowUpPassesOnlyValidatedPreviousContext() {
        AssistantExecutionEnvelope previous = previous("CUSTOMER");
        AssistantSemanticRequest request = semantic("query_verified_metric", "这个客户上月销售额呢", previous);
        when(orchestrator.execute(eq("这个客户上月销售额呢"), same(previous), anyList()))
                .thenReturn(outcome("query_verified_metric"));

        engine.execute(request, "这个客户上月销售额呢", previous, Collections.emptyList());

        verify(orchestrator).execute(eq("这个客户上月销售额呢"), same(previous), anyList());
        assertEquals("INHERIT", request.getContext().getMode());
        assertEquals("LAST_MONTH", request.getTimeRange().getPeriod());
    }

    @Test
    void missingContextReturnsAClarificationObjectWithoutExecutingBusinessQuery() {
        AssistantSemanticRequest request = semantic("query_verified_metric", "销量呢", null);

        AssistantOrchestrationClarification failure = assertThrows(AssistantOrchestrationClarification.class,
                () -> engine.execute(request, "销量呢", null, Collections.emptyList()));

        assertEquals("CLARIFY", failure.getResponse().get("status"));
        assertEquals("MISSING_CONTEXT", failure.getResponse().get("reasonCode"));
        assertTrue(failure.getResponse().containsKey("missingFields"));
        assertTrue(failure.getResponse().containsKey("choices"));
        assertTrue(failure.getResponse().containsKey("candidates"));
        verifyNoInteractions(orchestrator);
    }

    @Test
    void legacyClarificationIsNormalizedWithoutDroppingCandidates() {
        AssistantSemanticRequest request = semantic("resolve_business_entity", "用户张三", null);
        AssistantExecutionEnvelope envelope = envelope("resolve_business_entity");
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("id", 1L);
        candidate.put("name", "张三");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("clarification", "请选择用户");
        response.put("candidates", Collections.singletonList(candidate));
        when(orchestrator.execute(anyString(), isNull(), anyList()))
                .thenThrow(new AssistantOrchestrationClarification(envelope, response));

        AssistantOrchestrationClarification failure = assertThrows(AssistantOrchestrationClarification.class,
                () -> engine.execute(request, "用户张三", null, Collections.emptyList()));

        assertEquals("CHOICE_REQUIRED", failure.getResponse().get("status"));
        assertEquals("ENTITY_AMBIGUOUS", failure.getResponse().get("reasonCode"));
        assertEquals(1, ((java.util.List<?>) failure.getResponse().get("candidates")).size());
    }

    private AssistantSemanticRequest semantic(String capability, String question,
                                               AssistantExecutionEnvelope previous) {
        AssistantSemanticRequest request = new AssistantSemanticRequest();
        request.setCapabilityId(capability);
        request.setQuestion(question);
        analyzer.analyze(request, previous);
        return request;
    }

    private static AssistantOrchestrator.Outcome outcome(String capability) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("rows", Collections.emptyList());
        return AssistantOrchestrator.Outcome.result(envelope(capability), result);
    }

    private static AssistantExecutionEnvelope envelope(String capability) {
        AssistantExecutionEnvelope envelope = new AssistantExecutionEnvelope();
        envelope.setToolName(capability);
        envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
        return envelope;
    }

    private static AssistantExecutionEnvelope previous(String type) {
        AssistantPlan plan = new AssistantPlan();
        plan.setMetric(AssistantPlan.Metric.SALE);
        plan.setPeriod("THIS_MONTH");
        AssistantExecutionEnvelope envelope = AssistantExecutionEnvelope.metric("上一轮", plan);
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("id", 1L);
        entity.put("name", "上一轮对象");
        entity.put("entityType", type);
        envelope.getArguments().put("resolvedEntity", entity);
        Map<String, Object> entities = new LinkedHashMap<>();
        entities.put(type, entity);
        envelope.getArguments().put("resolvedEntities", entities);
        return envelope;
    }
}
