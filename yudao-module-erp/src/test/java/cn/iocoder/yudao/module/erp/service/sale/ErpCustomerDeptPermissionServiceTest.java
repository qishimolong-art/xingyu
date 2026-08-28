package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ErpCustomerDeptPermissionServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerDeptPermissionService customerDeptPermissionService;

    @Mock
    private ErpCustomerService customerService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;

    @Test
    void getAvailableDeptSimpleList_intersectsCustomerDeptAndDataPermission() {
        when(customerService.getCustomerSaleDeptIds(10L)).thenReturn(Arrays.asList(100L, 200L, 300L));
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setDeptIds(new HashSet<>(Arrays.asList(200L, 300L, 400L)));
        when(permissionApi.getDeptDataPermission(7L, "erp_finance_receipt")).thenReturn(permission);
        when(deptApi.getDeptList(new HashSet<>(Arrays.asList(200L, 300L)))).thenReturn(Arrays.asList(
                dept(200L, "A", CommonStatusEnum.ENABLE.getStatus()),
                dept(300L, "B", CommonStatusEnum.DISABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);

            List<DeptSimpleRespVO> result = customerDeptPermissionService.getAvailableDeptSimpleList(
                    10L, "erp_finance_receipt");

            assertThat(result).extracting(DeptSimpleRespVO::getId).containsExactly(200L);
        }
    }

    @Test
    void hasAvailableDept_returnsFalseWhenPermissionHasNoIntersection() {
        when(customerService.getCustomerSaleDeptIds(10L)).thenReturn(Collections.singletonList(100L));
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setDeptIds(Collections.singleton(200L));
        when(permissionApi.getDeptDataPermission(7L, "erp_finance_receipt")).thenReturn(permission);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);

            assertThat(customerDeptPermissionService.hasAvailableDept(10L, 100L, "erp_finance_receipt")).isFalse();
        }
    }

    @Test
    void getAvailableDeptSimpleList_allPermissionKeepsEnabledCustomerDepartments() {
        when(customerService.getCustomerSaleDeptIds(10L)).thenReturn(Arrays.asList(100L, 200L));
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setAll(true);
        when(permissionApi.getDeptDataPermission(7L, "erp_finance_receipt")).thenReturn(permission);
        when(deptApi.getDeptList(new HashSet<>(Arrays.asList(100L, 200L)))).thenReturn(Arrays.asList(
                dept(100L, "A", CommonStatusEnum.ENABLE.getStatus()),
                dept(200L, "B", CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);

            List<DeptSimpleRespVO> result = customerDeptPermissionService.getAvailableDeptSimpleList(
                    10L, "erp_finance_receipt");

            assertThat(result).extracting(DeptSimpleRespVO::getId).containsExactly(100L, 200L);
        }
    }

    @Test
    void getCustomerAppAvailableDeptList_keepsEnabledCustomerDepartmentsWithoutAdminPermission() {
        when(customerService.getCustomerSaleDeptIdsIgnoreDataPermission(10L))
                .thenReturn(Arrays.asList(100L, 200L, 300L));
        when(deptApi.getDeptList(new LinkedHashSet<>(Arrays.asList(100L, 200L, 300L)))).thenReturn(Arrays.asList(
                dept(100L, "A", CommonStatusEnum.ENABLE.getStatus()),
                dept(200L, "B", CommonStatusEnum.DISABLE.getStatus()),
                dept(300L, "C", CommonStatusEnum.ENABLE.getStatus())));

        List<DeptRespDTO> result = customerDeptPermissionService.getCustomerAppAvailableDeptList(10L);

        assertThat(result).extracting(DeptRespDTO::getId).containsExactly(100L, 300L);
    }

    private DeptRespDTO dept(Long id, String name, Integer status) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(0L);
        dept.setStatus(status);
        return dept;
    }

}
