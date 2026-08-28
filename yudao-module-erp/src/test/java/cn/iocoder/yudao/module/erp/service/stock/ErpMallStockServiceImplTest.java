package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockSummaryBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ErpMallStockServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpMallStockServiceImpl mallStockService;

    @Mock
    private ErpMallProductMappingMapper mallProductMappingMapper;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpWarehouseMapper warehouseMapper;

    @Test
    void testGetMallStockSummary_sumAllMappedProductAvailableStock() {
        Long spuId = 1024L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Arrays.asList(
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(100L).build(),
                ErpMallProductMappingDO.builder().erpProductId(20L).mallSpuId(spuId).mallSkuId(200L).build(),
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(101L).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setProductId(10L).setCount(new BigDecimal("8")).setLockCount(new BigDecimal("2")),
                new ErpStockDO().setProductId(20L).setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO),
                new ErpStockDO().setProductId(20L).setCount(new BigDecimal("3")).setLockCount(BigDecimal.ONE)));

        BigDecimal result = mallStockService.getMallStockSummary(spuId);

        assertEquals(new BigDecimal("13"), result);
        verifyNoInteractions(warehouseMapper);
    }

    @Test
    void testGetMallStockSummaryDetail_returnMappingState() {
        Long spuId = 1024L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Arrays.asList(
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(100L).build(),
                ErpMallProductMappingDO.builder().erpProductId(20L).mallSpuId(spuId).mallSkuId(200L).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setProductId(10L).setCount(new BigDecimal("8")).setLockCount(new BigDecimal("2")),
                new ErpStockDO().setProductId(20L).setCount(new BigDecimal("1")).setLockCount(new BigDecimal("3"))));

        ErpMallStockSummaryBO result = mallStockService.getMallStockSummaryDetail(spuId);

        assertEquals(spuId, result.getSpuId());
        assertTrue(result.getMapped());
        assertEquals(2, result.getMappingCount());
        assertEquals(new BigDecimal("6"), result.getTotalAvailableCount());
    }

    @Test
    void testGetMallStockSummary_negativeAvailableCountDisplaysAsZero() {
        Long spuId = 1024L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Collections.singletonList(
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(100L).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setProductId(10L).setCount(new BigDecimal("1")).setLockCount(new BigDecimal("3")),
                new ErpStockDO().setProductId(10L).setCount(new BigDecimal("4")).setLockCount(BigDecimal.ONE)));

        BigDecimal result = mallStockService.getMallStockSummary(spuId);

        assertEquals(new BigDecimal("3"), result);
    }

    @Test
    void testGetMallStockSummary_noMappingReturnsZero() {
        Long spuId = 1024L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Collections.emptyList());

        BigDecimal result = mallStockService.getMallStockSummary(spuId);

        assertEquals(BigDecimal.ZERO, result);
        verifyNoInteractions(stockMapper, warehouseMapper);
    }

    @Test
    void testGetMallSpuAvailableStockMap_sumMappedProductAvailableStock() {
        when(mallProductMappingMapper.selectListByMallSpuIds(anyCollection())).thenReturn(Arrays.asList(
                ErpMallProductMappingDO.builder().mallSpuId(100L).mallSkuId(1000L).erpProductId(10L).build(),
                ErpMallProductMappingDO.builder().mallSpuId(100L).mallSkuId(1001L).erpProductId(10L).build(),
                ErpMallProductMappingDO.builder().mallSpuId(100L).mallSkuId(1002L).erpProductId(20L).build(),
                ErpMallProductMappingDO.builder().mallSpuId(200L).mallSkuId(2000L).erpProductId(30L).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setProductId(10L).setCount(new BigDecimal("8")).setLockCount(new BigDecimal("2")),
                new ErpStockDO().setProductId(20L).setCount(new BigDecimal("1")).setLockCount(new BigDecimal("3")),
                new ErpStockDO().setProductId(30L).setCount(new BigDecimal("5")).setLockCount(BigDecimal.ONE)));

        Map<Long, BigDecimal> result = mallStockService.getMallSpuAvailableStockMap(Arrays.asList(100L, 200L, 300L));

        assertEquals(new BigDecimal("6"), result.get(100L));
        assertEquals(new BigDecimal("4"), result.get(200L));
        assertEquals(BigDecimal.ZERO, result.get(300L));
        verifyNoInteractions(warehouseMapper);
    }

    @Test
    void testGetMallSkuAvailableStockMap_sumSkuMappedProductAvailableStock() {
        Long spuId = 1024L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Arrays.asList(
                ErpMallProductMappingDO.builder().mallSpuId(spuId).mallSkuId(100L).erpProductId(10L).build(),
                ErpMallProductMappingDO.builder().mallSpuId(spuId).mallSkuId(100L).erpProductId(10L).build(),
                ErpMallProductMappingDO.builder().mallSpuId(spuId).mallSkuId(200L).erpProductId(20L).build(),
                ErpMallProductMappingDO.builder().mallSpuId(spuId).mallSkuId(300L).erpProductId(30L).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setProductId(10L).setCount(new BigDecimal("2.9")).setLockCount(new BigDecimal("0.4")),
                new ErpStockDO().setProductId(20L).setCount(new BigDecimal("1")).setLockCount(new BigDecimal("2"))));

        Map<Long, BigDecimal> result = mallStockService.getMallSkuAvailableStockMap(spuId, Arrays.asList(100L, 200L, 400L));

        assertEquals(new BigDecimal("2.5"), result.get(100L));
        assertEquals(BigDecimal.ZERO, result.get(200L));
        assertEquals(BigDecimal.ZERO, result.get(400L));
        verifyNoInteractions(warehouseMapper);
    }

    @Test
    void testGetMallStockSummaryDetail_noMappingReturnsEmptyState() {
        Long spuId = 1024L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Collections.emptyList());

        ErpMallStockSummaryBO result = mallStockService.getMallStockSummaryDetail(spuId);

        assertEquals(spuId, result.getSpuId());
        assertFalse(result.getMapped());
        assertEquals(0, result.getMappingCount());
        assertEquals(BigDecimal.ZERO, result.getTotalAvailableCount());
        verifyNoInteractions(stockMapper, warehouseMapper);
    }

    @Test
    void testGetMallStockOptions_includeUnavailableStockAsOrderOption() {
        Long spuId = 1024L;
        Long skuId = 2048L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Collections.singletonList(
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(skuId).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setId(1L).setProductId(10L).setWarehouseId(100L)
                        .setCount(new BigDecimal("5")).setLockCount(new BigDecimal("2")),
                new ErpStockDO().setId(2L).setProductId(10L).setWarehouseId(200L)
                        .setCount(BigDecimal.ZERO).setLockCount(BigDecimal.ZERO)));
        when(warehouseMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(100L).setName("现货仓"),
                new ErpWarehouseDO().setId(200L).setName("订货仓")));

        List<ErpMallStockOptionBO> result = mallStockService.getMallStockOptions(spuId, skuId);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getAvailable());
        assertEquals("现货", result.get(0).getAvailableStatusText());
        assertFalse(result.get(1).getAvailable());
        assertEquals("订货", result.get(1).getAvailableStatusText());
    }

}
