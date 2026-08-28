package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpSaleItemBatchUpdateSupportTest extends BaseMockitoUnitTest {

    private static final String MODULE = "erp_sale_quote";
    private static final ErrorCode ERROR_CODE = new ErrorCode(1_000_000, "error");

    @InjectMocks
    private ErpSaleItemBatchUpdateSupport batchUpdateSupport;

    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DeptApi deptApi;

    @Test
    void getWarehouseAvailableDeptSimpleList_keepsOwnerDeptAndFiltersDistributedDeptByPermission() {
        Long warehouseId = 8L;
        Long ownerDeptId = 10L;
        Long allowedDistributedDeptId = 20L;
        Long deniedDistributedDeptId = 30L;
        Long disabledDistributedDeptId = 40L;
        Long missingDistributedDeptId = 50L;
        Long loginUserId = 104L;
        when(warehouseService.validSaleWarehouseList(eq(Collections.singleton(warehouseId))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(warehouseId).setDeptId(ownerDeptId)));
        when(warehouseService.getWarehouseSaleDeptIds(eq(warehouseId)))
                .thenReturn(new LinkedHashSet<>(Arrays.asList(allowedDistributedDeptId, deniedDistributedDeptId,
                        disabledDistributedDeptId, missingDistributedDeptId)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq(MODULE)))
                .thenReturn(buildDeptPermission(allowedDistributedDeptId, disabledDistributedDeptId,
                        missingDistributedDeptId));
        when(deptApi.getDeptList(anyCollection()))
                .thenReturn(Arrays.asList(
                        buildDept(ownerDeptId, "兴宇路通"),
                        buildDept(allowedDistributedDeptId, "甘孜分公司"),
                        buildDept(disabledDistributedDeptId, "停用部门", CommonStatusEnum.DISABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = batchUpdateSupport.getWarehouseAvailableDeptSimpleList(
                    warehouseId, MODULE);

            assertIterableEquals(Arrays.asList(ownerDeptId, allowedDistributedDeptId), getDeptIds(result));
            assertEquals("兴宇路通", result.get(0).getName());
            assertEquals("甘孜分公司", result.get(1).getName());
            verify(warehouseService).getWarehouseSaleDeptIds(eq(warehouseId));
        }
    }

    @Test
    void resolveTargetDeptId_allowsOwnerDeptWithoutDeptDataPermission() {
        Long warehouseId = 8L;
        Long ownerDeptId = 10L;
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(warehouseId).setDeptId(ownerDeptId);
        when(warehouseService.getWarehouseSaleDeptIds(eq(warehouseId))).thenReturn(Collections.emptySet());
        when(deptApi.getDeptList(anyCollection()))
                .thenReturn(Collections.singletonList(buildDept(ownerDeptId, "兴宇路通")));

        Long result = batchUpdateSupport.resolveTargetDeptId(warehouse, ownerDeptId, MODULE, ERROR_CODE);

        assertEquals(ownerDeptId, result);
        verify(permissionApi, never()).getDeptDataPermission(any(), any());
    }

    @Test
    void validateWarehouseDept_allowsOwnerDeptWhenWarehouseSelectableWithoutDeptDataPermission() {
        Long warehouseId = 8L;
        Long ownerDeptId = 10L;
        when(warehouseService.validSaleWarehouseList(eq(Collections.singleton(warehouseId))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(warehouseId).setDeptId(ownerDeptId)));
        when(deptApi.getDeptList(anyCollection()))
                .thenReturn(Collections.singletonList(buildDept(ownerDeptId, "兴宇路通")));

        assertDoesNotThrow(() -> batchUpdateSupport.validateWarehouseDept(warehouseId, ownerDeptId,
                MODULE, ERROR_CODE));

        verify(permissionApi, never()).getDeptDataPermission(any(), any());
        verify(warehouseService, never()).validateWarehouseSaleAllowedForDept(any(), any());
    }

    @Test
    void validateWarehouseDept_rejectsNonOwnerDeptWithoutDeptDataPermission() {
        Long warehouseId = 8L;
        Long ownerDeptId = 10L;
        Long targetDeptId = 20L;
        Long loginUserId = 104L;
        when(warehouseService.validSaleWarehouseList(eq(Collections.singleton(warehouseId))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(warehouseId).setDeptId(ownerDeptId)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq(MODULE)))
                .thenReturn(buildDeptPermission(ownerDeptId));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> batchUpdateSupport.validateWarehouseDept(warehouseId, targetDeptId, MODULE, ERROR_CODE));

            assertEquals(ERROR_CODE.getCode(), ex.getCode());
            verify(warehouseService, never()).validateWarehouseSaleAllowedForDept(any(), any());
        }
    }

    private DeptRespDTO buildDept(Long id, String name) {
        return buildDept(id, name, CommonStatusEnum.ENABLE.getStatus());
    }

    private DeptRespDTO buildDept(Long id, String name, Integer status) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(0L);
        dept.setStatus(status);
        return dept;
    }

    private DeptDataPermissionRespDTO buildDeptPermission(Long... deptIds) {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setDeptIds(new LinkedHashSet<>(Arrays.asList(deptIds)));
        return permission;
    }

    private List<Long> getDeptIds(List<DeptSimpleRespVO> depts) {
        return depts.stream().map(DeptSimpleRespVO::getId).collect(Collectors.toList());
    }

}
