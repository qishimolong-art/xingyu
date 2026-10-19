package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceiptDetailReportModels.Summary;
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
public class ErpReceiptDetailReportRepository {

    @Resource private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        Long count = jdbc().queryForObject(sql.cte + " SELECT COUNT(*) FROM report WHERE 1=1" + sql.where,
                sql.params, Long.class);
        return count == null ? 0L : count;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = reportSql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(sql.cte + " SELECT report.* FROM report WHERE 1=1" + sql.where
                        + orderBy(filter) + " LIMIT :limit OFFSET :offset", sql.params,
                new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        String query = sql.cte + " SELECT COUNT(*) row_count,"
                + "COALESCE(SUM(settlement_amount),0) settlement_amount,"
                + "COALESCE(SUM(actual_receipt_amount),0) actual_receipt_amount "
                + "FROM report WHERE 1=1" + sql.where;
        return jdbc().queryForObject(query, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private Sql reportSql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        String receiptScope = documentScope(scope.receiptScope, sql.params);
        String customerScope = customerScope(scope.customerScope, sql.params);
        String commonWhere = " WHERE r.tenant_id=:tenant AND r.deleted=b'0' AND r.status=20"
                + " AND i.tenant_id=r.tenant_id AND i.deleted=b'0' AND i.write_off_status=1"
                + receiptScope + customerScope;
        String raw = String.join(" UNION ALL ",
                source("erp_sale_out", 21, "SALE_OUT", "销售单", "s.out_time", "s.sale_user_id",
                        "s.settle_method", commonWhere),
                source("erp_sale_return", 22, "SALE_RETURN", "销售退货单", "s.return_time", "s.sale_user_id",
                        "s.settle_method", commonWhere),
                source("erp_sale_price_adjust", 23, "SALE_PRICE_ADJUST", "销售调价单", "s.adjust_date",
                        "s.adjust_user_id", "s.settle_method", commonWhere),
                source("erp_receivable_misc", 24, "RECEIVABLE_MISC", "其他应收单", "s.biz_time", "s.handler_id",
                        "NULL", commonWhere));
        sql.cte = "WITH raw AS (" + raw + "), allocated AS (SELECT raw.*," +
                "SUM(settlement_amount) OVER(PARTITION BY receipt_id) allocated_total," +
                "MAX(receipt_item_id) OVER(PARTITION BY receipt_id) last_item_id," +
                "CASE WHEN receipt_total=0 THEN NULL ELSE ROUND(settlement_amount*receipt_actual/receipt_total,2) END rounded_actual " +
                "FROM raw), report AS (SELECT allocated.*," +
                "CASE WHEN ROUND(allocated_total,2)=ROUND(receipt_total,2) AND receipt_item_id=last_item_id " +
                "THEN ROUND(receipt_actual-SUM(CASE WHEN receipt_item_id<>last_item_id THEN COALESCE(rounded_actual,0) ELSE 0 END) " +
                "OVER(PARTITION BY receipt_id),2) ELSE rounded_actual END actual_receipt_amount FROM allocated)";
        applyFilters(sql, filter);
        return sql;
    }

    private String source(String table, int bizType, String typeCode, String typeName, String businessDate,
                          String businessUser, String settleMethod, String commonWhere) {
        String sourceSettle = settleMethod == null || "NULL".equals(settleMethod) ? "NULL" : settleMethod;
        return "SELECT " + text("CONCAT('RECEIPT_ITEM:',i.id)") + " row_key,r.id receipt_id,i.id receipt_item_id,"
                + "s.id business_id,r.customer_id,r.account_id,s.dept_id," +
                "CAST(NULLIF(r.creator,'') AS UNSIGNED) receipt_user_id," +
                "COALESCE(" + businessUser + ",CAST(NULLIF(s.creator,'') AS UNSIGNED)) business_user_id," +
                "CAST(NULLIF(s.creator,'') AS UNSIGNED) business_creator_id,c.customer_type customer_type_id," +
                text("'" + typeCode + "'") + " business_type_code," + text("c.name") + " customer_name,"
                + "r.receipt_time receipt_date," + text("r.no") + " receipt_no,"
                + text("COALESCE(NULLIF(i.biz_no,''),s.no)") + " business_no," + text("'" + typeName + "'")
                + " business_type,ROUND(i.receipt_price,2) settlement_amount,"
                + "ROUND(r.total_price,2) receipt_total,ROUND(r.receipt_price,2) receipt_actual,"
                + text("COALESCE(NULLIF(TRIM(" + sourceSettle + "),''),CASE c.settle_method "
                        + "WHEN 1 THEN '现金' WHEN 2 THEN '挂账' WHEN 3 THEN '汇款' WHEN 4 THEN '网上支付' END)")
                + " settle_method," + text("CASE a.account_type WHEN 1 THEN '银行账户' WHEN 2 THEN '现金账户' "
                        + "WHEN 3 THEN '其他账户' END") + " account_type," + text("a.name") + " account_name,"
                + text("r.payment_voucher_no") + " payment_voucher_no,"
                + text("(SELECT GROUP_CONCAT(DISTINCT v.voucher_no ORDER BY v.id SEPARATOR '、') FROM erp_voucher v "
                        + "WHERE v.tenant_id=r.tenant_id AND v.deleted=b'0' AND v.source_biz_type=6 AND v.source_biz_id=r.id)")
                + " voucher_no," + text("ru.nickname") + " receipt_user_name," + text("bu.nickname")
                + " business_user_name,CAST(NULL AS CHAR) developer_name," + text("bc.nickname")
                + " business_creator_name," + businessDate + " business_date,"
                + "CAST(NULL AS DECIMAL(20,2)) cost_amount,CAST(NULL AS DECIMAL(20,2)) gross_profit,"
                + "CAST(NULL AS DECIMAL(20,6)) gross_profit_rate," + text("bd.name") + " customer_type,"
                + text("d.name") + " dept_name," + text("COALESCE(NULLIF(i.remark,''),r.remark)") + " remark "
                + "FROM erp_finance_receipt_item i INNER JOIN erp_finance_receipt r ON r.id=i.receipt_id "
                + "AND r.tenant_id=i.tenant_id INNER JOIN " + table + " s ON s.id=i.biz_id AND s.tenant_id=i.tenant_id "
                + "AND s.deleted=b'0' AND s.status=20 INNER JOIN erp_customer c ON c.id=r.customer_id "
                + "AND c.tenant_id=r.tenant_id AND c.deleted=b'0' LEFT JOIN erp_account a ON a.id=r.account_id "
                + "AND a.tenant_id=r.tenant_id AND a.deleted=b'0' LEFT JOIN erp_base_data bd ON bd.id=c.customer_type "
                + "AND bd.tenant_id=c.tenant_id AND bd.deleted=b'0' AND bd.type='customer_type' "
                + "LEFT JOIN system_dept d ON d.id=s.dept_id AND d.deleted=b'0' "
                + "LEFT JOIN system_users ru ON ru.id=CAST(NULLIF(r.creator,'') AS UNSIGNED) "
                + "AND ru.tenant_id=r.tenant_id AND ru.deleted=b'0' "
                + "LEFT JOIN system_users bu ON bu.id=COALESCE(" + businessUser
                + ",CAST(NULLIF(s.creator,'') AS UNSIGNED)) AND bu.tenant_id=r.tenant_id AND bu.deleted=b'0' "
                + "LEFT JOIN system_users bc ON bc.id=CAST(NULLIF(s.creator,'') AS UNSIGNED) "
                + "AND bc.tenant_id=r.tenant_id AND bc.deleted=b'0'" + commonWhere + " AND i.biz_type=" + bizType;
    }

    private String text(String expression) {
        return "CONVERT(" + expression + " USING utf8mb4) COLLATE utf8mb4_unicode_ci";
    }

    private void applyFilters(Sql sql, Filter filter) {
        between(sql, "report.receipt_date", "receipt", filter.getReceiptDateFrom(), filter.getReceiptDateTo());
        between(sql, "report.business_date", "business", filter.getBusinessDateFrom(), filter.getBusinessDateTo());
        if (filter.getBusinessTypes() != null && !filter.getBusinessTypes().isEmpty()) {
            sql.where += " AND report.business_type_code IN (:businessTypes)";
            sql.params.put("businessTypes", filter.getBusinessTypes());
        }
        equal(sql, "report.customer_id", "customerId", filter.getCustomerId());
        like(sql, "report.receipt_no", "receiptNo", filter.getReceiptNo());
        like(sql, "report.business_no", "businessNo", filter.getBusinessNo());
        equal(sql, "report.settle_method", "settleMethod", filter.getSettleMethod());
        equal(sql, "report.account_id", "accountId", filter.getAccountId());
        equal(sql, "report.receipt_user_id", "receiptUserId", filter.getReceiptUserId());
        equal(sql, "report.business_user_id", "businessUserId", filter.getBusinessUserId());
        equal(sql, "report.business_creator_id", "businessCreatorId", filter.getBusinessCreatorId());
        equal(sql, "report.customer_type_id", "customerTypeId", filter.getCustomerTypeId());
        equal(sql, "report.dept_id", "deptId", filter.getDeptId());
        like(sql, "report.payment_voucher_no", "paymentVoucherNo", filter.getPaymentVoucherNo());
        like(sql, "report.voucher_no", "voucherNo", filter.getVoucherNo());
        if (filter.getAccountType() != null) {
            sql.where += " AND report.account_type=:accountTypeName";
            sql.params.put("accountTypeName", accountTypeName(filter.getAccountType()));
        }
    }

    private String accountTypeName(Integer type) {
        if (Integer.valueOf(1).equals(type)) return "银行账户";
        if (Integer.valueOf(2).equals(type)) return "现金账户";
        if (Integer.valueOf(3).equals(type)) return "其他账户";
        return "";
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("customerName", "report.customer_name");
        fields.put("receiptDate", "report.receipt_date");
        fields.put("receiptNo", "report.receipt_no");
        fields.put("businessNo", "report.business_no");
        fields.put("businessType", "report.business_type");
        fields.put("settlementAmount", "report.settlement_amount");
        fields.put("actualReceiptAmount", "report.actual_receipt_amount");
        fields.put("businessDate", "report.business_date");
        fields.put("customerType", "report.customer_type");
        fields.put("deptName", "report.dept_name");
        String field = fields.getOrDefault(filter.getOrderField(), "report.receipt_date");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",report.receipt_id" + direction
                + ",report.receipt_item_id" + direction;
    }

    private String documentScope(ErpFinanceVisibleScope scope, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append("r.dept_id IN (:receiptDeptIds)");
            params.put("receiptDeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append("r.finance_user_id=:receiptSelf");
            params.put("receiptSelf", scope.getSelfUserId());
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private String customerScope(ErpFinanceVisibleScope scope, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append("c.dept_id IN (:customerDeptIds) OR (c.allow_multi_dept=b'1' AND EXISTS (")
                    .append("SELECT 1 FROM erp_customer_dept cd WHERE cd.tenant_id=c.tenant_id ")
                    .append("AND cd.customer_id=c.id AND cd.deleted=b'0' AND cd.dept_id IN (:customerDeptIds)))");
            params.put("customerDeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append("c.sale_user_id=:customerSelf");
            params.put("customerSelf", scope.getSelfUserId());
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private void between(Sql sql, String column, String prefix, Object from, Object to) {
        if (from != null) {
            sql.where += " AND " + column + ">=:" + prefix + "From";
            sql.params.put(prefix + "From", from);
        }
        if (to != null) {
            sql.where += " AND " + column + "<=:" + prefix + "To";
            sql.params.put(prefix + "To", to);
        }
    }

    private void like(Sql sql, String column, String name, String value) {
        if (!hasText(value)) return;
        sql.where += " AND " + column + " LIKE :" + name;
        sql.params.put(name, "%" + value.trim() + "%");
    }

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null || value instanceof String && !hasText((String) value)) return;
        sql.where += " AND " + column + "=:" + name;
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
        private final ErpFinanceVisibleScope receiptScope;
        private final ErpFinanceVisibleScope customerScope;

        public Scope(long tenantId, ErpFinanceVisibleScope receiptScope, ErpFinanceVisibleScope customerScope) {
            this.tenantId = tenantId;
            this.receiptScope = receiptScope;
            this.customerScope = customerScope;
        }
    }

    private static class Sql {
        private String cte;
        private String where = "";
        private final Map<String, Object> params = new HashMap<>();
    }
}
