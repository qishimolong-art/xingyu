package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErpPurchaseDecimalUtilsTest {

    @Test
    void shouldKeepSixDecimalInputDuringMultiplication() {
        BigDecimal result = ErpPurchaseDecimalUtils.multiply(
                new BigDecimal("0.123456"), new BigDecimal("100000"));

        assertEquals(0, new BigDecimal("12345.6").compareTo(result));
    }

    @Test
    void shouldRoundOnlyAtSixDecimalResultBoundary() {
        BigDecimal result = ErpPurchaseDecimalUtils.multiply(
                new BigDecimal("0.123456"), new BigDecimal("0.654321"));

        assertEquals(0, new BigDecimal("0.08078").compareTo(result));
    }

    @Test
    void shouldAcceptTrailingZerosWithinEffectiveScale() {
        BigDecimal result = ErpPurchaseDecimalUtils.requireScale(new BigDecimal("1.230000"), "采购单价");

        assertEquals("1.230000", result.toPlainString());
    }

    @Test
    void shouldRejectSeventhEffectiveDecimal() {
        assertThrows(ServiceException.class,
                () -> ErpPurchaseDecimalUtils.requireScale(new BigDecimal("0.1234567"), "采购单价"));
    }

}
