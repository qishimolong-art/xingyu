package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockProfitLossDetailReportModels.Summary;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 配件盈亏逐明细查询；一次联表完成分页，避免按盘点单逐项查询。 */
@Repository
public class ErpStockProfitLossDetailReportRepository {

    private static final int APPROVED_STATUS = 20;

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
        return jdbc().query("SELECT r.* " + sql.text + orderBy(filter) + " LIMIT :limit OFFSET :offset",
                sql.params, new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = base(filter, scope);
        return jdbc().queryForObject("SELECT COUNT(*) AS row_count,"
                        + "COALESCE(SUM(r.profit_loss_count),0) AS profit_loss_count,"
                        + "COALESCE(SUM(r.profit_loss_amount),0) AS profit_loss_amount " + sql.text,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    Sql base(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        sql.params.put("approvedStatus", APPROVED_STATUS);
        sql.text = "FROM (SELECT i.id AS item_id,c.id AS check_id,i.warehouse_id,i.product_id,"
                + "c.check_type,c.no AS document_no,c.check_time AS check_date,p.code AS product_code,"
                + "p.name AS product_name,w.name AS warehouse_name,p.shelf,i.count AS profit_loss_count,"
                + "p.purchase_price AS cost_unit_price,i.product_price AS adjusted_unit_price,"
                + "i.actual_count,i.total_price AS profit_loss_amount,u.nickname AS creator_name,"
                + "p.vehicle_model,c.remark AS reason,p.origin_place,p.standard,p.drawing_no,"
                + "NULL AS related_no,i.remark AS item_remark,c.creator "
                + "FROM erp_stock_check_item i INNER JOIN erp_stock_check c ON c.id=i.check_id "
                + "AND c.tenant_id=i.tenant_id AND c.deleted=b'0' "
                + "INNER JOIN erp_product p ON p.id=i.product_id AND p.tenant_id=i.tenant_id AND p.deleted=b'0' "
                + "INNER JOIN erp_warehouse w ON w.id=i.warehouse_id AND w.tenant_id=i.tenant_id AND w.deleted=b'0' "
                + "LEFT JOIN system_users u ON u.id=c.creator AND u.tenant_id=c.tenant_id AND u.deleted=b'0' "
                + "WHERE i.tenant_id=:tenant AND i.deleted=b'0' AND c.status=:approvedStatus"
                + scopeCondition(scope, sql.params) + ") r WHERE 1=1";
        dateRange(sql, "r.check_date", "checkDate", filter.getCheckDateFrom(), filter.getCheckDateTo());
        equal(sql, "r.check_type", "checkType", filter.getCheckType());
        equal(sql, "r.warehouse_id", "warehouseId", filter.getWarehouseId());
        like(sql, "r.document_no", "documentNo", filter.getDocumentNo());
        like(sql, "r.creator_name", "creatorKeyword", filter.getCreatorKeyword());
        if (StringUtils.hasText(filter.getKeyword())) {
            sql.params.put("keyword", "%" + filter.getKeyword().trim().replaceAll("\\s+", "%") + "%");
            sql.text += " AND (r.product_code LIKE :keyword OR r.product_name LIKE :keyword"
                    + " OR r.vehicle_model LIKE :keyword OR r.origin_place LIKE :keyword"
                    + " OR r.standard LIKE :keyword OR r.drawing_no LIKE :keyword"
                    + " OR r.shelf LIKE :keyword OR r.warehouse_name LIKE :keyword)";
        }
        return sql;
    }

    private String scopeCondition(Scope scope, Map<String, Object> params) {
        if (scope.all) {
            return "";
        }
        boolean hasDepartment = scope.departmentWarehouseIds != null && !scope.departmentWarehouseIds.isEmpty();
        boolean hasSelf = scope.selfWarehouseIds != null && !scope.selfWarehouseIds.isEmpty()
                && scope.selfCreator != null;
        if (!hasDepartment && !hasSelf) {
            return " AND 1=0";
        }
        StringBuilder condition = new StringBuilder(" AND (");
        if (hasDepartment) {
            condition.append("i.warehouse_id IN (:departmentWarehouseIds)");
            params.put("departmentWarehouseIds", scope.departmentWarehouseIds);
        }
        if (hasSelf) {
            if (hasDepartment) {
                condition.append(" OR ");
            }
            condition.append("(i.warehouse_id IN (:selfWarehouseIds) AND c.creator=:selfCreator)");
            params.put("selfWarehouseIds", scope.selfWarehouseIds);
            params.put("selfCreator", scope.selfCreator);
        }
        return condition.append(')').toString();
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("checkTypeName", "r.check_type");
        fields.put("documentNo", "r.document_no");
        fields.put("checkDate", "r.check_date");
        fields.put("productCode", "r.product_code");
        fields.put("productName", "r.product_name");
        fields.put("warehouseName", "r.warehouse_name");
        fields.put("profitLossCount", "r.profit_loss_count");
        fields.put("costUnitPrice", "r.cost_unit_price");
        fields.put("adjustedUnitPrice", "r.adjusted_unit_price");
        fields.put("actualCount", "r.actual_count");
        fields.put("profitLossAmount", "r.profit_loss_amount");
        fields.put("creatorName", "r.creator_name");
        String field = fields.get(filter.getOrderField());
        if (field == null) {
            return " ORDER BY r.check_date DESC,r.check_id DESC,r.item_id ASC";
        }
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",r.check_id DESC,r.item_id ASC";
    }

    private void dateRange(Sql sql, String column, String prefix, Object from, Object to) {
        if (from != null) {
            sql.text += " AND " + column + ">=:" + prefix + "From";
            sql.params.put(prefix + "From", from);
        }
        if (to != null) {
            sql.text += " AND " + column + "<=:" + prefix + "To";
            sql.params.put(prefix + "To", to);
        }
    }

    private void like(Sql sql, String column, String name, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        sql.text += " AND " + column + " LIKE :" + name;
        sql.params.put(name, "%" + value.trim() + "%");
    }

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null) {
            return;
        }
        sql.text += " AND " + column + "=:" + name;
        sql.params.put(name, value);
    }

    private NamedParameterJdbcTemplate jdbc() {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public static class Scope {
        private final long tenantId;
        private final boolean all;
        private final Collection<Long> departmentWarehouseIds;
        private final Collection<Long> selfWarehouseIds;
        private final String selfCreator;

        public Scope(long tenantId, boolean all, Collection<Long> departmentWarehouseIds,
                     Collection<Long> selfWarehouseIds, String selfCreator) {
            this.tenantId = tenantId;
            this.all = all;
            this.departmentWarehouseIds = departmentWarehouseIds;
            this.selfWarehouseIds = selfWarehouseIds;
            this.selfCreator = selfCreator;
        }
    }

    static class Sql {
        private String text;
        private final Map<String, Object> params = new HashMap<>();
    }
}
