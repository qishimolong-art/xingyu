package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_ITEM_SORT_INVALID;

/**
 * 采购单据明细排序公共 SQL 片段。
 *
 * <p>调用方只能从固定白名单返回 {@link OrderSpec}，禁止将客户端字段直接拼接到 SQL。</p>
 */
public final class ErpPurchaseItemSortUtils {

    private ErpPurchaseItemSortUtils() {
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

    public static String warehouseName(String tableName) {
        return "(SELECT w.name FROM erp_warehouse w WHERE w.id = " + tableName
                + ".warehouse_id AND w.tenant_id = " + tableName
                + ".tenant_id AND w.deleted = 0 LIMIT 1)";
    }

    public static String deptName(String tableName) {
        return "(SELECT d.name FROM system_dept d WHERE d.id = " + tableName
                + ".dept_id AND d.tenant_id = " + tableName
                + ".tenant_id AND d.deleted = 0 LIMIT 1)";
    }

    public static String stockCount(String tableName) {
        return "(SELECT s.count FROM erp_stock s WHERE s.product_id = " + tableName
                + ".product_id AND s.warehouse_id = " + tableName
                + ".warehouse_id AND s.tenant_id = " + tableName
                + ".tenant_id AND s.deleted = 0 LIMIT 1)";
    }

    public static String productPackageQty(String tableName) {
        return productField(tableName, "package_qty");
    }

    public static String pieceCount(String countExpression, String packageExpression) {
        return "(CASE WHEN " + packageExpression + " > 0 THEN " + countExpression
                + " / " + packageExpression + " ELSE NULL END)";
    }

    public static String productAmount(String tableName) {
        return "(" + tableName + ".product_price * " + tableName + ".count)";
    }

    public static String returnedCount(String tableName, String sourceItemExpression) {
        return "(SELECT COALESCE(SUM(pri.count), 0) FROM erp_purchase_return_items pri"
                + " INNER JOIN erp_purchase_return pr ON pr.id = pri.return_id"
                + " AND pr.tenant_id = pri.tenant_id AND pr.deleted = 0 AND pr.status = 20"
                + " WHERE pri.source_in_item_id = " + sourceItemExpression
                + " AND pri.tenant_id = " + tableName + ".tenant_id AND pri.deleted = 0)";
    }

    public record OrderSpec(String expression, boolean text) {
    }
}
