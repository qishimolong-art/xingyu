package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.service.stock.cost.ErpStockDimensionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpProductStockInitServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpProductStockInitService productStockInitService;

    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockDimensionService stockDimensionService;

    @Test
    void ensureProductsStockForEnabledRealWarehouses_excludesDirectWarehouseAndExistingStock() {
        ErpWarehouseDO realWarehouse = new ErpWarehouseDO().setId(20L).setName("主仓").setDeptId(30L);
        ErpWarehouseDO directWarehouse = new ErpWarehouseDO().setId(21L).setName("直发仓").setDeptId(31L);
        ErpWarehouseDO branchWarehouse = new ErpWarehouseDO().setId(22L).setName("分仓").setDeptId(32L);
        when(warehouseService.getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()))
                .thenReturn(Arrays.asList(realWarehouse, directWarehouse, branchWarehouse));
        when(stockDimensionService.initializeDimensions(anyCollection())).thenReturn(false);
        when(stockMapper.selectListByProductIdsAndWarehouseIds(anyCollection(), anyCollection()))
                .thenReturn(Collections.singletonList(new ErpStockDO()
                        .setProductId(10L).setWarehouseId(20L)));

        productStockInitService.ensureProductsStockForEnabledRealWarehouses(Arrays.asList(10L, 11L, 10L, null));

        ArgumentCaptor<Collection<ErpStockDO>> stockCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(stockMapper).insertBatch(stockCaptor.capture(), eq(500));
        List<ErpStockDO> insertedStocks = (List<ErpStockDO>) stockCaptor.getValue();
        assertEquals(3, insertedStocks.size());
        assertStock(insertedStocks.get(0), 10L, 22L, 32L);
        assertStock(insertedStocks.get(1), 11L, 20L, 30L);
        assertStock(insertedStocks.get(2), 11L, 22L, 32L);
        assertFalse(insertedStocks.stream().anyMatch(stock -> Long.valueOf(21L).equals(stock.getWarehouseId())));
    }

    @Test
    void ensureProductsStockForEnabledRealWarehouses_whenDimensionServiceHandles_thenSkipsLegacyInsert() {
        when(warehouseService.getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(20L).setName("主仓").setDeptId(30L)));
        when(stockDimensionService.initializeDimensions(anyCollection())).thenReturn(true);

        productStockInitService.ensureProductsStockForEnabledRealWarehouses(Collections.singletonList(10L));

        verify(stockMapper, never()).selectListByProductIdsAndWarehouseIds(anyCollection(), anyCollection());
        verify(stockMapper, never()).insertBatch(anyCollection(), eq(500));
    }

    @Test
    void ensureStockExists_existingStockAlignsWarehouseDepartmentWithoutResettingCounts() {
        ErpStockDO existing = new ErpStockDO().setId(1L).setProductId(10L).setWarehouseId(20L)
                .setDeptId(99L).setCount(new BigDecimal("8"));
        when(stockDimensionService.initializeDimensions(anyCollection())).thenReturn(false);
        when(warehouseService.getWarehouse(20L)).thenReturn(new ErpWarehouseDO().setId(20L).setDeptId(30L));
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(existing);

        productStockInitService.ensureStockExists(10L, 20L);

        verify(stockMapper).updateById(org.mockito.ArgumentMatchers.<ErpStockDO>argThat(update ->
                Long.valueOf(1L).equals(update.getId())
                        && Long.valueOf(30L).equals(update.getDeptId())
                        && update.getCount() == null));
        verify(stockMapper, never()).insert(any(ErpStockDO.class));
    }

    private void assertStock(ErpStockDO stock, Long productId, Long warehouseId, Long deptId) {
        assertEquals(productId, stock.getProductId());
        assertEquals(warehouseId, stock.getWarehouseId());
        assertEquals(deptId, stock.getDeptId());
        assertEquals(BigDecimal.ZERO, stock.getCount());
        assertEquals(BigDecimal.ZERO, stock.getLockCount());
        assertEquals(BigDecimal.ZERO, stock.getCostPrice());
        assertEquals(BigDecimal.ZERO, stock.getCostAmount());
    }

}
