package cn.iocoder.yudao.module.erp.service.finance;

import java.math.BigDecimal;

/** Signed availability: an over-reserved positive document must never become a negative candidate. */
public final class ErpMiscTransferAmount {
    private ErpMiscTransferAmount() { }

    public static BigDecimal available(BigDecimal original, BigDecimal balance, BigDecimal pending) {
        if (original == null || balance == null) return BigDecimal.ZERO;
        BigDecimal result = balance.subtract(pending == null ? BigDecimal.ZERO : pending);
        return result.signum() == original.signum() ? result : BigDecimal.ZERO;
    }
}
