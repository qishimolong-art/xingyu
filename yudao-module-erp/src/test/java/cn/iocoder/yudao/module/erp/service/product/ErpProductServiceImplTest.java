package cn.iocoder.yudao.module.erp.service.product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpProductServiceImplTest {

    @Test
    void isLowStockWarning_whenStockIsZero_thenWarns() {
        assertTrue(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ZERO, null));
    }

    @Test
    void isLowStockWarning_whenStockIsPositiveAndBelowMin_thenWarns() {
        assertTrue(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ONE, 5));
    }

    @Test
    void isLowStockWarning_whenStockIsPositiveAndMinMissing_thenDoesNotWarn() {
        assertFalse(ErpProductServiceImpl.isLowStockWarning(BigDecimal.ONE, null));
    }

    @Test
    void isLowStockWarning_whenStockIsAboveMin_thenDoesNotWarn() {
        assertFalse(ErpProductServiceImpl.isLowStockWarning(BigDecimal.valueOf(13), 10));
    }

}
