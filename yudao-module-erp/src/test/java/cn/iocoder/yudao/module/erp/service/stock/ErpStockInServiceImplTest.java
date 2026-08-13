package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_ITEM_DUPLICATE;
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
    private ErpStockItemBatchUpdateSupport batchUpdateSupport;
    @Mock
    private ErpOperateLogService operateLogService;

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
                STOCK_IN_ITEM_DUPLICATE, "100-20");
        verify(stockInItemMapper, never()).updateBatch(anyList());
    }

    private ErpStockInItemBatchUpdateReqVO buildReq(Long inId, Long warehouseId, Long... itemIds) {
        ErpStockInItemBatchUpdateReqVO reqVO = new ErpStockInItemBatchUpdateReqVO();
        reqVO.setInId(inId);
        reqVO.setWarehouseId(warehouseId);
        reqVO.setItemIds(Arrays.asList(itemIds));
        return reqVO;
    }

}
