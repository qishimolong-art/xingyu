package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserBatchUpdateReqVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpUserWarehousePermissionBatchUpdateExtensionTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpUserWarehousePermissionBatchUpdateExtension extension;

    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private PermissionApi permissionApi;

    @Test
    void testValidate_noPermission_throwsForbidden() {
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setUpdateWarehousePermissions(true)
                .setWarehousePermissionMode(UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_ADD)
                .setWarehouseIds(asSet(1L));
        when(permissionApi.hasAnyPermissions(eq(104L), eq("erp:warehouse-permission:update"))).thenReturn(false);

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> extension.validate(reqVO, asSet(288L)));

            assertEquals(403, ex.getCode());
            verify(warehouseService, never()).validWarehouseList(any());
        }
    }

    @Test
    void testValidate_success_normalizesWarehouseIds() {
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setUpdateWarehousePermissions(true)
                .setWarehousePermissionMode(UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_ADD)
                .setWarehouseIds(asSet(1L, 2L, null));
        when(permissionApi.hasAnyPermissions(eq(104L), eq("erp:warehouse-permission:update"))).thenReturn(true);
        when(warehouseService.getAssignableWarehouseList()).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(1L),
                new ErpWarehouseDO().setId(2L)));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            extension.validate(reqVO, asSet(288L));

            assertEquals(asSet(1L, 2L), reqVO.getWarehouseIds());
            verify(warehouseService).validWarehouseList(eq(asSet(1L, 2L)));
        }
    }

    @Test
    void testUpdate_replace() {
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setUpdateWarehousePermissions(true)
                .setWarehousePermissionMode(UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_REPLACE)
                .setWarehouseIds(asSet(4L, 5L));

        extension.update(288L, reqVO);

        verify(warehouseService).updateUserWarehousePermissions(eq(288L), eq(asSet(4L, 5L)));
    }

    @Test
    void testUpdate_add() {
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setUpdateWarehousePermissions(true)
                .setWarehousePermissionMode(UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_ADD)
                .setWarehouseIds(asSet(2L, 3L));
        when(warehouseService.getUserWarehouseIds(eq(288L))).thenReturn(Arrays.asList(1L, 2L));

        extension.update(288L, reqVO);

        assertUpdatedWarehouseIds(Arrays.asList(1L, 2L, 3L));
    }

    @Test
    void testUpdate_remove() {
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setUpdateWarehousePermissions(true)
                .setWarehousePermissionMode(UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_REMOVE)
                .setWarehouseIds(asSet(2L));
        when(warehouseService.getUserWarehouseIds(eq(288L))).thenReturn(Arrays.asList(1L, 2L, 3L));

        extension.update(288L, reqVO);

        assertUpdatedWarehouseIds(Arrays.asList(1L, 3L));
    }

    @Test
    void testUpdate_notEnabled_noop() {
        extension.update(288L, new UserBatchUpdateReqVO().setUpdateWarehousePermissions(false));

        verify(warehouseService, never()).updateUserWarehousePermissions(any(), any());
    }

    private void assertUpdatedWarehouseIds(List<Long> expectedWarehouseIds) {
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(warehouseService).updateUserWarehousePermissions(eq(288L), captor.capture());
        assertEquals(expectedWarehouseIds, captor.getValue().stream().collect(java.util.stream.Collectors.toList()));
    }

}
