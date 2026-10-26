package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockInStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_ITEM_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_MANUAL_APPROVE_FORBIDDEN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_IN_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockInServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockInServiceImpl stockInService;

    @Mock
    private ErpStockInMapper stockInMapper;
    @Mock
    private ErpStockInItemMapper stockInItemMapper;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpStockItemBatchUpdateSupport batchUpdateSupport;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpSalePickDeliveryService salePickDeliveryService;

    @Test
    void batchUpdateStockInItems_successAndEnsureStockExists() {
        ErpStockInDO stockIn = new ErpStockInDO().setId(1L).setNo("IN001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus()).setDeptId(10L);
        ErpWarehouseDO targetWarehouse = new ErpWarehouseDO().setId(20L);
        ErpStockInItemDO item = new ErpStockInItemDO().setId(11L).setInId(1L).setProductId(100L).setWarehouseId(1L);
        when(stockInMapper.selectById(1L)).thenReturn(stockIn);
        when(batchUpdateSupport.validateTargetWarehouse(20L)).thenReturn(targetWarehouse);
        when(stockInItemMapper.selectListByInId(1L)).thenReturn(Collections.singletonList(item));

        stockInService.batchUpdateStockInItems(buildReq(1L, 20L, 11L));

        ArgumentCaptor<List<ErpStockInItemDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockInItemMapper).updateBatch(captor.capture());
        assertEquals(20L, captor.getValue().get(0).getWarehouseId());
        verify(stockService).ensureStockExists(100L, 20L);
        verify(operateLogService).recordUpdate(ERP_STOCK_IN_TYPE, 1L, "IN001");
    }

    @Test
    void batchUpdateStockInItems_rejectsApproved() {
        when(stockInMapper.selectById(1L)).thenReturn(new ErpStockInDO().setId(1L).setNo("IN001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus()));

        assertServiceException(() -> stockInService.batchUpdateStockInItems(buildReq(1L, 20L, 11L)),
                STOCK_IN_UPDATE_FAIL_APPROVE, "IN001");
        verify(stockInItemMapper, never()).updateBatch(anyList());
    }

    @Test
    void batchUpdateStockInItems_rejectsDuplicateAfterWarehouseChange() {
        ErpStockInDO stockIn = new ErpStockInDO().setId(1L).setNo("IN001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpWarehouseDO targetWarehouse = new ErpWarehouseDO().setId(20L);
        when(stockInMapper.selectById(1L)).thenReturn(stockIn);
        when(batchUpdateSupport.validateTargetWarehouse(20L)).thenReturn(targetWarehouse);
        when(stockInItemMapper.selectListByInId(1L)).thenReturn(Arrays.asList(
                new ErpStockInItemDO().setId(11L).setInId(1L).setProductId(100L).setWarehouseId(1L),
                new ErpStockInItemDO().setId(12L).setInId(1L).setProductId(100L).setWarehouseId(20L)));

        assertServiceException(() -> stockInService.batchUpdateStockInItems(buildReq(1L, 20L, 11L)),
                STOCK_IN_ITEM_DUPLICATE, "100-20-");
        verify(stockInItemMapper, never()).updateBatch(anyList());
    }

    @Test
    void updateStockInStatus_rejectsManualApprove() {
        assertServiceException(() -> stockInService.updateStockInStatus(1L, ErpAuditStatus.APPROVE.getStatus()),
                STOCK_IN_MANUAL_APPROVE_FORBIDDEN);
        verify(stockInMapper, never()).updateByIdAndStatus(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void autoApproveAfterPick_postsInventoryAndApproveUser() {
        ErpStockInDO stockIn = new ErpStockInDO().setId(1L).setNo("IN001")
                .setStatus(ErpStockInStatusEnum.PICKING.getStatus())
                .setInTime(LocalDateTime.of(2026, 10, 20, 9, 0));
        ErpStockInItemDO item = new ErpStockInItemDO().setId(11L).setInId(1L)
                .setProductId(100L).setWarehouseId(20L).setCount(BigDecimal.ONE)
                .setProductPrice(new BigDecimal("12.50"));
        when(stockInMapper.selectByIdForUpdate(1L)).thenReturn(stockIn);
        when(salePickDeliveryService.getSummaryMapByStockInIds(Collections.singleton(1L))).thenReturn(Map.of(
                1L, new ErpSalePickDeliverySummaryRespVO().setPickStatus(ErpSalePickStatusEnum.DONE.getStatus())));
        when(stockInItemMapper.selectListByInIdForUpdate(1L)).thenReturn(Collections.singletonList(item));
        when(stockInMapper.updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(ErpStockInStatusEnum.PICKING.getStatus()),
                org.mockito.ArgumentMatchers.any(ErpStockInDO.class))).thenReturn(1);

        stockInService.autoApproveAfterPick(1L, 9L);

        ArgumentCaptor<ErpStockInDO> captor = ArgumentCaptor.forClass(ErpStockInDO.class);
        verify(stockInMapper).updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(ErpStockInStatusEnum.PICKING.getStatus()), captor.capture());
        assertEquals(ErpStockInStatusEnum.APPROVE.getStatus(), captor.getValue().getStatus());
        assertEquals(9L, captor.getValue().getApproveUserId());
        verify(stockRecordService).createStockRecord(org.mockito.ArgumentMatchers.any());
    }

    private ErpStockInItemBatchUpdateReqVO buildReq(Long inId, Long warehouseId, Long... itemIds) {
        ErpStockInItemBatchUpdateReqVO reqVO = new ErpStockInItemBatchUpdateReqVO();
        reqVO.setInId(inId);
        reqVO.setWarehouseId(warehouseId);
        reqVO.setItemIds(Arrays.asList(itemIds));
        return reqVO;
    }

}
