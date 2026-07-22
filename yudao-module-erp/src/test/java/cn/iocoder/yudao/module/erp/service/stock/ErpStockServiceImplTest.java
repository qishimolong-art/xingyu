package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockBatchNoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpStockServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockServiceImpl stockService;

    @Mock
    private ErpProductService productService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpStockLockMapper stockLockMapper;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private ErpStockCheckService stockCheckService;

    @Test
    public void testGetStockBatchBalanceListMap_groupsLedgerBalancesByStock() {
        ErpStockDO stock = new ErpStockDO().setProductId(10L).setWarehouseId(20L);
        Map<String, Object> row = new HashMap<>();
        row.put("product_id", 10L);
        row.put("warehouse_id", 20L);
        row.put("batch_no", "B-001");
        row.put("available_count", new BigDecimal("8"));
        LocalDateTime firstInTime = LocalDateTime.of(2026, 7, 1, 9, 0);
        row.put("first_in_time", firstInTime);
        when(stockRecordMapper.selectBatchBalanceList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.singletonList(row));

        Map<String, List<ErpStockBatchNoRespVO>> result =
                stockService.getStockBatchBalanceListMap(Collections.singletonList(stock));

        assertEquals(1, result.size());
        ErpStockBatchNoRespVO batch = result.get("10_20").get(0);
        assertEquals("B-001", batch.getBatchNo());
        assertEquals(new BigDecimal("8"), batch.getAvailableCount());
        assertEquals(firstInTime, batch.getFirstInTime());
    }

    @Test
    public void testGetStockBatchBalanceListMap_keepsZeroBalanceNamedBatch() {
        ErpStockDO stock = new ErpStockDO().setProductId(10L).setWarehouseId(20L);
        Map<String, Object> row = new HashMap<>();
        row.put("product_id", 10L);
        row.put("warehouse_id", 20L);
        row.put("batch_no", "B-SOLD-OUT");
        row.put("available_count", BigDecimal.ZERO);
        row.put("first_in_time", LocalDateTime.of(2026, 7, 20, 9, 0));
        when(stockRecordMapper.selectBatchBalanceList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.singletonList(row));

        Map<String, List<ErpStockBatchNoRespVO>> result =
                stockService.getStockBatchBalanceListMap(Collections.singletonList(stock));

        assertEquals(1, result.get("10_20").size());
        assertEquals("B-SOLD-OUT", result.get("10_20").get(0).getBatchNo());
        assertEquals(BigDecimal.ZERO, result.get("10_20").get(0).getAvailableCount());
    }

    @Test
    public void testGetStockPage_productName_usesProductIdFilter() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setProductName("brake pad");
        List<Long> productIds = Arrays.asList(10L, 20L);
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.getCurrentUserProductStockPermissionScope()).thenReturn(
                new ErpProductStockPermissionScope(true,
                        new LinkedHashSet<>(Arrays.asList(10L, 20L)), Collections.emptySet(), 1L));
        when(productMapper.selectIdsByComplexQueryWithoutKeyword(eq(reqVO), isNull(), isNull())).thenReturn(productIds);
        when(stockMapper.selectPage(eq(reqVO), eq(productIds),
                eq(new LinkedHashSet<>(Arrays.asList(10L, 20L))), isNull(), isNull())).thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(productMapper).selectIdsByComplexQueryWithoutKeyword(eq(reqVO), isNull(), isNull());
        verify(stockMapper).selectPage(eq(reqVO), eq(productIds),
                eq(new LinkedHashSet<>(Arrays.asList(10L, 20L))), isNull(), isNull());
    }

    @Test
    public void testGetStockPage_blankProductName_doesNotUseProductIdFilter() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setProductName("   ");
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.getCurrentUserProductStockPermissionScope()).thenReturn(
                new ErpProductStockPermissionScope(true,
                        new LinkedHashSet<>(Arrays.asList(10L, 20L)), Collections.emptySet(), 1L));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Arrays.asList(10L, 20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull()))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(productMapper, never()).selectIdsByComplexQuery(eq(reqVO), isNull(), isNull());
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Arrays.asList(10L, 20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull());
    }

    @Test
    public void testGetStockPage_saleBizType_includesSaleDistributedWarehouses() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("sale");
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(false);
        when(warehouseService.getCurrentUserVisibleSaleWarehouseList()).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(10L), new ErpWarehouseDO().setId(20L)));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Arrays.asList(10L, 20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull()))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(warehouseService).getCurrentUserVisibleSaleWarehouseList();
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Arrays.asList(10L, 20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull());
    }

    @Test
    public void testGetStockPage_nonSaleBizType_usesProductStockDepartmentAndSelfScope() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.getCurrentUserProductStockPermissionScope()).thenReturn(
                new ErpProductStockPermissionScope(false,
                        new LinkedHashSet<>(Collections.singletonList(10L)),
                        new LinkedHashSet<>(Collections.singletonList(20L)), 104L));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Arrays.asList(10L, 20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(10L))),
                eq(new LinkedHashSet<>(Collections.singletonList(20L))), eq("104")))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(warehouseService, never()).getCurrentUserVisibleSaleWarehouseList();
        verify(warehouseService).getCurrentUserProductStockPermissionScope();
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Arrays.asList(10L, 20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(10L))),
                eq(new LinkedHashSet<>(Collections.singletonList(20L))), eq("104"));
    }

    @Test
    public void testGetStockPage_purchaseBizType_usesOnlyAuthorizedPurchaseWarehouses() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("purchase");
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(false);
        when(warehouseService.getCurrentUserAuthorizedPurchaseWarehouseList()).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(10L)));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(10L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull()))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(warehouseService).getCurrentUserAuthorizedPurchaseWarehouseList();
        verify(warehouseService, never()).getCurrentUserVisibleSaleWarehouseList();
        verify(warehouseService, never()).getCurrentUserStockVisibleWarehouseList();
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(10L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull());
    }

    @Test
    public void testGetStockPage_purchaseBizType_allWarehousePermissionStillFiltersPurchaseEnabledWarehouses() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("purchase");
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(true);
        when(warehouseService.getCurrentUserAuthorizedPurchaseWarehouseList()).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(10L)));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(10L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull()))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(warehouseService).getCurrentUserAuthorizedPurchaseWarehouseList();
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(10L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull());
    }

    @Test
    public void testGetStockPage_purchaseBizType_deptFilterIntersectsAuthorizedPurchaseWarehouses() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("purchase");
        reqVO.setDeptId(30L);
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.getWarehouseListByDeptId(30L)).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(10L), new ErpWarehouseDO().setId(20L)));
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(false);
        when(warehouseService.getCurrentUserAuthorizedPurchaseWarehouseList()).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(20L)));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull()))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(warehouseService).getWarehouseListByDeptId(30L);
        verify(warehouseService).getCurrentUserAuthorizedPurchaseWarehouseList();
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull());
    }

    @Test
    public void testGetStockPage_saleBizType_deptFilterIntersectsCurrentUserVisibleWarehouses() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("sale");
        reqVO.setDeptId(30L);
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.getWarehouseListByDeptId(30L)).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(10L),
                new ErpWarehouseDO().setId(20L)));
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(false);
        when(warehouseService.getCurrentUserVisibleSaleWarehouseList()).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(20L)));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull()))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(warehouseService, never()).getSaleWarehouseListByDeptId(30L);
        verify(warehouseService).getWarehouseListByDeptId(30L);
        verify(warehouseService).getCurrentUserVisibleSaleWarehouseList();
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull());
    }

    @Test
    public void testGetStockPage_nonSaleDeptFilterExcludesExternalDistributedWarehouse() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setDeptId(30L);
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.getWarehouseListByDeptId(30L)).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(20L)));
        when(warehouseService.getCurrentUserProductStockPermissionScope()).thenReturn(
                new ErpProductStockPermissionScope(false,
                        new LinkedHashSet<>(Collections.singletonList(10L)), Collections.emptySet(), 104L));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>()),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(10L))),
                eq(Collections.emptySet()), eq("104")))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(warehouseService).getWarehouseListByDeptId(30L);
        verify(warehouseService).getCurrentUserProductStockPermissionScope();
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>()), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(10L))), eq(Collections.emptySet()), eq("104"));
    }

    @Test
    public void testEnsureStockExists_directWarehouseCreatesNormalZeroStockCarrier() {
        when(warehouseService.getWarehouse(20L)).thenReturn(
                new ErpWarehouseDO().setId(20L).setDeptId(30L).setName("直发仓"));
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(null);

        stockService.ensureStockExists(10L, 20L);

        ArgumentCaptor<ErpStockDO> stockCaptor = ArgumentCaptor.forClass(ErpStockDO.class);
        verify(stockMapper).insert(stockCaptor.capture());
        ErpStockDO stock = stockCaptor.getValue();
        assertEquals(Long.valueOf(10L), stock.getProductId());
        assertEquals(Long.valueOf(20L), stock.getWarehouseId());
        assertEquals(Long.valueOf(30L), stock.getDeptId());
        assertEquals(BigDecimal.ZERO, stock.getCount());
        assertEquals(BigDecimal.ZERO, stock.getLockCount());
        assertEquals(BigDecimal.ZERO, stock.getCostPrice());
        assertEquals(BigDecimal.ZERO, stock.getCostAmount());
    }

    @Test
    public void testEnsureStockExists_existingStockAlignsWarehouseDepartmentWithoutResettingCounts() {
        ErpStockDO existing = new ErpStockDO().setId(1L).setProductId(10L).setWarehouseId(20L)
                .setDeptId(99L).setCount(new BigDecimal("8"));
        when(warehouseService.getWarehouse(20L)).thenReturn(
                new ErpWarehouseDO().setId(20L).setDeptId(30L));
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(existing);

        stockService.ensureStockExists(10L, 20L);

        verify(stockMapper).updateById(org.mockito.ArgumentMatchers.<ErpStockDO>argThat(update ->
                Long.valueOf(1L).equals(update.getId())
                        && Long.valueOf(30L).equals(update.getDeptId())
                        && update.getCount() == null));
        verify(stockMapper, never()).insert(any(ErpStockDO.class));
    }

    @Test
    public void testGetStockPage_saleDeptFilter_intersectsCurrentUserVisibleWarehouses() {
        ErpStockPageReqVO reqVO = new ErpStockPageReqVO();
        reqVO.setBizType("sale");
        reqVO.setSaleDeptId(30L);
        PageResult<ErpStockDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(warehouseService.hasCurrentUserAllWarehousePermission()).thenReturn(false);
        when(warehouseService.getSaleWarehouseListByDeptId(30L)).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(20L), new ErpWarehouseDO().setId(30L)));
        when(warehouseService.getCurrentUserVisibleSaleWarehouseList()).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(10L), new ErpWarehouseDO().setId(20L)));
        when(stockMapper.selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull()))
                .thenReturn(pageResult);

        PageResult<ErpStockDO> result = stockService.getStockPage(reqVO);

        assertSame(pageResult, result);
        verify(warehouseService).getSaleWarehouseListByDeptId(30L);
        verify(warehouseService).getCurrentUserVisibleSaleWarehouseList();
        verify(stockMapper).selectPage(eq(reqVO), org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                eq(new LinkedHashSet<>(Collections.singletonList(20L))),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull(),
                org.mockito.ArgumentMatchers.<Collection<Long>>isNull());
    }

    @Test
    public void testUpdateStockCountAndCost_outWithUnitPrice_deductsByBusinessPrice() {
        ErpStockDO stock = new ErpStockDO().setId(1L)
                .setProductId(10L).setWarehouseId(20L)
                .setCount(new BigDecimal("10"))
                .setCostPrice(new BigDecimal("12.300000"))
                .setCostAmount(new BigDecimal("123"));
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(stock);
        when(stockMapper.updateCountAndCost(eq(1L), eq(new BigDecimal("10")), eq(new BigDecimal("9")),
                eq(new BigDecimal("12.555556")), eq(new BigDecimal("113")), eq(false)))
                .thenReturn(1);

        ErpStockService.StockUpdateResult result = stockService.updateStockCountAndCost(
                10L, 20L, new BigDecimal("-1"), new BigDecimal("10"),
                ErpStockRecordBizTypeEnum.PURCHASE_RETURN.getType());

        assertEquals(new BigDecimal("9"), result.getTotalCount());
        assertEquals(new BigDecimal("12.555556"), result.getCostPrice());
        assertEquals(new BigDecimal("113"), result.getCostAmount());
        verify(stockMapper).updateCountAndCost(eq(1L), eq(new BigDecimal("10")), eq(new BigDecimal("9")),
                eq(new BigDecimal("12.555556")), eq(new BigDecimal("113")), eq(false));
    }

    @Test
    public void testUpdateStockCountAndCost_outWithoutUnitPrice_keepsAverageCost() {
        ErpStockDO stock = new ErpStockDO().setId(1L)
                .setProductId(10L).setWarehouseId(20L)
                .setCount(new BigDecimal("10"))
                .setCostPrice(new BigDecimal("12.300000"))
                .setCostAmount(new BigDecimal("123"));
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(stock);
        when(stockMapper.updateCountAndCost(eq(1L), eq(new BigDecimal("10")), eq(new BigDecimal("9")),
                eq(new BigDecimal("12.300000")), eq(new BigDecimal("110.70")), eq(false)))
                .thenReturn(1);

        ErpStockService.StockUpdateResult result = stockService.updateStockCountAndCost(
                10L, 20L, new BigDecimal("-1"), null,
                ErpStockRecordBizTypeEnum.OTHER_OUT.getType());

        assertEquals(new BigDecimal("9"), result.getTotalCount());
        assertEquals(new BigDecimal("12.300000"), result.getCostPrice());
        assertEquals(new BigDecimal("110.70"), result.getCostAmount());
        verify(stockMapper).updateCountAndCost(eq(1L), eq(new BigDecimal("10")), eq(new BigDecimal("9")),
                eq(new BigDecimal("12.300000")), eq(new BigDecimal("110.70")), eq(false));
    }

    @Test
    public void testUpdateStockCountAndCost_saleOutWithUnitPrice_keepsAverageCost() {
        ErpStockDO stock = new ErpStockDO().setId(1L)
                .setProductId(10L).setWarehouseId(20L)
                .setCount(new BigDecimal("10"))
                .setCostPrice(new BigDecimal("12.300000"))
                .setCostAmount(new BigDecimal("123"));
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(stock);
        when(stockMapper.updateCountAndCost(eq(1L), eq(new BigDecimal("10")), eq(new BigDecimal("9")),
                eq(new BigDecimal("12.300000")), eq(new BigDecimal("110.70")), eq(false)))
                .thenReturn(1);

        ErpStockService.StockUpdateResult result = stockService.updateStockCountAndCost(
                10L, 20L, new BigDecimal("-1"), new BigDecimal("20"),
                ErpStockRecordBizTypeEnum.SALE_OUT.getType());

        assertEquals(new BigDecimal("9"), result.getTotalCount());
        assertEquals(new BigDecimal("12.300000"), result.getCostPrice());
        assertEquals(new BigDecimal("110.70"), result.getCostAmount());
        verify(stockMapper).updateCountAndCost(eq(1L), eq(new BigDecimal("10")), eq(new BigDecimal("9")),
                eq(new BigDecimal("12.300000")), eq(new BigDecimal("110.70")), eq(false));
    }

    @Test
    public void testUpdateStockCountAndCost_saleReturnRetryAfterUpdateMiss_success() {
        ErpStockDO stock = new ErpStockDO().setId(1L)
                .setProductId(10L).setWarehouseId(20L)
                .setCount(new BigDecimal("2"))
                .setCostPrice(new BigDecimal("12.300000"))
                .setCostAmount(new BigDecimal("24.60"));
        ErpStockDO latest = new ErpStockDO().setId(1L)
                .setProductId(10L).setWarehouseId(20L)
                .setCount(new BigDecimal("2"))
                .setCostPrice(new BigDecimal("12.300000"))
                .setCostAmount(new BigDecimal("24.60"));
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(stock, latest);
        when(stockMapper.updateCountAndCost(eq(1L), eq(new BigDecimal("2")), eq(new BigDecimal("3")),
                eq(new BigDecimal("11.533333")), eq(new BigDecimal("34.60")), eq(false)))
                .thenReturn(0, 1);

        ErpStockService.StockUpdateResult result = stockService.updateStockCountAndCost(
                10L, 20L, new BigDecimal("1"), new BigDecimal("10"),
                ErpStockRecordBizTypeEnum.SALE_RETURN.getType());

        assertEquals(new BigDecimal("3"), result.getTotalCount());
        assertEquals(new BigDecimal("11.533333"), result.getCostPrice());
        assertEquals(new BigDecimal("34.60"), result.getCostAmount());
        verify(stockMapper, org.mockito.Mockito.times(2)).selectByProductIdAndWarehouseId(10L, 20L);
        verify(stockMapper, org.mockito.Mockito.times(2)).updateCountAndCost(eq(1L),
                eq(new BigDecimal("2")), eq(new BigDecimal("3")),
                eq(new BigDecimal("11.533333")), eq(new BigDecimal("34.60")), eq(false));
    }

    @Test
    public void testUpdateStockCountAndCost_negativeStockWhenProductInvisible_throwBusinessException() {
        ErpStockDO stock = new ErpStockDO().setId(1L)
                .setProductId(10L).setWarehouseId(20L)
                .setCount(BigDecimal.ZERO)
                .setCostPrice(BigDecimal.ZERO)
                .setCostAmount(BigDecimal.ZERO);
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(stock);
        when(productService.getProduct(10L)).thenReturn(null);
        when(warehouseService.getWarehouse(20L)).thenReturn(new ErpWarehouseDO().setId(20L).setName("邛崃仓"));

        ServiceException exception = assertThrows(ServiceException.class, () -> stockService.updateStockCountAndCost(
                10L, 20L, new BigDecimal("-1"), null,
                ErpStockRecordBizTypeEnum.SALE_OUT.getType()));

        assertEquals(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE.getCode(),
                exception.getCode());
        verify(stockMapper, never()).updateCountAndCost(eq(1L), eq(BigDecimal.ZERO), eq(new BigDecimal("-1")),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq(false));
    }

    @Test
    public void testAdjustStockCostAmount_fillsRecordDeptIdFromWarehouseWhenStockDeptMissing() {
        ErpStockDO stock = new ErpStockDO().setId(1L)
                .setProductId(10L).setWarehouseId(20L)
                .setDeptId(null)
                .setCount(new BigDecimal("10"))
                .setCostPrice(new BigDecimal("12.300000"))
                .setCostAmount(new BigDecimal("123"));
        when(stockMapper.selectByProductIdAndWarehouseId(10L, 20L)).thenReturn(stock);
        when(stockMapper.updateCostAmountAndPrice(eq(1L), eq(new BigDecimal("10")), eq(new BigDecimal("123")),
                eq(new BigDecimal("128.00")), eq(new BigDecimal("12.800000"))))
                .thenReturn(1);
        when(warehouseService.getWarehouse(20L)).thenReturn(new ErpWarehouseDO().setId(20L).setDeptId(40L));
        LocalDateTime bizDate = LocalDateTime.of(2026, 7, 7, 10, 0);

        stockService.adjustStockCostAmount(10L, 20L, new BigDecimal("5"), new BigDecimal("10"),
                100L, "PA001", bizDate);

        ArgumentCaptor<ErpStockRecordDO> captor = ArgumentCaptor.forClass(ErpStockRecordDO.class);
        verify(stockRecordMapper).insert(captor.capture());
        ErpStockRecordDO record = captor.getValue();
        assertEquals(40L, record.getDeptId());
        assertEquals(ErpStockRecordBizTypeEnum.PURCHASE_PRICE_ADJUST.getType(), record.getBizType());
        assertEquals(BigDecimal.ZERO, record.getCount());
        assertEquals(new BigDecimal("128.00"), record.getCostAmount());
    }

}
