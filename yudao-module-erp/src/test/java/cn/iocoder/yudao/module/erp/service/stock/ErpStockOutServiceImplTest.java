package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_ITEM_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_OUT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpStockOutServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockOutServiceImpl stockOutService;

    @Mock
    private ErpStockOutMapper stockOutMapper;
    @Mock
    private ErpStockOutItemMapper stockOutItemMapper;
    @Mock
    private ErpStockItemBatchUpdateSupport batchUpdateSupport;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void batchUpdateStockOutItems_success() {
        ErpStockOutDO stockOut = new ErpStockOutDO().setId(1L).setNo("OUT001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus()).setDeptId(10L);
        ErpWarehouseDO targetWarehouse = new ErpWarehouseDO().setId(20L);
        ErpStockOutItemDO item = new ErpStockOutItemDO().setId(11L).setOutId(1L).setProductId(100L).setWarehouseId(1L);
        when(stockOutMapper.selectById(1L)).thenReturn(stockOut);
        when(batchUpdateSupport.validateTargetWarehouse(20L)).thenReturn(targetWarehouse);
        when(stockOutItemMapper.selectListByOutId(1L)).thenReturn(Collections.singletonList(item));

        stockOutService.batchUpdateStockOutItems(buildReq(1L, 20L, 11L));

        ArgumentCaptor<List<ErpStockOutItemDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockOutItemMapper).updateBatch(captor.capture());
        assertEquals(20L, captor.getValue().get(0).getWarehouseId());
        verify(operateLogService).recordUpdate(ERP_STOCK_OUT_TYPE, 1L, "OUT001");
    }

    @Test
    void batchUpdateStockOutItems_rejectsApproved() {
        when(stockOutMapper.selectById(1L)).thenReturn(new ErpStockOutDO().setId(1L).setNo("OUT001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus()));

        assertServiceException(() -> stockOutService.batchUpdateStockOutItems(buildReq(1L, 20L, 11L)),
                STOCK_OUT_UPDATE_FAIL_APPROVE, "OUT001");
        verify(stockOutItemMapper, never()).updateBatch(anyList());
    }

    @Test
    void batchUpdateStockOutItems_rejectsDuplicateAfterWarehouseChange() {
        ErpStockOutDO stockOut = new ErpStockOutDO().setId(1L).setNo("OUT001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpWarehouseDO targetWarehouse = new ErpWarehouseDO().setId(20L);
        when(stockOutMapper.selectById(1L)).thenReturn(stockOut);
        when(batchUpdateSupport.validateTargetWarehouse(20L)).thenReturn(targetWarehouse);
        when(stockOutItemMapper.selectListByOutId(1L)).thenReturn(Arrays.asList(
                new ErpStockOutItemDO().setId(11L).setOutId(1L).setProductId(100L).setWarehouseId(1L),
                new ErpStockOutItemDO().setId(12L).setOutId(1L).setProductId(100L).setWarehouseId(20L)));

        assertServiceException(() -> stockOutService.batchUpdateStockOutItems(buildReq(1L, 20L, 11L)),
                STOCK_OUT_ITEM_DUPLICATE, "100-20");
        verify(stockOutItemMapper, never()).updateBatch(anyList());
    }

    private ErpStockOutItemBatchUpdateReqVO buildReq(Long outId, Long warehouseId, Long... itemIds) {
        ErpStockOutItemBatchUpdateReqVO reqVO = new ErpStockOutItemBatchUpdateReqVO();
        reqVO.setOutId(outId);
        reqVO.setWarehouseId(warehouseId);
        reqVO.setItemIds(Arrays.asList(itemIds));
        return reqVO;
    }

}
