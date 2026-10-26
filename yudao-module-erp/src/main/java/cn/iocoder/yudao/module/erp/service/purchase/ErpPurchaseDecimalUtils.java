package cn.iocoder.yudao.module.erp.service.purchase;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ERP_PURCHASE_DECIMAL_SCALE_EXCEEDED;

/**
 * 采购业务六位小数运算边界。
 *
 * <p>该工具只供采购单据使用，避免改变全局 MoneyUtils 的两位金额约定。</p>
 */
public final class ErpPurchaseDecimalUtils {

    public static final int SCALE = 6;
    private static final BigDecimal PERCENT_100 = BigDecimal.valueOf(100);

    private ErpPurchaseDecimalUtils() {
    }

    public static BigDecimal requireScale(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }
        int effectiveScale = Math.max(value.stripTrailingZeros().scale(), 0);
        if (effectiveScale > SCALE) {
            throw exception(ERP_PURCHASE_DECIMAL_SCALE_EXCEEDED, fieldName, value.toPlainString());
        }
        return value;
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    public static BigDecimal multiply(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return null;
        }
        requireScale(left, "单价/金额");
        requireScale(right, "数量");
        return normalize(left.multiply(right));
    }

    public static BigDecimal multiplyPercent(BigDecimal amount, BigDecimal percent) {
        if (amount == null || percent == null) {
            return null;
        }
        requireScale(amount, "金额");
        requireScale(percent, "比例");
        return amount.multiply(percent).divide(PERCENT_100, SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

}
