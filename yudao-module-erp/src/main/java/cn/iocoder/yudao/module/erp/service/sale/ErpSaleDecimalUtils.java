package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 销售单据的六位小数边界，不改变全局两位金额约定。 */
public final class ErpSaleDecimalUtils {

    public static final int SCALE = 6;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ErpSaleDecimalUtils() {
    }

    public static BigDecimal requireScale(BigDecimal value, String fieldName) {
        if (value != null && Math.max(value.stripTrailingZeros().scale(), 0) > SCALE) {
            throw new ServiceException(400, fieldName + "最多支持 6 位小数：" + value.toPlainString());
        }
        return value;
    }

    /** 仅在行金额、税额等六位持久化字段边界调用。 */
    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /** 返回逐行导入错误，调用方继续使用原有失败明细与分组事务语义。 */
    public static String importScaleError(Object row) {
        if (row == null) return null;
        for (java.lang.reflect.Field field : cn.hutool.core.util.ReflectUtil.getFields(row.getClass())) {
            if (field.getType() != BigDecimal.class) continue;
            BigDecimal value = (BigDecimal) cn.hutool.core.util.ReflectUtil.getFieldValue(row, field);
            if (value == null) continue;
            if (value.scale() <= SCALE && value.precision() - value.scale() <= 18) continue;
            cn.idev.excel.annotation.ExcelProperty property = field.getAnnotation(cn.idev.excel.annotation.ExcelProperty.class);
            String label = property != null && property.value().length > 0 ? property.value()[0] : field.getName();
            return label + "最多18位整数、6位小数：" + value.toPlainString();
        }
        return null;
    }

    public static BigDecimal multiply(BigDecimal price, BigDecimal count) {
        if (price == null || count == null) {
            return null;
        }
        requireScale(price, "单价/金额");
        requireScale(count, "数量");
        return normalize(price.multiply(count));
    }

    public static BigDecimal multiplyPercent(BigDecimal amount, BigDecimal percent) {
        if (amount == null || percent == null) {
            return null;
        }
        requireScale(percent, "比例/税率");
        return amount.multiply(percent).divide(HUNDRED, SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null || divisor.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
