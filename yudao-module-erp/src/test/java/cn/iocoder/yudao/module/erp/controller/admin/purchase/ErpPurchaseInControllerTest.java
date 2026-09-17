package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPaymentSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInTransferOutableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInTransferOutableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInBillService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseInControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInController controller;

    @Mock
    private ErpPurchaseInService purchaseInService;
    @Mock
    private ErpPurchaseInvoiceService purchaseInvoiceService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockInBillService stockInBillService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpPurchaseItemPriceReferenceFiller itemPriceReferenceFiller;
    @Mock
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;

    private final List<ErpPurchaseInItemDO> items = Arrays.asList(
            new ErpPurchaseInItemDO().setId(101L).setInId(10L).setProductId(1001L)
                    .setWarehouseId(1L).setCount(new BigDecimal("4")),
            new ErpPurchaseInItemDO().setId(102L).setInId(10L).setProductId(1002L)
                    .setWarehouseId(1L).setCount(new BigDecimal("6")));

    @BeforeEach
    void setUp() {
        items.forEach(item -> item.setAdjusted(false));
        lenient().when(purchaseInService.getPurchaseIn(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK-001").setTotalCount(new BigDecimal("10"))
                .setTotalPrice(new BigDecimal("100")));
        lenient().when(purchaseInService.getPurchaseInItemListByInId(eq(10L))).thenReturn(items);
        lenient().when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(purchaseInvoiceService.getPurchaseInvoiceItemListBySourceInIds(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        lenient().when(purchaseInService.getApprovedReturnCountMapByInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        lenient().when(purchaseInService.getTransferOutCountMapByInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        lenient().when(adminUserApi.getUserMap(any())).thenReturn(Collections.emptyMap());
        lenient().when(stockInBillService.getStockInBillListByPurchaseInId(eq(10L)))
                .thenReturn(Collections.emptyList());
        lenient().when(stockInBillService.getPurchaseInSourceItemList(eq(10L)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void getPurchaseInKeepsEmptyTransferOutStatusAtZero() {
        when(purchaseInService.getTransferOutCountMapByInItemIds(any()))
                .thenReturn(Collections.emptyMap());

        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L, true, true).getData();

        assertNotNull(data);
        assertEquals(Integer.valueOf(0), data.getAdjustStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getTransferOutCount()));
        assertEquals(Integer.valueOf(0), data.getTransferOutStatus());
    }

    @Test
    void getPurchaseInAggregatesPartialAdjustStatus() {
        items.get(0).setAdjusted(true);

        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L, true, true).getData();

        assertNotNull(data);
        assertEquals(Integer.valueOf(1), data.getAdjustStatus());
        verify(fieldPermissionMasker).mask(eq("erp_purchase_in"), eq(data));
    }

    @Test
    void getPurchaseInPaymentSummaryReturnsUnpaidStatus() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_IN.getType()))).thenReturn(BigDecimal.ZERO);

        ErpPurchaseInPaymentSummaryRespVO data = controller.getPurchaseInPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getPayableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getPaidAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(data.getUnpaidAmount()));
        assertEquals(Integer.valueOf(0), data.getPaymentStatus());
    }

    @Test
    void getPurchaseInPaymentSummaryReturnsPartialStatus() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_IN.getType()))).thenReturn(new BigDecimal("40"));

        ErpPurchaseInPaymentSummaryRespVO data = controller.getPurchaseInPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getPayableAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(data.getPaidAmount()));
        assertEquals(0, new BigDecimal("60").compareTo(data.getUnpaidAmount()));
        assertEquals(Integer.valueOf(1), data.getPaymentStatus());
    }

    @Test
    void getPurchaseInPaymentSummaryReturnsPaidStatus() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_IN.getType()))).thenReturn(new BigDecimal("100"));

        ErpPurchaseInPaymentSummaryRespVO data = controller.getPurchaseInPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getPayableAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(data.getPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getUnpaidAmount()));
        assertEquals(Integer.valueOf(2), data.getPaymentStatus());
    }

    @Test
    void getPurchaseInPaymentSummaryUsesOriginalSettlementAmount() {
        when(purchaseInService.getPurchaseIn(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK-001").setTotalPrice(new BigDecimal("120"))
                .setDiscountPercent(new BigDecimal("10")).setFeeAmount(new BigDecimal("5")));
        when(purchaseInService.getPurchaseInItemListByInId(eq(10L))).thenReturn(Arrays.asList(
                new ErpPurchaseInItemDO().setInId(10L).setCount(new BigDecimal("4"))
                        .setProductPrice(new BigDecimal("12")).setOriginalProductPrice(new BigDecimal("10")),
                new ErpPurchaseInItemDO().setInId(10L).setCount(new BigDecimal("6"))
                        .setProductPrice(new BigDecimal("12"))));
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_IN.getType()))).thenReturn(new BigDecimal("40"));

        ErpPurchaseInPaymentSummaryRespVO data = controller.getPurchaseInPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("105.80").compareTo(data.getPayableAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(data.getPaidAmount()));
        assertEquals(0, new BigDecimal("65.80").compareTo(data.getUnpaidAmount()));
        assertEquals(Integer.valueOf(1), data.getPaymentStatus());
    }

    @Test
    void getPurchaseInPageAggregatesPartialAdjustStatus() {
        items.get(0).setAdjusted(true);
        when(purchaseInService.getPurchaseInPage(any())).thenReturn(new PageResult<>(
                Collections.singletonList(new ErpPurchaseInDO()
                        .setId(10L).setNo("CGRK-001").setTotalCount(new BigDecimal("10"))),
                1L));
        when(purchaseInService.getPurchaseInItemListByInIds(any())).thenReturn(items);
        when(supplierService.getSupplierMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        ErpPurchaseInPageReqVO reqVO = new ErpPurchaseInPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        PageResult<ErpPurchaseInRespVO> data = controller.getPurchaseInPage(reqVO).getData();

        assertNotNull(data);
        assertEquals(1L, data.getTotal());
        assertEquals(Integer.valueOf(1), data.getList().get(0).getAdjustStatus());
        verify(fieldPermissionMasker).maskList(eq("erp_purchase_in"), eq(data.getList()));
    }

    @Test
    void getPurchaseInPageSkipsFullItemsWhenIncludeItemsIsFalse() {
        items.get(0).setAdjusted(true);
        when(purchaseInService.getPurchaseInPage(any())).thenReturn(new PageResult<>(
                Collections.singletonList(new ErpPurchaseInDO()
                        .setId(10L).setNo("CGRK-001").setTotalCount(new BigDecimal("10"))),
                1L));
        when(purchaseInItemMapper.selectLightListByInIds(any())).thenReturn(items);
        when(supplierService.getSupplierMap(any())).thenReturn(Collections.emptyMap());
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        ErpPurchaseInPageReqVO reqVO = new ErpPurchaseInPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setIncludeItems(false);
        PageResult<ErpPurchaseInRespVO> data = controller.getPurchaseInPage(reqVO).getData();

        assertNotNull(data);
        assertEquals(1L, data.getTotal());
        assertEquals(Integer.valueOf(2), data.getList().get(0).getItemCount());
        assertEquals(Integer.valueOf(1), data.getList().get(0).getAdjustStatus());
        verify(purchaseInService, never()).getPurchaseInItemListByInIds(any());
        verify(purchaseInItemMapper).selectLightListByInIds(any());
        verify(fieldPermissionMasker).maskList(eq("erp_purchase_in"), eq(data.getList()));
    }

    @Test
    void getPurchaseInAggregatesFullAdjustStatus() {
        items.forEach(item -> item.setAdjusted(true));

        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L, true, true).getData();

        assertNotNull(data);
        assertEquals(Integer.valueOf(2), data.getAdjustStatus());
    }

    @Test
    void getPurchaseInFallsBackToMainAdjustedWhenItemsAreSkipped() {
        when(purchaseInService.getPurchaseIn(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK-001").setTotalCount(new BigDecimal("10")).setAdjusted(true));

        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L, true, false).getData();

        assertNotNull(data);
        assertEquals(Integer.valueOf(2), data.getAdjustStatus());
        verify(purchaseInService, never()).getPurchaseInItemListByInId(eq(10L));
    }

    @Test
    void getPurchaseInUsesAggregatedStockCountForItems() {
        when(purchaseInService.getTransferOutCountMapByInItemIds(any()))
                .thenReturn(Collections.emptyMap());
        when(stockService.getStockCount(1001L, 1L)).thenReturn(new BigDecimal("12"));
        when(stockService.getStockCount(1002L, 1L)).thenReturn(new BigDecimal("8"));

        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L, true, true).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("12").compareTo(data.getItems().get(0).getStockCount()));
        assertEquals(0, new BigDecimal("8").compareTo(data.getItems().get(1).getStockCount()));
        verify(stockService).getStockCount(1001L, 1L);
        verify(stockService).getStockCount(1002L, 1L);
        verify(stockService, never()).getStock(eq(1001L), eq(1L));
        verify(stockService, never()).getStock(eq(1002L), eq(1L));
    }

    @Test
    void getSupplierAvailableDeptSimpleListDelegatesToPurchaseInService() {
        List<DeptSimpleRespVO> depts = Collections.singletonList(
                new DeptSimpleRespVO(20L, "采购二部", 0L));
        when(purchaseInService.getSupplierAvailableDeptSimpleList(eq(100L))).thenReturn(depts);

        controller.getPurchaseIn(10L, true, true);
        CommonResult<List<DeptSimpleRespVO>> result = controller.getSupplierAvailableDeptSimpleList(100L);

        assertSame(depts, result.getData());
        verify(purchaseInService).getSupplierAvailableDeptSimpleList(eq(100L));
    }

    @Test
    void getPurchaseInAggregatesPartialTransferOutStatus() {
        Map<Long, BigDecimal> movedCountMap = new HashMap<>();
        movedCountMap.put(101L, new BigDecimal("2"));
        movedCountMap.put(102L, new BigDecimal("3"));
        when(purchaseInService.getTransferOutCountMapByInItemIds(any())).thenReturn(movedCountMap);

        CommonResult<ErpPurchaseInRespVO> result = controller.getPurchaseIn(10L, true, true);

        ErpPurchaseInRespVO data = result.getData();
        assertNotNull(data);
        assertEquals(0, new BigDecimal("5").compareTo(data.getTransferOutCount()));
        assertEquals(Integer.valueOf(1), data.getTransferOutStatus());
        verify(purchaseInService).getTransferOutCountMapByInItemIds(any());
        verify(fieldPermissionMasker).mask(eq("erp_purchase_in"), eq(data));
    }

    @Test
    void getPurchaseInAggregatesFullTransferOutStatus() {
        Map<Long, BigDecimal> movedCountMap = new HashMap<>();
        movedCountMap.put(101L, new BigDecimal("4"));
        movedCountMap.put(102L, new BigDecimal("6"));
        when(purchaseInService.getTransferOutCountMapByInItemIds(any())).thenReturn(movedCountMap);

        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L, true, true).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("10").compareTo(data.getTransferOutCount()));
        assertEquals(Integer.valueOf(2), data.getTransferOutStatus());
    }

    @Test
    void getPurchaseInCanSkipItemsForFastMainDetailLoad() {
        ErpPurchaseInRespVO data = controller.getPurchaseIn(10L, true, false).getData();

        assertNotNull(data);
        verify(purchaseInService, never()).getPurchaseInItemListByInId(eq(10L));
        verify(fieldPermissionMasker).mask(eq("erp_purchase_in"), eq(data));
    }

    @Test
    void getPurchaseInItemPageBuildsPagedItemsAndMasksHiddenFields() {
        when(purchaseInService.getPurchaseInItemPage(any())).thenReturn(new PageResult<>(items, 2L));
        when(stockService.getStockCount(1001L, 1L)).thenReturn(new BigDecimal("12"));
        when(stockService.getStockCount(1002L, 1L)).thenReturn(new BigDecimal("8"));

        ErpPurchaseInItemPageReqVO reqVO = new ErpPurchaseInItemPageReqVO();
        reqVO.setInId(10L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setMask(true);
        PageResult<ErpPurchaseInRespVO.Item> data = controller.getPurchaseInItemPage(reqVO).getData();

        assertNotNull(data);
        assertEquals(2L, data.getTotal());
        assertEquals(2, data.getList().size());
        assertEquals(0, new BigDecimal("12").compareTo(data.getList().get(0).getStockCount()));
        assertEquals(0, new BigDecimal("8").compareTo(data.getList().get(1).getStockCount()));
        verify(purchaseInService).getPurchaseInItemPage(eq(reqVO));
        verify(fieldPermissionMasker).clearHiddenItemFields(eq("erp_purchase_in"), eq(data.getList()));
    }

    @Test
    void getSaleCartableItemPageReturnsServicePage() {
        controller.getPurchaseIn(10L, true, true);

        ErpPurchaseInSaleCartableItemPageReqVO reqVO = new ErpPurchaseInSaleCartableItemPageReqVO();
        reqVO.setInId(10L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        ErpPurchaseInSaleCartableItemRespVO item = new ErpPurchaseInSaleCartableItemRespVO();
        item.setSourceInItemId(100L);
        PageResult<ErpPurchaseInSaleCartableItemRespVO> pageResult = new PageResult<>(
                Collections.singletonList(item), 1L);
        when(purchaseInService.getSaleCartableItemPage(eq(reqVO))).thenReturn(pageResult);

        CommonResult<PageResult<ErpPurchaseInSaleCartableItemRespVO>> result =
                controller.getSaleCartableItemPage(reqVO);

        assertSame(pageResult, result.getData());
        assertEquals(1L, result.getData().getTotal());
        verify(purchaseInService).getSaleCartableItemPage(eq(reqVO));
    }

    @Test
    void getTransferOutableItemPageReturnsServicePage() {
        ErpPurchaseInTransferOutableItemPageReqVO reqVO = new ErpPurchaseInTransferOutableItemPageReqVO();
        reqVO.setInId(10L);
        reqVO.setProductKeyword("P001");
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        ErpPurchaseInTransferOutableItemRespVO item = new ErpPurchaseInTransferOutableItemRespVO();
        item.setSourceInItemId(101L);
        item.setProductCode("P001");
        item.setTransferOutableCount(new BigDecimal("4"));
        PageResult<ErpPurchaseInTransferOutableItemRespVO> pageResult =
                new PageResult<>(Collections.singletonList(item), 1L);
        when(purchaseInService.getTransferOutableItemPage(eq(reqVO))).thenReturn(pageResult);

        CommonResult<PageResult<ErpPurchaseInTransferOutableItemRespVO>> result =
                controller.getTransferOutableItemPage(reqVO);

        assertSame(pageResult, result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals("P001", result.getData().getList().get(0).getProductCode());
        verify(purchaseInService).getTransferOutableItemPage(eq(reqVO));
    }

}
