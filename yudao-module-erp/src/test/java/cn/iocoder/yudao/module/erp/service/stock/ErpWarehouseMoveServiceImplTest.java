package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpWarehouseMoveStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpAssemblyWarehouseMoveCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_SUBMIT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_MANUAL_APPROVE_FORBIDDEN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_UPDATE_FAIL_NOT_DRAFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpWarehouseMoveServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpWarehouseMoveServiceImpl warehouseMoveService;

    @Mock
    private ErpWarehouseMoveMapper warehouseMoveMapper;
    @Mock
    private ErpWarehouseMoveItemMapper warehouseMoveItemMapper;
    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpSalePickDeliveryService salePickDeliveryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(warehouseMoveService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260727000001";
            }
        });
        ReflectionTestUtils.setField(warehouseMoveService, "snapshotSupport", new ErpStockItemSnapshotSupport());
    }

    @Test
    void createDraft_withoutValidItems_throwException() {
        ErpWarehouseMoveDraftCreateReqVO request = new ErpWarehouseMoveDraftCreateReqVO();
        request.setRemark("待补充仓库");
        request.setItems(Collections.emptyList());
        assertServiceException(() -> warehouseMoveService.createWarehouseMoveDraft(request),
                WAREHOUSE_MOVE_DRAFT_ITEMS_REQUIRED);
        verify(warehouseMoveMapper, never()).insert(any(ErpWarehouseMoveDO.class));
        verify(warehouseMoveItemMapper, never()).deleteByMoveId(any());
        verify(warehouseMoveItemMapper, never()).insertBatch(anyList());
    }

    @Test
    void updateDraft_rejectsNonDraft() {
        ErpWarehouseMoveDraftUpdateReqVO request = new ErpWarehouseMoveDraftUpdateReqVO();
        request.setId(10L);
        when(warehouseMoveMapper.selectById(10L)).thenReturn(new ErpWarehouseMoveDO()
                .setId(10L).setNo("YH001").setStatus(ErpWarehouseMoveStatusEnum.PROCESS.getStatus()));

        assertServiceException(() -> warehouseMoveService.updateWarehouseMoveDraft(request),
                WAREHOUSE_MOVE_UPDATE_FAIL_NOT_DRAFT, "YH001");
        verify(warehouseMoveMapper, never()).updateDraftByIdAndStatus(any(), any(), any());
    }

    @Test
    void updateDraft_emptyIncrementalChangeKeepsExistingItemsAndDraftStatusGuard() {
        ErpWarehouseMoveDraftUpdateReqVO request = new ErpWarehouseMoveDraftUpdateReqVO();
        request.setId(10L);
        request.setRemark("继续补充");
        request.setItems(Collections.emptyList());
        when(warehouseMoveMapper.selectById(10L)).thenReturn(new ErpWarehouseMoveDO()
                .setId(10L).setNo("YH001").setDeptId(9L)
                .setStatus(ErpWarehouseMoveStatusEnum.DRAFT.getStatus()));
        when(warehouseMoveMapper.updateDraftByIdAndStatus(eq(10L),
                eq(ErpWarehouseMoveStatusEnum.DRAFT.getStatus()), any(ErpWarehouseMoveDO.class)))
                .thenReturn(1);

        warehouseMoveService.updateWarehouseMoveDraft(request);

        verify(warehouseMoveMapper).updateDraftByIdAndStatus(eq(10L),
                eq(ErpWarehouseMoveStatusEnum.DRAFT.getStatus()), any(ErpWarehouseMoveDO.class));
        verify(warehouseMoveItemMapper, never()).deleteByMoveId(10L);
        verify(warehouseMoveItemMapper, never()).insertBatch(anyList());
    }

    @Test
    void createAndApproveAssemblyMove_postsOutAndInImmediately() {
        ErpAssemblyWarehouseMoveCreateReqBO request = new ErpAssemblyWarehouseMoveCreateReqBO()
                .setAssemblyOrderId(100L).setAssemblyOrderNo("ZZD0001")
                .setProductId(300L).setProductUnitId(1L)
                .setFromWarehouseId(10L).setToWarehouseId(20L)
                .setCount(new BigDecimal("5")).setCostPrice(new BigDecimal("12.500000"))
                .setMoveTime(LocalDateTime.of(1970, 1, 1, 8, 0));
        when(productService.validProductList(Collections.singleton(300L)))
                .thenReturn(Collections.singletonList(new ErpProductDO()
                        .setId(300L).setUnitId(1L).setPackageQty(1)));
        when(stockMapper.selectByProductIdAndWarehouseIdForUpdate(300L, 10L)).thenReturn(new ErpStockDO()
                .setProductId(300L).setWarehouseId(10L)
                .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO)
                .setCostPrice(new BigDecimal("12.500000")));
        org.mockito.Mockito.doAnswer(invocation -> {
            ErpWarehouseMoveDO move = invocation.getArgument(0);
            move.setId(700L);
            return 1;
        }).when(warehouseMoveMapper).insert(any(ErpWarehouseMoveDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            ErpWarehouseMoveItemDO item = invocation.getArgument(0);
            item.setId(701L);
            return 1;
        }).when(warehouseMoveItemMapper).insert(any(ErpWarehouseMoveItemDO.class));

        ErpWarehouseMoveDO result = warehouseMoveService.createAndApproveAssemblyMove(request);

        assertEquals(700L, result.getId());
        assertEquals(ErpWarehouseMoveStatusEnum.APPROVE.getStatus(), result.getStatus());
        assertEquals(100L, result.getSourceId());
        assertNotEquals(1970, result.getMoveTime().getYear());
        ArgumentCaptor<ErpStockRecordCreateReqBO> captor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, org.mockito.Mockito.times(2)).createStockRecord(captor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT.getType(),
                captor.getAllValues().get(0).getBizType());
        assertEquals(new BigDecimal("-5"), captor.getAllValues().get(0).getCount());
        assertEquals(ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN.getType(),
                captor.getAllValues().get(1).getBizType());
        assertEquals(new BigDecimal("5"), captor.getAllValues().get(1).getCount());
        assertEquals(new BigDecimal("12.500000"), captor.getAllValues().get(1).getUnitPrice());
        verify(salePickDeliveryService).generateCompletedForWarehouseMove(700L, null);
        verify(salePickDeliveryService, never()).generateForWarehouseMove(any());
    }

    @Test
    void voidAssemblyMove_reversesTargetAndSourceInventoryImmediately() {
        ErpWarehouseMoveDO move = new ErpWarehouseMoveDO()
                .setId(700L).setNo("YH0001").setSourceType(ErpBizTypeEnum.ASSEMBLY_ORDER.getType())
                .setStatus(ErpWarehouseMoveStatusEnum.APPROVE.getStatus())
                .setMoveTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        ErpWarehouseMoveItemDO item = new ErpWarehouseMoveItemDO()
                .setId(701L).setMoveId(700L).setProductId(300L)
                .setFromWarehouseId(10L).setToWarehouseId(20L)
                .setProductUnitId(1L).setCount(new BigDecimal("5"))
                .setCostPrice(new BigDecimal("12.500000"));
        when(warehouseMoveMapper.selectByIdForUpdate(700L)).thenReturn(move);
        when(warehouseMoveItemMapper.selectListByMoveIdForUpdate(700L))
                .thenReturn(Collections.singletonList(item));
        when(stockRecordMapper.selectListByBizTypesAndBizId(anyList(), eq(700L)))
                .thenReturn(Arrays.asList(
                        new ErpStockRecordDO().setId(801L).setBizItemId(701L)
                                .setBizType(ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT.getType()),
                        new ErpStockRecordDO().setId(802L).setBizItemId(701L)
                                .setBizType(ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN.getType())));
        when(stockMapper.selectByProductIdAndWarehouseIdForUpdate(300L, 20L))
                .thenReturn(new ErpStockDO().setProductId(300L).setWarehouseId(20L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO));
        when(warehouseMoveMapper.updateByIdAndStatus(eq(700L),
                eq(ErpWarehouseMoveStatusEnum.APPROVE.getStatus()), any(ErpWarehouseMoveDO.class)))
                .thenReturn(1);

        warehouseMoveService.voidAssemblyMove(700L, "组装单作废");

        ArgumentCaptor<ErpStockRecordCreateReqBO> captor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, org.mockito.Mockito.times(2)).createStockRecord(captor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN_CANCEL.getType(),
                captor.getAllValues().get(0).getBizType());
        assertEquals(new BigDecimal("-5"), captor.getAllValues().get(0).getCount());
        assertEquals(802L, captor.getAllValues().get(0).getSourceRecordId());
        assertEquals(ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT_CANCEL.getType(),
                captor.getAllValues().get(1).getBizType());
        assertEquals(new BigDecimal("5"), captor.getAllValues().get(1).getCount());
        assertEquals(801L, captor.getAllValues().get(1).getSourceRecordId());
        ArgumentCaptor<ErpWarehouseMoveDO> updateCaptor = ArgumentCaptor.forClass(ErpWarehouseMoveDO.class);
        verify(warehouseMoveMapper).updateByIdAndStatus(eq(700L),
                eq(ErpWarehouseMoveStatusEnum.APPROVE.getStatus()), updateCaptor.capture());
        assertEquals(ErpWarehouseMoveStatusEnum.VOIDED.getStatus(), updateCaptor.getValue().getStatus());
        assertEquals("组装单作废", updateCaptor.getValue().getVoidReason());
    }

    @Test
    void submitDraft_movesToProcessWithoutCreatingStockRecords() {
        ErpWarehouseMoveDO draft = new ErpWarehouseMoveDO()
                .setId(10L)
                .setNo("YH001")
                .setStatus(ErpWarehouseMoveStatusEnum.DRAFT.getStatus())
                .setMoveTime(LocalDateTime.of(2026, 7, 27, 10, 0))
                .setFromWarehouseId(100L)
                .setToWarehouseId(200L);
        ErpWarehouseMoveItemDO item = new ErpWarehouseMoveItemDO()
                .setId(20L)
                .setMoveId(10L)
                .setProductId(300L)
                .setFromWarehouseId(100L)
                .setToWarehouseId(200L)
                .setCount(BigDecimal.ONE)
                .setCostPrice(BigDecimal.TEN)
                .setProductPrice(BigDecimal.TEN);
        when(warehouseMoveMapper.selectById(10L)).thenReturn(draft);
        when(warehouseMoveItemMapper.selectListByMoveId(10L))
                .thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(300L).setUnitId(1L)));
        when(warehouseMoveMapper.updateByIdAndStatus(eq(10L),
                eq(ErpWarehouseMoveStatusEnum.DRAFT.getStatus()), any(ErpWarehouseMoveDO.class)))
                .thenReturn(1);

        warehouseMoveService.submitWarehouseMove(10L);

        ArgumentCaptor<ErpWarehouseMoveDO> captor = ArgumentCaptor.forClass(ErpWarehouseMoveDO.class);
        verify(warehouseMoveMapper).updateByIdAndStatus(eq(10L),
                eq(ErpWarehouseMoveStatusEnum.DRAFT.getStatus()), captor.capture());
        assertEquals(ErpWarehouseMoveStatusEnum.PROCESS.getStatus(), captor.getValue().getStatus());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void submitDraft_rejectsEmptyItems() {
        when(warehouseMoveMapper.selectById(10L)).thenReturn(new ErpWarehouseMoveDO()
                .setId(10L)
                .setNo("YH001")
                .setStatus(ErpWarehouseMoveStatusEnum.DRAFT.getStatus())
                .setMoveTime(LocalDateTime.of(2026, 7, 27, 10, 0))
                .setFromWarehouseId(100L)
                .setToWarehouseId(200L));
        when(warehouseMoveItemMapper.selectListByMoveId(10L)).thenReturn(Collections.emptyList());

        assertServiceException(() -> warehouseMoveService.submitWarehouseMove(10L),
                WAREHOUSE_MOVE_SUBMIT_ITEMS_REQUIRED);
        verify(warehouseMoveMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void confirmProcess_generatesPickTaskWithoutCreatingStockRecords() {
        ErpWarehouseMoveDO warehouseMove = new ErpWarehouseMoveDO()
                .setId(10L)
                .setNo("YH20260727000001")
                .setStatus(ErpWarehouseMoveStatusEnum.PROCESS.getStatus())
                .setMoveTime(LocalDateTime.of(2026, 7, 27, 16, 38))
                .setFromWarehouseId(100L)
                .setToWarehouseId(200L);
        ErpWarehouseMoveItemDO item = new ErpWarehouseMoveItemDO()
                .setId(20L)
                .setMoveId(10L)
                .setProductId(300L)
                .setFromWarehouseId(100L)
                .setToWarehouseId(200L)
                .setCount(BigDecimal.ONE)
                .setCostPrice(BigDecimal.TEN);
        when(warehouseMoveMapper.selectByIdForUpdate(10L)).thenReturn(warehouseMove);
        when(warehouseMoveItemMapper.selectListByMoveIdForUpdate(10L))
                .thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenAnswer(invocation -> {
            assertFalse(DataPermissionContextHolder.get().enable());
            return Collections.singletonList(new ErpProductDO().setId(300L));
        });
        when(stockService.getStock(300L, 100L))
                .thenReturn(new ErpStockDO().setProductId(300L).setWarehouseId(100L)
                        .setCount(BigDecimal.TEN));
        when(warehouseMoveMapper.updateByIdAndStatus(eq(10L),
                eq(ErpWarehouseMoveStatusEnum.PROCESS.getStatus()), any(ErpWarehouseMoveDO.class)))
                .thenReturn(1);

        warehouseMoveService.confirmWarehouseMove(10L);

        assertNull(DataPermissionContextHolder.get());
        verify(productService).validProductList(Collections.singleton(300L));
        verify(salePickDeliveryService).generateForWarehouseMove(10L);
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void cancelConfirm_rollsBackUnstartedPickTask() {
        ErpWarehouseMoveDO move = new ErpWarehouseMoveDO()
                .setId(10L)
                .setNo("YH001")
                .setStatus(ErpWarehouseMoveStatusEnum.PICKING.getStatus());
        when(warehouseMoveMapper.selectById(10L)).thenReturn(move);
        when(warehouseMoveMapper.selectByIdForUpdate(10L)).thenReturn(move);
        when(warehouseMoveMapper.cancelConfirmByIdAndStatus(10L,
                ErpWarehouseMoveStatusEnum.PICKING.getStatus(),
                ErpWarehouseMoveStatusEnum.PROCESS.getStatus())).thenReturn(1);

        warehouseMoveService.cancelConfirmWarehouseMove(10L);

        verify(salePickDeliveryService).rollbackUnstartedWarehouseMoveFulfillment(10L);
        verify(warehouseMoveMapper).cancelConfirmByIdAndStatus(10L,
                ErpWarehouseMoveStatusEnum.PICKING.getStatus(),
                ErpWarehouseMoveStatusEnum.PROCESS.getStatus());
    }

    @Test
    void completedPick_autoApprovesAndCreatesStockRecords() {
        ErpWarehouseMoveDO move = new ErpWarehouseMoveDO()
                .setId(10L).setNo("YH001")
                .setStatus(ErpWarehouseMoveStatusEnum.PICKING.getStatus())
                .setMoveTime(LocalDateTime.of(2026, 10, 20, 14, 0))
                .setFromWarehouseId(100L).setToWarehouseId(200L);
        ErpWarehouseMoveItemDO item = new ErpWarehouseMoveItemDO()
                .setId(20L).setMoveId(10L).setProductId(300L)
                .setFromWarehouseId(100L).setToWarehouseId(200L)
                .setCount(BigDecimal.ONE).setCostPrice(BigDecimal.TEN);
        when(warehouseMoveMapper.selectByIdForUpdate(10L)).thenReturn(move);
        when(warehouseMoveItemMapper.selectListByMoveIdForUpdate(10L))
                .thenReturn(Collections.singletonList(item));
        ErpSalePickDeliverySummaryRespVO summary = new ErpSalePickDeliverySummaryRespVO();
        summary.setPickStatus(ErpSalePickStatusEnum.DONE.getStatus());
        when(salePickDeliveryService.getSummaryMapByWarehouseMoveIds(Collections.singleton(10L)))
                .thenReturn(Collections.singletonMap(10L, summary));
        when(stockService.getStock(300L, 100L)).thenReturn(new ErpStockDO()
                .setProductId(300L).setWarehouseId(100L).setCount(BigDecimal.TEN));
        when(warehouseMoveMapper.updateByIdAndStatus(eq(10L),
                eq(ErpWarehouseMoveStatusEnum.PICKING.getStatus()), any(ErpWarehouseMoveDO.class)))
                .thenReturn(1);

        warehouseMoveService.autoApproveAfterPick(10L, 99L);

        verify(stockRecordService, org.mockito.Mockito.times(2)).createStockRecord(any());
    }

    @Test
    void manualApprove_isForbidden() {

        assertServiceException(() -> warehouseMoveService.updateWarehouseMoveStatus(
                10L, ErpWarehouseMoveStatusEnum.APPROVE.getStatus()),
                WAREHOUSE_MOVE_MANUAL_APPROVE_FORBIDDEN);
        verify(warehouseMoveMapper, never()).updateByIdAndStatus(any(), any(), any());
        verify(stockRecordService, never()).createStockRecord(any());
    }

}
