package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.DetailFilter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.DetailRow;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Summary;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_3_TO_6_MONTHS;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_6_TO_12_MONTHS;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_ALL;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_OVER_1_YEAR;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_WITHIN_1_MONTH;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_WITHIN_2_MONTHS;
import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_WITHIN_3_MONTHS;

/** 应收账龄事件净额查询。 */
@Repository
public class ErpReceivableAgingReportRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        Long count = jdbc().queryForObject(sql.cte + " SELECT COUNT(*) FROM report WHERE 1=1" + sql.displayWhere,
                sql.params, Long.class);
        return count == null ? 0L : count;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = reportSql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(sql.cte + " SELECT report.* FROM report WHERE 1=1" + sql.displayWhere
                        + orderBy(filter) + " LIMIT :limit OFFSET :offset", sql.params,
                new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        String query = sql.cte + ",matched AS (SELECT * FROM report WHERE 1=1" + sql.displayWhere + ")"
                + " SELECT COUNT(*) row_count,COALESCE(SUM(balance),0) balance,SUM(credit_limit) credit_limit,"
                + "SUM(credit_balance) credit_balance,COALESCE(SUM(within1_month),0) within1_month,"
                + "COALESCE(SUM(within2_months),0) within2_months,COALESCE(SUM(within3_months),0) within3_months,"
                + "COALESCE(SUM(months3_to6),0) months3_to6,COALESCE(SUM(months6_to12),0) months6_to12,"
                + "COALESCE(SUM(over1_year),0) over1_year FROM matched";
        return jdbc().queryForObject(query, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    public long detailCount(DetailFilter filter, Scope scope) {
        Sql sql = detailSql(filter, scope);
        Long count = jdbc().queryForObject(sql.cte + " SELECT COUNT(*) FROM detail WHERE 1=1" + sql.displayWhere,
                sql.params, Long.class);
        return count == null ? 0L : count;
    }

    public List<DetailRow> detailPage(DetailFilter filter, Scope scope, long offset, int limit) {
        Sql sql = detailSql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(sql.cte + " SELECT detail.* FROM detail WHERE 1=1" + sql.displayWhere
                        + detailOrderBy(filter) + " LIMIT :limit OFFSET :offset", sql.params,
                new BeanPropertyRowMapper<>(DetailRow.class));
    }

    Sql reportSql(Filter filter, Scope scope) {
        Sql sql = baseSql(filter.getAnalysisDate(), scope);
        String customerScope = customerScope(scope.customerScope, "c", "customer", sql.params);
        sql.cte += ",aged_events AS (SELECT raw_events.*,DATEDIFF(:analysisDate,DATE(event_date)) age_days "
                + "FROM raw_events WHERE event_date<DATE_ADD(:analysisDate,INTERVAL 1 DAY)),"
                + "event_sums AS (SELECT customer_id,dept_id,ROUND(SUM(amount),6) balance,"
                + bucket("within1_month", "age_days BETWEEN 0 AND 30") + ","
                + bucket("within2_months", "age_days BETWEEN 31 AND 60") + ","
                + bucket("within3_months", "age_days BETWEEN 61 AND 90") + ","
                + bucket("months3_to6", "age_days BETWEEN 91 AND 180") + ","
                + bucket("months6_to12", "age_days BETWEEN 181 AND 365") + ","
                + bucket("over1_year", "age_days>=366")
                + " FROM aged_events GROUP BY customer_id,dept_id),"
                + "report AS (SELECT CONCAT(c.id,':',IFNULL(CAST(e.dept_id AS CHAR),'none')) row_key,"
                + "c.id customer_id,e.dept_id,c.customer_type customer_type_id,c.area_id,c.sale_user_id,"
                + "c.developer_user_id,c.name customer_name,ct.name customer_type_name,e.balance,"
                + "COALESCE(cdc.credit_limit,c.credit_limit) credit_limit,"
                + "CASE WHEN COALESCE(cdc.credit_limit,c.credit_limit) IS NULL THEN NULL "
                + "ELSE ROUND(COALESCE(cdc.credit_limit,c.credit_limit)-e.balance,6) END credit_balance,"
                + "e.within1_month,e.within2_months,e.within3_months,e.months3_to6,e.months6_to12,e.over1_year,"
                + "area.name area_name,su.nickname sale_user_name,COALESCE(d.name,'未归属部门') dept_name,"
                + "du.nickname developer_name FROM event_sums e INNER JOIN erp_customer c ON c.id=e.customer_id "
                + "AND c.tenant_id=:tenant AND c.deleted=b'0' "
                + "LEFT JOIN erp_customer_dept_credit cdc ON cdc.customer_id=c.id AND cdc.tenant_id=c.tenant_id "
                + "AND cdc.deleted=b'0' AND cdc.dept_id=e.dept_id "
                + "LEFT JOIN erp_base_data ct ON ct.tenant_id=c.tenant_id AND ct.deleted=b'0' "
                + "AND ct.type='customer_type' AND ct.code=" + customerTypeCode("c.customer_type") + " "
                + "LEFT JOIN erp_base_data area ON area.id=c.area_id AND area.tenant_id=c.tenant_id "
                + "AND area.deleted=b'0' AND area.type='sales_area' "
                + "LEFT JOIN system_users su ON su.id=c.sale_user_id AND su.tenant_id=c.tenant_id AND su.deleted=b'0' "
                + "LEFT JOIN system_users du ON du.id=c.developer_user_id AND du.tenant_id=c.tenant_id AND du.deleted=b'0' "
                + "LEFT JOIN system_dept d ON d.id=e.dept_id AND d.tenant_id=c.tenant_id AND d.deleted=b'0' "
                + "WHERE ROUND(e.balance,6)<>0" + customerScope + ")";
        applyFilters(sql, filter);
        return sql;
    }

    private Sql detailSql(DetailFilter filter, Scope scope) {
        Sql sql = baseSql(filter.getAnalysisDate(), scope);
        String customerScope = customerScope(scope.customerScope, "c", "detailCustomer", sql.params);
        sql.cte += ",detail AS (SELECT e.row_key,e.document_type_code,e.business_id,e.customer_id,e.dept_id,"
                + "e.business_user_id,e.event_date business_date,e.business_type,e.document_no,c.name customer_name,"
                + "COALESCE(d.name,'未归属部门') dept_name,u.nickname business_user_name,e.amount,e.description,"
                + "DATEDIFF(:analysisDate,DATE(e.event_date)) age_days FROM raw_events e "
                + "INNER JOIN erp_customer c ON c.id=e.customer_id AND c.tenant_id=:tenant AND c.deleted=b'0' "
                + "LEFT JOIN system_dept d ON d.id=e.dept_id AND d.tenant_id=:tenant AND d.deleted=b'0' "
                + "LEFT JOIN system_users u ON u.id=e.business_user_id AND u.tenant_id=:tenant AND u.deleted=b'0' "
                + "WHERE e.event_date<DATE_ADD(:analysisDate,INTERVAL 1 DAY)" + customerScope + ")";
        sql.displayWhere += " AND detail.customer_id=:detailCustomerId";
        sql.params.put("detailCustomerId", filter.getCustomerId());
        if (Boolean.TRUE.equals(filter.getDeptUnassigned())) {
            sql.displayWhere += " AND detail.dept_id IS NULL";
        } else {
            sql.displayWhere += " AND detail.dept_id=:detailDeptId";
            sql.params.put("detailDeptId", filter.getDeptId());
        }
        sql.displayWhere += bucketCondition(filter.getBucket());
        return sql;
    }

    private Sql baseSql(java.time.LocalDate analysisDate, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        sql.params.put("analysisDate", analysisDate);
        String rawEvents = String.join(" UNION ALL ",
                saleOut(documentScope(scope.accountScope, "t.dept_id", "t.sale_user_id", "out", sql.params)),
                saleReturn(documentScope(scope.accountScope, "t.dept_id", "t.sale_user_id", "return", sql.params)),
                saleAdjust(documentScope(scope.accountScope, "t.dept_id", "t.adjust_user_id", "adjust", sql.params)),
                receipt(documentScope(scope.accountScope, "t.dept_id", "t.finance_user_id", "receipt", sql.params)),
                receivableAdjust(documentScope(scope.accountScope, "t.dept_id", "t.handler_id", "other", sql.params)));
        sql.cte = "WITH raw_events AS (" + rawEvents + ")";
        return sql;
    }

    private String saleOut(String scope) {
        return eventSelect("t", "SALE_OUT", "销售出库", 10, "COALESCE(t.out_time,t.create_time)",
                ErpSaleOutMapper.RECEIVABLE_ACCOUNT_SALE_AMOUNT_EXPRESSION, "t.sale_user_id", "t.remark")
                + " FROM erp_sale_out t WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope;
    }

    private String saleReturn(String scope) {
        return eventSelect("t", "SALE_RETURN", "销售退货", 20, "COALESCE(t.return_time,t.create_time)",
                "-ABS(COALESCE(t.total_price,0))", "t.sale_user_id", "t.remark")
                + " FROM erp_sale_return t WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope;
    }

    private String saleAdjust(String scope) {
        return eventSelect("t", "SALE_ADJUST", "销售调价", 30, "COALESCE(t.adjust_date,t.create_time)",
                "COALESCE(t.total_adjust_price,0)", "t.adjust_user_id", "t.remark")
                + " FROM erp_sale_price_adjust t WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope;
    }

    private String receipt(String scope) {
        String amount = "-(CASE WHEN EXISTS (SELECT 1 FROM erp_receivable_other dro WHERE dro.tenant_id=t.tenant_id "
                + "AND dro.deleted=b'0' AND dro.status=20 AND dro.source_type='收款单优惠' AND dro.source_id=t.id) "
                + "THEN COALESCE(t.receipt_price,0) ELSE COALESCE(t.total_price,0) END)";
        return eventSelect("t", "FINANCE_RECEIPT", "收款单", 40, "COALESCE(t.receipt_time,t.create_time)",
                amount, "t.finance_user_id", "t.remark")
                + " FROM erp_finance_receipt t WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope;
    }

    private String receivableAdjust(String scope) {
        return eventSelect("t", "RECEIVABLE_ADJUST", "应收调账", 50,
                "COALESCE(TIMESTAMP(t.biz_time),t.create_time)", "COALESCE(t.receivable_amount,0)",
                "t.handler_id", "t.remark")
                + " FROM erp_receivable_other t WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope;
    }

    private String eventSelect(String alias, String code, String name, int eventOrder, String eventDate,
                               String amount, String businessUserId, String description) {
        return "SELECT " + collate("CONCAT('" + code + ":'," + alias + ".id)") + " row_key,"
                + collate("'" + code + "'") + " document_type_code," + alias + ".id business_id,"
                + alias + ".customer_id," + alias + ".dept_id," + businessUserId + " business_user_id,"
                + eventDate + " event_date," + collate("'" + name + "'") + " business_type," + eventOrder
                + " event_order," + collate(alias + ".no") + " document_no,ROUND(" + amount + ",6) amount,"
                + collate(description) + " description";
    }

    private String collate(String expression) {
        return "CONVERT(" + expression + " USING utf8mb4) COLLATE utf8mb4_unicode_ci";
    }

    private String bucket(String alias, String condition) {
        return "ROUND(SUM(CASE WHEN " + condition + " THEN amount ELSE 0 END),6) " + alias;
    }

    private String customerTypeCode(String column) {
        return "CASE " + column + " WHEN 1 THEN 'car_owner' WHEN 2 THEN 'repair_factory' "
                + "WHEN 3 THEN 'terminal_customer' WHEN 4 THEN 'customer_supplier' WHEN 5 THEN 'supplier' "
                + "WHEN 6 THEN 'store' WHEN 7 THEN 'dealer' END";
    }

    private void applyFilters(Sql sql, Filter filter) {
        equal(sql, "report.customer_id", "customerId", filter.getCustomerId());
        equal(sql, "report.customer_type_id", "customerType", filter.getCustomerType());
        equal(sql, "report.area_id", "areaId", filter.getAreaId());
        equal(sql, "report.sale_user_id", "saleUserId", filter.getSaleUserId());
        equal(sql, "report.dept_id", "deptId", filter.getDeptId());
        equal(sql, "report.developer_user_id", "developerUserId", filter.getDeveloperUserId());
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("customerName", "report.customer_name");
        fields.put("customerTypeName", "report.customer_type_name");
        fields.put("balance", "report.balance");
        fields.put("creditLimit", "report.credit_limit");
        fields.put("creditBalance", "report.credit_balance");
        fields.put("within1Month", "report.within1_month");
        fields.put("within2Months", "report.within2_months");
        fields.put("within3Months", "report.within3_months");
        fields.put("months3To6", "report.months3_to6");
        fields.put("months6To12", "report.months6_to12");
        fields.put("over1Year", "report.over1_year");
        fields.put("areaName", "report.area_name");
        fields.put("saleUserName", "report.sale_user_name");
        fields.put("deptName", "report.dept_name");
        fields.put("developerName", "report.developer_name");
        String requested = fields.get(filter.getOrderField());
        if (requested == null) return " ORDER BY report.balance DESC,report.customer_id DESC,IFNULL(report.dept_id,-1) DESC";
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + requested + direction + ",report.customer_id DESC,IFNULL(report.dept_id,-1) DESC";
    }

    private String detailOrderBy(DetailFilter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("businessDate", "detail.business_date");
        fields.put("businessType", "detail.business_type");
        fields.put("documentNo", "detail.document_no");
        fields.put("amount", "detail.amount");
        fields.put("deptName", "detail.dept_name");
        fields.put("businessUserName", "detail.business_user_name");
        String requested = fields.get(filter.getOrderField());
        if (requested == null) return " ORDER BY detail.business_date DESC,detail.document_type_code DESC,detail.business_id DESC";
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + requested + direction + ",detail.business_date DESC,detail.business_id DESC";
    }

    private String bucketCondition(String bucket) {
        if (bucket == null || BUCKET_ALL.equals(bucket)) return "";
        if (BUCKET_WITHIN_1_MONTH.equals(bucket)) return " AND detail.age_days BETWEEN 0 AND 30";
        if (BUCKET_WITHIN_2_MONTHS.equals(bucket)) return " AND detail.age_days BETWEEN 31 AND 60";
        if (BUCKET_WITHIN_3_MONTHS.equals(bucket)) return " AND detail.age_days BETWEEN 61 AND 90";
        if (BUCKET_3_TO_6_MONTHS.equals(bucket)) return " AND detail.age_days BETWEEN 91 AND 180";
        if (BUCKET_6_TO_12_MONTHS.equals(bucket)) return " AND detail.age_days BETWEEN 181 AND 365";
        if (BUCKET_OVER_1_YEAR.equals(bucket)) return " AND detail.age_days>=366";
        return " AND 1=0";
    }

    private String documentScope(ErpFinanceVisibleScope scope, String deptColumn, String userColumn,
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
            condition.append(userColumn).append("=:").append(prefix).append("Self");
            params.put(prefix + "Self", scope.getSelfUserId());
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private String customerScope(ErpFinanceVisibleScope scope, String alias, String prefix, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(alias).append(".dept_id IN (:").append(prefix).append("DeptIds) OR (")
                    .append(alias).append(".allow_multi_dept=b'1' AND EXISTS (SELECT 1 FROM erp_customer_dept cd ")
                    .append("WHERE cd.tenant_id=").append(alias).append(".tenant_id AND cd.customer_id=")
                    .append(alias).append(".id AND cd.deleted=b'0' AND cd.dept_id IN (:").append(prefix)
                    .append("DeptIds)))");
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

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null) return;
        sql.displayWhere += " AND " + column + "=:" + name;
        sql.params.put(name, value);
    }

    private NamedParameterJdbcTemplate jdbc() {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public static class Scope {
        private final long tenantId;
        private final ErpFinanceVisibleScope accountScope;
        private final ErpFinanceVisibleScope customerScope;

        public Scope(long tenantId, ErpFinanceVisibleScope accountScope, ErpFinanceVisibleScope customerScope) {
            this.tenantId = tenantId;
            this.accountScope = accountScope;
            this.customerScope = customerScope;
        }
    }

    static class Sql {
        String cte;
        String displayWhere = "";
        final Map<String, Object> params = new HashMap<>();
    }
}
