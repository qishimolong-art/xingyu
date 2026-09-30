package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallAutoWarehouseResultBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

class ErpMallAutoWarehouseServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpMallAutoWarehouseServiceImpl service;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpMallProductMappingMapper mappingMapper;
    @Mock
    private ErpStockMapper stockMapper;

    @Test
    void allocate_nearestWarehouseInsufficient_selectNextWarehouse() {
        prepareWarehouses(warehouse(10L, "近仓", "104.000000", "30.000000", 1L),
                warehouse(20L, "次近仓", "105.000000", "30.000000", 1L));
        prepareMappingAndStocks(Arrays.asList(
                stock(100L, 1000L, 10L, "2", "0"),
                stock(200L, 1000L, 20L, "8", "1")));

        ErpMallAutoWarehouseResultBO result = allocate(item(0, 1L, 11L, 3));

        assertTrue(result.isReady());
        assertEquals(20L, result.getAssignments().get(0).getWarehouseId());
        assertEquals(200L, result.getAssignments().get(0).getStockId());
    }

    @Test
    void allocate_twoItems_accumulateStockWithinRequest() {
        prepareWarehouses(warehouse(10L, "近仓", "104.000000", "30.000000", 1L),
                warehouse(20L, "次近仓", "105.000000", "30.000000", 1L));
        prepareMappingAndStocks(Arrays.asList(
                stock(100L, 1000L, 10L, "5", "0"),
                stock(200L, 1000L, 20L, "9", "0")));

        ErpMallAutoWarehouseResultBO result = allocate(
                item(0, 1L, 11L, 3), item(1, 1L, 11L, 3));

        assertTrue(result.isReady());
        assertEquals(10L, result.getAssignments().get(0).getWarehouseId());
        assertEquals(20L, result.getAssignments().get(1).getWarehouseId());
    }

    @Test
    void allocate_noSingleWarehouseEnough_returnShortage() {
        prepareWarehouses(warehouse(10L, "仓一", "104.000000", "30.000000", 1L),
                warehouse(20L, "仓二", "105.000000", "30.000000", 1L));
        prepareMappingAndStocks(Arrays.asList(
                stock(100L, 1000L, 10L, "2", "0"),
                stock(200L, 1000L, 20L, "2", "0")));

        ErpMallAutoWarehouseResultBO result = allocate(item(0, 1L, 11L, 3));

        assertEquals(ErpMallAutoWarehouseResultBO.STATUS_STOCK_SHORTAGE, result.getStatus());
        assertEquals(1, result.getIssues().size());
        assertTrue(result.getAssignments().isEmpty());
    }

    @Test
    void allocate_sameDistance_sortByWarehouseSortThenId() {
        prepareWarehouses(warehouse(20L, "仓二", "104.100000", "30.000000", 2L),
                warehouse(10L, "仓一", "104.100000", "30.000000", 1L));
        prepareMappingAndStocks(Arrays.asList(
                stock(200L, 1000L, 20L, "10", "0"),
                stock(100L, 1000L, 10L, "10", "0")));

        ErpMallAutoWarehouseResultBO result = allocate(item(0, 1L, 11L, 1));

        assertEquals(10L, result.getAssignments().get(0).getWarehouseId());
    }

    @Test
    void allocate_noWarehouseCoordinate_returnLocationMissing() {
        when(warehouseService.getSaleWarehouseListByDeptId(1L)).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(10L).setName("未定位仓")));

        ErpMallAutoWarehouseResultBO result = allocate(item(0, 1L, 11L, 1));

        assertEquals(ErpMallAutoWarehouseResultBO.STATUS_WAREHOUSE_LOCATION_MISSING, result.getStatus());
        assertEquals(1, result.getIssues().size());
    }

    private ErpMallAutoWarehouseResultBO allocate(ErpMallAutoWarehouseResultBO.Item... items) {
        return service.allocate(1L, new BigDecimal("104.000000"), new BigDecimal("30.000000"),
                Arrays.asList(items));
    }

    private void prepareWarehouses(ErpWarehouseDO... warehouses) {
        when(warehouseService.getSaleWarehouseListByDeptId(1L)).thenReturn(Arrays.asList(warehouses));
    }

    private void prepareMappingAndStocks(List<ErpStockDO> stocks) {
        when(mappingMapper.selectListByMallSpuIds(anyCollection())).thenReturn(Collections.singletonList(
                ErpMallProductMappingDO.builder().mallSpuId(1L).mallSkuId(11L).erpProductId(1000L).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(stocks);
    }

    private ErpMallAutoWarehouseResultBO.Item item(int index, Long spuId, Long skuId, int count) {
        return new ErpMallAutoWarehouseResultBO.Item().setIndex(index).setSpuId(spuId).setSkuId(skuId).setCount(count);
    }

    private ErpWarehouseDO warehouse(Long id, String name, String longitude, String latitude, Long sort) {
        return new ErpWarehouseDO().setId(id).setName(name).setLongitude(new BigDecimal(longitude))
                .setLatitude(new BigDecimal(latitude)).setSort(sort);
    }

    private ErpStockDO stock(Long id, Long productId, Long warehouseId, String count, String lockCount) {
        return new ErpStockDO().setId(id).setProductId(productId).setWarehouseId(warehouseId)
                .setCount(new BigDecimal(count)).setLockCount(new BigDecimal(lockCount));
    }

}
