package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Stock math rounds only at the persisted field boundary, never at display precision. */
public final class StockDecimalMath {

    private StockDecimalMath() {
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return multiply(a, b, 6);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b, int scale) {
        if (a == null || b == null) {
            return null;
        }
        BigDecimal result = a.multiply(b).setScale(scale, RoundingMode.HALF_UP);
        require(result, "计算结果", 24 - scale, scale);
        return result;
    }

    public static BigDecimal boundary(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        BigDecimal result = value.setScale(scale, RoundingMode.HALF_UP);
        require(result, "库存金额", scale == 4 ? 16 : 24 - scale, scale);
        return result;
    }

    public static void require(BigDecimal value, String label, int integer, int scale) {
        if (value == null) {
            return;
        }
        if (value.stripTrailingZeros().scale() > scale
                || value.abs().compareTo(BigDecimal.TEN.pow(integer)) >= 0) {
            throw new ServiceException(400, label + "最多支持" + integer + "位整数和"
                    + scale + "位有效小数");
        }
    }
}
