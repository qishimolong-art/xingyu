package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssistantToolPermissionServiceTest {
    private AssistantToolPermissionService service;
    private PermissionApi permission;

    @BeforeEach
    void setUp() {
        service=new AssistantToolPermissionService();permission=mock(PermissionApi.class);
        ReflectionTestUtils.setField(service,"permissions",permission);
        LoginUser user=new LoginUser();user.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        when(permission.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);
    }

    @AfterEach
    void clear() {TenantContextHolder.clear();SecurityContextHolder.clearContext();}

    @Test
    void semanticSchemaRemovesHiddenFieldsAndRejectsTheirUse() {
        when(permission.getCurrentUserHiddenFields("erp_purchase_order")).thenReturn(Collections.singletonList("col_totalPrice"));
        Map<String,String> fields=new LinkedHashMap<>();fields.put("id","订单标识");fields.put("total_price","订单金额");
        assertEquals(Collections.singletonMap("id","订单标识"),service.visibleFields("erp:purchase-order:query","erp_purchase_order",fields));
        assertDoesNotThrow(()->service.authorize("erp:purchase-order:query","erp_purchase_order",Collections.singleton("id")));
        assertEquals("FIELD_FORBIDDEN",assertThrows(AssistantFailure.class,
                ()->service.authorize("erp:purchase-order:query","erp_purchase_order",Collections.singleton("total_price"))).getCode());
    }

    @Test
    void toolPermissionRequiresAssistantEntryAndBusinessPermission() {
        when(permission.getCurrentUserHiddenFields("erp_purchase_order")).thenReturn(Collections.emptyList());
        when(permission.hasAnyPermissions(101L,"erp:purchase-order:query")).thenReturn(false);
        assertEquals("FORBIDDEN",assertThrows(AssistantFailure.class,
                ()->service.authorize("erp:purchase-order:query","erp_purchase_order",Collections.emptySet())).getCode());
    }
}
