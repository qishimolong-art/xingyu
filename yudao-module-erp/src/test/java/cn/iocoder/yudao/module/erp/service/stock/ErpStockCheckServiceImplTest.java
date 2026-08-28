package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_CHECK_ITEM_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_CHECK_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_CHECK_SUBMIT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_CHECK_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_CHECK_UPDATE_FAIL_NOT_DRAFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockCheckServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockCheckServiceImpl stockCheckService;

    @Mock
    private ErpStockCheckMapper stockCheckMapper;
    @Mock
    private ErpStockCheckItemMapper stockCheckItemMapper;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpStockService stockService;
    @Spy
    private ErpStockItemSnapshotSupport snapshotSupport = new ErpStockItemSnapshotSupport();
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpStockItemBatchUpdateSupport batchUpdateSupport;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stockCheckService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260727000001";
            }
        });
    }

    @Test
    void createDraft_withoutValidItems_throwException() {
        ErpStockCheckDraftCreateReqVO request = new ErpStockCheckDraftCreateReqVO();
        request.setItems(Collections.emptyList());

        assertServiceException(() -> stockCheckService.createStockCheckDraft(request),
                STOCK_CHECK_DRAFT_ITEMS_REQUIRED);
        verify(stockCheckMapper, never()).insert(any(ErpStockCheckDO.class));
        verify(stockCheckItemMapper, never()).deleteByCheckId(any());
        verify(stockCheckItemMapper, never()).insertBatch(any());
    }

    @Test
    void updateDraft_rejectsNonDraft() {
        ErpStockCheckDraftUpdateReqVO request = new ErpStockCheckDraftUpdateReqVO();
        request.setId(10L);
        when(stockCheckMapper.selectById(10L)).thenReturn(new ErpStockCheckDO()
                .setId(10L).setNo("PD001").setStatus(ErpStockCheckStatusEnum.PROCESS.getStatus()));

        assertServiceException(() -> stockCheckService.updateStockCheckDraft(request),
                STOCK_CHECK_UPDATE_FAIL_NOT_DRAFT, "PD001");
        verify(stockCheckMapper, never()).updateById(any(ErpStockCheckDO.class));
    }

    @Test
    void submitDraft_movesToProcessAfterStrictValidation() {
        ErpStockCheckDO draft = new ErpStockCheckDO()
                .setId(10L)
                .setNo("PD001")
                .setStatus(ErpStockCheckStatusEnum.DRAFT.getStatus())
                .setCheckType(ErpStockCheckTypeEnum.COUNT.getType())
                .setCheckTime(LocalDateTime.of(2026, 7, 27, 10, 0));
        ErpStockCheckItemDO item = new ErpStockCheckItemDO()
                .setId(20L)
                .setCheckId(10L)
                .setProductId(100L)
                .setWarehouseId(200L)
                .setProductPrice(BigDecimal.TEN)
                .setStockCount(BigDecimal.ONE)
                .setCount(BigDecimal.ONE);
        when(stockCheckMapper.selectById(10L)).thenReturn(draft);
        when(stockCheckItemMapper.selectListByCheckId(10L))
                .thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(100L).setUnitId(1L)));
        when(stockCheckMapper.updateByIdAndStatus(eq(10L),
                eq(ErpStockCheckStatusEnum.DRAFT.getStatus()), any(ErpStockCheckDO.class)))
                .thenReturn(1);

        stockCheckService.submitStockCheck(10L);

        ArgumentCaptor<ErpStockCheckDO> captor = ArgumentCaptor.forClass(ErpStockCheckDO.class);
        verify(stockCheckMapper).updateByIdAndStatus(eq(10L),
                eq(ErpStockCheckStatusEnum.DRAFT.getStatus()), captor.capture());
        assertEquals(ErpStockCheckStatusEnum.PROCESS.getStatus(), captor.getValue().getStatus());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void submitDraft_rejectsEmptyItems() {
        when(stockCheckMapper.selectById(10L)).thenReturn(new ErpStockCheckDO()
                .setId(10L)
                .setNo("PD001")
                .setStatus(ErpStockCheckStatusEnum.DRAFT.getStatus())
                .setCheckTime(LocalDateTime.of(2026, 7, 27, 10, 0)));
        when(stockCheckItemMapper.selectListByCheckId(10L)).thenReturn(Collections.emptyList());

        assertServiceException(() -> stockCheckService.submitStockCheck(10L),
                STOCK_CHECK_SUBMIT_ITEMS_REQUIRED);
        verify(stockCheckMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void batchUpdateStockCheckItems_successAndRecalculateStockCount() {
        ErpStockCheckDO stockCheck = new ErpStockCheckDO()
                .setId(10L)
                .setNo("PD001")
                .setStatus(ErpStockCheckStatusEnum.PROCESS.getStatus())
                .setCheckType(ErpStockCheckTypeEnum.COUNT.getType())
                .setDeptId(1L);
        ErpStockCheckItemDO selectedItem = new ErpStockCheckItemDO()
                .setId(20L)
                .setCheckId(10L)
                .setProductId(100L)
                .setWarehouseId(1L)
                .setBatchNo("B001")
                .setProductPrice(BigDecimal.TEN)
                .setStockCount(BigDecimal.ONE)
                .setCount(new BigDecimal("2"));
        ErpStockCheckItemDO otherItem = new ErpStockCheckItemDO()
                .setId(21L)
                .setCheckId(10L)
                .setProductId(101L)
                .setWarehouseId(2L)
                .setProductPrice(new BigDecimal("5"))
                .setStockCount(BigDecimal.ZERO)
                .setCount(BigDecimal.ONE);
        when(stockCheckMapper.selectById(10L)).thenReturn(stockCheck);
        when(batchUpdateSupport.validateTargetWarehouse(30L)).thenReturn(new ErpWarehouseDO().setId(30L));
        when(stockCheckItemMapper.selectListByCheckId(10L)).thenReturn(Arrays.asList(selectedItem, otherItem));
        when(stockService.getStockCount(100L, 30L)).thenReturn(new BigDecimal("8"));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(100L).setUnitId(1L),
                new ErpProductDO().setId(101L).setUnitId(1L)));

        stockCheckService.batchUpdateStockCheckItems(buildBatchReq(10L, 30L, 20L));

        ArgumentCaptor<List<ErpStockCheckItemDO>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockCheckItemMapper).updateBatch(itemCaptor.capture());
        ErpStockCheckItemDO updatedItem = itemCaptor.getValue().get(0);
        assertEquals(30L, updatedItem.getWarehouseId());
        assertEquals(null, updatedItem.getBatchNo());
        assertEquals(new BigDecimal("8"), updatedItem.getStockCount());
        assertEquals(new BigDecimal("10"), updatedItem.getActualCount());
        assertEquals(0, new BigDecimal("20").compareTo(updatedItem.getTotalPrice()));
        ArgumentCaptor<ErpStockCheckDO> checkCaptor = ArgumentCaptor.forClass(ErpStockCheckDO.class);
        verify(stockCheckMapper).updateById(checkCaptor.capture());
        assertEquals(0, new BigDecimal("3").compareTo(checkCaptor.getValue().getTotalCount()));
        assertEquals(0, new BigDecimal("25").compareTo(checkCaptor.getValue().getTotalPrice()));
    }

    @Test
    void batchUpdateStockCheckItems_rejectsApproved() {
        when(stockCheckMapper.selectById(10L)).thenReturn(new ErpStockCheckDO()
                .setId(10L).setNo("PD001").setStatus(ErpStockCheckStatusEnum.APPROVE.getStatus()));

        assertServiceException(() -> stockCheckService.batchUpdateStockCheckItems(buildBatchReq(10L, 30L, 20L)),
                STOCK_CHECK_UPDATE_FAIL_APPROVE, "PD001");
        verify(stockCheckItemMapper, never()).updateBatch(anyList());
    }

    @Test
    void batchUpdateStockCheckItems_rejectsDuplicateAfterWarehouseChange() {
        ErpStockCheckDO stockCheck = new ErpStockCheckDO()
                .setId(10L)
                .setNo("PD001")
                .setStatus(ErpStockCheckStatusEnum.PROCESS.getStatus())
                .setCheckType(ErpStockCheckTypeEnum.COUNT.getType());
        when(stockCheckMapper.selectById(10L)).thenReturn(stockCheck);
        when(batchUpdateSupport.validateTargetWarehouse(30L)).thenReturn(new ErpWarehouseDO().setId(30L));
        when(stockCheckItemMapper.selectListByCheckId(10L)).thenReturn(Arrays.asList(
                new ErpStockCheckItemDO().setId(20L).setCheckId(10L).setProductId(100L).setWarehouseId(1L),
                new ErpStockCheckItemDO().setId(21L).setCheckId(10L).setProductId(100L).setWarehouseId(30L)));

        assertServiceException(() -> stockCheckService.batchUpdateStockCheckItems(buildBatchReq(10L, 30L, 20L)),
                STOCK_CHECK_ITEM_DUPLICATE, "100-30-");
        verify(stockCheckItemMapper, never()).updateBatch(anyList());
    }

    private ErpStockCheckItemBatchUpdateReqVO buildBatchReq(Long checkId, Long warehouseId, Long... itemIds) {
        ErpStockCheckItemBatchUpdateReqVO reqVO = new ErpStockCheckItemBatchUpdateReqVO();
        reqVO.setCheckId(checkId);
        reqVO.setWarehouseId(warehouseId);
        reqVO.setItemIds(Arrays.asList(itemIds));
        return reqVO;
    }

}
