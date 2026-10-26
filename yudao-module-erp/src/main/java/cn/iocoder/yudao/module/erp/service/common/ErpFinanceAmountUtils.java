package cn.iocoder.yudao.module.erp.service.common;

import java.math.BigDecimal;

/**
 * ERP 财务金额工具。
 */
public final class ErpFinanceAmountUtils {


    private ErpFinanceAmountUtils() {
    }

    /**
     * 保持资金业务完整精度；保存边界由字段校验及计算结果舍入负责。
     */
    public static BigDecimal normalize(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros();
    }

}
