package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PRINT_ITEM_SORT_INVALID;

/**
 * 销售单据明细排序公共 SQL 片段。
 *
 * <p>调用方必须只从固定白名单返回 {@link OrderSpec}，禁止把客户端字段直接拼接到 SQL。</p>
 */
public final class ErpSaleItemSortUtils {

    private ErpSaleItemSortUtils() {
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

    public static String productUnitName(String tableName) {
        return "(SELECT u.name FROM erp_product p LEFT JOIN erp_product_unit u"
                + " ON u.id = p.unit_id AND u.tenant_id = p.tenant_id AND u.deleted = 0"
                + " WHERE p.id = " + tableName + ".product_id AND p.tenant_id = " + tableName
                + ".tenant_id AND p.deleted = 0 LIMIT 1)";
    }

    public static String warehouseName(String tableName, String warehouseColumn) {
        return "(SELECT w.name FROM erp_warehouse w WHERE w.id = " + tableName + "." + warehouseColumn
                + " AND w.tenant_id = " + tableName + ".tenant_id AND w.deleted = 0 LIMIT 1)";
    }

    public static String deptName(String tableName, String deptColumn) {
        return "(SELECT d.name FROM system_dept d WHERE d.id = " + tableName + "." + deptColumn
                + " AND d.tenant_id = " + tableName + ".tenant_id AND d.deleted = 0 LIMIT 1)";
    }

    public static String stockCount(String tableName) {
        return "(SELECT s.count FROM erp_stock s WHERE s.product_id = " + tableName
                + ".product_id AND s.warehouse_id = " + tableName
                + ".warehouse_id AND s.tenant_id = " + tableName
                + ".tenant_id AND s.deleted = 0 LIMIT 1)";
    }

    public static String lastSalePrice(String tableName) {
        return "(SELECT soi.product_price FROM erp_sale_out_items soi"
                + " INNER JOIN erp_sale_out so ON so.id = soi.out_id AND so.tenant_id = soi.tenant_id"
                + " AND so.deleted = 0 AND so.status = 20"
                + " WHERE soi.product_id = " + tableName + ".product_id AND soi.tenant_id = " + tableName
                + ".tenant_id AND soi.deleted = 0 AND soi.product_price IS NOT NULL"
                + " ORDER BY so.out_time DESC, so.id DESC, soi.id DESC LIMIT 1)";
    }

    public static String pieceCount(String tableName, String countColumn) {
        return "(CASE WHEN " + tableName + ".package_qty > 0 THEN " + tableName + "." + countColumn
                + " / " + tableName + ".package_qty ELSE NULL END)";
    }

    public static String saleOutCostPrice(String tableName) {
        return "(SELECT sr.unit_price FROM erp_stock_record sr WHERE sr.biz_type = 50"
                + " AND sr.biz_item_id = " + tableName + ".id AND sr.tenant_id = " + tableName
                + ".tenant_id AND sr.deleted = 0 ORDER BY sr.id DESC LIMIT 1)";
    }

    public static String saleOutCostAmount(String tableName) {
        return "(SELECT ABS(sr.total_price) FROM erp_stock_record sr WHERE sr.biz_type = 50"
                + " AND sr.biz_item_id = " + tableName + ".id AND sr.tenant_id = " + tableName
                + ".tenant_id AND sr.deleted = 0 ORDER BY sr.id DESC LIMIT 1)";
    }

    public record OrderSpec(String expression, boolean text) {
    }
}
