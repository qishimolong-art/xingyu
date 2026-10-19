package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Summary;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.*;

/** 库龄辅助报表查询。库存流水只聚合一次，避免逐库存查询。 */
@Repository
public class ErpStockAgeReportRepository {

    private static final int WAREHOUSE_MOVE_IN = 34;

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope, LocalDate asOfDate) {
        Sql sql = reportSql(filter, scope, asOfDate);
        Long count = jdbc().queryForObject(sql.cte + " SELECT COUNT(*) FROM report WHERE 1=1" + sql.displayWhere,
                sql.params, Long.class);
        return count == null ? 0L : count;
    }

    public List<Row> page(Filter filter, Scope scope, LocalDate asOfDate, long offset, int limit) {
        Sql sql = reportSql(filter, scope, asOfDate);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(sql.cte + " SELECT report.* FROM report WHERE 1=1" + sql.displayWhere
                        + orderBy(filter) + " LIMIT :limit OFFSET :offset", sql.params,
                new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope, LocalDate asOfDate) {
        Sql sql = reportSql(filter, scope, asOfDate);
        String query = sql.cte + ",matched AS (SELECT * FROM report WHERE 1=1" + sql.displayWhere + ")"
                + " SELECT COUNT(*) row_count,COALESCE(SUM(stock_quantity),0) total_quantity,"
                + "COALESCE(SUM(cost_amount),0) total_amount,"
                + sum("within1_month_quantity") + "," + sum("within1_month_amount") + ","
                + sum("months2_to3_quantity") + "," + sum("months2_to3_amount") + ","
                + sum("months4_to6_quantity") + "," + sum("months4_to6_amount") + ","
                + sum("months7_to12_quantity") + "," + sum("months7_to12_amount") + ","
                + sum("years1_to2_quantity") + "," + sum("years1_to2_amount") + ","
                + sum("years2_to3_quantity") + "," + sum("years2_to3_amount") + ","
                + sum("over3_years_quantity") + "," + sum("over3_years_amount") + ","
                + "COALESCE(SUM(CASE WHEN age_days IS NULL THEN 1 ELSE 0 END),0) unclassified_row_count,"
                + "COALESCE(SUM(CASE WHEN age_days IS NULL THEN stock_quantity ELSE 0 END),0) unclassified_quantity,"
                + "COALESCE(SUM(CASE WHEN age_days IS NULL THEN cost_amount ELSE 0 END),0) unclassified_amount"
                + " FROM matched";
        return jdbc().queryForObject(query, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    Sql reportSql(Filter filter, Scope scope, LocalDate asOfDate) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        sql.params.put("asOfDate", asOfDate);
        sql.cte = "WITH movement AS ("
                + "SELECT tenant_id,product_id,warehouse_id,"
                + "MAX(CASE WHEN count>0 THEN COALESCE(biz_date,create_time) END) last_inbound_time,"
                + "MAX(CASE WHEN count>0 AND biz_type=" + WAREHOUSE_MOVE_IN
                + " THEN COALESCE(biz_date,create_time) END) last_warehouse_move_inbound_time,"
                + "MAX(CASE WHEN count<0 THEN COALESCE(biz_date,create_time) END) last_outbound_time "
                + "FROM erp_stock_record WHERE tenant_id=:tenant AND deleted=b'0' "
                + "GROUP BY tenant_id,product_id,warehouse_id),"
                + "base AS (SELECT s.id stock_id,s.product_id,s.warehouse_id,COALESCE(s.dept_id,w.dept_id) dept_id,"
                + "w.name warehouse_name,d.name dept_name,p.code product_code,p.name product_name,"
                + "s.count stock_quantity,s.cost_price cost_unit_price,"
                + "COALESCE(s.cost_amount,s.count*s.cost_price) cost_amount,"
                + "m.last_inbound_time,m.last_warehouse_move_inbound_time,m.last_outbound_time,"
                + "CASE WHEN m.last_outbound_time IS NOT NULL THEN '最后有效出库' "
                + "WHEN m.last_inbound_time IS NOT NULL THEN '最后入库' ELSE '无法计算' END aging_basis,"
                + "CASE WHEN COALESCE(m.last_outbound_time,m.last_inbound_time) IS NULL THEN NULL "
                + "ELSE GREATEST(DATEDIFF(:asOfDate,DATE(COALESCE(m.last_outbound_time,m.last_inbound_time))),0) END age_days,"
                + "p.standard,p.vehicle_model,p.drawing_no,p.origin_place,c.name category_name,p.feature_code,p.brand "
                + "FROM erp_stock s INNER JOIN erp_product p ON p.id=s.product_id AND p.tenant_id=s.tenant_id "
                + "AND p.deleted=b'0' INNER JOIN erp_warehouse w ON w.id=s.warehouse_id AND w.tenant_id=s.tenant_id "
                + "AND w.deleted=b'0' LEFT JOIN system_dept d ON d.id=COALESCE(s.dept_id,w.dept_id) "
                + "AND d.tenant_id=s.tenant_id AND d.deleted=b'0' "
                + "LEFT JOIN erp_product_category c ON c.id=p.category_id AND c.tenant_id=p.tenant_id AND c.deleted=b'0' "
                + "LEFT JOIN movement m ON m.tenant_id=s.tenant_id AND m.product_id=s.product_id "
                + "AND m.warehouse_id=s.warehouse_id WHERE s.tenant_id=:tenant AND s.deleted=b'0' AND s.count>0"
                + baseFilters(filter, scope, sql.params) + "),"
                + "report AS (SELECT base.*," + bucket("within1_month", "age_days BETWEEN 0 AND 30") + ","
                + bucket("months2_to3", "age_days BETWEEN 31 AND 90") + ","
                + bucket("months4_to6", "age_days BETWEEN 91 AND 180") + ","
                + bucket("months7_to12", "age_days BETWEEN 181 AND 365") + ","
                + bucket("years1_to2", "age_days BETWEEN 366 AND 730") + ","
                + bucket("years2_to3", "age_days BETWEEN 731 AND 1095") + ","
                + bucket("over3_years", "age_days>=1096") + " FROM base)";
        sql.displayWhere = ageBucketCondition(filter.getAgeBucket()) + outboundCondition(filter.getOutboundState());
        return sql;
    }

    private String baseFilters(Filter filter, Scope scope, Map<String, Object> params) {
        StringBuilder where = new StringBuilder();
        if (filter.getDeptId() != null) {
            where.append(" AND COALESCE(s.dept_id,w.dept_id)=:deptId");
            params.put("deptId", filter.getDeptId());
        }
        if (filter.getWarehouseId() != null) {
            where.append(" AND s.warehouse_id=:warehouseId");
            params.put("warehouseId", filter.getWarehouseId());
        }
        if (filter.getCategoryId() != null) {
            where.append(" AND p.category_id=:categoryId");
            params.put("categoryId", filter.getCategoryId());
        }
        if (StringUtils.hasText(filter.getBrand())) {
            where.append(" AND p.brand LIKE :brand");
            params.put("brand", "%" + filter.getBrand().trim() + "%");
        }
        if (StringUtils.hasText(filter.getKeyword())) {
            where.append(" AND (p.code LIKE :keyword OR p.name LIKE :keyword OR p.pinyin_code LIKE :keyword")
                    .append(" OR p.wubi_code LIKE :keyword OR p.bar_code LIKE :keyword OR p.vehicle_model LIKE :keyword")
                    .append(" OR p.drawing_no LIKE :keyword OR p.feature_code LIKE :keyword OR p.brand LIKE :keyword")
                    .append(" OR p.standard LIKE :keyword OR w.name LIKE :keyword OR d.name LIKE :keyword)");
            params.put("keyword", "%" + filter.getKeyword().trim().replaceAll("\\s+", "%") + "%");
        }
        appendScope(where, scope, params);
        return where.toString();
    }

    private void appendScope(StringBuilder where, Scope scope, Map<String, Object> params) {
        if (scope.all) {
            return;
        }
        boolean hasDepartment = scope.departmentWarehouseIds != null && !scope.departmentWarehouseIds.isEmpty();
        boolean hasSelf = scope.selfWarehouseIds != null && !scope.selfWarehouseIds.isEmpty()
                && scope.selfCreator != null;
        if (!hasDepartment && !hasSelf) {
            where.append(" AND 1=0");
            return;
        }
        where.append(" AND (");
        if (hasDepartment) {
            where.append("s.warehouse_id IN (:departmentWarehouseIds)");
            params.put("departmentWarehouseIds", scope.departmentWarehouseIds);
        }
        if (hasSelf) {
            if (hasDepartment) {
                where.append(" OR ");
            }
            where.append("(s.warehouse_id IN (:selfWarehouseIds) AND s.creator=:selfCreator)");
            params.put("selfWarehouseIds", scope.selfWarehouseIds);
            params.put("selfCreator", scope.selfCreator);
        }
        where.append(')');
    }

    private String bucket(String prefix, String condition) {
        return "CASE WHEN " + condition + " THEN stock_quantity ELSE 0 END " + prefix + "_quantity,"
                + "CASE WHEN " + condition + " THEN cost_amount ELSE 0 END " + prefix + "_amount";
    }

    private String sum(String column) {
        return "COALESCE(SUM(" + column + "),0) " + column;
    }

    static String ageBucketCondition(String bucket) {
        if (!StringUtils.hasText(bucket) || BUCKET_ALL.equals(bucket)) return "";
        if (BUCKET_WITHIN_1_MONTH.equals(bucket)) return " AND report.age_days BETWEEN 0 AND 30";
        if (BUCKET_MONTHS_2_TO_3.equals(bucket)) return " AND report.age_days BETWEEN 31 AND 90";
        if (BUCKET_MONTHS_4_TO_6.equals(bucket)) return " AND report.age_days BETWEEN 91 AND 180";
        if (BUCKET_MONTHS_7_TO_12.equals(bucket)) return " AND report.age_days BETWEEN 181 AND 365";
        if (BUCKET_YEARS_1_TO_2.equals(bucket)) return " AND report.age_days BETWEEN 366 AND 730";
        if (BUCKET_YEARS_2_TO_3.equals(bucket)) return " AND report.age_days BETWEEN 731 AND 1095";
        if (BUCKET_OVER_3_YEARS.equals(bucket)) return " AND report.age_days>=1096";
        if (BUCKET_UNCLASSIFIED.equals(bucket)) return " AND report.age_days IS NULL";
        return " AND 1=0";
    }

    static String outboundCondition(String state) {
        if (!StringUtils.hasText(state) || OUTBOUND_ALL.equals(state)) return "";
        if (OUTBOUND_NEVER.equals(state)) return " AND report.last_outbound_time IS NULL";
        if (OUTBOUND_HAS.equals(state)) return " AND report.last_outbound_time IS NOT NULL";
        return " AND 1=0";
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("warehouseName", "report.warehouse_name");
        fields.put("productCode", "report.product_code");
        fields.put("productName", "report.product_name");
        fields.put("stockQuantity", "report.stock_quantity");
        fields.put("costUnitPrice", "report.cost_unit_price");
        fields.put("costAmount", "report.cost_amount");
        fields.put("ageDays", "report.age_days");
        fields.put("lastInboundTime", "report.last_inbound_time");
        fields.put("lastOutboundTime", "report.last_outbound_time");
        fields.put("categoryName", "report.category_name");
        fields.put("brand", "report.brand");
        String field = fields.get(filter.getOrderField());
        if (field == null) {
            return " ORDER BY report.age_days IS NULL ASC,report.age_days DESC,report.stock_id DESC";
        }
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",report.stock_id DESC";
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
        String cte;
        String displayWhere = "";
        final Map<String, Object> params = new HashMap<>();
    }
}
