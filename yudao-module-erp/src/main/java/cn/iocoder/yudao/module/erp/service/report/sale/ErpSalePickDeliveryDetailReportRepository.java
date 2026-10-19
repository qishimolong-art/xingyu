package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels.Summary;
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
public class ErpSalePickDeliveryDetailReportRepository {

    private static final int CART_SOURCE = 30;

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope, boolean delivery) {
        Sql sql = base(filter, scope, delivery);
        Long value = jdbc().queryForObject("SELECT COUNT(*) " + sql.text, sql.params, Long.class);
        return value == null ? 0L : value;
    }

    public List<Row> page(Filter filter, Scope scope, boolean delivery, long offset, int limit) {
        Sql sql = base(filter, scope, delivery);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        String query = "SELECT r.*,ROW_NUMBER() OVER (" + windowOrder(filter) + ") AS sequence "
                + sql.text + orderBy(filter) + " LIMIT :limit OFFSET :offset";
        return jdbc().query(query, sql.params, new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope, boolean delivery) {
        Sql sql = base(filter, scope, delivery);
        return jdbc().queryForObject("SELECT COUNT(*) AS row_count,"
                        + "COALESCE(SUM(r.count),0) AS total_count,"
                        + "COALESCE(SUM(r.picked_count),0) AS picked_count,"
                        + "COALESCE(SUM(r.remaining_pick_count),0) AS remaining_pick_count,"
                        + "COALESCE(SUM(r.delivered_count),0) AS delivered_count,"
                        + "COALESCE(SUM(r.available_delivery_count),0) AS available_delivery_count,"
                        + "COALESCE(SUM(r.remaining_delivery_count),0) AS remaining_delivery_count,"
                        + "COALESCE(SUM(r.piece_count),0) AS piece_count " + sql.text,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private Sql base(Filter filter, Scope scope, boolean delivery) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        sql.params.put("cartSource", CART_SOURCE);
        String picked = "CASE WHEN i.picked_count IS NULL AND i.pick_status=30 THEN COALESCE(i.count,0) "
                + "ELSE COALESCE(i.picked_count,0) END";
        String delivered = "CASE WHEN i.delivered_count IS NULL AND i.delivery_status=30 THEN COALESCE(i.count,0) "
                + "ELSE COALESCE(i.delivered_count,0) END";
        String complete = delivery
                ? "CASE WHEN NOT EXISTS (SELECT 1 FROM erp_sale_pick_delivery_item ci WHERE ci.pick_task_id=t.id "
                + "AND ci.tenant_id=t.tenant_id AND ci.deleted=b'0' AND (CASE WHEN ci.delivered_count IS NULL "
                + "AND ci.delivery_status=30 THEN COALESCE(ci.count,0) ELSE COALESCE(ci.delivered_count,0) END)"
                + "<COALESCE(ci.count,0)) THEN (SELECT MAX(di.delivery_time) FROM erp_sale_pick_delivery_item di "
                + "WHERE di.pick_task_id=t.id AND di.tenant_id=t.tenant_id AND di.deleted=b'0') END"
                : "t.complete_time";
        String status = delivery ? "i.delivery_status" : "i.pick_status";
        String operationUser = delivery ? "i.delivery_user_id" : "i.pick_user_id";
        String operationTime = delivery ? "i.delivery_time" : "i.pick_time";
        String packageQty = "COALESCE(NULLIF(i.package_qty,0),p.package_qty)";
        sql.text = "FROM (SELECT i.id AS item_id,t.id AS task_id,o.id AS order_id,t.source_type,"
                + "CASE WHEN t.source_type=:cartSource THEN t.source_id END AS source_cart_id,t.sale_out_id,"
                + "t.customer_id,COALESCE(sc.dept_id,so.dept_id,o.dept_id) AS dept_id,"
                + "COALESCE(sc.sale_user_id,so.sale_user_id) AS sale_user_id,t.warehouse_id,i.product_id,"
                + "t.create_time AS task_create_date,so.out_time AS sale_out_date,"
                + "CASE WHEN t.source_type=:cartSource THEN t.source_no END AS source_cart_no,t.sale_out_no,"
                + "COALESCE(NULLIF(t.customer_name,''),c.name) AS customer_name,d.name AS dept_name,"
                + "su.nickname AS sale_user_name,COALESCE(NULLIF(t.warehouse_name,''),w.name) AS warehouse_name,"
                + "COALESCE(NULLIF(i.product_code,''),p.code) AS product_code,"
                + "COALESCE(NULLIF(i.product_name,''),p.name) AS product_name,"
                + "COALESCE(NULLIF(i.standard,''),p.standard) AS standard,"
                + "COALESCE(NULLIF(i.warehouse_position,''),s.shelf) AS warehouse_position,COALESCE(i.count,0) AS count,"
                + picked + " AS picked_count,GREATEST(COALESCE(i.count,0)-(" + picked + "),0) AS remaining_pick_count,"
                + delivered + " AS delivered_count,GREATEST((" + picked + ")-(" + delivered
                + "),0) AS available_delivery_count,GREATEST(COALESCE(i.count,0)-(" + delivered
                + "),0) AS remaining_delivery_count," + packageQty + " AS package_qty,CASE WHEN " + packageQty + ">0 "
                + "THEN ROUND(COALESCE(i.count,0)/(" + packageQty + "),3) END AS piece_count,"
                + status + " AS status," + operationUser + " AS operation_user_id,ou.nickname AS operation_user_name,"
                + operationTime + " AS operation_time," + complete + " AS complete_time "
                + "FROM erp_sale_pick_delivery_item i "
                + "INNER JOIN erp_sale_pick_delivery_pick_task t ON t.id=i.pick_task_id AND t.tenant_id=i.tenant_id "
                + "AND t.deleted=b'0' INNER JOIN erp_sale_pick_delivery_order o ON o.id=i.order_id "
                + "AND o.tenant_id=i.tenant_id AND o.deleted=b'0' "
                + "LEFT JOIN erp_sale_cart sc ON t.source_type=:cartSource AND sc.id=t.source_id "
                + "AND sc.tenant_id=t.tenant_id AND sc.deleted=b'0' "
                + "LEFT JOIN erp_sale_out so ON so.id=t.sale_out_id AND so.tenant_id=t.tenant_id AND so.deleted=b'0' "
                + "LEFT JOIN erp_customer c ON c.id=t.customer_id AND c.tenant_id=t.tenant_id AND c.deleted=b'0' "
                + "LEFT JOIN erp_warehouse w ON w.id=t.warehouse_id AND w.tenant_id=t.tenant_id AND w.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=i.product_id AND p.tenant_id=i.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_stock s ON s.product_id=i.product_id AND s.warehouse_id=i.warehouse_id "
                + "AND s.tenant_id=i.tenant_id AND s.deleted=b'0' "
                + "LEFT JOIN system_dept d ON d.id=COALESCE(sc.dept_id,so.dept_id,o.dept_id) "
                + "AND d.tenant_id=i.tenant_id AND d.deleted=b'0' "
                + "LEFT JOIN system_users su ON su.id=COALESCE(sc.sale_user_id,so.sale_user_id) "
                + "AND su.tenant_id=i.tenant_id AND su.deleted=b'0' "
                + "LEFT JOIN system_users ou ON ou.id=" + operationUser
                + " AND ou.tenant_id=i.tenant_id AND ou.deleted=b'0' "
                + "WHERE i.tenant_id=:tenant AND i.deleted=b'0'" + sourceScope(scope, sql.params) + ") r WHERE 1=1";
        dateRange(sql, "r.task_create_date", "taskCreate", filter.getTaskCreateDateFrom(), filter.getTaskCreateDateTo());
        dateRange(sql, "r.sale_out_date", "saleOut", filter.getSaleOutDateFrom(), filter.getSaleOutDateTo());
        dateRange(sql, "r.operation_time", "operation", filter.getOperationTimeFrom(), filter.getOperationTimeTo());
        dateRange(sql, "r.complete_time", "complete", filter.getCompleteTimeFrom(), filter.getCompleteTimeTo());
        like(sql, "r.source_cart_no", "sourceCartNo", filter.getSourceCartNo());
        like(sql, "r.sale_out_no", "saleOutNo", filter.getSaleOutNo());
        equal(sql, "r.customer_id", "customerId", filter.getCustomerId());
        equal(sql, "r.warehouse_id", "warehouseId", filter.getWarehouseId());
        equal(sql, "r.dept_id", "deptId", filter.getDeptId());
        equal(sql, "r.sale_user_id", "saleUserId", filter.getSaleUserId());
        equal(sql, "r.operation_user_id", "operationUserId", filter.getOperationUserId());
        equal(sql, "r.status", "status", filter.getStatus());
        if (hasText(filter.getProductKeyword())) {
            sql.params.put("productKeyword", "%" + filter.getProductKeyword().trim() + "%");
            sql.text += " AND (r.product_code LIKE :productKeyword OR r.product_name LIKE :productKeyword "
                    + "OR r.standard LIKE :productKeyword)";
        }
        range(sql, "r.count", "count", filter.getCountMin(), filter.getCountMax());
        range(sql, "r.picked_count", "picked", filter.getPickedCountMin(), filter.getPickedCountMax());
        if (delivery) {
            range(sql, "r.delivered_count", "delivered", filter.getDeliveredCountMin(), filter.getDeliveredCountMax());
            range(sql, "r.available_delivery_count", "available", filter.getAvailableCountMin(), filter.getAvailableCountMax());
            range(sql, "r.remaining_delivery_count", "remaining", filter.getRemainingCountMin(), filter.getRemainingCountMax());
        } else {
            range(sql, "r.remaining_pick_count", "remaining", filter.getRemainingCountMin(), filter.getRemainingCountMax());
        }
        return sql;
    }

    private String sourceScope(Scope scope, Map<String, Object> params) {
        String cart = scopeCondition(scope.cartScope, "COALESCE(sc.dept_id,o.dept_id)", "t.creator", "cart", params);
        String out = scopeCondition(scope.outScope, "COALESCE(so.dept_id,o.dept_id)", "t.creator", "out", params);
        return " AND ((t.source_type=:cartSource" + cart + ") OR ((t.source_type<>:cartSource OR t.source_type IS NULL)"
                + out + "))";
    }

    private String scopeCondition(ErpFinanceVisibleScope scope, String deptColumn, String creatorColumn,
                                  String prefix, Map<String, Object> params) {
        if (scope != null && scope.isAll()) return "";
        StringBuilder value = new StringBuilder(" AND (");
        boolean added = false;
        if (scope != null && !scope.getDeptIds().isEmpty()) {
            value.append(deptColumn).append(" IN (:").append(prefix).append("DeptIds)");
            params.put(prefix + "DeptIds", scope.getDeptIds());
            added = true;
        }
        if (scope != null && scope.getSelfUserId() != null) {
            if (added) value.append(" OR ");
            value.append(creatorColumn).append("=:").append(prefix).append("Self");
            params.put(prefix + "Self", String.valueOf(scope.getSelfUserId()));
            added = true;
        }
        return added ? value.append(')').toString() : " AND 1=0";
    }

    private String orderBy(Filter filter) { return " ORDER BY " + order(filter); }
    private String windowOrder(Filter filter) { return "ORDER BY " + order(filter); }
    private String order(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("taskCreateDate", "r.task_create_date");
        fields.put("saleOutDate", "r.sale_out_date");
        fields.put("sourceCartNo", "r.source_cart_no");
        fields.put("saleOutNo", "r.sale_out_no");
        fields.put("customerName", "r.customer_name");
        fields.put("warehouseName", "r.warehouse_name");
        fields.put("productCode", "r.product_code");
        fields.put("productName", "r.product_name");
        fields.put("count", "r.count");
        fields.put("pickedCount", "r.picked_count");
        fields.put("deliveredCount", "r.delivered_count");
        fields.put("operationTime", "r.operation_time");
        fields.put("completeTime", "r.complete_time");
        String field = fields.getOrDefault(filter.getOrderField(), "r.task_create_date");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return field + direction + ",COALESCE(r.sale_out_no,r.source_cart_no)" + direction + ",r.item_id ASC";
    }

    private void dateRange(Sql sql, String column, String prefix, Object from, Object to) {
        if (from != null) { sql.text += " AND " + column + ">=:" + prefix + "From"; sql.params.put(prefix + "From", from); }
        if (to != null) { sql.text += " AND " + column + "<=:" + prefix + "To"; sql.params.put(prefix + "To", to); }
    }
    private void range(Sql sql, String column, String prefix, Object min, Object max) {
        if (min != null) { sql.text += " AND " + column + ">=:" + prefix + "Min"; sql.params.put(prefix + "Min", min); }
        if (max != null) { sql.text += " AND " + column + "<=:" + prefix + "Max"; sql.params.put(prefix + "Max", max); }
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
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private NamedParameterJdbcTemplate jdbc() { return new NamedParameterJdbcTemplate(jdbcTemplate); }

    public static class Scope {
        private final long tenantId;
        private final ErpFinanceVisibleScope cartScope;
        private final ErpFinanceVisibleScope outScope;
        public Scope(long tenantId, ErpFinanceVisibleScope cartScope, ErpFinanceVisibleScope outScope) {
            this.tenantId = tenantId;
            this.cartScope = cartScope;
            this.outScope = outScope;
        }
    }
    private static class Sql {
        private String text;
        private final Map<String, Object> params = new HashMap<>();
    }
}
