package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantReadinessServiceTest {

    private AssistantReadinessService service;
    private AssistantReadOnly reader;
    private AssistantQueryService queries;
    private PermissionApi permissions;

    @BeforeEach
    void setUp() {
        login(101L, 1L, null);
        TenantContextHolder.setTenantId(1L);

        service = new AssistantReadinessService();
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-only");
        properties.setReadinessCacheSeconds(60);
        reader = mock(AssistantReadOnly.class);
        when(reader.configured()).thenReturn(true);
        queries = mock(AssistantQueryService.class);
        permissions = mock(PermissionApi.class);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(permissions).executeInReadSnapshot(any(Supplier.class));

        AssistantStore store = mock(AssistantStore.class);
        when(store.hybridAuditSchemaReady()).thenReturn(true);
        AssistantKnowledge knowledge = mock(AssistantKnowledge.class);
        Map<String, Object> sale = new HashMap<>();
        sale.put("id", "SALE");
        sale.put("title", "销售");
        sale.put("published", true);
        when(knowledge.catalog()).thenReturn(Collections.singletonList(sale));
        when(knowledge.version()).thenReturn("metric-v1");
        AssistantCompanyKnowledge companyKnowledge = mock(AssistantCompanyKnowledge.class);
        when(companyKnowledge.available()).thenReturn(true);
        when(companyKnowledge.version()).thenReturn("knowledge-v1");

        set("store", store);
        set("queries", queries);
        set("knowledge", knowledge);
        set("properties", properties);
        set("reader", reader);
        set("orchestrator", mock(AssistantOrchestrator.class));
        set("semanticCatalog", mock(AssistantSemanticCatalog.class));
        set("entityResolver", null);
        set("semanticKnowledge", new AssistantSemanticKnowledge());
        set("companyKnowledge", companyKnowledge);
        set("engineRouter", null);
        set("capabilityRegistry", null);
        AssistantPhoneticNormalizer phonetic = new AssistantPhoneticNormalizer();
        ReflectionTestUtils.setField(phonetic, "properties", properties);
        ReflectionTestUtils.setField(phonetic, "companyKnowledge", null);
        set("phoneticNormalizer", phonetic);
        set("permissions", permissions);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void reusesStableResultAndMetricAuthorization() {
        assertEquals("READY", service.get(false).get("status"));
        assertEquals("READY", service.get(false).get("status"));

        verify(permissions, times(1)).executeInReadSnapshot(any(Supplier.class));
        verify(reader, times(1)).probe();
        verify(queries, times(1)).authorize(eq(AssistantPlan.Metric.SALE));
    }

    @Test
    void refreshAndIdentityChangesBypassTheCachedIdentity() {
        service.get(false);
        service.get(true);
        login(202L, 1L, 2L);
        TenantContextHolder.setTenantId(2L);
        service.get(false);

        verify(permissions, times(3)).executeInReadSnapshot(any(Supplier.class));
        verify(reader, times(3)).probe();
    }

    @Test
    void transientReadOnlyFailureIsNotCached() {
        doThrow(new AssistantFailure("READ_ONLY_UNAVAILABLE", "只读数据源暂时不可用"))
                .when(reader).probe();

        service.get(false);
        service.get(false);

        verify(permissions, times(2)).executeInReadSnapshot(any(Supplier.class));
        verify(reader, times(2)).probe();
    }

    @Test
    void exposesPhoneticReadinessWithoutAdvertisingUnauthorizedEntityTypes() {
        Map<?, ?> phonetic = (Map<?, ?>) service.get(false).get("phonetic");

        assertEquals(true, phonetic.get("enabled"));
        assertEquals("2026-10-06.1", phonetic.get("version"));
        assertEquals(true, phonetic.get("nearSoundEnabled"));
        assertEquals(java.util.Arrays.asList("PRODUCT", "CUSTOMER", "SUPPLIER"), phonetic.get("entityTypes"));
        assertEquals(null, phonetic.get("reason"));
    }

    private void set(String field, Object value) {
        ReflectionTestUtils.setField(service, field, value);
    }

    private static void login(Long userId, Long tenantId, Long visitTenantId) {
        LoginUser user = new LoginUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setVisitTenantId(visitTenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "", Collections.emptyList()));
    }
}
