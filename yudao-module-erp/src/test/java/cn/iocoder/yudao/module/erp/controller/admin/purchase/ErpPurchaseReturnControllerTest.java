package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnRefundSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseReturnControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseReturnController controller;

    @Mock
    private ErpPurchaseReturnService purchaseReturnService;
    @Mock
    private ErpFinancePaymentItemMapper financePaymentItemMapper;

    @BeforeEach
    void setUp() {
        lenient().when(purchaseReturnService.getPurchaseReturn(eq(10L))).thenReturn(new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH-001").setTotalPrice(new BigDecimal("100")));
        lenient().when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType()))).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void getPurchaseReturnPaymentSummaryReturnsUnrefundedStatus() {
        ErpPurchaseReturnRefundSummaryRespVO data = controller.getPurchaseReturnPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getRefundableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getRefundedAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(data.getUnrefundedAmount()));
        assertEquals(Integer.valueOf(0), data.getRefundStatus());
        verify(financePaymentItemMapper).selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType()));
    }

    @Test
    void getPurchaseReturnPaymentSummaryReturnsPartialStatusWithAbsoluteRefundAmount() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType()))).thenReturn(new BigDecimal("-40"));

        ErpPurchaseReturnRefundSummaryRespVO data = controller.getPurchaseReturnPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getRefundableAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(data.getRefundedAmount()));
        assertEquals(0, new BigDecimal("60").compareTo(data.getUnrefundedAmount()));
        assertEquals(Integer.valueOf(1), data.getRefundStatus());
    }

    @Test
    void getPurchaseReturnPaymentSummaryReturnsRefundedStatus() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType()))).thenReturn(new BigDecimal("-100"));

        ErpPurchaseReturnRefundSummaryRespVO data = controller.getPurchaseReturnPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getRefundableAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(data.getRefundedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getUnrefundedAmount()));
        assertEquals(Integer.valueOf(2), data.getRefundStatus());
    }

}
