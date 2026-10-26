package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Validate manual values at every service entry, including draft and import callers. */
public final class ErpFinanceDecimalRules {
    private ErpFinanceDecimalRules() { }
    public static void validate(Object request) {
        validate(request, "", Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private static String label(String field) {
        switch (field) {
            case "writeOffAmount": return "核销金额";
            case "amount": return "金额";
            case "totalPrice": return "合计金额";
            case "paymentPrice": return "付款金额";
            case "receiptPrice": return "收款金额";
            case "discountPrice": return "优惠金额";
            case "exchangeRate": return "汇率";
            case "feePrice": return "手续费";
            default: return field;
        }
    }
    private static void validate(Object request, String prefix, Set<Object> seen) {
        if (request == null || !seen.add(request)) return;
        if (request instanceof Iterable<?> rows) {
            int index = 0;
            for (Object row : rows) validate(row, "第 " + (++index) + " 行", seen);
            return;
        }
        if (!request.getClass().getName().startsWith("cn.iocoder.yudao.module.erp.")) return;
        for (Class<?> type = request.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(request);
                    if (value instanceof BigDecimal decimal) {
                        int integer = "exchangeRate".equals(field.getName()) ? 12 : 18;
                        Schema schema = field.getAnnotation(Schema.class);
                        String label = prefix + (schema == null ? label(field.getName()) : schema.description());
                        if (decimal.stripTrailingZeros().scale() > 6 || decimal.abs().compareTo(BigDecimal.TEN.pow(integer)) >= 0)
                            throw new ServiceException(400, label + "最多支持 " + integer + " 位整数和 6 位有效小数，末尾零不计");
                    } else if (value instanceof Iterable<?>) validate(value, prefix, seen);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }
}
