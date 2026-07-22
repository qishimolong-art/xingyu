package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveApprovePermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveOperationPermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_DELETE_CART_SOURCE_DENIED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_APPROVE_DEPT_PERMISSION_DENIED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_TRANSFER_IN_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_MOVE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockMoveServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockMoveServiceImpl stockMoveService;

    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpStockMoveMapper stockMoveMapper;
    @Mock
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockLockService stockLockService;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void createStockMoveDraft_saleCartCrossDept_skipsTargetWarehousePermission() {
        ErpStockMoveSaveReqVO request = stockMoveRequest(128L, ErpSaleBizSourceTypeEnum.CART.getType());
        mockCreateStockMoveDependencies(148L, 148L);
        mockSaleWarehouseValidation(148L);

        stockMoveService.createStockMoveDraft(request);

        verify(warehouseService, never()).validateCurrentUserWarehousePermission(anyCollection());
        verify(warehouseService).validateWarehouseSaleAllowedForDept(1L, 128L);
        ArgumentCaptor<ErpStockMoveDO> moveCaptor = ArgumentCaptor.forClass(ErpStockMoveDO.class);
        verify(stockMoveMapper, times(2)).insert(moveCaptor.capture());
        ErpStockMoveDO transferIn = moveCaptor.getAllValues().stream()
                .filter(move -> Integer.valueOf(20).equals(move.getTransferDirection()))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), transferIn.getStatus());
        assertEquals(Long.valueOf(100L), transferIn.getRelatedMoveId());
        assertEquals("STO-TEST-001", transferIn.getRelatedMoveNo());
        verify(stockMoveItemMapper, times(2)).insertBatch(any());
        verify(stockService).ensureStockExists(955L, 12L);
    }

    @Test
    void createStockMoveDraft_saleCartSameDept_keepsTargetWarehousePermission() {
        ErpStockMoveSaveReqVO request = stockMoveRequest(128L, ErpSaleBizSourceTypeEnum.CART.getType());
        mockCreateStockMoveDependencies(128L, 148L);
        mockSaleWarehouseValidation(128L);

        stockMoveService.createStockMoveDraft(request);

        verify(warehouseService).validateCurrentUserWarehousePermission(
                org.mockito.ArgumentMatchers.<Collection<Long>>argThat(warehouseIds -> warehouseIds.contains(12L)));
    }

    @Test
    void createStockMoveDraft_manualMove_keepsSourceAndTargetWarehousePermissions() {
        ErpStockMoveSaveReqVO request = stockMoveRequest(128L, null);
        mockCreateStockMoveDependencies(128L, 148L);

        stockMoveService.createStockMoveDraft(request);

        verify(warehouseService).validateCurrentUserStockMoveFromWarehousePermission(
                org.mockito.ArgumentMatchers.<Collection<Long>>argThat(warehouseIds -> warehouseIds.contains(1L)));
        verify(warehouseService).validateCurrentUserWarehousePermission(
                org.mockito.ArgumentMatchers.<Collection<Long>>argThat(warehouseIds -> warehouseIds.contains(12L)));
    }

    @Test
    void createStockMove_warehouseDepartmentsOverrideRequestSnapshots() {
        ErpStockMoveSaveReqVO request = stockMoveRequest(100L, null)
                .setFromDeptId(999L)
                .setToDeptId(998L);
        request.getItems().get(0).setFromDeptId(997L).setToDeptId(996L);
        mockCreateStockMoveDependencies(200L, 300L);

        stockMoveService.createStockMove(request);

        verify(stockMoveMapper).insert(org.mockito.ArgumentMatchers.<ErpStockMoveDO>argThat(stockMove ->
                Integer.valueOf(10).equals(stockMove.getTransferDirection())
                        && Long.valueOf(200L).equals(stockMove.getFromDeptId())
                        && Long.valueOf(300L).equals(stockMove.getToDeptId())));
        verify(stockMoveItemMapper).insertBatch(org.mockito.ArgumentMatchers.<List<ErpStockMoveItemDO>>argThat(items ->
                items.size() == 1
                        && Long.valueOf(100L).equals(items.get(0).getMoveId())
                        && Long.valueOf(200L).equals(items.get(0).getFromDeptId())
                        && Long.valueOf(300L).equals(items.get(0).getToDeptId())));
    }

    @Test
    void getApprovePermission_saleCartCrossDept_deniedForSameBranchDept() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L);
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L).setFromDeptId(200L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 100L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockMoveApprovePermission permission = stockMoveService.getApprovePermission(stockMove,
                    Collections.singletonList(item));

            assertFalse(permission.getApproveAllowed());
        }
    }

    @Test
    void getApprovePermission_saleCartCrossDept_allDataScopeAllowed() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L);
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(true, false));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockMoveApprovePermission permission = stockMoveService.getApprovePermission(stockMove,
                    Collections.singletonList(item));

            assertTrue(permission.getApproveAllowed());
        }
    }

    @Test
    void getApprovePermission_normalStockMove_allowed() {
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setDeptId(100L).setFromDeptId(200L).setSourceType(null);
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L).setFromDeptId(200L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 200L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockMoveApprovePermission permission = stockMoveService.getApprovePermission(stockMove,
                    Collections.singletonList(item));

            assertTrue(permission.getApproveAllowed());
        }
    }

    @Test
    void getApprovePermission_fromDepartmentInScope_allowed() {
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setDeptId(100L).setFromDeptId(200L);
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L).setFromDeptId(200L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 200L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockMoveApprovePermission permission = stockMoveService.getApprovePermission(
                    stockMove, Collections.singletonList(item));

            assertTrue(permission.getApproveAllowed());
        }
    }

    @Test
    void getApprovePermission_onlyOwnerDepartmentInScope_denied() {
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setDeptId(100L).setFromDeptId(200L);
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L).setFromDeptId(200L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 100L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockMoveApprovePermission permission = stockMoveService.getApprovePermission(
                    stockMove, Collections.singletonList(item));

            assertFalse(permission.getApproveAllowed());
        }
    }

    @Test
    void getApprovePermission_partialMultipleFromDepartmentScope_denied() {
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setDeptId(100L).setFromDeptId(200L);
        List<ErpStockMoveItemDO> items = Arrays.asList(
                stockMoveItem(1L, 3L).setFromDeptId(200L),
                stockMoveItem(2L, 3L).setFromDeptId(201L));
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 200L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockMoveApprovePermission permission = stockMoveService.getApprovePermission(stockMove, items);

            assertFalse(permission.getApproveAllowed());
        }
    }

    @Test
    void getApprovePermissionMap_twentyRows_loadsDataScopeOnceWithoutSingleDepartmentQueries() {
        List<ErpStockMoveDO> stockMoves = new ArrayList<>();
        Map<Long, List<ErpStockMoveItemDO>> itemMap = new HashMap<>();
        for (long id = 1L; id <= 20L; id++) {
            stockMoves.add(new ErpStockMoveDO().setId(id).setDeptId(100L).setFromDeptId(200L));
            itemMap.put(id, Collections.singletonList(
                    stockMoveItem(id, id + 100L).setMoveId(id).setFromDeptId(200L)));
        }
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 200L));

        Map<Long, ErpStockMoveApprovePermission> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockTransferOutPermissionScope scope = stockMoveService.getTransferOutPermissionScope();
            result = stockMoveService.getApprovePermissionMap(stockMoves, itemMap, scope);
        }

        assertEquals(20, result.size());
        assertTrue(result.values().stream().allMatch(ErpStockMoveApprovePermission::getApproveAllowed));
        verify(permissionApi, times(1)).getDeptDataPermission(88L, "erp_stock_transfer_out");
        verify(warehouseService, never()).getWarehouseMap(anyCollection());
        verify(deptApi, never()).getDept(any());
        verify(deptApi, never()).getDeptMap(anyCollection());
    }

    @Test
    void getApprovePermissionMap_ownerDepartmentScopeCannotReplaceFromDepartmentScope() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L).setId(1L).setFromDeptId(200L);
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L).setMoveId(1L).setFromDeptId(200L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 100L));
        Map<Long, ErpStockMoveApprovePermission> result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
            security.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(10L);
            result = stockMoveService.getApprovePermissionMap(Collections.singletonList(stockMove),
                    Collections.singletonMap(1L, Collections.singletonList(item)));
        }

        assertFalse(result.get(1L).getApproveAllowed());
        verify(permissionApi, times(1)).getDeptDataPermission(88L, "erp_stock_transfer_out");
        verify(warehouseService, never()).getWarehouseMap(anyCollection());
        verify(deptApi, never()).getDeptMap(anyCollection());
        verify(deptApi, never()).getDept(any());
    }

    @Test
    void getVisibleStockTransferOutPage_usesIndependentFormDataScope() {
        ErpStockMovePageReqVO request = new ErpStockMovePageReqVO();
        PageResult<ErpStockMoveDO> expected = new PageResult<>(Collections.emptyList(), 0L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, true, 10L, 20L));
        when(stockMoveMapper.selectTransferOutPage(eq(request), eq(new HashSet<>(Arrays.asList(10L, 20L))),
                eq(false))).thenReturn(expected);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            PageResult<ErpStockMoveDO> result = stockMoveService.getVisibleStockTransferOutPage(request);

            assertSame(expected, result);
        }
        assertEquals(Integer.valueOf(10), request.getTransferDirection());
    }

    @Test
    void getVisibleStockTransferInPage_usesIndependentFormDataScope() {
        ErpStockMovePageReqVO request = new ErpStockMovePageReqVO();
        PageResult<ErpStockMoveDO> expected = new PageResult<>(Collections.emptyList(), 0L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_in"))
                .thenReturn(deptPermission(false, true, 10L, 20L));
        when(stockMoveMapper.selectTransferInPage(eq(request), eq(new HashSet<>(Arrays.asList(10L, 20L))),
                eq(88L), eq(false))).thenReturn(expected);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            PageResult<ErpStockMoveDO> result = stockMoveService.getVisibleStockTransferInPage(request);

            assertSame(expected, result);
        }
        assertEquals(Integer.valueOf(20), request.getTransferDirection());
    }

    @Test
    void getVisibleStockTransferIn_usesIndependentFormDataScope() {
        ErpStockMoveDO expected = new ErpStockMoveDO().setId(301L).setTransferDirection(20);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_in"))
                .thenReturn(deptPermission(false, true, 10L, 20L));
        when(stockMoveMapper.selectVisibleTransferInById(301L,
                new HashSet<>(Arrays.asList(10L, 20L)), 88L, false)).thenReturn(expected);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            ErpStockMoveDO result = stockMoveService.getVisibleStockTransferIn(301L);

            assertSame(expected, result);
        }
    }

    @Test
    void getVisibleStockTransferOut_doesNotIncludeCreatorVisibility() {
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, true));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertNull(stockMoveService.getVisibleStockTransferOut(301L));
        }

        verify(stockMoveMapper).selectVisibleTransferOutById(301L, Collections.emptySet(), false);
        verify(stockMoveMapper, never()).selectById(301L);
    }

    @Test
    void getVisibleStockTransferOut_missingIndependentScope_failsClosed() {
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out")).thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertNull(stockMoveService.getVisibleStockTransferOut(302L));
        }

        verify(stockMoveMapper).selectVisibleTransferOutById(302L, Collections.emptySet(), false);
        verify(stockMoveMapper, never()).selectById(302L);
    }

    @Test
    void validateStockTransferOutVisible_inboundDepartmentCannotUseDirectId() {
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 300L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertServiceException(() -> stockMoveService.validateStockTransferOutVisible(303L),
                    STOCK_MOVE_NOT_EXISTS);
        }

        verify(stockMoveMapper).selectVisibleTransferOutById(303L, Collections.singleton(300L), false);
    }

    @Test
    void getVisibleStockTransferIn_missingIndependentScope_failsClosed() {
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_in")).thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertNull(stockMoveService.getVisibleStockTransferIn(302L));
        }

        verify(stockMoveMapper).selectVisibleTransferInById(302L, Collections.emptySet(), 88L, false);
        verify(stockMoveMapper, never()).selectById(302L);
    }

    @Test
    void updateStockTransferOutStatus_missingIndependentScope_failsClosed() {
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out")).thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertServiceException(() -> stockMoveService.updateStockTransferOutStatus(
                    10L, ErpAuditStatus.APPROVE.getStatus()), STOCK_MOVE_APPROVE_DEPT_PERMISSION_DENIED);
        }

        verify(stockMoveMapper).selectVisibleTransferOutById(10L, Collections.emptySet(), false);
        verify(stockMoveMapper, never()).selectById(any());
    }

    @Test
    void updateStockMove_invisibleDirectId_failsBeforeBusinessQuery() {
        ErpStockMoveSaveReqVO request = new ErpStockMoveSaveReqVO().setId(401L);
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 300L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertServiceException(() -> stockMoveService.updateStockMove(request, "erp_stock_transfer_out"),
                    STOCK_MOVE_NOT_EXISTS);
        }

        verify(stockMoveMapper).selectVisibleTransferOutById(401L, Collections.singleton(300L), false);
        verify(stockMoveMapper, never()).selectById(401L);
    }

    @Test
    void deleteStockMove_batchContainingInvisibleId_failsBeforeDeleteQuery() {
        when(permissionApi.getDeptDataPermission(88L, "erp_stock_transfer_out"))
                .thenReturn(deptPermission(false, false, 200L));
        when(stockMoveMapper.selectVisibleTransferOutListByIds(
                new HashSet<>(Arrays.asList(501L, 502L)), Collections.singleton(200L), false))
                .thenReturn(Collections.singletonList(new ErpStockMoveDO().setId(501L)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser(88L)) {
            assertServiceException(() -> stockMoveService.deleteStockMove(Arrays.asList(501L, 502L)),
                    STOCK_MOVE_NOT_EXISTS);
        }

        verify(stockMoveMapper, never()).selectByIds(any());
        verify(stockMoveMapper, never()).deleteById(any());
    }

    @Test
    void getDeletePermission_saleCartCrossDept_deniedForSameBranchDept() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L);
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L);

        ErpStockMoveOperationPermission permission = stockMoveService.getDeletePermission(stockMove,
                Collections.singletonList(item));

        assertFalse(permission.getAllowed());
    }

    @Test
    void getDeletePermission_saleCartCrossDept_deniedForParentDept() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L);
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L);

        ErpStockMoveOperationPermission permission = stockMoveService.getDeletePermission(stockMove,
                Collections.singletonList(item));

        assertFalse(permission.getAllowed());
        assertEquals("销售手推车来源调拨出库单请使用“解锁手推车”操作", permission.getDisabledReason());
    }

    @Test
    void getUnlockCartPermission_validSaleCartTransferOut_allowed() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L)
                .setSourceId(72L)
                .setTransferDirection(10)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());

        ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(
                stockMove, Collections.emptyList());

        assertTrue(permission.getAllowed());
    }

    @Test
    void getUnlockCartPermission_legacyNullDirection_treatedAsTransferOut() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L)
                .setSourceId(72L)
                .setTransferDirection(null)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());

        ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(
                stockMove, Collections.emptyList());

        assertTrue(permission.getAllowed());
    }

    @Test
    void getUnlockCartPermission_transferIn_denied() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L)
                .setSourceId(72L)
                .setTransferDirection(20)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());

        ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(
                stockMove, Collections.emptyList());

        assertFalse(permission.getAllowed());
        assertEquals("当前单据不是调拨出库单，不能解锁手推车", permission.getDisabledReason());
    }

    @Test
    void getUnlockCartPermission_missingSourceId_denied() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L)
                .setTransferDirection(10)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());

        ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(
                stockMove, Collections.emptyList());

        assertFalse(permission.getAllowed());
        assertEquals("调拨出库单缺少来源手推车信息，不能解锁", permission.getDisabledReason());
    }

    @Test
    void getUnlockCartPermission_approved_denied() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L)
                .setSourceId(72L)
                .setTransferDirection(10)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());

        ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(
                stockMove, Collections.emptyList());

        assertFalse(permission.getAllowed());
        assertEquals("调拨出库单已审核，不能解锁手推车", permission.getDisabledReason());
    }

    @Test
    void getUnlockCartPermission_crossDept_allowedOnlyForParentDept() {
        mockWarehouseMap(200L, 300L);
        when(deptApi.getDept(100L)).thenReturn(dept(100L, 10L));
        ErpStockMoveDO stockMove = saleCartStockMove(100L)
                .setSourceId(72L)
                .setTransferDirection(10)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginDept(10L)) {
            ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(
                    stockMove, Collections.singletonList(item));

            assertTrue(permission.getAllowed());
        }
    }

    @Test
    void deleteStockMove_saleCartSource_denied() {
        ErpStockMoveDO stockMove = saleCartStockMove(100L)
                .setId(10L)
                .setNo("MOVE-001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(stockMoveMapper.selectByIds(any())).thenReturn(Collections.singletonList(stockMove));

        assertServiceException(() -> stockMoveService.deleteStockMove(Collections.singletonList(10L)),
                STOCK_MOVE_DELETE_CART_SOURCE_DENIED);
        verify(stockMoveMapper, never()).deleteById(any());
        verify(stockMoveItemMapper, never()).deleteByMoveId(any());
    }

    @Test
    void deleteStockMove_batchContainsSaleCartSource_rejectsWholeBatch() {
        ErpStockMoveDO normal = new ErpStockMoveDO().setId(11L).setNo("MOVE-002")
                .setTransferDirection(10).setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveDO saleCart = saleCartStockMove(100L).setId(12L).setNo("MOVE-003")
                .setTransferDirection(10).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(stockMoveMapper.selectByIds(any())).thenReturn(Arrays.asList(normal, saleCart));
        when(stockMoveItemMapper.selectListByMoveId(11L)).thenReturn(Collections.emptyList());

        assertServiceException(() -> stockMoveService.deleteStockMove(Arrays.asList(11L, 12L)),
                STOCK_MOVE_DELETE_CART_SOURCE_DENIED);

        verify(stockMoveMapper, never()).deleteById(any());
        verify(stockMoveItemMapper, never()).deleteByMoveId(any());
    }

    @Test
    void deleteStockMove_purchaseInSource_keepsExistingDeleteBehavior() {
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setId(13L).setNo("MOVE-004")
                .setSourceType(ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType())
                .setTransferDirection(10).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(stockMoveMapper.selectByIds(any())).thenReturn(Collections.singletonList(stockMove));
        when(stockMoveItemMapper.selectListByMoveId(13L)).thenReturn(Collections.emptyList());

        stockMoveService.deleteStockMove(Collections.singletonList(13L));

        verify(stockMoveMapper).deleteById(13L);
        verify(stockMoveItemMapper).deleteByMoveId(13L);
    }

    @Test
    void deleteStockMove_draftTransferOutAlsoDeletesDraftTransferInMirror() {
        ErpStockMoveDO transferOut = new ErpStockMoveDO().setId(13L).setNo("STO-013")
                .setTransferDirection(10).setRelatedMoveId(14L).setRelatedMoveNo("STI-014")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveDO transferIn = new ErpStockMoveDO().setId(14L).setNo("STI-014")
                .setTransferDirection(20).setRelatedMoveId(13L).setRelatedMoveNo("STO-013")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(stockMoveMapper.selectByIds(any())).thenReturn(Collections.singletonList(transferOut));
        when(stockMoveItemMapper.selectListByMoveId(13L)).thenReturn(Collections.emptyList());
        when(stockMoveMapper.selectById(14L)).thenReturn(transferIn);

        stockMoveService.deleteStockMove(Collections.singletonList(13L));

        verify(stockMoveMapper).deleteById(14L);
        verify(stockMoveItemMapper).deleteByMoveId(14L);
        verify(stockMoveMapper).clearRelatedMove(13L);
        verify(stockMoveMapper).deleteById(13L);
        verify(stockMoveItemMapper).deleteByMoveId(13L);
    }

    @Test
    void updateStockMoveStatus_saleCartTransferOut_approvesDraftTransferInMovesLocksAndPublishesEvent() {
        LocalDateTime moveTime = LocalDateTime.of(2026, 7, 15, 10, 0);
        ErpStockMoveDO stockMove = saleCartStockMove(200L)
                .setId(10L)
                .setNo("STO-001")
                .setSourceId(72L)
                .setTransferDirection(10)
                .setRelatedMoveId(12L)
                .setRelatedMoveNo("STI-001")
                .setMoveTime(moveTime)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L)
                .setId(101L)
                .setMoveId(10L)
                .setProductId(201L)
                .setProductPrice(new BigDecimal("15.00"))
                .setCount(new BigDecimal("3"));
        ErpStockMoveDO transferIn = new ErpStockMoveDO()
                .setId(12L)
                .setNo("STI-001")
                .setRelatedMoveId(10L)
                .setRelatedMoveNo("STO-001")
                .setTransferDirection(20)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveItemDO transferInItem = stockMoveItem(1L, 2L)
                .setId(201L)
                .setMoveId(12L)
                .setProductId(201L)
                .setProductPrice(new BigDecimal("15.00"))
                .setCount(new BigDecimal("3"));
        mockWarehouseMap(200L, 300L);
        when(stockMoveMapper.selectById(10L)).thenReturn(stockMove);
        when(stockMoveMapper.selectById(12L)).thenReturn(transferIn);
        when(stockMoveItemMapper.selectListByMoveId(10L)).thenReturn(Collections.singletonList(item));
        when(stockMoveItemMapper.selectListByMoveId(12L)).thenReturn(Collections.singletonList(transferInItem));
        when(warehouseService.validWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(2L).setDeptId(300L)));
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(201L)));
        when(stockMoveMapper.updateByIdAndStatus(eq(10L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(stockMoveMapper.updateByIdAndStatus(eq(12L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);

        stockMoveService.updateStockMoveStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        ArgumentCaptor<ErpStockRecordCreateReqBO> recordCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, times(2)).createStockRecord(recordCaptor.capture());
        List<ErpStockRecordCreateReqBO> records = recordCaptor.getAllValues();
        assertEquals(ErpStockRecordBizTypeEnum.MOVE_OUT.getType(), records.get(0).getBizType());
        assertEquals(Long.valueOf(1L), records.get(0).getWarehouseId());
        assertEquals(0, new BigDecimal("-3").compareTo(records.get(0).getCount()));
        assertEquals(ErpStockRecordBizTypeEnum.MOVE_IN.getType(), records.get(1).getBizType());
        assertEquals(Long.valueOf(2L), records.get(1).getWarehouseId());
        assertEquals(0, new BigDecimal("3").compareTo(records.get(1).getCount()));
        assertEquals(Long.valueOf(12L), records.get(1).getBizId());
        assertEquals(Long.valueOf(201L), records.get(1).getBizItemId());
        assertEquals("STI-001", records.get(1).getBizNo());
        verify(stockMoveMapper, never()).insert(any(ErpStockMoveDO.class));
        verify(stockMoveMapper).updateByIdAndStatus(eq(12L), eq(ErpAuditStatus.PROCESS.getStatus()), any());
        verify(stockMoveItemMapper).insertBatch(any());
        verify(stockService).ensureStockExists(201L, 2L);
        verify(stockLockService).transferStockLocks(
                ErpSaleBizSourceTypeEnum.CART.getType(), 72L, 201L, 1L, 2L);
        verify(eventPublisher).publishEvent(any(ErpSaleCartTransferOutApprovedEvent.class));
        verify(warehouseService).validSaleWarehouseListForDept(anyCollection(), eq(200L));
        verify(warehouseService, never()).validateCurrentUserWarehousePermission(anyCollection());
    }

    @Test
    void updateStockMoveStatus_saleCartTransferOut_existingTransferInRejectsDuplicateApproval() {
        ErpStockMoveDO stockMove = saleCartStockMove(200L)
                .setId(10L)
                .setNo("STO-001")
                .setSourceId(72L)
                .setTransferDirection(10)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveDO transferIn = new ErpStockMoveDO()
                .setId(12L)
                .setNo("STI-001")
                .setRelatedMoveId(10L)
                .setTransferDirection(20)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(stockMoveMapper.selectById(10L)).thenReturn(stockMove);
        when(stockMoveMapper.selectByRelatedMoveIdAndDirection(10L, 20)).thenReturn(transferIn);

        assertServiceException(() -> stockMoveService.updateStockMoveStatus(
                10L, ErpAuditStatus.APPROVE.getStatus()), STOCK_MOVE_TRANSFER_IN_EXISTS);

        verify(stockMoveItemMapper, never()).selectListByMoveId(any());
        verify(stockMoveMapper, never()).updateByIdAndStatus(any(), any(), any());
        verify(stockRecordService, never()).createStockRecord(any());
        verify(stockLockService, never()).transferStockLocks(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(ErpSaleCartTransferOutApprovedEvent.class));
    }

    @Test
    void updateStockMoveStatus_nonSaleTransferOut_approvesExistingInboundLinkage() {
        ErpStockMoveDO stockMove = new ErpStockMoveDO()
                .setId(11L)
                .setNo("STO-002")
                .setDeptId(200L)
                .setTransferDirection(10)
                .setRelatedMoveId(12L)
                .setRelatedMoveNo("STI-002")
                .setMoveTime(LocalDateTime.of(2026, 7, 15, 11, 0))
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveItemDO item = stockMoveItem(1L, 2L)
                .setId(102L)
                .setMoveId(11L)
                .setProductId(202L)
                .setProductPrice(new BigDecimal("20.00"))
                .setCount(new BigDecimal("4"));
        ErpStockMoveDO transferIn = new ErpStockMoveDO()
                .setId(12L)
                .setNo("STI-002")
                .setRelatedMoveId(11L)
                .setRelatedMoveNo("STO-002")
                .setTransferDirection(20)
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveItemDO transferInItem = stockMoveItem(1L, 2L)
                .setId(2021L)
                .setMoveId(12L)
                .setProductId(202L)
                .setProductPrice(new BigDecimal("20.00"))
                .setCount(new BigDecimal("4"));
        mockWarehouseMap(200L, 300L);
        when(stockMoveMapper.selectById(11L)).thenReturn(stockMove);
        when(stockMoveMapper.selectById(12L)).thenReturn(transferIn);
        when(stockMoveItemMapper.selectListByMoveId(11L)).thenReturn(Collections.singletonList(item));
        when(stockMoveItemMapper.selectListByMoveId(12L)).thenReturn(Collections.singletonList(transferInItem));
        when(warehouseService.validWarehouseList(anyCollection())).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(1L).setDeptId(200L),
                new ErpWarehouseDO().setId(2L).setDeptId(300L)));
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(202L)));
        when(stockMoveMapper.updateByIdAndStatus(eq(11L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(stockMoveMapper.updateByIdAndStatus(eq(12L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);

        stockMoveService.updateStockMoveStatus(11L, ErpAuditStatus.APPROVE.getStatus());

        ArgumentCaptor<ErpStockRecordCreateReqBO> recordCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, times(2)).createStockRecord(recordCaptor.capture());
        List<ErpStockRecordCreateReqBO> records = recordCaptor.getAllValues();
        assertEquals(ErpStockRecordBizTypeEnum.MOVE_OUT.getType(), records.get(0).getBizType());
        assertEquals(ErpStockRecordBizTypeEnum.MOVE_IN.getType(), records.get(1).getBizType());
        assertEquals(Long.valueOf(12L), records.get(1).getBizId());
        assertEquals(Long.valueOf(2021L), records.get(1).getBizItemId());
        assertEquals("STI-002", records.get(1).getBizNo());
        verify(stockMoveMapper, never()).insert(any(ErpStockMoveDO.class));
        verify(stockService).ensureStockExists(202L, 2L);
        verify(warehouseService, never()).validateCurrentUserStockMoveFromWarehousePermission(anyCollection());
        verify(warehouseService, never()).validateCurrentUserWarehousePermission(anyCollection());
        verify(stockLockService, never()).transferStockLocks(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(ErpSaleCartTransferOutApprovedEvent.class));
    }

    @Test
    void deleteUnapprovedTransferOutBySource_approvedTransferOutExists_rejectsDelete() {
        ErpStockMoveDO approved = new ErpStockMoveDO().setId(10L).setNo("STO-001")
                .setTransferDirection(10).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(stockMoveMapper.selectListBySourceAndDirectionForUpdate(
                ErpSaleBizSourceTypeEnum.CART.getType(), 72L, 10))
                .thenReturn(Collections.singletonList(approved));

        assertServiceException(() -> stockMoveService.deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), 72L), STOCK_MOVE_DELETE_FAIL_APPROVE, approved.getNo());

        verify(stockMoveMapper, never()).deleteById(any());
        verify(stockMoveItemMapper, never()).deleteByMoveId(any());
    }

    @Test
    void deleteUnapprovedTransferOutBySource_multipleDrafts_deletesAllMainAndItemRows() {
        ErpStockMoveDO first = new ErpStockMoveDO().setId(20L).setNo("STO-020")
                .setTransferDirection(10).setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveDO second = new ErpStockMoveDO().setId(21L).setNo("STO-021")
                .setTransferDirection(10).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(stockMoveMapper.selectListBySourceAndDirectionForUpdate(
                ErpSaleBizSourceTypeEnum.CART.getType(), 73L, 10))
                .thenReturn(Arrays.asList(first, second));

        stockMoveService.deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), 73L);

        verify(stockMoveMapper).deleteById(20L);
        verify(stockMoveMapper).deleteById(21L);
        verify(stockMoveItemMapper).deleteByMoveId(20L);
        verify(stockMoveItemMapper).deleteByMoveId(21L);
        verify(operateLogService).recordDelete(ERP_STOCK_MOVE_TYPE, 20L, "STO-020");
        verify(operateLogService).recordDelete(ERP_STOCK_MOVE_TYPE, 21L, "STO-021");
    }

    @Test
    void deleteUnapprovedTransferOutBySource_mixedDraftAndApproved_rejectsBeforeAnyDelete() {
        ErpStockMoveDO draft = new ErpStockMoveDO().setId(30L).setNo("STO-030")
                .setTransferDirection(10).setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpStockMoveDO approved = new ErpStockMoveDO().setId(31L).setNo("STO-031")
                .setTransferDirection(10).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(stockMoveMapper.selectListBySourceAndDirectionForUpdate(
                ErpSaleBizSourceTypeEnum.CART.getType(), 74L, 10))
                .thenReturn(Arrays.asList(draft, approved));

        assertServiceException(() -> stockMoveService.deleteUnapprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), 74L), STOCK_MOVE_DELETE_FAIL_APPROVE, approved.getNo());

        verify(stockMoveMapper, never()).deleteById(any());
        verify(stockMoveItemMapper, never()).deleteByMoveId(any());
        verify(operateLogService, never()).recordDelete(any(), any(), any());
    }

    @Test
    void getStockMoveForUpdate_usesLockingMapperQuery() {
        ErpStockMoveDO stockMove = new ErpStockMoveDO().setId(10L);
        when(stockMoveMapper.selectByIdForUpdate(10L)).thenReturn(stockMove);

        assertEquals(stockMove, stockMoveService.getStockMoveForUpdate(10L));

        verify(stockMoveMapper).selectByIdForUpdate(10L);
    }

    @Test
    void hasApprovedTransferOutBySource_queriesTransferOutDirectionOnly() {
        when(stockMoveMapper.selectListBySourceAndDirection(
                ErpSaleBizSourceTypeEnum.CART.getType(), 72L, 10))
                .thenReturn(Collections.singletonList(new ErpStockMoveDO()
                        .setTransferDirection(10).setStatus(ErpAuditStatus.APPROVE.getStatus())));

        assertTrue(stockMoveService.hasApprovedTransferOutBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), 72L));

        verify(stockMoveMapper).selectListBySourceAndDirection(
                ErpSaleBizSourceTypeEnum.CART.getType(), 72L, 10);
    }

    @Test
    void getApprovedTransferOutBySource_uniqueApproved_returnsDocument() {
        ErpStockMoveDO approved = new ErpStockMoveDO().setId(1L).setStatus(ErpAuditStatus.APPROVE.getStatus());
        ErpStockMoveDO draft = new ErpStockMoveDO().setId(2L).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(stockMoveMapper.selectListBySourceAndDirection(30, 100L, 10))
                .thenReturn(Arrays.asList(draft, approved));

        ErpStockMoveDO result = stockMoveService.getApprovedTransferOutBySource(30, 100L);

        assertSame(approved, result);
    }

    @Test
    void getApprovedTransferOutBySource_multipleApproved_returnsNull() {
        when(stockMoveMapper.selectListBySourceAndDirection(30, 100L, 10)).thenReturn(Arrays.asList(
                new ErpStockMoveDO().setId(1L).setStatus(ErpAuditStatus.APPROVE.getStatus()),
                new ErpStockMoveDO().setId(2L).setStatus(ErpAuditStatus.APPROVE.getStatus())));

        assertNull(stockMoveService.getApprovedTransferOutBySource(30, 100L));
    }

    @Test
    void syncTransferOutDraftsBySource_updatesMatchingDeptAndDeletesStaleDept() {
        ErpStockMoveSaveReqVO request = stockMoveRequest(10L, ErpSaleBizSourceTypeEnum.CART.getType())
                .setFromDeptId(20L).setToDeptId(10L);
        ErpStockMoveDO matching = new ErpStockMoveDO().setId(11L).setNo("STO-011")
                .setDeptId(10L).setFromDeptId(20L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType()).setSourceId(115L).setTransferDirection(10);
        ErpStockMoveDO stale = new ErpStockMoveDO().setId(12L).setNo("STO-012")
                .setDeptId(10L).setFromDeptId(30L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType()).setSourceId(115L).setTransferDirection(10);
        when(stockMoveMapper.selectListBySourceAndDirectionForUpdate(
                ErpSaleBizSourceTypeEnum.CART.getType(), 115L, 10)).thenReturn(Arrays.asList(matching, stale));
        ErpWarehouseDO fromWarehouse = new ErpWarehouseDO().setId(1L).setDeptId(20L);
        ErpWarehouseDO toWarehouse = new ErpWarehouseDO().setId(12L).setDeptId(10L);
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(955L).setUnitId(1L)));
        when(warehouseService.validWarehouseList(anyCollection())).thenReturn(
                Arrays.asList(fromWarehouse, toWarehouse));
        when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(
                cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                        Arrays.asList(fromWarehouse, toWarehouse), ErpWarehouseDO::getId));
        mockSaleWarehouseValidation(20L);

        List<Long> result = stockMoveService.syncTransferOutDraftsBySource(Collections.singletonList(request));

        assertEquals(Collections.singletonList(11L), result);
        verify(stockMoveMapper).updateById(org.mockito.ArgumentMatchers.<ErpStockMoveDO>argThat(
                move -> Long.valueOf(11L).equals(move.getId()) && Long.valueOf(20L).equals(move.getFromDeptId())));
        verify(stockMoveMapper).deleteById(12L);
        verify(stockMoveItemMapper).deleteByMoveId(12L);
    }

    @Test
    void getTransferOutListBySource_returnsAllDocuments() {
        List<ErpStockMoveDO> transfers = Arrays.asList(new ErpStockMoveDO().setId(1L),
                new ErpStockMoveDO().setId(2L));
        when(stockMoveMapper.selectListBySourceAndDirection(30, 100L, 10)).thenReturn(transfers);

        assertSame(transfers, stockMoveService.getTransferOutListBySource(30, 100L));
    }

    private void mockWarehouseMap(Long fromDeptId, Long toDeptId) {
        when(warehouseService.getWarehouseMap(any())).thenReturn(cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                Arrays.asList(new ErpWarehouseDO().setId(1L).setDeptId(fromDeptId),
                        new ErpWarehouseDO().setId(2L).setDeptId(toDeptId)), ErpWarehouseDO::getId));
    }

    private ErpStockMoveSaveReqVO stockMoveRequest(Long deptId, Integer sourceType) {
        ErpStockMoveSaveReqVO.Item item = new ErpStockMoveSaveReqVO.Item()
                .setFromWarehouseId(1L)
                .setToWarehouseId(12L)
                .setProductId(955L)
                .setProductPrice(new BigDecimal("10.00"))
                .setCount(BigDecimal.ONE);
        return new ErpStockMoveSaveReqVO()
                .setDeptId(deptId)
                .setSourceType(sourceType)
                .setSourceId(115L)
                .setSourceNo("XSST20260716000005")
                .setMoveTime(LocalDateTime.of(2026, 7, 16, 13, 35))
                .setItems(Collections.singletonList(item));
    }

    private void mockCreateStockMoveDependencies(Long fromDeptId, Long toDeptId) {
        ErpWarehouseDO fromWarehouse = new ErpWarehouseDO().setId(1L).setDeptId(fromDeptId);
        ErpWarehouseDO toWarehouse = new ErpWarehouseDO().setId(12L).setDeptId(toDeptId);
        when(productService.validProductList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(955L).setUnitId(1L)));
        when(warehouseService.validWarehouseList(anyCollection())).thenReturn(
                Arrays.asList(fromWarehouse, toWarehouse));
        when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(
                cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap(
                        Arrays.asList(fromWarehouse, toWarehouse), ErpWarehouseDO::getId));
        when(noRedisDAO.generate(ErpNoRedisDAO.STOCK_MOVE_NO_PREFIX)).thenReturn("STO-TEST-001");
        when(noRedisDAO.generate(ErpNoRedisDAO.STOCK_TRANSFER_IN_NO_PREFIX)).thenReturn("STI-TEST-001");
        when(stockMoveMapper.insert(any(ErpStockMoveDO.class))).thenAnswer(invocation -> {
            ErpStockMoveDO move = invocation.getArgument(0);
            move.setId(Integer.valueOf(20).equals(move.getTransferDirection()) ? 101L : 100L);
            return 1;
        });
    }

    private void mockSaleWarehouseValidation(Long fromDeptId) {
        when(warehouseService.validSaleWarehouseList(anyCollection())).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(1L).setDeptId(fromDeptId)));
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginDept(Long deptId) {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(deptId);
        return security;
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser(Long userId) {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
        return security;
    }

    private DeptDataPermissionRespDTO deptPermission(boolean all, boolean self, Long... deptIds) {
        DeptDataPermissionRespDTO permission = new DeptDataPermissionRespDTO();
        permission.setAll(all);
        permission.setSelf(self);
        permission.setDeptIds(new HashSet<>(Arrays.asList(deptIds)));
        return permission;
    }

    private ErpStockMoveDO saleCartStockMove(Long deptId) {
        return new ErpStockMoveDO().setDeptId(deptId).setSourceType(ErpSaleBizSourceTypeEnum.CART.getType());
    }

    private ErpStockMoveItemDO stockMoveItem(Long fromWarehouseId, Long toWarehouseId) {
        return new ErpStockMoveItemDO().setFromWarehouseId(fromWarehouseId).setToWarehouseId(toWarehouseId);
    }

    private DeptRespDTO dept(Long id, Long parentId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setParentId(parentId);
        return dept;
    }

}
