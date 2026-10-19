package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantFailure;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantProperties;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerAssistantContextServiceTest {

    private final AssistantProperties properties = new AssistantProperties();
    private final ErpCustomerMemberApi customerMemberApi = mock(ErpCustomerMemberApi.class);
    private final ErpCustomerDeptPermissionService customerDeptPermissionService =
            mock(ErpCustomerDeptPermissionService.class);
    private final CustomerAssistantContextService contextService = new CustomerAssistantContextService();

    @BeforeEach
    void setUp() {
        properties.getCustomerApp().setEnabled(true);
        ReflectionTestUtils.setField(contextService, "properties", properties);
        ReflectionTestUtils.setField(contextService, "customerMemberApi", customerMemberApi);
        ReflectionTestUtils.setField(contextService, "customerDeptPermissionService", customerDeptPermissionService);

        LoginUser login = new LoginUser();
        login.setId(286L);
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
    void current_authorizedMemberBuildsCustomerContextWithoutAdminCustomerLookup() {
        when(customerMemberApi.getCustomerMemberAuth(286L)).thenReturn(new ErpCustomerMemberAuthRespDTO()
                .setAuthorized(true).setCustomerId(1L).setCustomerName("测试").setPriceLevel(3)
                .setMemberUserId(286L).setPriceVisible(true).setOrderEnabled(true));
        DeptRespDTO dept1 = dept(10L, "兴宇路通");
        DeptRespDTO dept2 = dept(20L, "巴中分公司");
        when(customerDeptPermissionService.getCustomerAppAvailableDeptList(1L))
                .thenReturn(Arrays.asList(dept1, dept2));

        CustomerAssistantPrincipal principal = contextService.current();

        assertEquals(1L, principal.getTenantId());
        assertEquals(286L, principal.getMemberUserId());
        assertEquals(1L, principal.getCustomerId());
        assertEquals("测试", principal.getCustomerName());
        assertEquals(3, principal.getPriceLevel());
        assertTrue(principal.isPriceVisible());
        assertTrue(principal.isOrderEnabled());
        assertEquals(new java.util.LinkedHashSet<>(Arrays.asList(10L, 20L)), principal.getDepartmentIds());
    }

    @Test
    void current_unboundOrInvalidCustomerRejected() {
        when(customerMemberApi.getCustomerMemberAuth(286L)).thenReturn(
                ErpCustomerMemberAuthRespDTO.unauthorized().setMemberUserId(286L));

        AssistantFailure failure = assertThrows(AssistantFailure.class, contextService::current);

        assertEquals("CUSTOMER_UNBOUND", failure.getCode());
        verify(customerDeptPermissionService, never()).getCustomerAppAvailableDeptList(1L);
    }

    @Test
    void current_noAvailableDepartmentRejected() {
        when(customerMemberApi.getCustomerMemberAuth(286L)).thenReturn(new ErpCustomerMemberAuthRespDTO()
                .setAuthorized(true).setCustomerId(1L).setCustomerName("测试").setPriceLevel(3)
                .setMemberUserId(286L).setPriceVisible(true).setOrderEnabled(true));
        when(customerDeptPermissionService.getCustomerAppAvailableDeptList(1L)).thenReturn(Collections.emptyList());

        AssistantFailure failure = assertThrows(AssistantFailure.class, contextService::current);

        assertEquals("NO_AVAILABLE_DEPARTMENT", failure.getCode());
    }

    @Test
    void current_disabledFeatureStopsBeforeAuthorization() {
        properties.getCustomerApp().setEnabled(false);

        AssistantFailure failure = assertThrows(AssistantFailure.class, contextService::current);

        assertEquals("DISABLED", failure.getCode());
        verify(customerMemberApi, never()).getCustomerMemberAuth(286L);
    }

    private DeptRespDTO dept(Long id, String name) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        return dept;
    }

}
