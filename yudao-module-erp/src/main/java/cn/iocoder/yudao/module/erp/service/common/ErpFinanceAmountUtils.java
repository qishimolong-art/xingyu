package cn.iocoder.yudao.module.erp.service.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ERP 财务金额工具。
 */
public final class ErpFinanceAmountUtils {

    private static final int AMOUNT_SCALE = 2;

    private ErpFinanceAmountUtils() {
    }

    /**
     * 按系统金额精度标准化，消除前端浮点运算产生的无效尾差。
     */
    public static BigDecimal normalize(BigDecimal amount) {
        return amount == null ? null : amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

}
