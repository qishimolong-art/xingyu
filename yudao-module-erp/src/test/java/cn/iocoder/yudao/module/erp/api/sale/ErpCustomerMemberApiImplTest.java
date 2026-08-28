package cn.iocoder.yudao.module.erp.api.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerMemberDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerMemberService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_AUTH_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_MEMBER_DEPT_REQUIRED;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerMemberApiImpl} unit tests.
 */
public class ErpCustomerMemberApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerMemberApiImpl customerMemberApi;

    @Mock
    private ErpCustomerMemberService customerMemberService;
    @Mock
    private ErpCustomerService customerService;

    @Test
    void getCustomerMemberAuth_memberUserIdNull() {
        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.getCustomerMemberAuth(null);

        assertFalse(auth.getAuthorized());
        assertFalse(auth.getPriceVisible());
        assertFalse(auth.getOrderEnabled());
        assertNull(auth.getMemberUserId());
        verify(customerMemberService, never()).getEnabledCustomerMemberByMemberUserId(null);
    }

    @Test
    void getCustomerMemberAuth_notBound() {
        when(customerMemberService.getEnabledCustomerMemberByMemberUserId(eq(20L))).thenReturn(null);

        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.getCustomerMemberAuth(20L);

        assertFalse(auth.getAuthorized());
        assertFalse(auth.getPriceVisible());
        assertFalse(auth.getOrderEnabled());
        assertEquals(20L, auth.getMemberUserId());
        verify(customerService, never()).getCustomer(eq(10L));
    }

    @Test
    void getCustomerMemberAuth_authorized() {
        when(customerMemberService.getEnabledCustomerMemberByMemberUserId(eq(20L))).thenReturn(
                new ErpCustomerMemberDO().setId(1L).setCustomerId(10L).setMemberUserId(20L).setMobile("13800000000"));
        when(customerService.getCustomerSaleDeptIdsIgnoreDataPermission(eq(10L))).thenReturn(asList(1L, 2L));
        when(customerService.getCustomer(eq(10L))).thenReturn(new ErpCustomerDO().setId(10L).setName("测试客户"));

        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.getCustomerMemberAuth(20L);

        assertTrue(auth.getAuthorized());
        assertTrue(auth.getPriceVisible());
        assertTrue(auth.getOrderEnabled());
        assertEquals(1L, auth.getId());
        assertEquals(10L, auth.getCustomerId());
        assertEquals("测试客户", auth.getCustomerName());
        assertEquals(20L, auth.getMemberUserId());
        assertEquals("13800000000", auth.getMobile());
    }

    @Test
    void getCustomerMemberAuth_customerInvalid() {
        when(customerMemberService.getEnabledCustomerMemberByMemberUserId(eq(20L))).thenReturn(
                new ErpCustomerMemberDO().setId(1L).setCustomerId(10L).setMemberUserId(20L).setMobile("13800000000"));
        when(customerService.getCustomerSaleDeptIdsIgnoreDataPermission(eq(10L))).thenThrow(exception(CUSTOMER_NOT_EXISTS));

        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.getCustomerMemberAuth(20L);

        assertFalse(auth.getAuthorized());
        assertFalse(auth.getPriceVisible());
        assertFalse(auth.getOrderEnabled());
        assertEquals(20L, auth.getMemberUserId());
        verify(customerService, never()).getCustomer(eq(10L));
    }

    @Test
    void validateCustomerMemberAuth_notAuthorized() {
        when(customerMemberService.getEnabledCustomerMemberByMemberUserId(eq(20L))).thenReturn(null);

        assertServiceException(() -> customerMemberApi.validateCustomerMemberAuth(20L), CUSTOMER_MEMBER_AUTH_REQUIRED);
    }

    @Test
    void validateCustomerMemberAuth_deptRequired() {
        when(customerMemberService.getEnabledCustomerMemberByMemberUserId(eq(20L))).thenReturn(
                new ErpCustomerMemberDO().setId(1L).setCustomerId(10L).setMemberUserId(20L));
        when(customerService.getCustomerSaleDeptIdsIgnoreDataPermission(eq(10L))).thenReturn(asList(1L, 2L));

        assertServiceException(() -> customerMemberApi.validateCustomerMemberAuth(20L, null),
                CUSTOMER_MEMBER_DEPT_REQUIRED);
    }

    @Test
    void validateCustomerMemberAuth_deptNotAllowed() {
        when(customerMemberService.getEnabledCustomerMemberByMemberUserId(eq(20L))).thenReturn(
                new ErpCustomerMemberDO().setId(1L).setCustomerId(10L).setMemberUserId(20L));
        when(customerService.getCustomerSaleDeptIdsIgnoreDataPermission(eq(10L))).thenReturn(asList(1L, 2L));

        assertServiceException(() -> customerMemberApi.validateCustomerMemberAuth(20L, 3L),
                CUSTOMER_MEMBER_DEPT_NOT_ALLOWED);
    }

    @Test
    void validateCustomerMemberAuth_deptAllowed() {
        when(customerMemberService.getEnabledCustomerMemberByMemberUserId(eq(20L))).thenReturn(
                new ErpCustomerMemberDO().setId(1L).setCustomerId(10L).setMemberUserId(20L));
        when(customerService.getCustomerSaleDeptIdsIgnoreDataPermission(eq(10L))).thenReturn(asList(1L, 2L));

        ErpCustomerMemberAuthRespDTO auth = customerMemberApi.validateCustomerMemberAuth(20L, 2L);

        assertTrue(auth.getAuthorized());
        assertEquals(10L, auth.getCustomerId());
    }

    @Test
    void isCustomerMemberAuthorized_authorized() {
        when(customerMemberService.getEnabledCustomerMemberByMemberUserId(eq(20L))).thenReturn(
                new ErpCustomerMemberDO().setId(1L).setCustomerId(10L).setMemberUserId(20L));
        when(customerService.getCustomerSaleDeptIdsIgnoreDataPermission(eq(10L))).thenReturn(asList(1L, 2L));

        assertTrue(customerMemberApi.isCustomerMemberAuthorized(20L));
    }

}
