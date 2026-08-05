package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpWarehouseMoveStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_SUBMIT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_UPDATE_FAIL_NOT_DRAFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpOperateLogService operateLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(warehouseMoveService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260727000001";
            }
        });
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
    void updateDraft_replacesItemsAndKeepsDraftStatusGuard() {
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
        verify(warehouseMoveItemMapper).deleteByMoveId(10L);
        verify(warehouseMoveItemMapper, never()).insertBatch(anyList());
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
    void approveProcess_ignoresGenericProductDataPermission() {
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
        when(warehouseMoveMapper.selectById(10L)).thenReturn(warehouseMove);
        when(warehouseMoveItemMapper.selectListByMoveId(10L))
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

        warehouseMoveService.updateWarehouseMoveStatus(
                10L, ErpWarehouseMoveStatusEnum.APPROVE.getStatus());

        assertNull(DataPermissionContextHolder.get());
        verify(productService).validProductList(Collections.singleton(300L));
        verify(stockRecordService, org.mockito.Mockito.times(2)).createStockRecord(any());
    }

    @Test
    void approveDraft_cannotBypassFormalSubmission() {
        when(warehouseMoveMapper.selectById(10L)).thenReturn(new ErpWarehouseMoveDO()
                .setId(10L)
                .setNo("YH001")
                .setStatus(ErpWarehouseMoveStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> warehouseMoveService.updateWarehouseMoveStatus(
                10L, ErpWarehouseMoveStatusEnum.APPROVE.getStatus()), WAREHOUSE_MOVE_APPROVE_FAIL);
        verify(warehouseMoveMapper, never()).updateByIdAndStatus(any(), any(), any());
        verify(stockRecordService, never()).createStockRecord(any());
    }

}
