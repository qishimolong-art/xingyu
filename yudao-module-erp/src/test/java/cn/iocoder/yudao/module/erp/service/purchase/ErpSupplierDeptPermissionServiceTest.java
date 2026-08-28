package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
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
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpSupplierDeptPermissionServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSupplierDeptPermissionService supplierDeptPermissionService;

    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;

    @Test
    void getAvailableDeptSimpleList_selfPermissionUsesLoginUserDepartments() {
        Long supplierId = 23L;
        Long loginUserId = 17L;
        ErpSupplierDO supplier = new ErpSupplierDO().setId(supplierId).setDeptId(1L);
        when(supplierService.validateSupplier(eq(supplierId))).thenReturn(supplier);
        when(supplierService.getSupplierDeptMap(eq(Collections.singleton(supplierId))))
                .thenReturn(Collections.singletonMap(supplierId, Arrays.asList(1L, 2L)));
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setSelf(true);
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("erp_purchase_order"))).thenReturn(permission);
        when(permissionApi.getDeptIdsByUserId(eq(loginUserId)))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(2L)));
        when(deptApi.getDeptList(eq(new LinkedHashSet<>(Collections.singletonList(2L)))))
                .thenReturn(Collections.singletonList(buildDept(2L, "甘孜分公司")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = supplierDeptPermissionService.getAvailableDeptSimpleList(
                    supplierId, "erp_purchase_order");

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getId());
            assertEquals("甘孜分公司", result.get(0).getName());
        }
        verify(permissionApi).getDeptIdsByUserId(eq(loginUserId));
    }

    private DeptRespDTO buildDept(Long id, String name) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(0L);
        dept.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return dept;
    }

}
