package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockItemBatchUpdateSupportTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockItemBatchUpdateSupport batchUpdateSupport;

    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DeptApi deptApi;

    @Test
    void getWarehouseAvailableDeptSimpleList_onlyUsesWarehouseOwnerDept() {
        Long warehouseId = 8L;
        Long ownerDeptId = 10L;
        Long loginUserId = 104L;
        when(warehouseService.validWarehouseList(eq(Collections.singleton(warehouseId))))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(warehouseId).setDeptId(ownerDeptId)));
        when(permissionApi.getDeptDataPermission(eq(loginUserId), eq("erp_stock_in")))
                .thenReturn(buildAllDeptPermission());
        when(deptApi.getDeptList(anyCollection()))
                .thenReturn(Collections.singletonList(buildDept(ownerDeptId, "甘孜分公司")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            List<DeptSimpleRespVO> result = batchUpdateSupport.getWarehouseAvailableDeptSimpleList(
                    warehouseId, "erp_stock_in");

            assertEquals(1, result.size());
            assertEquals(ownerDeptId, result.get(0).getId());
            assertEquals("甘孜分公司", result.get(0).getName());
            verify(warehouseService).validateCurrentUserWarehousePermission(eq(Collections.singleton(warehouseId)));
            verify(warehouseService, never()).getWarehouseSaleDeptIds(any());
        }
    }

    private DeptDataPermissionRespDTO buildAllDeptPermission() {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setAll(true);
        return permission;
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
