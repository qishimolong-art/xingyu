package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssistantEngineRouterTest {

    private AssistantProperties properties;
    private AssistantEngineRouter router;
    private AssistantOrchestrator orchestrator;
    private AssistantCapabilityRegistry registry;

    @BeforeEach
    void setUp() {
        properties = new AssistantProperties();
        orchestrator = mock(AssistantOrchestrator.class);
        registry = new AssistantCapabilityRegistry();
        ReflectionTestUtils.setField(registry, "tools", new AssistantToolRegistry());
        router = new AssistantEngineRouter();
        ReflectionTestUtils.setField(router, "properties", properties);
        ReflectionTestUtils.setField(router, "orchestrator", orchestrator);
        ReflectionTestUtils.setField(router, "registry", registry);
        ReflectionTestUtils.setField(router, "semanticAnalyzer", new AssistantSemanticAnalyzer());
        LoginUser login = new LoginUser();
        login.setId(101L);
        login.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID, "12"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "", Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void capabilityRegistryExactlyCoversExecutableTools() {
        assertDoesNotThrow(registry::validateAgainstToolRegistry);
        assertEquals(24, registry.all().size());
        assertTrue(registry.find(AssistantMixedQuery.TOOL_NAME).get().isVirtual());
        assertEquals(AssistantSemanticRequest.QueryMode.KNOWLEDGE,
                registry.getRequired(AssistantCompanyKnowledge.TOOL_NAME).getQueryMode());
    }

    @Test
    void defaultModeKeepsV1AndBuildsUnifiedRequest() {
        when(orchestrator.previewCapability("本月销售额")).thenReturn("query_verified_metric");
        AssistantExecutionService.Request source = request("WEB_DRAWER", "/erp/sale/out?page=2");

        AssistantSemanticRequest request = router.request("r1", source, 1L, null);
        AssistantEngineRouter.Decision decision = router.decide(request);

        assertEquals(AssistantSemanticRequest.Source.WEB_DRAWER, request.getSource());
        assertEquals("/erp/sale/out?page=2", request.getCurrentPage());
        assertEquals(1L, request.getTenantId());
        assertEquals(101L, request.getUserId());
        assertEquals(12L, request.getDepartmentId());
        assertEquals("query_verified_metric", request.getCapabilityId());
        assertEquals("METRIC_QUERY", request.getIntentId());
        assertEquals("SALE", request.getMetric());
        assertEquals("THIS_MONTH", request.getTimeRange().getPeriod());
        assertEquals("NEW_TOPIC", request.getContext().getMode());
        assertEquals("V1", decision.getSelectedEngine());
        assertEquals("V2_DISABLED", decision.getReason());
    }

    @Test
    void requestPreviewUsesPreviousKnowledgeEnvelopeForEllipticalFollowUp() {
        AssistantExecutionEnvelope previous = knowledgePrevious("公司的组织架构");
        when(orchestrator.previewCapability("具体的人员组成", previous))
                .thenReturn(AssistantCompanyKnowledge.TOOL_NAME);
        AssistantExecutionService.Request source = request("WEB_PAGE", "/erp/assistant");
        source.setQuestion("具体的人员组成");

        AssistantSemanticRequest request = router.request("r-knowledge-follow-up", source, 1L, previous);

        assertEquals(AssistantCompanyKnowledge.TOOL_NAME, request.getCapabilityId());
        assertEquals(AssistantSemanticRequest.QueryMode.KNOWLEDGE, request.getQueryMode());
        assertEquals("COMPANY_KNOWLEDGE", request.getIntentId());
        assertEquals("INHERIT", request.getContext().getMode());
        assertEquals("KNOWLEDGE_FOLLOW_UP", request.getContext().getReason());
        verify(orchestrator).previewCapability("具体的人员组成", previous);
    }

    @Test
    void shadowModeKeepsV1ForegroundAndEnablesComparison() {
        properties.getV2().setMode("V2_SHADOW");
        AssistantSemanticRequest request = semantic("query_sale_documents");

        AssistantEngineRouter.Decision decision = router.decide(request);

        assertEquals("V1", decision.getSelectedEngine());
        assertTrue(decision.isShadowV2());
    }

    @Test
    void grayModeRequiresEveryConfiguredDimensionToMatch() {
        properties.getV2().setMode("V2_GRAY");
        properties.getV2().setGrayTenantIds(Collections.singletonList(1L));
        properties.getV2().setGrayUserIds(Collections.singletonList(101L));
        properties.getV2().setGrayDepartmentIds(Collections.singletonList(12L));
        properties.getV2().setGrayEntries(Collections.singletonList("WEB_DRAWER"));
        properties.getV2().setGrayCapabilities(Collections.singletonList("query_sale_documents"));
        AssistantSemanticRequest request = semantic("query_sale_documents");
        request.setTenantId(1L);
        request.setDepartmentId(12L);
        request.setSource(AssistantSemanticRequest.Source.WEB_DRAWER);

        assertEquals("V2", router.decide(request).getSelectedEngine());
        request.setSource(AssistantSemanticRequest.Source.WECOM);
        assertEquals("V1", router.decide(request).getSelectedEngine());
        request.setSource(AssistantSemanticRequest.Source.WEB_DRAWER);
        request.setDepartmentId(13L);
        assertEquals("V1", router.decide(request).getSelectedEngine());
    }

    @Test
    void fullV2SupportsEntryRollbackWithoutChangingOtherEntries() {
        properties.getV2().setMode("V2");
        properties.getV2().setFallbackEntries(Collections.singletonList("WECOM"));
        AssistantSemanticRequest request = semantic("query_sale_documents");
        request.setSource(AssistantSemanticRequest.Source.WECOM);
        AssistantEngineRouter.Decision rollback = router.decide(request);
        assertEquals("V1", rollback.getSelectedEngine());
        assertTrue(rollback.isEntryFallback());
        assertFalse(rollback.isCapabilityFallback());
        request.setSource(AssistantSemanticRequest.Source.WEB_PAGE);
        assertEquals("V2", router.decide(request).getSelectedEngine());
    }

    @Test
    void disabledEntryCapabilityKnowledgeAndMixedFailBeforeExecution() {
        when(orchestrator.previewCapability(anyString())).thenReturn("query_sale_documents");
        properties.getV2().setDisabledEntries(Collections.singletonList("WEB_DRAWER"));
        AssistantFailure entry = assertThrows(AssistantFailure.class,
                () -> router.request("r-entry", request("WEB_DRAWER", "/erp/sale/out"), 1L, null));
        assertEquals("CAPABILITY_UNAVAILABLE", entry.getCode());

        properties.getV2().setDisabledEntries(Collections.emptyList());
        properties.getV2().setDisabledCapabilities(Collections.singletonList("query_sale_documents"));
        AssistantFailure capability = assertThrows(AssistantFailure.class,
                () -> router.request("r-cap", request("WEB_PAGE", "/erp/sale/out"), 1L, null));
        assertEquals("CAPABILITY_UNAVAILABLE", capability.getCode());

        properties.getV2().setDisabledCapabilities(Collections.emptyList());
        properties.getV2().setKnowledgeEnabled(false);
        when(orchestrator.previewCapability(anyString())).thenReturn(AssistantCompanyKnowledge.TOOL_NAME);
        assertEquals("CAPABILITY_UNAVAILABLE", assertThrows(AssistantFailure.class,
                () -> router.request("r-knowledge", request("WEB_PAGE", "/erp/assistant"), 1L, null)).getCode());

        properties.getV2().setKnowledgeEnabled(true);
        properties.getV2().setMixedEnabled(false);
        when(orchestrator.previewCapability(anyString())).thenReturn(AssistantMixedQuery.TOOL_NAME);
        assertEquals("CAPABILITY_UNAVAILABLE", assertThrows(AssistantFailure.class,
                () -> router.request("r-mixed", request("WEB_PAGE", "/erp/assistant"), 1L, null)).getCode());
    }

    @Test
    void oneCapabilityCanBeForcedBackToV1EvenInFullV2() {
        properties.getV2().setMode("V2");
        properties.getV2().setFallbackCapabilities(Arrays.asList("query_account_balance"));

        AssistantEngineRouter.Decision decision = router.decide(semantic("query_account_balance"));

        assertEquals("V1", decision.getSelectedEngine());
        assertTrue(decision.isCapabilityFallback());
        assertEquals("CAPABILITY_FALLBACK", decision.getReason());
    }

    @Test
    void stage5CapabilitiesSupportIndependentGrayAndRollbackControls() {
        properties.getV2().setMode("V2_GRAY");
        properties.getV2().setGrayCapabilities(Collections.singletonList(AssistantStage5BusinessQuery.SALE_TOOL));
        assertEquals("V2", router.decide(semantic(AssistantStage5BusinessQuery.SALE_TOOL)).getSelectedEngine());
        assertEquals("V1", router.decide(semantic(AssistantStage5BusinessQuery.PURCHASE_TOOL)).getSelectedEngine());

        properties.getV2().setMode("V2");
        properties.getV2().setFallbackCapabilities(Collections.singletonList(AssistantStage5BusinessQuery.FINANCE_TOOL));
        AssistantEngineRouter.Decision rollback=router.decide(semantic(AssistantStage5BusinessQuery.FINANCE_TOOL));
        assertEquals("V1",rollback.getSelectedEngine());
        assertTrue(rollback.isCapabilityFallback());
    }

    @Test
    void unknownModeFailsClosedToV1() {
        properties.getV2().setMode("something-else");
        assertEquals(AssistantEngineMode.V1, router.decide(semantic("model_routed")).getConfiguredMode());
        assertEquals("V1", router.decide(semantic("model_routed")).getSelectedEngine());
    }

    private AssistantExecutionService.Request request(String entry, String page) {
        AssistantExecutionService.Request request = new AssistantExecutionService.Request();
        request.setConversationId("c1");
        request.setQuestion("本月销售额");
        request.setEntry(entry);
        request.setCurrentPage(page);
        return request;
    }

    private AssistantSemanticRequest semantic(String capability) {
        AssistantSemanticRequest request = new AssistantSemanticRequest();
        request.setTenantId(1L);
        request.setUserId(101L);
        request.setSource(AssistantSemanticRequest.Source.WEB_PAGE);
        request.setCapabilityId(capability);
        return request;
    }

    private static AssistantExecutionEnvelope knowledgePrevious(String question) {
        AssistantExecutionEnvelope envelope = new AssistantExecutionEnvelope();
        envelope.setQuestion(question);
        envelope.setRouteType(AssistantExecutionEnvelope.RouteType.KNOWLEDGE);
        envelope.setToolName(AssistantCompanyKnowledge.TOOL_NAME);
        AssistantSemanticRequest request = new AssistantSemanticRequest();
        request.setRequestId("previous-knowledge");
        envelope.setSemanticRequest(request);
        return envelope;
    }
}
