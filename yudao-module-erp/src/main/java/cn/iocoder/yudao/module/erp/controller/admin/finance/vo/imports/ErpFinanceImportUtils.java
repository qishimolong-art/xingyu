package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.imports;

import cn.hutool.core.util.StrUtil;

import javax.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

public final class ErpFinanceImportUtils {

    private ErpFinanceImportUtils() {
    }

    public static boolean allBlank(Object... values) {
        return Arrays.stream(values).allMatch(ErpFinanceImportUtils::isBlank);
    }

    public static String trimToNull(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }

    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static LocalDate parseDate(String value, LocalDate defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        return parseDateTime(normalized, defaultValue == null ? null : defaultValue.atStartOfDay()).toLocalDate();
    }

    public static LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        }) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("日期格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    public static String failureReason(Exception ex) {
        if (ex instanceof ConstraintViolationException) {
            return ex.getMessage();
        }
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    private static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof String && StrUtil.isBlank((String) value);
    }
}
