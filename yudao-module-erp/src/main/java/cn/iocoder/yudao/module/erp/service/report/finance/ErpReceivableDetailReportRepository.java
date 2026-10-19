package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Summary;
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

@Repository
public class ErpReceivableDetailReportRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        Long value = jdbc().queryForObject(sql.cte + " SELECT COUNT(*) FROM report WHERE 1=1" + sql.displayWhere,
                sql.params, Long.class);
        return value == null ? 0L : value;
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
        String query = sql.cte
                + ", matched AS (SELECT * FROM report WHERE 1=1" + sql.displayWhere + ")"
                + ", matched_customers AS (SELECT DISTINCT customer_id FROM matched)"
                + ", ending_ranked AS (SELECT report.customer_id,report.balance,ROW_NUMBER() OVER("
                + "PARTITION BY report.customer_id ORDER BY report.business_date DESC,report.event_order DESC,"
                + "report.document_no DESC,report.business_id DESC) rn FROM report INNER JOIN matched_customers mc "
                + "ON mc.customer_id=report.customer_id"
                + (filter.getBusinessDateTo() == null ? "" : " WHERE report.business_date<=:businessTo") + ")"
                + " SELECT COUNT(*) row_count,COALESCE(SUM(increase_amount),0) increase_amount,"
                + "COALESCE(SUM(receipt_amount),0) receipt_amount,COALESCE(SUM(write_off_amount),0) write_off_amount,"
                + "COALESCE((SELECT SUM(balance) FROM ending_ranked WHERE rn=1),0) ending_balance FROM matched";
        return jdbc().queryForObject(query, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private Sql reportSql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        String accountOut = documentScope(scope.accountScope, "t.dept_id", "t.sale_user_id", "out", sql.params);
        String accountReturn = documentScope(scope.accountScope, "t.dept_id", "t.sale_user_id", "ret", sql.params);
        String accountAdjust = documentScope(scope.accountScope, "t.dept_id", "t.adjust_user_id", "adj", sql.params);
        String accountReceipt = documentScope(scope.accountScope, "t.dept_id", "t.finance_user_id", "receipt", sql.params);
        String accountOther = documentScope(scope.accountScope, "t.dept_id", "t.handler_id", "other", sql.params);
        String progressReceipt = documentScope(scope.accountScope, "fr.dept_id", "fr.finance_user_id",
                "progressReceipt", sql.params);
        String progressWriteoff = documentScope(scope.accountScope, "wo.dept_id", "wo.operator_user_id",
                "progressWriteoff", sql.params);
        String customerScope = customerScope(scope.customerScope, "c", "customer", sql.params);
        String requestedDept = filter.getDeptId() == null ? "" : " AND t.dept_id=:ledgerDept";
        if (filter.getDeptId() != null) sql.params.put("ledgerDept", filter.getDeptId());

        String rawEvents = String.join(" UNION ALL ",
                saleOut(accountOut, customerScope, requestedDept, progressReceipt, progressWriteoff),
                saleReturn(accountReturn, customerScope, requestedDept, progressReceipt, progressWriteoff),
                saleAdjust(accountAdjust, customerScope, requestedDept, progressReceipt, progressWriteoff),
                receipt(accountReceipt, customerScope, requestedDept),
                receivableAdjust(accountOther, customerScope, requestedDept));
        sql.cte = "WITH raw_events AS (" + rawEvents + "), ledger AS (SELECT raw_events.*,(increase_amount-receipt_amount) change_amount FROM raw_events),"
                + "report AS (SELECT ledger.*,"
                + "COALESCE(SUM(change_amount) OVER(PARTITION BY customer_id ORDER BY business_date,event_order,document_no,business_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING),0) previous_balance,"
                + "COALESCE(SUM(change_amount) OVER(PARTITION BY customer_id ORDER BY business_date,event_order,document_no,business_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW),0) balance "
                + "FROM ledger)";
        applyDisplayFilters(sql, filter);
        return sql;
    }

    private String saleOut(String scope, String customerScope, String dept,
                           String progressReceipt, String progressWriteoff) {
        String settled = settledAmount("t", 21, progressReceipt, progressWriteoff);
        String amount = ErpSaleOutMapper.RECEIVABLE_ACCOUNT_SALE_AMOUNT_EXPRESSION;
        String sourceId = "CASE WHEN t.order_no IS NOT NULL THEN CASE WHEN t.order_id>0 THEN t.order_id END "
                + "WHEN t.source_id>0 AND t.source_type IN (10,20,30,40,50,70) THEN t.source_id END";
        String sourceType = "CASE WHEN t.order_no IS NOT NULL AND t.order_id>0 THEN 'SALE_ORDER' "
                + "WHEN t.order_no IS NULL AND t.source_id>0 THEN CASE t.source_type "
                + "WHEN 10 THEN 'SALE_ORDER' WHEN 20 THEN 'SALE_QUOTE' WHEN 30 THEN 'SALE_CART' "
                + "WHEN 40 THEN 'SALE_PRICE_ADJUST' WHEN 50 THEN 'PURCHASE_IN' WHEN 70 THEN 'SALE_RETURN' END END";
        return selectPrefix("t", "SALE_OUT", "销售出库", 10, "t.out_time", "t.approve_time", amount,
                "0", settled, "COALESCE(t.order_no,t.source_no)", sourceId, sourceType,
                "t.settle_method", "t.delivery_method", "t.sale_user_id", "t.remark",
                voucher(2), settled, amount)
                + " FROM erp_sale_out t INNER JOIN erp_customer c ON c.id=t.customer_id AND c.tenant_id=t.tenant_id AND c.deleted=b'0'"
                + users("t") + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + customerScope + dept;
    }

    private String saleReturn(String scope, String customerScope, String dept,
                              String progressReceipt, String progressWriteoff) {
        String amount = "-ABS(COALESCE(t.total_price,0))";
        String settled = settledAmount("t", 22, progressReceipt, progressWriteoff);
        return selectPrefix("t", "SALE_RETURN", "销售退货", 20, "t.return_time", "t.approve_time", amount,
                "0", settled, "t.source_out_no", "CASE WHEN t.source_out_id>0 THEN t.source_out_id END",
                "CASE WHEN t.source_out_id>0 THEN 'SALE_OUT' END",
                "t.settle_method", "t.delivery_method", "t.sale_user_id", "t.remark",
                voucher(3), settled, amount)
                + " FROM erp_sale_return t INNER JOIN erp_customer c ON c.id=t.customer_id AND c.tenant_id=t.tenant_id AND c.deleted=b'0'"
                + users("t") + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + customerScope + dept;
    }

    private String saleAdjust(String scope, String customerScope, String dept,
                              String progressReceipt, String progressWriteoff) {
        String amount = "COALESCE(t.total_adjust_price,0)";
        String settled = settledAmount("t", 23, progressReceipt, progressWriteoff);
        return selectPrefix("t", "SALE_ADJUST", "销售调价", 30, "t.adjust_date", "t.approve_time", amount,
                "0", settled, "t.original_sale_out_no", "CASE WHEN t.original_sale_out_id>0 THEN t.original_sale_out_id END",
                "CASE WHEN t.original_sale_out_id>0 THEN 'SALE_OUT' END",
                "t.settle_method", "t.delivery_method", "t.adjust_user_id", "t.remark",
                "NULL", settled, amount)
                + " FROM erp_sale_price_adjust t INNER JOIN erp_customer c ON c.id=t.customer_id AND c.tenant_id=t.tenant_id AND c.deleted=b'0'"
                + users("t") + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + customerScope + dept;
    }

    private String receipt(String scope, String customerScope, String dept) {
        String received = "CASE WHEN EXISTS (SELECT 1 FROM erp_receivable_other dro WHERE dro.tenant_id=t.tenant_id "
                + "AND dro.deleted=b'0' AND dro.status=20 AND dro.source_type='收款单优惠' AND dro.source_id=t.id) "
                + "THEN COALESCE(t.receipt_price,0) ELSE COALESCE(t.total_price,0) END";
        String source = "(SELECT GROUP_CONCAT(DISTINCT fri.biz_no ORDER BY fri.biz_no SEPARATOR '、') FROM erp_finance_receipt_item fri "
                + "WHERE fri.tenant_id=t.tenant_id AND fri.deleted=b'0' AND fri.write_off_status=1 AND fri.receipt_id=t.id)";
        String sourceId = receiptUniqueSource("MIN(fri.biz_id)");
        String sourceType = receiptUniqueSource("CASE MIN(fri.biz_type) WHEN 21 THEN 'SALE_OUT' "
                + "WHEN 22 THEN 'SALE_RETURN' WHEN 23 THEN 'SALE_PRICE_ADJUST' "
                + "WHEN 24 THEN 'RECEIVABLE_MISC' END");
        return selectPrefix("t", "FINANCE_RECEIPT", "收款单", 40, "t.receipt_time", "t.approve_time", "0",
                received, "0", source, sourceId, sourceType, "NULL", "NULL", "t.finance_user_id", "t.remark",
                voucher(6), "0", "0")
                + " FROM erp_finance_receipt t INNER JOIN erp_customer c ON c.id=t.customer_id AND c.tenant_id=t.tenant_id AND c.deleted=b'0'"
                + users("t") + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + customerScope + dept;
    }

    private String receivableAdjust(String scope, String customerScope, String dept) {
        String amount = "COALESCE(t.receivable_amount,0)";
        String settled = "COALESCE(t.settled_amount,0)";
        return selectPrefix("t", "RECEIVABLE_ADJUST", "应收调账", 50,
                "COALESCE(TIMESTAMP(t.biz_time),t.create_time)", "t.approve_time", amount, "0", settled, "t.source_no",
                "CASE WHEN t.source_id>0 AND t.source_type IN ('销售手推车','收款单优惠') THEN t.source_id END",
                "CASE t.source_type WHEN '销售手推车' THEN 'SALE_CART' WHEN '收款单优惠' THEN 'FINANCE_RECEIPT' END",
                "NULL", "NULL", "t.handler_id", "t.remark", "t.voucher_no", settled, amount)
                + " FROM erp_receivable_other t INNER JOIN erp_customer c ON c.id=t.customer_id AND c.tenant_id=t.tenant_id AND c.deleted=b'0'"
                + users("t") + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + customerScope + dept;
    }

    private String selectPrefix(String alias, String code, String name, int eventOrder, String businessDate,
                                String approveTime, String increase, String receipt, String writeoff, String sourceNo,
                                String sourceBusinessId, String sourceDocumentTypeCode,
                                String settleMethod, String deliveryMethod, String businessUserId, String description,
                                String voucherNo, String settledAmount, String businessAmount) {
        String status = "CASE WHEN ABS(" + businessAmount + ")>0 THEN CASE WHEN ABS(" + settledAmount
                + ")>=ROUND(ABS(" + businessAmount + "),2) THEN '是' ELSE '否' END ELSE '-' END";
        return "SELECT " + collate("CONCAT('" + code + ":',t.id)") + " row_key,t.id business_id,t.customer_id,t.dept_id,"
                + collate("'" + code + "'") + " document_type_code," + collate("'" + name + "'")
                + " document_type," + eventOrder + " event_order," + businessDate + " business_date,"
                + collate("c.name") + " customer_name," + collate("c.code") + " customer_code,"
                + collate(settleMethod) + " settle_method,CAST(NULLIF(t.updater,'') AS UNSIGNED) updater_id,"
                + "CAST(NULLIF(t.creator,'') AS UNSIGNED) creator_id," + businessUserId + " business_user_id,"
                + collate("uu.nickname") + " updater_name," + collate("cu.nickname") + " creator_name,"
                + collate("(SELECT su.nickname FROM system_users su WHERE su.id=" + businessUserId
                + " AND su.tenant_id=t.tenant_id AND su.deleted=b'0')") + " business_user_name,"
                + collate(deliveryMethod) + " delivery_method," + collate("t.no") + " document_no,ROUND(" + increase
                + ",2) increase_amount,ROUND(" + receipt
                + ",2) receipt_amount,ROUND(" + writeoff + ",2) write_off_amount,ROUND(" + increase
                + ",2) unconfirmed_receivable,0 received_amount," + approveTime + " approve_time," + collate(voucherNo)
                + " voucher_no," + collate(sourceNo) + " source_no," + sourceBusinessId + " source_business_id,"
                + collate(sourceDocumentTypeCode) + " source_document_type_code," + collate(description) + " description,"
                + collate(status) + " settled_status";
    }

    private String receiptUniqueSource(String valueExpression) {
        return "(SELECT CASE WHEN COUNT(*)>0 AND COUNT(*)=SUM(CASE WHEN fri.biz_id>0 "
                + "AND fri.biz_type IN (21,22,23,24) AND NULLIF(TRIM(fri.biz_no),'') IS NOT NULL THEN 1 ELSE 0 END) "
                + "AND COUNT(DISTINCT CONCAT(fri.biz_type,':',fri.biz_id))=1 "
                + "AND COUNT(DISTINCT NULLIF(TRIM(fri.biz_no),''))=1 "
                + "AND MIN(fri.biz_type) IN (21,22,23,24) THEN " + valueExpression + " END "
                + "FROM erp_finance_receipt_item fri WHERE fri.tenant_id=t.tenant_id AND fri.deleted=b'0' "
                + "AND fri.write_off_status=1 AND fri.receipt_id=t.id)";
    }

    private String collate(String expression) {
        return "CONVERT(" + expression + " USING utf8mb4) COLLATE utf8mb4_unicode_ci";
    }

    private String users(String alias) {
        return " LEFT JOIN system_users cu ON cu.id=CAST(NULLIF(" + alias + ".creator,'') AS UNSIGNED) AND cu.tenant_id=" + alias + ".tenant_id AND cu.deleted=b'0'"
                + " LEFT JOIN system_users uu ON uu.id=CAST(NULLIF(" + alias + ".updater,'') AS UNSIGNED) AND uu.tenant_id=" + alias + ".tenant_id AND uu.deleted=b'0'";
    }

    private String settledAmount(String alias, int bizType, String receiptScope, String writeoffScope) {
        return "(COALESCE((SELECT SUM(fri.receipt_price) FROM erp_finance_receipt_item fri INNER JOIN erp_finance_receipt fr "
                + "ON fr.id=fri.receipt_id AND fr.tenant_id=fri.tenant_id AND fr.deleted=b'0' AND fr.status=20 WHERE fri.tenant_id="
                + alias + ".tenant_id AND fri.deleted=b'0' AND fri.write_off_status=1 AND fri.biz_type=" + bizType
                + " AND fri.biz_id=" + alias + ".id" + receiptScope
                + "),0)+COALESCE((SELECT SUM(wo.write_off_amount) FROM erp_receivable_writeoff wo "
                + "WHERE wo.tenant_id=" + alias + ".tenant_id AND wo.deleted=b'0' AND wo.biz_type=" + bizType
                + " AND wo.biz_id=" + alias + ".id" + writeoffScope + "),0))";
    }

    private String voucher(int sourceType) {
        return "(SELECT GROUP_CONCAT(DISTINCT v.voucher_no ORDER BY v.id SEPARATOR '、') FROM erp_voucher v WHERE "
                + "v.tenant_id=t.tenant_id AND v.deleted=b'0' AND v.source_biz_type=" + sourceType + " AND v.source_biz_id=t.id)";
    }

    private void applyDisplayFilters(Sql sql, Filter filter) {
        between(sql, "report.business_date", "business", filter.getBusinessDateFrom(), filter.getBusinessDateTo());
        between(sql, "report.approve_time", "approve", filter.getApproveDateFrom(), filter.getApproveDateTo());
        if (filter.getDocumentTypes() != null && !filter.getDocumentTypes().isEmpty()) {
            sql.displayWhere += " AND report.document_type_code IN (:documentTypes)";
            sql.params.put("documentTypes", filter.getDocumentTypes());
        }
        equal(sql, "report.customer_id", "customerId", filter.getCustomerId());
        equal(sql, "report.customer_code", "customerCode", filter.getCustomerCode());
        equal(sql, "report.settle_method", "settleMethod", filter.getSettleMethod());
        equal(sql, "report.delivery_method", "deliveryMethod", filter.getDeliveryMethod());
        equal(sql, "report.settled_status", "settledStatus", filter.getSettledStatus());
        like(sql, "report.document_no", "documentNo", filter.getDocumentNo());
        like(sql, "report.source_no", "sourceNo", filter.getSourceNo());
        equal(sql, "report.creator_id", "creatorId", filter.getCreatorId());
        equal(sql, "report.updater_id", "updaterId", filter.getUpdaterId());
        equal(sql, "report.business_user_id", "businessUserId", filter.getBusinessUserId());
        like(sql, "report.voucher_no", "voucherNo", filter.getVoucherNo());
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("businessDate", "report.business_date"); fields.put("customerName", "report.customer_name");
        fields.put("documentType", "report.document_type"); fields.put("documentNo", "report.document_no");
        fields.put("increaseAmount", "report.increase_amount"); fields.put("receiptAmount", "report.receipt_amount");
        fields.put("balance", "report.balance"); fields.put("approveTime", "report.approve_time");
        String field = fields.getOrDefault(filter.getOrderField(), "report.business_date");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",report.event_order" + direction + ",report.document_no" + direction
                + ",report.business_id" + direction;
    }

    private String documentScope(ErpFinanceVisibleScope scope, String deptColumn, String userColumn,
                                 String prefix, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(deptColumn).append(" IN (:").append(prefix).append("DeptIds)");
            params.put(prefix + "DeptIds", scope.getDeptIds()); appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append(userColumn).append("=:").append(prefix).append("Self");
            params.put(prefix + "Self", scope.getSelfUserId()); appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private String customerScope(ErpFinanceVisibleScope scope, String alias, String prefix, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append(alias).append(".dept_id IN (:").append(prefix).append("DeptIds) OR (")
                    .append(alias).append(".allow_multi_dept=b'1' AND EXISTS (SELECT 1 FROM erp_customer_dept cd WHERE cd.tenant_id=")
                    .append(alias).append(".tenant_id AND cd.customer_id=").append(alias).append(".id AND cd.deleted=b'0' AND cd.dept_id IN (:")
                    .append(prefix).append("DeptIds)))");
            params.put(prefix + "DeptIds", scope.getDeptIds()); appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append(alias).append(".creator=:").append(prefix).append("Self");
            params.put(prefix + "Self", String.valueOf(scope.getSelfUserId())); appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private void between(Sql sql, String column, String prefix, Object from, Object to) {
        if (from != null) { sql.displayWhere += " AND " + column + ">=:" + prefix + "From"; sql.params.put(prefix + "From", from); }
        if (to != null) { sql.displayWhere += " AND " + column + "<=:" + prefix + "To"; sql.params.put(prefix + "To", to); }
    }
    private void like(Sql sql, String column, String name, String value) {
        if (!hasText(value)) return; sql.displayWhere += " AND " + column + " LIKE :" + name; sql.params.put(name, "%" + value.trim() + "%");
    }
    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null || value instanceof String && !hasText((String) value)) return;
        sql.displayWhere += " AND " + column + "=:" + name; sql.params.put(name, value);
    }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private NamedParameterJdbcTemplate jdbc() { return new NamedParameterJdbcTemplate(jdbcTemplate); }

    public static class Scope {
        private final long tenantId;
        private final ErpFinanceVisibleScope accountScope;
        private final ErpFinanceVisibleScope customerScope;
        public Scope(long tenantId, ErpFinanceVisibleScope accountScope, ErpFinanceVisibleScope customerScope) {
            this.tenantId = tenantId; this.accountScope = accountScope; this.customerScope = customerScope;
        }
    }
    private static class Sql {
        private String cte;
        private String displayWhere = "";
        private final Map<String, Object> params = new HashMap<>();
    }
}
