package cn.iocoder.yudao.module.erp.service.stock.cost;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ErpDualCostCalculatorTest {
    private static BigDecimal n(String value) { return new BigDecimal(value); }

    @Test
    void weightedInboundAndPartialOutboundConserveBothAmounts() {
        ErpDualCostCalculator.Change financial = ErpDualCostCalculator.calculate(n("10"), n("1000"), n("5"), n("130"));
        ErpDualCostCalculator.Change settlement = ErpDualCostCalculator.calculate(n("10"), n("1200"), n("5"), n("150"));
        assertEquals(0, financial.getAmount().compareTo(n("1650")));
        assertEquals(0, settlement.getAmount().compareTo(n("1950")));
        assertEquals(0, ErpDualCostCalculator.calculate(n("15"), financial.getAmount(), n("-4"), null)
                .getMovement().compareTo(n("-440")));
        assertEquals(0, ErpDualCostCalculator.calculate(n("15"), settlement.getAmount(), n("-4"), null)
                .getMovement().compareTo(n("-520")));
    }

    @Test
    void lastOutboundConsumesRoundingRemainder() {
        ErpDualCostCalculator.Change first = ErpDualCostCalculator.calculate(n("3"), n("1"), n("-1"), null);
        ErpDualCostCalculator.Change last = ErpDualCostCalculator.calculate(first.getQuantity(), first.getAmount(), n("-2"), null);
        assertEquals(0, last.getAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, first.getMovement().add(last.getMovement()).compareTo(n("-1")));
    }

    @Test
    void realZeroCostGiftDilutesCostButMissingCostFails() {
        assertEquals(0, ErpDualCostCalculator.calculate(n("10"), n("100"), n("10"), n("0"))
                .getAmount().compareTo(n("100")));
        assertThrows(IllegalArgumentException.class, () -> ErpDualCostCalculator.calculate(n("0"), null, n("1"), n("0")));
        assertThrows(IllegalArgumentException.class, () -> ErpDualCostCalculator.calculate(n("0"), n("0"), n("1"), null));
    }

    @Test
    void invalidBalancesAndOverdrawDoNotProduceProfit() {
        assertThrows(IllegalArgumentException.class, () -> ErpDualCostCalculator.calculate(n("1"), n("10"), n("-2"), null));
        assertThrows(IllegalArgumentException.class, () -> ErpDualCostCalculator.calculate(n("0"), n("10"), n("1"), n("1")));
        assertThrows(IllegalArgumentException.class, () -> ErpDualCostCalculator.calculate(n("1"), n("10"), n("0"), null));
    }
}
