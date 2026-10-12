package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSettlementSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchasePriceAdjustControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchasePriceAdjustController controller;

    @Mock
    private ErpPurchasePriceAdjustService priceAdjustService;
    @Mock
    private ErpFinancePaymentItemMapper financePaymentItemMapper;

    @BeforeEach
    void setUp() {
        lenient().when(priceAdjustService.getPurchasePriceAdjust(eq(10L))).thenReturn(new ErpPurchasePriceAdjustDO()
                .setId(10L).setNo("CGTJ-001").setTotalAdjustPrice(new BigDecimal("100")));
        lenient().when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()))).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void getPurchasePriceAdjustPaymentSummaryReturnsUnwrittenStatus() {
        ErpPurchasePriceAdjustSettlementSummaryRespVO data = controller.getPurchasePriceAdjustPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getSettlementAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getWrittenOffAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(data.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(0), data.getSettlementStatus());
        verify(financePaymentItemMapper).selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()));
    }

    @Test
    void getPurchasePriceAdjustPaymentSummaryReturnsPartialStatusForPositiveAdjustment() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()))).thenReturn(new BigDecimal("40"));

        ErpPurchasePriceAdjustSettlementSummaryRespVO data = controller.getPurchasePriceAdjustPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getSettlementAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(data.getWrittenOffAmount()));
        assertEquals(0, new BigDecimal("60").compareTo(data.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(1), data.getSettlementStatus());
    }

    @Test
    void getPurchasePriceAdjustPaymentSummaryKeepsNegativeAdjustmentDirection() {
        when(priceAdjustService.getPurchasePriceAdjust(eq(10L))).thenReturn(new ErpPurchasePriceAdjustDO()
                .setId(10L).setNo("CGTJ-001").setTotalAdjustPrice(new BigDecimal("-100")));
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()))).thenReturn(new BigDecimal("-100"));

        ErpPurchasePriceAdjustSettlementSummaryRespVO data = controller.getPurchasePriceAdjustPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("-100").compareTo(data.getSettlementAmount()));
        assertEquals(0, new BigDecimal("-100").compareTo(data.getWrittenOffAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(2), data.getSettlementStatus());
    }

    @Test
    void getSupplierAvailableDeptSimpleListDelegatesToPriceAdjustService() {
        List<DeptSimpleRespVO> expected = Collections.singletonList(
                new DeptSimpleRespVO(7L, "采购部", 1L));
        when(priceAdjustService.getSupplierAvailableDeptSimpleList(100L)).thenReturn(expected);

        assertEquals(expected, controller.getSupplierAvailableDeptSimpleList(100L).getData());
        verify(priceAdjustService).getSupplierAvailableDeptSimpleList(100L);
    }

    @Test
    void getSupplierAvailableDeptSimplePageDelegatesToPriceAdjustService() {
        PageParam pageParam = new PageParam().setPageNo(1).setPageSize(20);
        PageResult<DeptSimpleRespVO> expected = new PageResult<>(Collections.singletonList(
                new DeptSimpleRespVO(7L, "采购部", 1L)), 1L);
        when(priceAdjustService.getSupplierAvailableDeptSimplePage(100L, pageParam)).thenReturn(expected);

        assertEquals(expected, controller.getSupplierAvailableDeptSimplePage(100L, pageParam).getData());
        verify(priceAdjustService).getSupplierAvailableDeptSimplePage(100L, pageParam);
    }

}
