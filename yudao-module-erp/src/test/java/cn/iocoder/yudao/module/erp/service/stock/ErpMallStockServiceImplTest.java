package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockSummaryBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock
    private DeptApi deptApi;

    @BeforeEach
    void setUp() {
        lenient().when(warehouseMapper.selectBatchIds(anyCollection())).thenReturn(Collections.emptyList());
        lenient().when(stockMapper.selectOccupiedCountMap(anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyMap());
        lenient().when(stockMapper.selectPendingInCountMap(anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyMap());
        lenient().when(purchaseOrderItemMapper.selectInTransitCountMap(anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyMap());
        lenient().when(deptApi.getDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
    }

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
        assertEquals(new BigDecimal("9"), result.getTotalCount());
        assertEquals(new BigDecimal("5"), result.getTotalLockCount());
        assertEquals(1, result.getWarehouseStocks().size());
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
    void testGetMallStockSummaryDetail_returnWarehouseStockRows() {
        Long spuId = 1024L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Arrays.asList(
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(100L).build(),
                ErpMallProductMappingDO.builder().erpProductId(20L).mallSpuId(spuId).mallSkuId(200L).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setProductId(10L).setWarehouseId(1L).setShelf("A-01")
                        .setCount(new BigDecimal("8")).setLockCount(new BigDecimal("2")),
                new ErpStockDO().setProductId(20L).setWarehouseId(1L).setShelf("B-02")
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ONE),
                new ErpStockDO().setProductId(20L).setWarehouseId(2L)
                        .setCount(new BigDecimal("3")).setLockCount(BigDecimal.ZERO)));
        when(warehouseMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(1L).setName("成都总仓").setDeptId(11L),
                new ErpWarehouseDO().setId(2L).setName("绵阳仓").setDeptId(12L)));
        when(deptApi.getDeptMap(anyCollection())).thenReturn(Map.of(
                11L, new DeptRespDTO().setId(11L).setName("成都分部"),
                12L, new DeptRespDTO().setId(12L).setName("绵阳分部")));
        when(stockMapper.selectOccupiedCountMap(anyCollection(), anyCollection())).thenReturn(Map.of(
                "10_1", new BigDecimal("1.5"),
                "20_1", new BigDecimal("0.5"),
                "20_2", BigDecimal.ONE));
        when(stockMapper.selectPendingInCountMap(anyCollection(), anyCollection())).thenReturn(Map.of(
                "10_1", new BigDecimal("4")));
        when(purchaseOrderItemMapper.selectInTransitCountMap(anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(Map.of("20_2", new BigDecimal("6")));

        ErpMallStockSummaryBO result = mallStockService.getMallStockSummaryDetail(spuId);

        assertEquals(new BigDecimal("16"), result.getTotalCount());
        assertEquals(new BigDecimal("13"), result.getTotalAvailableCount());
        assertEquals(new BigDecimal("3.0"), result.getTotalOccupiedCount());
        assertEquals(new BigDecimal("4"), result.getTotalPendingInCount());
        assertEquals(new BigDecimal("6"), result.getTotalInTransitCount());
        assertEquals(2, result.getWarehouseStocks().size());
        ErpMallStockSummaryBO.WarehouseStock chengdu = result.getWarehouseStocks().get(0);
        assertEquals(1L, chengdu.getWarehouseId());
        assertEquals("成都总仓", chengdu.getWarehouseName());
        assertEquals("成都分部", chengdu.getDeptName());
        assertEquals("A-01 / B-02", chengdu.getShelf());
        assertEquals(new BigDecimal("13"), chengdu.getCount());
        assertEquals(new BigDecimal("3"), chengdu.getLockCount());
        assertEquals(new BigDecimal("10"), chengdu.getAvailableCount());
    }

    @Test
    void testGetMallStockSummary_noMappingReturnsZero() {
        Long spuId = 1024L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Collections.emptyList());

        BigDecimal result = mallStockService.getMallStockSummary(spuId);

        assertEquals(BigDecimal.ZERO, result);
        verifyNoInteractions(stockMapper, warehouseMapper, purchaseOrderItemMapper, deptApi);
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
        verifyNoInteractions(stockMapper, warehouseMapper, purchaseOrderItemMapper, deptApi);
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

    @Test
    void testGetMallStockOptionPage_sortByAvailableAndDistance() {
        Long spuId = 1024L;
        Long skuId = 2048L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Collections.singletonList(
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(skuId).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setId(1L).setProductId(10L).setWarehouseId(100L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO),
                new ErpStockDO().setId(2L).setProductId(10L).setWarehouseId(200L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO),
                new ErpStockDO().setId(3L).setProductId(10L).setWarehouseId(300L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO),
                new ErpStockDO().setId(4L).setProductId(10L).setWarehouseId(400L)
                        .setCount(BigDecimal.ZERO).setLockCount(BigDecimal.ZERO)));
        when(warehouseMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(100L).setName("远仓")
                        .setLongitude(new BigDecimal("104.200000")).setLatitude(new BigDecimal("30.800000")),
                new ErpWarehouseDO().setId(200L).setName("近仓")
                        .setLongitude(new BigDecimal("104.070000")).setLatitude(new BigDecimal("30.580000")),
                new ErpWarehouseDO().setId(300L).setName("未定位仓"),
                new ErpWarehouseDO().setId(400L).setName("缺货近仓")
                        .setLongitude(new BigDecimal("104.060000")).setLatitude(new BigDecimal("30.570000"))));

        PageResult<ErpMallStockOptionBO> result = mallStockService.getMallStockOptionPage(spuId, skuId, 1, 10,
                new BigDecimal("104.066800"), new BigDecimal("30.572800"));

        assertEquals(4L, result.getTotal());
        assertEquals(2L, result.getList().get(0).getStockId());
        assertEquals(1L, result.getList().get(1).getStockId());
        assertEquals(3L, result.getList().get(2).getStockId());
        assertEquals(4L, result.getList().get(3).getStockId());
        assertTrue(result.getList().get(0).getDistanceMeters() < result.getList().get(1).getDistanceMeters());
        assertNull(result.getList().get(2).getDistanceMeters());
        assertFalse(result.getList().get(3).getAvailable());
    }

    @Test
    void testGetMallStockOptionPage_pageAfterSorted() {
        Long spuId = 1024L;
        Long skuId = 2048L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Collections.singletonList(
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(skuId).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setId(1L).setProductId(10L).setWarehouseId(100L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO),
                new ErpStockDO().setId(2L).setProductId(10L).setWarehouseId(200L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO),
                new ErpStockDO().setId(3L).setProductId(10L).setWarehouseId(300L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO)));
        when(warehouseMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(100L).setName("第三仓")
                        .setLongitude(new BigDecimal("104.300000")).setLatitude(new BigDecimal("30.900000")),
                new ErpWarehouseDO().setId(200L).setName("第一仓")
                        .setLongitude(new BigDecimal("104.070000")).setLatitude(new BigDecimal("30.580000")),
                new ErpWarehouseDO().setId(300L).setName("第二仓")
                        .setLongitude(new BigDecimal("104.090000")).setLatitude(new BigDecimal("30.600000"))));

        PageResult<ErpMallStockOptionBO> result = mallStockService.getMallStockOptionPage(spuId, skuId, 2, 1,
                new BigDecimal("104.066800"), new BigDecimal("30.572800"));

        assertEquals(3L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(3L, result.getList().get(0).getStockId());
    }

    @Test
    void testGetMallStockOptionPage_invalidCoordinatesDoNotCalculateDistance() {
        Long spuId = 1024L;
        Long skuId = 2048L;
        when(mallProductMappingMapper.selectListByMallSpuId(spuId)).thenReturn(Collections.singletonList(
                ErpMallProductMappingDO.builder().erpProductId(10L).mallSpuId(spuId).mallSkuId(skuId).build()));
        when(stockMapper.selectListByProductIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpStockDO().setId(1L).setProductId(10L).setWarehouseId(100L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO),
                new ErpStockDO().setId(2L).setProductId(10L).setWarehouseId(200L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO)));
        when(warehouseMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(100L).setName("零坐标仓")
                        .setLongitude(BigDecimal.ZERO).setLatitude(BigDecimal.ZERO),
                new ErpWarehouseDO().setId(200L).setName("正常仓")
                        .setLongitude(new BigDecimal("104.070000")).setLatitude(new BigDecimal("30.580000"))));

        PageResult<ErpMallStockOptionBO> result = mallStockService.getMallStockOptionPage(spuId, skuId, 1, 10,
                BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(2L, result.getTotal());
        assertNull(result.getList().get(0).getDistanceMeters());
        assertNull(result.getList().get(1).getDistanceMeters());
    }

}
