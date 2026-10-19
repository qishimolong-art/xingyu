package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels.ItemProgress;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ErpSaleQuoteDetailReportRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = base(filter, scope);
        Long value = jdbc().queryForObject("SELECT COUNT(*) " + sql.text, sql.params, Long.class);
        return value == null ? 0L : value;
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
                        + "COALESCE(SUM(COALESCE(sqi.count,0)),0) AS total_count,"
                        + "COALESCE(SUM(COALESCE(sqi.count,0)*COALESCE(sqi.product_price,0)),0) AS total_price,"
                        + "COALESCE(SUM(COALESCE(sqi.converted_count,0)),0) AS converted_count " + sql.text,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    public Summary progressSummary(Filter filter, Scope scope) {
        Sql sql = base(filter, scope);
        String progress = "SELECT r.source_item_id,"
                + "SUM(CASE WHEN c.status=50 THEN r.count ELSE 0 END) AS actual_out_count "
                + "FROM erp_sale_convert_record r INNER JOIN erp_sale_cart c ON c.id=r.target_id "
                + "AND c.tenant_id=r.tenant_id AND c.deleted=b'0' "
                + "WHERE r.tenant_id=:tenant AND r.deleted=b'0' AND r.source_type=20 AND r.target_type=30 "
                + "GROUP BY r.source_item_id";
        String query = "SELECT COALESCE(SUM(CASE WHEN filtered.raw_status=50 AND p.source_item_id IS NULL "
                + "THEN filtered.count ELSE COALESCE(p.actual_out_count,0) END),0) AS actual_out_count,"
                + "COALESCE(SUM(GREATEST(filtered.count-(CASE WHEN filtered.raw_status=50 AND p.source_item_id IS NULL "
                + "THEN filtered.count ELSE COALESCE(p.actual_out_count,0) END),0)),0) AS pending_out_count "
                + "FROM (SELECT sqi.id AS item_id,COALESCE(sqi.count,0) AS count,sq.status AS raw_status "
                + sql.text + ") filtered LEFT JOIN (" + progress + ") p ON p.source_item_id=filtered.item_id";
        return jdbc().queryForObject(query, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    public List<ItemProgress> itemProgress(Collection<Long> itemIds, long tenantId) {
        if (itemIds == null || itemIds.isEmpty()) return List.of();
        Map<String, Object> params = new HashMap<>();
        params.put("tenant", tenantId);
        params.put("itemIds", itemIds);
        String sql = "SELECT r.source_item_id AS item_id,COALESCE(SUM(r.count),0) AS converted_count,"
                + "COALESCE(SUM(CASE WHEN c.status=50 THEN r.count ELSE 0 END),0) AS actual_out_count "
                + "FROM erp_sale_convert_record r INNER JOIN erp_sale_cart c ON c.id=r.target_id "
                + "AND c.tenant_id=r.tenant_id AND c.deleted=b'0' "
                + "WHERE r.tenant_id=:tenant AND r.deleted=b'0' AND r.source_item_id IN (:itemIds) "
                + "AND r.source_type=20 AND r.target_type=30 GROUP BY r.source_item_id";
        return jdbc().query(sql, params, new BeanPropertyRowMapper<>(ItemProgress.class));
    }

    private String selectFields() {
        return "SELECT sqi.id AS item_id,sq.id AS quote_id,sq.source_id AS source_business_id,"
                + "CASE sq.source_type WHEN 10 THEN 'SALE_ORDER' WHEN 20 THEN 'SALE_QUOTE' "
                + "WHEN 30 THEN 'SALE_CART' WHEN 40 THEN 'SALE_PRICE_ADJUST' WHEN 50 THEN 'PURCHASE_IN' "
                + "WHEN 60 THEN 'MALL_ORDER' WHEN 70 THEN 'SALE_RETURN' END AS source_document_type_code,"
                + "sqi.product_id,sqi.warehouse_id,"
                + "sq.quote_time AS quote_date,sq.no AS document_no,sq.source_no,sq.status AS raw_status,"
                + "c.name AS customer_name,c.code AS customer_code,c.area_id AS customer_area_id,d.name AS dept_name,"
                + "su.nickname AS sale_user_name,cu.nickname AS creator_name,sq.expected_delivery_time AS expected_delivery_date,"
                + "sq.price_type,p.code AS product_code,p.name AS product_name,"
                + "COALESCE(NULLIF(sqi.vehicle_model,''),p.vehicle_model) AS vehicle_model,p.feature_code,"
                + "COALESCE(NULLIF(sqi.drawing_no,''),p.drawing_no) AS drawing_no,"
                + "COALESCE(NULLIF(sqi.standard,''),p.standard) AS standard,"
                + "COALESCE(NULLIF(sqi.brand,''),p.brand) AS brand,"
                + "COALESCE(NULLIF(sqi.origin_place,''),p.origin_place) AS origin_place,u.name AS unit_name,"
                + "w.name AS warehouse_name,sqi.warehouse_position,"
                + "CASE WHEN sqi.gift_flag=b'1' THEN '是' ELSE '否' END AS gift_flag_text,"
                + "COALESCE(sqi.count,0) AS count,COALESCE(sqi.product_price,0) AS product_price,"
                + "ROUND(COALESCE(sqi.count,0)*COALESCE(sqi.product_price,0),2) AS total_price,"
                + "sqi.stock_count_at_order,p.stock_max,p.stock_min,p.stock_standard,"
                + "COALESCE(sqi.converted_count,0) AS converted_count,sq.remark AS document_remark ";
    }

    private Sql base(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        sql.text = "FROM erp_sale_quote_items sqi INNER JOIN erp_sale_quote sq ON sq.id=sqi.quote_id "
                + "AND sq.tenant_id=sqi.tenant_id AND sq.deleted=b'0' AND sq.status IN (20,30,40,50) "
                + "INNER JOIN erp_customer c ON c.id=sq.customer_id AND c.tenant_id=sq.tenant_id AND c.deleted=b'0' "
                + "LEFT JOIN system_dept d ON d.id=sq.dept_id AND d.tenant_id=sq.tenant_id AND d.deleted=b'0' "
                + "LEFT JOIN system_users su ON su.id=sq.sale_user_id AND su.tenant_id=sq.tenant_id AND su.deleted=b'0' "
                + "LEFT JOIN system_users cu ON cu.id=sq.creator AND cu.tenant_id=sq.tenant_id AND cu.deleted=b'0' "
                + "LEFT JOIN erp_product p ON p.id=sqi.product_id AND p.tenant_id=sqi.tenant_id AND p.deleted=b'0' "
                + "LEFT JOIN erp_product_unit u ON u.id=sqi.product_unit_id AND u.tenant_id=sqi.tenant_id AND u.deleted=b'0' "
                + "LEFT JOIN erp_warehouse w ON w.id=sqi.warehouse_id AND w.tenant_id=sqi.tenant_id AND w.deleted=b'0' "
                + "WHERE sqi.tenant_id=:tenant AND sqi.deleted=b'0'";
        sql.text += documentScope(scope.quoteScope, "sq.dept_id", "sq.creator", "quote", sql.params);
        sql.text += customerScope(scope.customerScope, "c", "customer", sql.params);
        if (filter.getQuoteDateFrom() != null) {
            sql.text += " AND sq.quote_time>=:dateFrom";
            sql.params.put("dateFrom", filter.getQuoteDateFrom());
        }
        if (filter.getQuoteDateTo() != null) {
            sql.text += " AND sq.quote_time<=:dateTo";
            sql.params.put("dateTo", filter.getQuoteDateTo());
        }
        like(sql, "sq.no", "documentNo", filter.getDocumentNo());
        like(sql, "sq.source_no", "sourceNo", filter.getSourceNo());
        equal(sql, "sq.customer_id", "customerId", filter.getCustomerId());
        equal(sql, "sq.sale_user_id", "saleUserId", filter.getSaleUserId());
        equal(sql, "sq.dept_id", "deptId", filter.getDeptId());
        equal(sql, "sqi.warehouse_id", "warehouseId", filter.getWarehouseId());
        if (filter.getOrderStatus() != null) {
            sql.text += " AND :orderStatus=20";
            sql.params.put("orderStatus", filter.getOrderStatus());
        }
        if (filter.getOutStatus() != null) {
            if (filter.getOutStatus() == 0) sql.text += " AND sq.status=20";
            else {
                sql.text += " AND sq.status=:outStatus";
                sql.params.put("outStatus", filter.getOutStatus());
            }
        }
        if (hasText(filter.getProductKeyword())) {
            sql.params.put("productKeyword", "%" + filter.getProductKeyword().trim() + "%");
            sql.text += " AND (p.code LIKE :productKeyword OR p.name LIKE :productKeyword "
                    + "OR p.factory_code LIKE :productKeyword OR p.drawing_no LIKE :productKeyword "
                    + "OR p.feature_code LIKE :productKeyword OR sqi.drawing_no LIKE :productKeyword)";
        }
        return sql;
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("quoteDate", "sq.quote_time");
        fields.put("documentNo", "sq.no");
        fields.put("productCode", "p.code");
        fields.put("productName", "p.name");
        fields.put("customerName", "c.name");
        fields.put("warehouseName", "w.name");
        fields.put("count", "sqi.count");
        fields.put("productPrice", "sqi.product_price");
        fields.put("totalPrice", "COALESCE(sqi.count,0)*COALESCE(sqi.product_price,0)");
        fields.put("convertedCount", "sqi.converted_count");
        String field = fields.getOrDefault(filter.getOrderField(), "sq.quote_time");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",sq.no" + direction + ",sqi.id" + direction;
    }

    private String documentScope(ErpFinanceVisibleScope scope, String deptColumn, String creatorColumn,
                                 String prefix, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(deptColumn).append(" IN (:").append(prefix).append("DeptIds)");
            params.put(prefix + "DeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append(creatorColumn).append("=:").append(prefix).append("Self");
            params.put(prefix + "Self", String.valueOf(scope.getSelfUserId()));
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private String customerScope(ErpFinanceVisibleScope scope, String alias, String prefix,
                                 Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(alias).append(".dept_id IN (:").append(prefix).append("DeptIds) OR (")
                    .append(alias).append(".allow_multi_dept=b'1' AND EXISTS (SELECT 1 FROM erp_customer_dept ecd ")
                    .append("WHERE ecd.tenant_id=").append(alias).append(".tenant_id AND ecd.customer_id=")
                    .append(alias).append(".id AND ecd.deleted=b'0' AND ecd.dept_id IN (:")
                    .append(prefix).append("DeptIds)))");
            params.put(prefix + "DeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append(alias).append(".creator=:").append(prefix).append("Self");
            params.put(prefix + "Self", String.valueOf(scope.getSelfUserId()));
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
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
        private final ErpFinanceVisibleScope quoteScope;
        private final ErpFinanceVisibleScope customerScope;

        public Scope(long tenantId, ErpFinanceVisibleScope quoteScope, ErpFinanceVisibleScope customerScope) {
            this.tenantId = tenantId;
            this.quoteScope = quoteScope;
            this.customerScope = customerScope;
        }
    }

    private static class Sql {
        private String text;
        private final Map<String, Object> params = new HashMap<>();
    }
}
