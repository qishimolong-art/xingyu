package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveApprovePermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveOperationPermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_LEGACY_READ_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockMoveControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockMoveController controller;

    @Mock
    private ErpStockMoveService stockMoveService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;

    @Test
    void createStockMove_legacyEntryRejected() {
        ErpStockMoveSaveReqVO reqVO = new ErpStockMoveSaveReqVO();

        assertServiceException(() -> controller.createStockMove(reqVO), STOCK_MOVE_LEGACY_READ_ONLY);

        verify(stockMoveService, never()).createStockMove(reqVO);
    }

    @Test
    void updateStockMoveStatus_legacyEntryRejected() {
        assertServiceException(() -> controller.updateStockMoveStatus(10L, 20), STOCK_MOVE_LEGACY_READ_ONLY);

        verify(stockMoveService, never()).updateStockMoveStatus(10L, 20);
    }

    @Test
    void buildStockMoveVOPageResult_batchesApprovePermissionAndAllDepartmentNames() {
        List<ErpStockMoveDO> stockMoves = Arrays.asList(
                new ErpStockMoveDO().setId(1L).setDeptId(10L).setFromDeptId(20L).setToDeptId(30L),
                new ErpStockMoveDO().setId(2L).setDeptId(11L).setFromDeptId(21L).setToDeptId(31L));
        List<ErpStockMoveItemDO> items = Arrays.asList(
                new ErpStockMoveItemDO().setMoveId(1L).setProductId(101L)
                        .setFromWarehouseId(1001L).setToWarehouseId(1002L)
                        .setFromDeptId(20L).setToDeptId(30L),
                new ErpStockMoveItemDO().setMoveId(2L).setProductId(102L)
                        .setFromWarehouseId(1003L).setToWarehouseId(1004L)
                        .setFromDeptId(21L).setToDeptId(31L));
        when(stockMoveService.getStockMoveItemListByMoveIds(anyCollection())).thenReturn(items);
        when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.singletonMap(
                101L, new ErpProductRespVO().setId(101L).setName("product-101")));
        Map<Long, ErpWarehouseDO> warehouseMap = new HashMap<>();
        warehouseMap.put(1001L, new ErpWarehouseDO().setId(1001L).setName("warehouse-1001").setDeptId(40L));
        warehouseMap.put(1002L, new ErpWarehouseDO().setId(1002L).setName("warehouse-1002").setDeptId(50L));
        warehouseMap.put(1003L, new ErpWarehouseDO().setId(1003L).setName("warehouse-1003").setDeptId(41L));
        warehouseMap.put(1004L, new ErpWarehouseDO().setId(1004L).setName("warehouse-1004").setDeptId(51L));
        when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(warehouseMap);
        when(deptApi.getDeptMap(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> deptIds = invocation.getArgument(0);
            Map<Long, DeptRespDTO> result = new HashMap<>();
            deptIds.forEach(deptId -> result.put(deptId, dept(deptId, "dept-" + deptId)));
            return result;
        });
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        Map<Long, ErpStockMoveApprovePermission> approvePermissionMap = new HashMap<>();
        approvePermissionMap.put(1L, ErpStockMoveApprovePermission.allowed());
        approvePermissionMap.put(2L, ErpStockMoveApprovePermission.denied("denied"));
        ErpStockTransferOutPermissionScope permissionScope = new ErpStockTransferOutPermissionScope(
                false, Arrays.asList(20L, 21L));
        when(stockMoveService.getApprovePermissionMap(
                eq(stockMoves), anyMap(), eq(permissionScope))).thenReturn(approvePermissionMap);
        when(stockMoveService.getDeletePermission(any(), anyList()))
                .thenReturn(ErpStockMoveOperationPermission.allowed());

        PageResult<ErpStockMoveRespVO> result = controller.buildStockMoveVOPageResult(
                new PageResult<>(stockMoves, 2L), "erp_stock_transfer_out", permissionScope);

        assertEquals(2, result.getList().size());
        assertEquals("dept-20", result.getList().get(0).getFromDeptName());
        assertEquals("dept-40", result.getList().get(0).getItems().get(0).getFromWarehouseDeptName());
        assertTrue(result.getList().get(0).getApproveAllowed());
        assertFalse(result.getList().get(1).getApproveAllowed());
        verify(stockMoveService, times(1)).getApprovePermissionMap(
                eq(stockMoves), anyMap(), eq(permissionScope));
        verify(stockMoveService, never()).getApprovePermissionMap(anyList(), anyMap());
        verify(stockMoveService, never()).getApprovePermission(any(), anyList());
        verify(deptApi, times(1)).getDeptMap(argThat(deptIds -> deptIds.containsAll(Arrays.asList(
                10L, 11L, 20L, 21L, 30L, 31L, 40L, 41L, 50L, 51L))));
        verify(deptApi, never()).getDept(any());
    }

    private DeptRespDTO dept(Long id, String name) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        return dept;
    }

    @Test
    void fillUnlockCartPermission_copiesAllowedStateAndReasonToDetailResponse() {
        ErpStockMoveRespVO response = new ErpStockMoveRespVO();
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setId(10L);
        ErpStockMoveItemDO item = new ErpStockMoveItemDO().setMoveId(10L);
        when(stockMoveService.getUnlockCartPermission(stockMove, Collections.singletonList(item)))
                .thenReturn(ErpStockMoveOperationPermission.denied("当前单据不能解锁"));

        ReflectionTestUtils.invokeMethod(controller, "fillUnlockCartPermission", response,
                stockMove, Collections.singletonList(item));

        assertFalse(response.getUnlockCartAllowed());
        assertEquals("当前单据不能解锁", response.getUnlockCartDisabledReason());
    }

}
