package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_ITEM_SORT_INVALID;

/**
 * 库存单据明细排序公共 SQL 片段。
 *
 * <p>排序表达式必须由各 Mapper 的固定白名单提供，禁止拼接客户端字段。</p>
 */
public final class ErpStockItemSortUtils {

    private ErpStockItemSortUtils() {
    }

    public static <T> QueryWrapper<T> applyOrder(QueryWrapper<T> query, String tableName,
                                                  String orderField, String orderDirection,
                                                  OrderSpec orderSpec) {
        boolean hasField = orderField != null && !orderField.isBlank();
        boolean hasDirection = orderDirection != null && !orderDirection.isBlank();
        if (hasField != hasDirection
                || (hasDirection && !"asc".equalsIgnoreCase(orderDirection)
                && !"desc".equalsIgnoreCase(orderDirection))
                || (hasField && orderSpec == null)) {
            throw exception(PRINT_ITEM_SORT_INVALID);
        }
        if (orderSpec == null) {
            return query.orderByAsc(tableName + ".id");
        }
        String expression = orderSpec.expression();
        String blankOrder = "CASE WHEN " + expression + " IS NULL"
                + (orderSpec.text() ? " OR TRIM(" + expression + ") = ''" : "")
                + " THEN 1 ELSE 0 END";
        query.orderByAsc(blankOrder)
                .orderBy(true, !"desc".equalsIgnoreCase(orderDirection), expression);
        if (!"id".equals(orderField == null ? null : orderField.trim())) {
            query.orderByAsc(tableName + ".id");
        }
        return query;
    }

    public static String productField(String tableName, String field) {
        return "(SELECT p." + field + " FROM erp_product p WHERE p.id = " + tableName
                + ".product_id AND p.tenant_id = " + tableName
                + ".tenant_id AND p.deleted = 0 LIMIT 1)";
    }

    public static String itemUnitName(String tableName) {
        return "(SELECT u.name FROM erp_product_unit u WHERE u.id = " + tableName
                + ".product_unit_id AND u.tenant_id = " + tableName
                + ".tenant_id AND u.deleted = 0 LIMIT 1)";
    }

    public static String warehouseName(String tableName, String warehouseColumn) {
        return "(SELECT w.name FROM erp_warehouse w WHERE w.id = " + tableName + "." + warehouseColumn
                + " AND w.tenant_id = " + tableName + ".tenant_id AND w.deleted = 0 LIMIT 1)";
    }

    public static String warehouseDeptName(String tableName, String warehouseColumn) {
        return "(SELECT d.name FROM erp_warehouse w LEFT JOIN system_dept d"
                + " ON d.id = w.dept_id AND d.tenant_id = w.tenant_id AND d.deleted = 0"
                + " WHERE w.id = " + tableName + "." + warehouseColumn + " AND w.tenant_id = "
                + tableName + ".tenant_id AND w.deleted = 0 LIMIT 1)";
    }

    public static String stockCount(String tableName, String warehouseColumn) {
        return "(SELECT s.count FROM erp_stock s WHERE s.product_id = " + tableName
                + ".product_id AND s.warehouse_id = " + tableName + "." + warehouseColumn
                + " AND s.tenant_id = " + tableName + ".tenant_id AND s.deleted = 0 LIMIT 1)";
    }

    public static String pieceCount(String tableName) {
        return "(CASE WHEN " + tableName + ".package_qty > 0 THEN " + tableName
                + ".count / " + tableName + ".package_qty ELSE NULL END)";
    }

    public record OrderSpec(String expression, boolean text) {
    }
}
