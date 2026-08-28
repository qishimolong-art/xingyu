package cn.iocoder.yudao.module.erp.controller.app.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.api.sale.ErpCustomerMemberApi;
import cn.iocoder.yudao.module.erp.api.sale.dto.ErpCustomerMemberAuthRespDTO;
import cn.iocoder.yudao.module.erp.controller.app.sale.vo.customermember.AppErpCustomerMemberAuthRespVO;
import cn.iocoder.yudao.module.erp.controller.app.sale.vo.customermember.AppErpCustomerMemberDeptRespVO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@link AppErpCustomerMemberController} unit tests.
 */
public class AppErpCustomerMemberControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppErpCustomerMemberController controller;

    @Mock
    private ErpCustomerMemberApi customerMemberApi;
    @Mock
    private ErpCustomerDeptPermissionService customerDeptPermissionService;

    @Test
    void getCustomerMemberAuthStatus_authorized() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(new ErpCustomerMemberAuthRespDTO()
                    .setAuthorized(true)
                    .setCustomerId(10L)
                    .setCustomerName("测试客户")
                    .setMemberUserId(20L)
                    .setMobile("13800000000")
                    .setPriceVisible(true)
                    .setOrderEnabled(true));

            CommonResult<AppErpCustomerMemberAuthRespVO> result = controller.getCustomerMemberAuthStatus();

            assertEquals(0, result.getCode());
            assertTrue(result.getData().getAuthorized());
            assertTrue(result.getData().getPriceVisible());
            assertTrue(result.getData().getOrderEnabled());
            assertEquals(10L, result.getData().getCustomerId());
            assertEquals("测试客户", result.getData().getCustomerName());
            assertEquals(20L, result.getData().getMemberUserId());
            assertEquals("13800000000", result.getData().getMobile());
        }
    }

    @Test
    void getCustomerMemberAuthStatus_notAuthorized() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(
                    ErpCustomerMemberAuthRespDTO.unauthorized().setMemberUserId(20L));

            CommonResult<AppErpCustomerMemberAuthRespVO> result = controller.getCustomerMemberAuthStatus();

            assertEquals(0, result.getCode());
            assertFalse(result.getData().getAuthorized());
            assertFalse(result.getData().getPriceVisible());
            assertFalse(result.getData().getOrderEnabled());
            assertEquals(20L, result.getData().getMemberUserId());
        }
    }

    @Test
    void getCustomerMemberDeptSimpleList_authorized() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(new ErpCustomerMemberAuthRespDTO()
                    .setAuthorized(true)
                    .setCustomerId(10L));
            when(customerDeptPermissionService.getCustomerAppAvailableDeptList(eq(10L))).thenReturn(Arrays.asList(
                    dept(100L, "成都主城区销售账套"),
                    dept(200L, "德阳销售账套")));

            CommonResult<List<AppErpCustomerMemberDeptRespVO>> result = controller.getCustomerMemberDeptSimpleList();

            assertEquals(0, result.getCode());
            assertEquals(2, result.getData().size());
            assertEquals(100L, result.getData().get(0).getId());
            assertEquals("成都主城区销售账套", result.getData().get(0).getName());
            assertEquals("028-88888888", result.getData().get(0).getPhone());
        }
    }

    @Test
    void getCustomerMemberDeptSimpleList_notAuthorizedReturnsEmptyList() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
            when(customerMemberApi.getCustomerMemberAuth(eq(20L))).thenReturn(
                    ErpCustomerMemberAuthRespDTO.unauthorized().setMemberUserId(20L));

            CommonResult<List<AppErpCustomerMemberDeptRespVO>> result = controller.getCustomerMemberDeptSimpleList();

            assertEquals(0, result.getCode());
            assertEquals(Collections.emptyList(), result.getData());
        }
    }

    private DeptRespDTO dept(Long id, String name) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(0L);
        dept.setPhone("028-88888888");
        return dept;
    }

}
