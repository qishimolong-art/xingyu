package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ErpSaleDecimalUtilsTest {

    static class ImportRow {
        @cn.idev.excel.annotation.ExcelProperty("单价")
        private BigDecimal productPrice;
    }

    @Test
    void importRejectsSeventhDigitWithoutChangingTheRow() {
        var row = new ImportRow();
        row.productPrice = new BigDecimal("0.1234567");
        assertTrue(ErpSaleDecimalUtils.importScaleError(row).contains("6位小数"));
        assertEquals("0.1234567", row.productPrice.toPlainString());
        row.productPrice = new BigDecimal("0.123456");
        assertNull(ErpSaleDecimalUtils.importScaleError(row));
    }

    @Test
    void largeQuantityMustNotRoundUnitPriceToCents() {
        assertEquals(0, new BigDecimal("12345.6").compareTo(
                ErpSaleDecimalUtils.multiply(new BigDecimal("0.123456"), new BigDecimal("100000"))));
    }

    @Test
    void fractionalQuantityAndSixDigitResultUseHalfUp() {
        assertEquals(0, new BigDecimal("0.061728").compareTo(
                ErpSaleDecimalUtils.multiply(new BigDecimal("0.123456"), new BigDecimal("0.5"))));
        assertEquals(0, new BigDecimal("0.015241").compareTo(
                ErpSaleDecimalUtils.multiply(new BigDecimal("0.123456"), new BigDecimal("0.123456"))));
    }

    @Test
    void percentageAndDivisionKeepSixDigitPrecision() {
        assertEquals(0, new BigDecimal("0.000001").compareTo(
                ErpSaleDecimalUtils.multiplyPercent(new BigDecimal("0.01"), new BigDecimal("0.01"))));
        assertEquals(0, new BigDecimal("0.333333").compareTo(
                ErpSaleDecimalUtils.divide(BigDecimal.ONE, new BigDecimal("3"))));
    }

    @Test
    void seventhDigitIsRejectedWithFieldAndRow() {
        ServiceException error = assertThrows(ServiceException.class, () ->
                ErpSaleDecimalUtils.requireScale(new BigDecimal("1.2345678"), "第2行单价"));
        assertTrue(error.getMessage().contains("第2行单价"));
        assertTrue(error.getMessage().contains("6 位小数"));
    }

    @Test
    void zeroNegativeBusinessAmountsAndTrailingZerosRemainSupported() {
        assertEquals(0, BigDecimal.ZERO.compareTo(ErpSaleDecimalUtils.multiply(BigDecimal.ZERO, BigDecimal.ONE)));
        assertEquals(0, new BigDecimal("-0.061728").compareTo(
                ErpSaleDecimalUtils.multiply(new BigDecimal("-0.123456"), new BigDecimal("0.5"))));
        assertEquals("1.23", ErpSaleDecimalUtils.normalize(new BigDecimal("1.230000")).toPlainString());
    }
}
