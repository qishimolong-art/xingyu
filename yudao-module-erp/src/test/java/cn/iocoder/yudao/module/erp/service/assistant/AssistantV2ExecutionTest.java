package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantV2ExecutionTest {

    private AssistantExecutionService service;
    private AssistantStore store;
    private AssistantQueryService queries;
    private AssistantModelClient model;
    private AssistantKnowledge knowledge;
    private AssistantOrchestrator orchestrator;
    private AssistantV2Engine v2Engine;
    private AssistantProperties properties;
    private AtomicReference<AssistantExecutionEnvelope> persisted;

    @BeforeEach
    void setUp() {
        LoginUser login = new LoginUser();
        login.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "", Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);

        store = mock(AssistantStore.class);
        queries = mock(AssistantQueryService.class);
        model = mock(AssistantModelClient.class);
        knowledge = mock(AssistantKnowledge.class);
        orchestrator = mock(AssistantOrchestrator.class);
        v2Engine = mock(AssistantV2Engine.class);
        properties = new AssistantProperties();
        persisted = new AtomicReference<>();

        AssistantCapabilityRegistry registry = new AssistantCapabilityRegistry();
        ReflectionTestUtils.setField(registry, "tools", new AssistantToolRegistry());
        AssistantEngineRouter router = new AssistantEngineRouter();
        ReflectionTestUtils.setField(router, "properties", properties);
        ReflectionTestUtils.setField(router, "orchestrator", orchestrator);
        ReflectionTestUtils.setField(router, "registry", registry);
        AssistantSemanticAnalyzer semanticAnalyzer = new AssistantSemanticAnalyzer();
        ReflectionTestUtils.setField(router, "semanticAnalyzer", semanticAnalyzer);

        service = new AssistantExecutionService();
        ReflectionTestUtils.setField(service, "store", store);
        ReflectionTestUtils.setField(service, "queries", queries);
        ReflectionTestUtils.setField(service, "model", model);
        ReflectionTestUtils.setField(service, "knowledge", knowledge);
        ReflectionTestUtils.setField(service, "orchestrator", orchestrator);
        ReflectionTestUtils.setField(service, "codec", new AssistantExecutionCodec());
        ReflectionTestUtils.setField(service, "engineRouter", router);
        ReflectionTestUtils.setField(service, "v2Engine", v2Engine);
        ReflectionTestUtils.setField(service, "semanticAnalyzer", semanticAnalyzer);
        ReflectionTestUtils.setField(service, "companyKnowledge", mock(AssistantCompanyKnowledge.class));

        when(store.begin(anyString(), anyString())).thenReturn("m1");
        when(store.hybridAuditSchemaReady()).thenReturn(true);
        when(knowledge.catalog()).thenReturn(Collections.emptyList());
        when(knowledge.retrieve(anyString(), any(), anyList())).thenReturn(Collections.emptyList());
        when(knowledge.version()).thenReturn("v1");
        when(orchestrator.previewCapability(anyString())).thenReturn("query_verified_metric");
        doAnswer(invocation -> {
            persisted.set(invocation.getArgument(7));
            return null;
        }).when(store).finish(eq("m1"), anyString(), any(), anyString(), anyString(), anyLong(), anyInt(),
                any(AssistantExecutionEnvelope.class));
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void shadowModeReturnsV1AndPersistsRealV2Comparison() throws Exception {
        properties.getV2().setMode("V2_SHADOW");
        when(orchestrator.enabledForCurrentUser()).thenReturn(false);
        when(model.parse(anyString(), any(), anyList())).thenReturn(plan());
        when(queries.execute(any())).thenReturn(result(1));
        when(model.explain(anyMap())).thenReturn("完成");
        when(v2Engine.execute(any(AssistantSemanticRequest.class), anyString(), isNull(), anyList()))
                .thenAnswer(invocation -> {
                    AssistantExecutionEnvelope envelope=envelope();
                    envelope.setSemanticRequest(invocation.getArgument(0));
                    return AssistantOrchestrator.Outcome.result(envelope,result(1));
                });

        CapturingSink sink = execute();

        assertTrue(sink.await());
        assertTrue(sink.events.contains("result"));
        verify(queries).execute(any());
        verify(v2Engine).execute(any(AssistantSemanticRequest.class), anyString(), isNull(), anyList());
        AssistantExecutionEnvelope saved = persisted.get();
        assertNotNull(saved);
        assertEquals("V1", saved.getEngine());
        assertEquals(AssistantSemanticRequest.Source.WEB_PAGE, saved.getSemanticRequest().getSource());
        assertEquals(true, saved.getExecutionAudit().getShadowComparison().get("executed"));
        assertEquals(true, saved.getExecutionAudit().getShadowComparison().get("matched"));
    }

    @Test
    void v2TechnicalFailureFallsBackBeforeReturningAResult() throws Exception {
        properties.getV2().setMode("V2");
        when(v2Engine.execute(any(AssistantSemanticRequest.class), anyString(), isNull(), anyList()))
                .thenThrow(new AssistantFailure("QUERY_FAILED", "V2 查询失败"));
        when(orchestrator.enabledForCurrentUser()).thenReturn(false);
        when(model.parse(anyString(), any(), anyList())).thenReturn(plan());
        when(queries.execute(any())).thenReturn(result(1));
        when(model.explain(anyMap())).thenReturn("V1 完成");

        CapturingSink sink = execute();

        assertTrue(sink.await());
        assertTrue(sink.events.contains("result"));
        AssistantExecutionAudit audit = persisted.get().getExecutionAudit();
        assertTrue(audit.isAutomaticFallback());
        assertEquals("QUERY_FAILED", audit.getFallbackCode());
        assertEquals("V1", audit.getSelectedEngine());
    }

    @Test
    void capabilityFallbackDoesNotInvokeV2() throws Exception {
        properties.getV2().setMode("V2");
        properties.getV2().setFallbackCapabilities(Collections.singletonList("query_verified_metric"));
        when(orchestrator.enabledForCurrentUser()).thenReturn(false);
        when(model.parse(anyString(), any(), anyList())).thenReturn(plan());
        when(queries.execute(any())).thenReturn(result(1));
        when(model.explain(anyMap())).thenReturn("完成");

        CapturingSink sink = execute();

        assertTrue(sink.await());
        verify(v2Engine, never()).execute(any(AssistantSemanticRequest.class), anyString(), any(), anyList());
        assertTrue(persisted.get().getExecutionAudit().isCapabilityFallback());
        assertEquals("CAPABILITY_FALLBACK", persisted.get().getExecutionAudit().getDecisionReason());
    }

    private CapturingSink execute() {
        AssistantExecutionService.Request request = new AssistantExecutionService.Request();
        request.setConversationId("c1");
        request.setQuestion("本月收款");
        request.setEntry("WEB_PAGE");
        request.setCurrentPage("/erp/assistant");
        CapturingSink sink = new CapturingSink();
        service.execute(request, sink);
        return sink;
    }

    private AssistantPlan plan() {
        AssistantPlan plan = new AssistantPlan();
        plan.setMetric(AssistantPlan.Metric.RECEIPT);
        plan.setPeriod("THIS_MONTH");
        return plan;
    }

    private AssistantExecutionEnvelope envelope() {
        return AssistantExecutionEnvelope.metric("本月收款", plan());
    }

    private Map<String, Object> result(int rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        List<Map<String, Object>> values = new ArrayList<>();
        for (int i = 0; i < rows; i++) values.add(Collections.singletonMap("amount", 1));
        result.put("rows", values);
        result.put("summary", values);
        return result;
    }

    private static class CapturingSink implements AssistantExecutionService.EventSink {
        final Set<String> events = Collections.synchronizedSet(new LinkedHashSet<>());
        final CountDownLatch completed = new CountDownLatch(1);
        @Override public void emit(String event, Object data) { events.add(event); }
        @Override public void complete() { completed.countDown(); }
        boolean await() throws InterruptedException { return completed.await(5, TimeUnit.SECONDS); }
    }
}
