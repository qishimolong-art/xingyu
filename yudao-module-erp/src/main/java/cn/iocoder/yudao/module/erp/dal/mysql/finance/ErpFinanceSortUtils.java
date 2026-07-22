package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;

import java.util.Arrays;

/**
 * 资金管理分页排序工具。排序字段只允许来自调用方声明的白名单。
 */
public final class ErpFinanceSortUtils {

    private static final String MPJ_MAIN_TABLE_ALIAS = "t";

    private ErpFinanceSortUtils() {
    }

    public static void apply(AbstractWrapper<?, ?, ?> wrapper, String orderField, String orderDirection,
                             String tableName, String... allowedFields) {
        wrapper.last(buildOrderClause(orderField, orderDirection, tableName, allowedFields));
    }

    public static void apply(MPJLambdaWrapper<?> wrapper, String orderField, String orderDirection,
                             String... allowedFields) {
        wrapper.last(buildOrderClause(orderField, orderDirection, MPJ_MAIN_TABLE_ALIAS, allowedFields));
    }

    private static String buildOrderClause(String orderField, String orderDirection,
                                           String tableName, String... allowedFields) {
        String expression = null;
        if (orderField != null && Arrays.asList(allowedFields).contains(orderField)) {
            expression = tableName + "." + camelToUnderline(orderField);
        }
        boolean validDirection = "asc".equalsIgnoreCase(orderDirection) || "desc".equalsIgnoreCase(orderDirection);
        if (expression == null || !validDirection) {
            return "ORDER BY " + tableName + ".id DESC";
        }
        String direction = "asc".equalsIgnoreCase(orderDirection) ? "ASC" : "DESC";
        return "ORDER BY " + expression + " " + direction + ", " + tableName + ".id DESC";
    }

    private static String camelToUnderline(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

}
