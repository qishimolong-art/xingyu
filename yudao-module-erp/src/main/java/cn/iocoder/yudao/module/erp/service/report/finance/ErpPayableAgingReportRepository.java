package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableAgingReportModels.Summary;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ErpPayableAgingReportRepository {

    private static final String PAYMENT_DISCOUNT_SOURCE_TYPE = "付款单折让";

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        Long value = jdbc().queryForObject(sql.cte + " SELECT COUNT(*) FROM report", sql.params, Long.class);
        return value == null ? 0L : value;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = reportSql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(sql.cte + " SELECT report.* FROM report" + orderBy(filter)
                        + " LIMIT :limit OFFSET :offset", sql.params, new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        return jdbc().queryForObject(sql.cte + " SELECT COUNT(*) row_count," + sumColumns() + " FROM report",
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    Sql reportSql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        applyBoundaries(sql.params, filter.getAsOfDate());

        String supplierWhere = supplierScope(scope.supplierScope, "s", "supplier", sql.params);
        if (filter.getSupplierId() != null) {
            supplierWhere += " AND s.id=:supplierId";
            sql.params.put("supplierId", filter.getSupplierId());
        }
        if (hasText(filter.getSupplierType())) {
            supplierWhere += " AND s.supplier_type LIKE :supplierType";
            sql.params.put("supplierType", "%" + filter.getSupplierType().trim() + "%");
        }
        if (hasText(filter.getPurchaser())) {
            supplierWhere += " AND s.purchaser LIKE :purchaser";
            sql.params.put("purchaser", "%" + filter.getPurchaser().trim() + "%");
        }

        String accountIn = documentScope(scope.accountScope, "t.dept_id", "t.creator", "pin", sql.params, true);
        String accountReturn = documentScope(scope.accountScope, "t.dept_id", "t.handler", "pret", sql.params, false);
        String accountAdjust = documentScope(scope.accountScope, "t.dept_id", "t.adjuster", "padj", sql.params, false);
        String accountOther = documentScope(scope.accountScope, "t.dept_id", "t.handler_id", "pother", sql.params, false);
        String accountPayment = documentScope(scope.accountScope, "t.dept_id", "t.finance_user_id", "payment", sql.params, false);

        String visibleSuppliers = "SELECT s.id supplier_id," + collate("s.name") + " supplier_name,"
                + collate("s.supplier_type") + " supplier_type," + collate("s.purchaser") + " purchaser"
                + " FROM erp_supplier s WHERE s.tenant_id=:tenant AND s.deleted=b'0'" + supplierWhere;
        String rawEvents = String.join(" UNION ALL ",
                purchaseIn(accountIn), purchaseReturn(accountReturn), purchaseAdjust(accountAdjust),
                payableAdjust(accountOther), payment(accountPayment));
        sql.cte = "WITH visible_suppliers AS (" + visibleSuppliers + "),raw_events AS (" + rawEvents + "),"
                + "aged AS (SELECT supplier_id,"
                + "ROUND(COALESCE(SUM(amount),0),6) balance," + bucketColumns()
                + " FROM raw_events GROUP BY supplier_id),"
                + "report AS (SELECT vs.supplier_id,vs.supplier_name,vs.supplier_type,vs.purchaser,"
                + coalescedColumns("a") + " FROM visible_suppliers vs LEFT JOIN aged a ON a.supplier_id=vs.supplier_id)";
        return sql;
    }

    private String purchaseIn(String scope) {
        return eventSelect("t.in_time", ErpPurchaseInMapper.ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION,
                "erp_purchase_in", scope);
    }

    private String purchaseReturn(String scope) {
        return eventSelect("t.return_time", "-ABS(COALESCE(t.total_price,0))", "erp_purchase_return", scope);
    }

    private String purchaseAdjust(String scope) {
        return eventSelect("t.adjust_time", "COALESCE(t.total_adjust_price,0)",
                "erp_purchase_price_adjust", scope);
    }

    private String payableAdjust(String scope) {
        String date = "CASE WHEN t.source_type='" + PAYMENT_DISCOUNT_SOURCE_TYPE + "' AND t.source_id IS NOT NULL "
                + "THEN COALESCE((SELECT fp.payment_time FROM erp_finance_payment fp WHERE fp.tenant_id=t.tenant_id "
                + "AND fp.deleted=b'0' AND fp.id=t.source_id LIMIT 1),TIMESTAMP(t.biz_time),t.create_time) "
                + "ELSE COALESCE(TIMESTAMP(t.biz_time),t.create_time) END";
        return eventSelect(date, "COALESCE(t.payable_amount,0)", "erp_payable_other", scope);
    }

    private String payment(String scope) {
        String amount = "-(CASE WHEN EXISTS (SELECT 1 FROM erp_payable_other dpo WHERE "
                + "dpo.tenant_id=t.tenant_id AND dpo.deleted=b'0' AND dpo.status=20 "
                + "AND dpo.source_type='" + PAYMENT_DISCOUNT_SOURCE_TYPE + "' AND dpo.source_id=t.id) "
                + "THEN COALESCE(t.payment_price,0) ELSE COALESCE(t.total_price,0) END)";
        return eventSelect("t.payment_time", amount, "erp_finance_payment", scope);
    }

    private String eventSelect(String date, String amount, String table, String scope) {
        return "SELECT t.supplier_id,DATE(" + date + ") event_date,ROUND(" + amount + ",6) amount FROM "
                + table + " t INNER JOIN visible_suppliers vs ON vs.supplier_id=t.supplier_id"
                + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20 AND " + date
                + "<:cutoff_exclusive" + scope;
    }

    private String bucketColumns() {
        StringBuilder columns = new StringBuilder();
        for (int month = 1; month <= 12; month++) {
            String upper = month == 1 ? ":cutoff_exclusive" : ":month" + two(month - 1) + "_start";
            columns.append("ROUND(COALESCE(SUM(CASE WHEN event_date>=:month")
                    .append(two(month)).append("_start AND event_date<").append(upper)
                    .append(" THEN amount ELSE 0 END),0),6) month").append(two(month)).append(',');
        }
        columns.append("ROUND(COALESCE(SUM(CASE WHEN event_date>=:month24_start AND event_date<:month12_start ")
                .append("THEN amount ELSE 0 END),0),6) within_two_years,")
                .append("ROUND(COALESCE(SUM(CASE WHEN event_date>=:month36_start AND event_date<:month24_start ")
                .append("THEN amount ELSE 0 END),0),6) within_three_years,")
                .append("ROUND(COALESCE(SUM(CASE WHEN event_date<:month36_start THEN amount ELSE 0 END),0),6) ")
                .append("over_three_years");
        return columns.toString();
    }

    private String coalescedColumns(String alias) {
        StringBuilder columns = new StringBuilder("COALESCE(").append(alias).append(".balance,0) balance,");
        for (int month = 1; month <= 12; month++) {
            columns.append("COALESCE(").append(alias).append(".month").append(two(month))
                    .append(",0) month").append(two(month)).append(',');
        }
        return columns.append("COALESCE(").append(alias).append(".within_two_years,0) within_two_years,")
                .append("COALESCE(").append(alias).append(".within_three_years,0) within_three_years,")
                .append("COALESCE(").append(alias).append(".over_three_years,0) over_three_years").toString();
    }

    private String sumColumns() {
        StringBuilder columns = new StringBuilder("COALESCE(SUM(balance),0) balance,");
        for (int month = 1; month <= 12; month++) {
            columns.append("COALESCE(SUM(month").append(two(month)).append("),0) month")
                    .append(two(month)).append(',');
        }
        return columns.append("COALESCE(SUM(within_two_years),0) within_two_years,")
                .append("COALESCE(SUM(within_three_years),0) within_three_years,")
                .append("COALESCE(SUM(over_three_years),0) over_three_years").toString();
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("supplierName", "report.supplier_name");
        fields.put("supplierType", "report.supplier_type");
        fields.put("purchaser", "report.purchaser");
        fields.put("balance", "report.balance");
        for (int month = 1; month <= 12; month++) {
            fields.put("month" + two(month), "report.month" + two(month));
        }
        fields.put("withinTwoYears", "report.within_two_years");
        fields.put("withinThreeYears", "report.within_three_years");
        fields.put("overThreeYears", "report.over_three_years");
        String field = fields.getOrDefault(filter.getOrderField(), "report.supplier_name");
        String direction = "desc".equalsIgnoreCase(filter.getOrderDirection()) ? " DESC" : " ASC";
        return " ORDER BY " + field + direction + ",report.supplier_id" + direction;
    }

    static void applyBoundaries(Map<String, Object> params, LocalDate asOfDate) {
        params.put("cutoff_exclusive", asOfDate.plusDays(1).atStartOfDay());
        for (int month = 1; month <= 12; month++) {
            params.put("month" + two(month) + "_start", asOfDate.minusMonths(month));
        }
        params.put("month24_start", asOfDate.minusMonths(24));
        params.put("month36_start", asOfDate.minusMonths(36));
    }

    private String documentScope(ErpFinanceVisibleScope scope, String deptColumn, String userColumn,
                                 String prefix, Map<String, Object> params, boolean stringUserColumn) {
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
            condition.append(userColumn).append("=:").append(prefix).append("Self");
            params.put(prefix + "Self", stringUserColumn
                    ? String.valueOf(scope.getSelfUserId()) : scope.getSelfUserId());
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private String supplierScope(ErpFinanceVisibleScope scope, String alias, String prefix,
                                 Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(alias).append(".dept_id IN (:").append(prefix).append("DeptIds) OR (")
                    .append(alias).append(".allow_multi_dept=b'1' AND EXISTS (SELECT 1 FROM erp_supplier_dept sd ")
                    .append("WHERE sd.tenant_id=").append(alias).append(".tenant_id AND sd.supplier_id=")
                    .append(alias).append(".id AND sd.deleted=b'0' AND sd.dept_id IN (:")
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

    private String collate(String expression) {
        return "CONVERT(" + expression + " USING utf8mb4) COLLATE utf8mb4_unicode_ci";
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private NamedParameterJdbcTemplate jdbc() {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public static class Scope {
        private final long tenantId;
        private final ErpFinanceVisibleScope accountScope;
        private final ErpFinanceVisibleScope supplierScope;

        public Scope(long tenantId, ErpFinanceVisibleScope accountScope, ErpFinanceVisibleScope supplierScope) {
            this.tenantId = tenantId;
            this.accountScope = accountScope;
            this.supplierScope = supplierScope;
        }
    }

    static class Sql {
        String cte;
        final Map<String, Object> params = new HashMap<>();
    }
}
