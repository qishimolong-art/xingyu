package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.ProductWarehousePair;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ErpPurchaseOrderDetailReportRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = base(filter, scope);
        Long count = jdbc().queryForObject("SELECT COUNT(*) " + sql.text, sql.params, Long.class);
        return count == null ? 0L : count;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = base(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(selectFields() + sql.text + orderBy(filter) + " LIMIT :limit OFFSET :offset",
                sql.params, new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = base(filter, scope);
        return jdbc().queryForObject("SELECT COUNT(*) AS row_count,"
                        + "COALESCE(SUM(COALESCE(poi.count,0)),0) AS total_count,"
                        + "COALESCE(SUM(COALESCE(poi.count,0)*COALESCE(poi.product_price,0)),0) AS total_price,"
                        + "COALESCE(SUM(GREATEST(COALESCE(poi.count,0)-COALESCE(poi.in_count,0),0)),0) AS order_in_transit_count "
                        + sql.text,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    public List<ProductWarehousePair> distinctPairs(Filter filter, Scope scope) {
        Sql sql = base(filter, scope);
        return jdbc().query("SELECT DISTINCT poi.product_id,poi.warehouse_id " + sql.text,
                sql.params, new BeanPropertyRowMapper<>(ProductWarehousePair.class));
    }

    private String selectFields() {
        return "SELECT poi.id AS item_id,po.id AS order_id,poi.product_id,poi.warehouse_id,"
                + "po.create_time AS order_date,po.factory_order_no,"
                + "p.name AS product_name,s.name AS supplier_name,"
                + "COALESCE(NULLIF(poi.vehicle_model,''),p.vehicle_model) AS vehicle_model,p.code AS product_code,"
                + "w.name AS warehouse_name,poi.warehouse_position,"
                + "COALESCE(NULLIF(poi.drawing_no,''),p.drawing_no) AS drawing_no,"
                + "COALESCE(NULLIF(poi.standard,''),p.standard) AS standard,"
                + "COALESCE(NULLIF(poi.brand,''),p.brand) AS brand,"
                + "COALESCE(NULLIF(poi.origin_place,''),p.origin_place) AS origin_place,"
                + "pu.nickname AS purchaser_name,cu.nickname AS creator_name,po.no AS order_no,"
                + "COALESCE(poi.count,0) AS count,COALESCE(poi.product_price,0) AS product_price,"
                + "COALESCE(poi.count,0)*COALESCE(poi.product_price,0) AS total_price,"
                + "poi.stock_count_at_order,p.stock_max,p.stock_min,p.stock_standard,"
                + "GREATEST(COALESCE(poi.count,0)-COALESCE(poi.in_count,0),0) AS order_in_transit_count,"
                + "po.arrival_date AS expected_arrival_date,po.remark AS document_remark,"
                + "COALESCE(NULLIF(poi.feature_code,''),p.feature_code) AS feature_code ";
    }

    private Sql base(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        sql.text = "FROM erp_purchase_order_items poi "
                + "INNER JOIN erp_purchase_order po ON po.id=poi.order_id AND po.tenant_id=poi.tenant_id "
                + "AND po.deleted=b'0' AND po.status=20 "
                + "INNER JOIN erp_supplier s ON s.id=po.supplier_id AND s.tenant_id=po.tenant_id AND s.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=poi.product_id AND p.tenant_id=poi.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_warehouse w ON w.id=poi.warehouse_id AND w.tenant_id=poi.tenant_id AND w.deleted=b'0' "
                + "LEFT JOIN system_users pu ON pu.id=po.purchaser AND pu.tenant_id=po.tenant_id AND pu.deleted=b'0' "
                + "LEFT JOIN system_users cu ON cu.id=po.creator AND cu.tenant_id=po.tenant_id AND cu.deleted=b'0' "
                + "WHERE poi.tenant_id=:tenant AND poi.deleted=b'0' ";
        appendScope(sql, scope.documentScope, "po.dept_id", "po.creator", "document");
        appendSupplierScope(sql, scope.supplierScope);
        if (filter.getOrderDateFrom() != null) {
            sql.text += " AND po.create_time>=:dateFrom";
            sql.params.put("dateFrom", filter.getOrderDateFrom());
        }
        if (filter.getOrderDateTo() != null) {
            sql.text += " AND po.create_time<=:dateTo";
            sql.params.put("dateTo", filter.getOrderDateTo());
        }
        like(sql, "po.no", "orderNo", filter.getOrderNo());
        like(sql, "po.factory_order_no", "factoryOrderNo", filter.getFactoryOrderNo());
        equal(sql, "po.supplier_id", "supplierId", filter.getSupplierId());
        equal(sql, "po.purchaser", "purchaserId", filter.getPurchaserId());
        equal(sql, "poi.warehouse_id", "warehouseId", filter.getWarehouseId());
        if (hasText(filter.getProductKeyword())) {
            sql.params.put("productKeyword", "%" + filter.getProductKeyword().trim() + "%");
            sql.text += " AND (p.code LIKE :productKeyword OR p.name LIKE :productKeyword "
                    + "OR p.factory_code LIKE :productKeyword OR p.drawing_no LIKE :productKeyword "
                    + "OR p.feature_code LIKE :productKeyword OR poi.factory_code LIKE :productKeyword "
                    + "OR poi.drawing_no LIKE :productKeyword OR poi.feature_code LIKE :productKeyword)";
        }
        return sql;
    }

    private void appendScope(Sql sql, ErpFinanceVisibleScope scope, String deptColumn,
                             String creatorColumn, String prefix) {
        if (scope.isAll()) {
            return;
        }
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(deptColumn).append(" IN (:").append(prefix).append("DeptIds)");
            sql.params.put(prefix + "DeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append(creatorColumn).append("=:").append(prefix).append("Self");
            sql.params.put(prefix + "Self", String.valueOf(scope.getSelfUserId()));
            appended = true;
        }
        sql.text += appended ? condition.append(')').toString() : " AND 1=0";
    }

    private void appendSupplierScope(Sql sql, ErpFinanceVisibleScope scope) {
        if (scope.isAll()) {
            return;
        }
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append("s.dept_id IN (:supplierDeptIds) OR (s.allow_multi_dept=b'1' AND EXISTS ("
                    + "SELECT 1 FROM erp_supplier_dept esd WHERE esd.tenant_id=s.tenant_id "
                    + "AND esd.supplier_id=s.id AND esd.deleted=b'0' AND esd.dept_id IN (:supplierDeptIds)))");
            sql.params.put("supplierDeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append("s.creator=:supplierSelf");
            sql.params.put("supplierSelf", String.valueOf(scope.getSelfUserId()));
            appended = true;
        }
        sql.text += appended ? condition.append(')').toString() : " AND 1=0";
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("orderDate", "po.create_time");
        fields.put("orderNo", "po.no");
        fields.put("factoryOrderNo", "po.factory_order_no");
        fields.put("productCode", "p.code");
        fields.put("productName", "p.name");
        fields.put("supplierName", "s.name");
        fields.put("warehouseName", "w.name");
        fields.put("count", "poi.count");
        fields.put("productPrice", "poi.product_price");
        fields.put("totalPrice", "COALESCE(poi.count,0)*COALESCE(poi.product_price,0)");
        fields.put("orderInTransitCount", "GREATEST(COALESCE(poi.count,0)-COALESCE(poi.in_count,0),0)");
        String field = fields.getOrDefault(filter.getOrderField(), "po.create_time");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",po.no" + direction + ",poi.id" + direction;
    }

    private void like(Sql sql, String column, String name, String value) {
        if (!hasText(value)) return;
        sql.text += " AND " + column + " LIKE :" + name;
        sql.params.put(name, "%" + value.trim() + "%");
    }

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null) return;
        sql.text += " AND " + column + "=:" + name;
        sql.params.put(name, value);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private NamedParameterJdbcTemplate jdbc() {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public static class Scope {
        private final long tenantId;
        private final ErpFinanceVisibleScope documentScope;
        private final ErpFinanceVisibleScope supplierScope;

        public Scope(long tenantId, ErpFinanceVisibleScope documentScope, ErpFinanceVisibleScope supplierScope) {
            this.tenantId = tenantId;
            this.documentScope = documentScope;
            this.supplierScope = supplierScope;
        }
    }

    private static class Sql {
        private String text;
        private final Map<String, Object> params = new HashMap<>();
    }
}
