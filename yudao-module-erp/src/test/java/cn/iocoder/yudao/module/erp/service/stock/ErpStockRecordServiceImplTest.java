package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordSummaryVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpStockRecordServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockRecordServiceImpl stockRecordService;

    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private cn.iocoder.yudao.module.erp.service.stock.cost.ErpDualCostPostingService dualCostPostingService;
    @Mock
    private ErpStockItemSnapshotSupport snapshotSupport;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpStockInItemMapper stockInItemMapper;
    @Mock
    private ErpStockOutItemMapper stockOutItemMapper;
    @Mock
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Mock
    private ErpStockCheckItemMapper stockCheckItemMapper;

    @Test
    public void testGetStockRecordSummary_totalPriceFallback() {
        ErpStockRecordDO inRecord = new ErpStockRecordDO()
                .setProductId(10L)
                .setWarehouseId(20L)
                .setBizType(10)
                .setBizItemId(1001L)
                .setCount(new BigDecimal("4"))
                .setTotalCount(new BigDecimal("14"))
                .setCostAmount(new BigDecimal("140.00"))
                .setUnitPrice(new BigDecimal("2.50"))
                .setTotalPrice(null);
        ErpStockRecordDO outRecord = new ErpStockRecordDO()
                .setProductId(10L)
                .setWarehouseId(20L)
                .setBizType(20)
                .setBizItemId(1002L)
                .setCount(new BigDecimal("-3"))
                .setTotalCount(new BigDecimal("11"))
                .setCostAmount(new BigDecimal("110.00"))
                .setUnitPrice(new BigDecimal("5.00"))
                .setTotalPrice(BigDecimal.ZERO);
        when(stockRecordMapper.selectPageWithProductFilter(any(), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Arrays.asList(inRecord, outRecord), 2L));

        ErpStockRecordSummaryVO summary = stockRecordService.getStockRecordSummary(new ErpStockRecordPageReqVO());

        assertEquals(new BigDecimal("4"), summary.getTotalInCount());
        assertEquals(new BigDecimal("10.00"), summary.getTotalInAmount());
        assertEquals(new BigDecimal("3"), summary.getTotalOutCount());
        assertEquals(new BigDecimal("15.00"), summary.getTotalOutAmount());
        assertEquals(new BigDecimal("14"), summary.getBalanceCount());
        assertEquals(new BigDecimal("140.00"), summary.getBalanceAmount());
        assertEquals(2L, summary.getRecordCount());
    }

    @Test
    public void testGetStockRecordSummary_purchaseInFallbackFromSourceItem() {
        ErpStockRecordDO inRecord = new ErpStockRecordDO()
                .setBizType(ErpStockRecordBizTypeEnum.PURCHASE_IN.getType())
                .setBizItemId(1001L)
                .setCount(new BigDecimal("4"))
                .setUnitPrice(null)
                .setTotalPrice(null);
        when(stockRecordMapper.selectPageWithProductFilter(any(), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(inRecord), 1L));
        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1001L)
                        .setProductPrice(new BigDecimal("2.50"))
                        .setTotalPrice(new BigDecimal("10.00"))));

        ErpStockRecordSummaryVO summary = stockRecordService.getStockRecordSummary(new ErpStockRecordPageReqVO());

        assertEquals(new BigDecimal("10.00"), summary.getTotalInAmount());
        assertEquals(1L, summary.getRecordCount());
    }

    @Test
    public void testGetStockRecordPage_saleOutFallbackFromSourceItem() {
        ErpStockRecordDO outRecord = new ErpStockRecordDO()
                .setBizType(ErpStockRecordBizTypeEnum.SALE_OUT.getType())
                .setBizItemId(2001L)
                .setCount(new BigDecimal("-3"))
                .setUnitPrice(null)
                .setTotalPrice(null);
        when(stockRecordMapper.selectPageWithProductFilter(any(), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(outRecord), 1L));
        when(saleOutItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                new ErpSaleOutItemDO().setId(2001L)
                        .setProductPrice(new BigDecimal("12.00"))
                        .setTotalPrice(new BigDecimal("36.00"))));

        PageResult<ErpStockRecordDO> page = stockRecordService.getStockRecordPage(new ErpStockRecordPageReqVO());
        ErpStockRecordDO row = page.getList().get(0);

        assertEquals(new BigDecimal("12.00"), row.getUnitPrice());
        assertEquals(new BigDecimal("-36.00"), row.getTotalPrice());
    }

    @Test
    public void testGetStockRecordPage_withoutFilters_keepsAllRowsFromMapper() {
        ErpStockRecordDO first = new ErpStockRecordDO().setId(1L).setCount(new BigDecimal("1"));
        ErpStockRecordDO second = new ErpStockRecordDO().setId(2L).setCount(new BigDecimal("-1"));
        when(stockRecordMapper.selectPageWithProductFilter(any(), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Arrays.asList(first, second), 2L));

        PageResult<ErpStockRecordDO> page = stockRecordService.getStockRecordPage(new ErpStockRecordPageReqVO());

        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getList().size());
    }

    @Test
    public void testGetStockRecordPage_stockViewValidatesProductStockWarehousePermission() {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setProductId(10L);
        reqVO.setWarehouseId(20L);
        reqVO.setStockView(true);
        when(stockRecordMapper.selectPageWithProductFilter(eq(reqVO), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(
                        new ErpStockRecordDO().setProductId(10L).setWarehouseId(20L)
                                .setCount(BigDecimal.ONE)), 1L));

        PageResult<ErpStockRecordDO> page = stockRecordService.getStockRecordPage(reqVO);

        assertEquals(1L, page.getTotal());
        verify(warehouseService).validateCurrentUserStockWarehousePermission(Collections.singleton(20L));
        verify(stockRecordMapper).selectPageWithProductFilter(eq(reqVO), nullable(Collection.class));
    }

    @Test
    public void testGetStockRecordPage_batchViewRecalculatesRunningBalanceAcrossPagination() {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setProductId(10L);
        reqVO.setWarehouseId(20L);
        reqVO.setStockView(true);
        reqVO.setBatchNo(" B-001 ");
        ErpStockRecordDO first = new ErpStockRecordDO().setId(1L).setCount(new BigDecimal("10"));
        ErpStockRecordDO second = new ErpStockRecordDO().setId(2L).setCount(new BigDecimal("-4"))
                .setCostPrice(new BigDecimal("2"));
        ErpStockRecordDO third = new ErpStockRecordDO().setId(3L).setCount(new BigDecimal("2"))
                .setCostPrice(new BigDecimal("2"));
        when(stockRecordMapper.selectPageWithProductFilter(eq(reqVO), nullable(Collection.class)))
                .thenReturn(new PageResult<>(Arrays.asList(third, second), 3L));
        when(stockRecordMapper.selectBatchRunningBalanceRecords(10L, 20L, " B-001 ", null))
                .thenReturn(Arrays.asList(first, second, third));

        PageResult<ErpStockRecordDO> page = stockRecordService.getStockRecordPage(reqVO);

        assertEquals(new BigDecimal("8"), page.getList().get(0).getTotalCount());
        assertEquals(new BigDecimal("16"), page.getList().get(0).getCostAmount());
        assertEquals(new BigDecimal("6"), page.getList().get(1).getTotalCount());
        assertEquals(new BigDecimal("12"), page.getList().get(1).getCostAmount());
        verify(stockRecordMapper).selectBatchRunningBalanceRecords(10L, 20L, " B-001 ", null);

        ErpStockRecordSummaryVO summary = stockRecordService.getStockRecordSummary(reqVO);
        assertEquals(new BigDecimal("8"), summary.getBalanceCount());
        assertEquals(new BigDecimal("16"), summary.getBalanceAmount());
    }

    @Test
    public void testGetStockRecordSummary_emptyProductFilter_returnsEmpty() {
        ErpStockRecordPageReqVO reqVO = new ErpStockRecordPageReqVO();
        reqVO.setProductCode("P-1");
        when(productMapper.selectMaps(any())).thenReturn(Collections.emptyList());

        ErpStockRecordSummaryVO summary = stockRecordService.getStockRecordSummary(reqVO);

        assertEquals(BigDecimal.ZERO, summary.getTotalInAmount());
        assertEquals(BigDecimal.ZERO, summary.getTotalOutAmount());
        assertEquals(0L, summary.getRecordCount());
    }

    @Test
    public void testCreateStockRecord_fillsDeptIdFromStock() {
        when(stockService.updateStockCountAndCost(eq(10L), eq(20L), eq(new BigDecimal("-1")),
                eq(new BigDecimal("10")), eq(ErpStockRecordBizTypeEnum.PURCHASE_RETURN.getType())))
                .thenReturn(new ErpStockService.StockUpdateResult(
                        new BigDecimal("9"), new BigDecimal("12.555556"), new BigDecimal("113")));
        when(stockService.getStock(10L, 20L)).thenReturn(new ErpStockDO().setDeptId(30L));
        ErpStockRecordCreateReqBO reqBO = new ErpStockRecordCreateReqBO(
                10L, 20L, " B-001 ", new BigDecimal("-1"),
                ErpStockRecordBizTypeEnum.PURCHASE_RETURN.getType(),
                100L, 1001L, "PR001", new BigDecimal("10"), null);

        stockRecordService.createStockRecord(reqBO);

        ArgumentCaptor<ErpStockRecordDO> captor = ArgumentCaptor.forClass(ErpStockRecordDO.class);
        verify(stockRecordMapper).insert(captor.capture());
        ErpStockRecordDO record = captor.getValue();
        assertEquals("B-001", record.getBatchNo());
        assertEquals(30L, record.getDeptId());
        assertEquals(new BigDecimal("113"), record.getCostAmount());
    }

    @Test
    public void testCreateStockRecord_fillsDeptIdFromWarehouseWhenStockDeptMissing() {
        when(stockService.updateStockCountAndCost(eq(10L), eq(20L), eq(new BigDecimal("2")),
                eq(new BigDecimal("10")), eq(ErpStockRecordBizTypeEnum.PURCHASE_IN.getType())))
                .thenReturn(new ErpStockService.StockUpdateResult(
                        new BigDecimal("12"), new BigDecimal("11.916667"), new BigDecimal("143")));
        when(stockService.getStock(10L, 20L)).thenReturn(new ErpStockDO().setDeptId(null));
        when(warehouseService.getWarehouse(20L)).thenReturn(new ErpWarehouseDO().setId(20L).setDeptId(40L));
        ErpStockRecordCreateReqBO reqBO = new ErpStockRecordCreateReqBO(
                10L, 20L, new BigDecimal("2"),
                ErpStockRecordBizTypeEnum.PURCHASE_IN.getType(),
                100L, 1001L, "PI001", new BigDecimal("10"), null);

        stockRecordService.createStockRecord(reqBO);

        ArgumentCaptor<ErpStockRecordDO> captor = ArgumentCaptor.forClass(ErpStockRecordDO.class);
        verify(stockRecordMapper).insert(captor.capture());
        ErpStockRecordDO record = captor.getValue();
        assertEquals(40L, record.getDeptId());
        assertEquals(new BigDecimal("143"), record.getCostAmount());
    }

}
