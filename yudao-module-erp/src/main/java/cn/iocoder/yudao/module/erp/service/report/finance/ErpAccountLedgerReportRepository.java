package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpAccountLedgerReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 账户流水报表统一 SQL。余额始终先在完整账户历史范围计算，再应用展示筛选。 */
@Repository
public class ErpAccountLedgerReportRepository {

    @Resource private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope, false);
        Long count = jdbc().queryForObject(sql.cte + "SELECT COUNT(*)" + sql.fromWhere,
                sql.params, Long.class);
        return count == null ? 0L : count;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = reportSql(filter, scope, true);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(sql.cte + selectColumns() + sql.fromWhere + orderBy(filter)
                        + " LIMIT :limit OFFSET :offset", sql.params, new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope, false);
        return jdbc().queryForObject(sql.cte + "SELECT COUNT(*) row_count,"
                        + "COALESCE(SUM(q.income_amount),0) income_amount,"
                        + "COALESCE(SUM(q.expense_amount),0) expense_amount,"
                        + "COALESCE(SUM(COALESCE(q.income_amount,0)-COALESCE(q.expense_amount,0)),0) net_amount"
                        + sql.fromWhere, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private Sql reportSql(Filter filter, Scope scope, boolean withBalance) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        String scoped = "scoped_rows AS (SELECT s.*,a.name account_name,"
                + "(SELECT GROUP_CONCAT(DISTINCT v.voucher_no ORDER BY v.id SEPARATOR '、')"
                + " FROM erp_voucher v WHERE v.tenant_id=s.tenant_id AND v.deleted=b'0'"
                + " AND v.source_biz_id=s.source_id AND v.source_biz_type=CASE s.source_type"
                + " WHEN 'receipt' THEN 6 WHEN 'payment' THEN 12 WHEN 'other_income' THEN 13"
                + " WHEN 'expense' THEN 14 WHEN 'transfer' THEN 18 END) voucher_no FROM source_rows s"
                + " INNER JOIN erp_account a ON a.id=s.account_id AND a.tenant_id=s.tenant_id AND a.deleted=b'0'"
                + " WHERE 1=1" + accountScope(scope.visibleScope, sql.params)
                + (filter.getAccountId() == null ? "" : " AND s.account_id=:accountId") + ")";
        if (filter.getAccountId() != null) sql.params.put("accountId", filter.getAccountId());
        String ledger = withBalance
                ? "ledger_rows AS (SELECT s.*,ROUND(SUM(COALESCE(s.income_amount,0)-COALESCE(s.expense_amount,0))"
                    + " OVER(PARTITION BY s.account_id ORDER BY s.business_date ASC,s.create_time ASC,"
                    + "s.source_rank ASC,s.source_id ASC,s.leg_rank ASC ROWS UNBOUNDED PRECEDING),6) balance"
                    + " FROM scoped_rows s)"
                : "ledger_rows AS (SELECT s.*,CAST(NULL AS DECIMAL(24,6)) balance FROM scoped_rows s)";
        sql.cte = "WITH source_rows AS (" + sourceRowsSql() + ")," + scoped + "," + ledger + " ";
        sql.fromWhere = " FROM ledger_rows q"
                + " LEFT JOIN system_users ru ON ru.id=q.receiver_id AND ru.tenant_id=q.tenant_id AND ru.deleted=b'0'"
                + " LEFT JOIN system_users pu ON pu.id=q.payer_id AND pu.tenant_id=q.tenant_id AND pu.deleted=b'0'"
                + " WHERE 1=1";
        applyFilters(sql, filter);
        return sql;
    }

    private String sourceRowsSql() {
        String creator = "CASE WHEN %s.creator IS NOT NULL AND %s.creator REGEXP '^[0-9]+$'"
                + " THEN CAST(%s.creator AS UNSIGNED) END";
        return "SELECT r.tenant_id,'receipt' source_type,10 source_rank,r.id source_id,1 leg_rank,"
                + "CONCAT('receipt:',r.id) row_key,r.account_id,r.receipt_time business_date,r.create_time,"
                + text("r.no") + " business_no," + literal("收款单") + " document_type,'debit' direction,"
                + "ROUND(r.receipt_price,6) income_amount,CAST(NULL AS DECIMAL(24,6)) expense_amount,"
                + text("c.name") + " party_name," + text("c.code") + " party_code,"
                + String.format(creator, "r", "r", "r") + " receiver_id,NULL payer_id," + text("r.remark")
                + " remark," + text("r.payment_voucher_no")
                + " payment_voucher_no FROM erp_finance_receipt r LEFT JOIN erp_customer c ON c.id=r.customer_id"
                + " AND c.tenant_id=r.tenant_id WHERE r.tenant_id=:tenant AND r.deleted=b'0' AND r.status=20"
                + " AND r.account_id IS NOT NULL"
                + " UNION ALL SELECT p.tenant_id,'payment',20,p.id,1,CONCAT('payment:',p.id),p.account_id,"
                + "p.payment_time,p.create_time," + text("p.no") + "," + literal("付款单") + ",'credit',"
                + "CAST(NULL AS DECIMAL(24,6)),ROUND(p.payment_price,6)," + text("s.name") + ","
                + text("s.code") + ",NULL," + String.format(creator, "p", "p", "p") + "," + text("p.remark")
                + "," + text("p.payment_voucher_no")
                + " FROM erp_finance_payment p LEFT JOIN erp_supplier s ON s.id=p.supplier_id"
                + " AND s.tenant_id=p.tenant_id WHERE p.tenant_id=:tenant AND p.deleted=b'0' AND p.status=20"
                + " AND p.account_id IS NOT NULL"
                + " UNION ALL SELECT oi.tenant_id,'other_income',30,oi.id,1,CONCAT('other_income:',oi.id),"
                + "oi.account_id,oi.biz_time,oi.create_time," + text("oi.no") + "," + literal("其他收入")
                + ",'debit',ROUND(oi.total_amount,6),CAST(NULL AS DECIMAL(24,6))," + text("oi.party")
                + ",CAST(NULL AS CHAR CHARACTER SET utf8mb4),"
                + String.format(creator, "oi", "oi", "oi") + ",NULL," + text("oi.remark")
                + "," + text("oi.voucher_no")
                + " FROM erp_receivable_other_income oi WHERE oi.tenant_id=:tenant AND oi.deleted=b'0'"
                + " AND oi.status=20 AND (oi.source_type IS NULL OR oi.source_type<>'付款单折让')"
                + " AND oi.account_id IS NOT NULL"
                + " UNION ALL SELECT e.tenant_id,'expense',40,e.id,1,CONCAT('expense:',e.id),e.account_id,"
                + "CAST(e.biz_time AS DATETIME),e.create_time," + text("e.no") + "," + literal("费用支出")
                + ",'credit',CAST(NULL AS DECIMAL(24,6)),ROUND(e.total_amount,6)," + text("e.party")
                + ",CAST(NULL AS CHAR CHARACTER SET utf8mb4),NULL,"
                + String.format(creator, "e", "e", "e") + "," + text("e.remark")
                + "," + text("e.voucher_no")
                + " FROM erp_payable_expense e WHERE e.tenant_id=:tenant AND e.deleted=b'0' AND e.status=20"
                + " AND (e.source_type IS NULL OR e.source_type<>'收款单优惠')"
                + " AND e.account_id IS NOT NULL"
                + " UNION ALL SELECT t.tenant_id,'transfer',50,t.id,1,CONCAT('transfer:',t.id,':out'),"
                + "t.out_account_id,t.transfer_time,t.create_time," + text("t.no") + "," + literal("银行转账")
                + ",'credit',CAST(NULL AS DECIMAL(24,6)),ROUND(COALESCE(t.transfer_price,0)+COALESCE(t.fee_price,0),6),"
                + text("ia.name") + "," + text("ia.no") + ",NULL,"
                + String.format(creator, "t", "t", "t") + "," + text("t.remark")
                + ",CAST(NULL AS CHAR CHARACTER SET utf8mb4)"
                + " FROM erp_finance_transfer t LEFT JOIN erp_account ia ON ia.id=t.in_account_id"
                + " AND ia.tenant_id=t.tenant_id WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20"
                + " AND t.out_account_id IS NOT NULL"
                + " UNION ALL SELECT t.tenant_id,'transfer',50,t.id,2,CONCAT('transfer:',t.id,':in'),"
                + "t.in_account_id,t.transfer_time,t.create_time," + text("t.no") + "," + literal("银行转账")
                + ",'debit',ROUND(COALESCE(t.transfer_price,0),6),CAST(NULL AS DECIMAL(24,6)),"
                + text("oa.name") + "," + text("oa.no") + ","
                + String.format(creator, "t", "t", "t") + ",NULL," + text("t.remark")
                + ",CAST(NULL AS CHAR CHARACTER SET utf8mb4)"
                + " FROM erp_finance_transfer t LEFT JOIN erp_account oa ON oa.id=t.out_account_id"
                + " AND oa.tenant_id=t.tenant_id WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20"
                + " AND t.in_account_id IS NOT NULL";
    }

    private String selectColumns() {
        return "SELECT q.row_key,q.source_type,q.source_id,q.source_rank,q.leg_rank,q.account_id,"
                + "q.receiver_id,q.payer_id,q.party_name,q.party_code,q.account_name,"
                + "DATE_FORMAT(q.business_date,'%Y-%m-%d %H:%i:%s') business_date,q.business_no,q.document_type,"
                + "CASE q.direction WHEN 'debit' THEN '借方' ELSE '贷方' END direction,q.income_amount,"
                + "q.expense_amount,q.balance,ru.nickname receiver_name,pu.nickname payer_name,q.remark,"
                + "q.payment_voucher_no,q.voucher_no,CAST(NULL AS CHAR) online_payment_order_no,"
                + "CAST(NULL AS CHAR) online_payment_voucher_no";
    }

    private void applyFilters(Sql sql, Filter filter) {
        between(sql, "q.business_date", "businessDate", filter.getBusinessDateFrom(), filter.getBusinessDateTo());
        like(sql, "q.party_name", "partyName", filter.getPartyName());
        like(sql, "q.party_code", "partyCode", filter.getPartyCode());
        like(sql, "q.business_no", "businessNo", filter.getBusinessNo());
        if (filter.getDocumentTypes() != null && !filter.getDocumentTypes().isEmpty()) {
            sql.fromWhere += " AND q.source_type IN (:documentTypes)";
            sql.params.put("documentTypes", filter.getDocumentTypes());
        }
        equal(sql, "q.direction", "direction", filter.getDirection());
        equal(sql, "q.receiver_id", "receiverId", filter.getReceiverId());
        equal(sql, "q.payer_id", "payerId", filter.getPayerId());
        like(sql, "q.remark", "remark", filter.getRemark());
        like(sql, "q.payment_voucher_no", "paymentVoucherNo", filter.getPaymentVoucherNo());
        like(sql, "q.voucher_no", "voucherNo", filter.getVoucherNo());
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("partyName", "q.party_name");
        fields.put("partyCode", "q.party_code");
        fields.put("accountName", "q.account_name");
        fields.put("businessDate", "q.business_date");
        fields.put("businessNo", "q.business_no");
        fields.put("documentType", "q.document_type");
        fields.put("direction", "q.direction");
        fields.put("incomeAmount", "q.income_amount");
        fields.put("expenseAmount", "q.expense_amount");
        fields.put("balance", "q.balance");
        fields.put("receiverName", "ru.nickname");
        fields.put("payerName", "pu.nickname");
        fields.put("remark", "q.remark");
        String stable = "q.business_date DESC,q.create_time DESC,q.source_rank DESC,q.source_id DESC,q.leg_rank DESC";
        String requested = fields.get(filter.getOrderField());
        if (requested == null) return " ORDER BY " + stable;
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + requested + direction + "," + stable;
    }

    private String accountScope(ErpFinanceVisibleScope scope, Map<String, Object> params) {
        if (scope.isAll()) return "";
        StringBuilder condition = new StringBuilder(" AND (");
        boolean appended = false;
        if (!scope.getDeptIds().isEmpty()) {
            condition.append("a.dept_id IN (:accountDeptIds)");
            params.put("accountDeptIds", scope.getDeptIds());
            appended = true;
        }
        if (scope.getSelfUserId() != null) {
            if (appended) condition.append(" OR ");
            condition.append("CAST(a.creator AS UNSIGNED)=:accountSelf");
            params.put("accountSelf", scope.getSelfUserId());
            appended = true;
        }
        return appended ? condition.append(')').toString() : " AND 1=0";
    }

    private void between(Sql sql, String column, String prefix, Object from, Object to) {
        if (from != null) {
            sql.fromWhere += " AND " + column + ">=:" + prefix + "From";
            sql.params.put(prefix + "From", from);
        }
        if (to != null) {
            sql.fromWhere += " AND " + column + "<=:" + prefix + "To";
            sql.params.put(prefix + "To", to);
        }
    }

    private void like(Sql sql, String column, String name, String value) {
        if (!hasText(value)) return;
        sql.fromWhere += " AND " + column + " LIKE :" + name;
        sql.params.put(name, "%" + value.trim() + "%");
    }

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null) return;
        sql.fromWhere += " AND " + column + "=:" + name;
        sql.params.put(name, value);
    }

    private static String text(String expression) {
        return "CONVERT(" + expression + " USING utf8mb4) COLLATE utf8mb4_unicode_ci";
    }

    private static String literal(String value) {
        return "_utf8mb4'" + value + "' COLLATE utf8mb4_unicode_ci";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private NamedParameterJdbcTemplate jdbc() {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public static class Scope {
        private final long tenantId;
        private final ErpFinanceVisibleScope visibleScope;

        public Scope(long tenantId, ErpFinanceVisibleScope visibleScope) {
            this.tenantId = tenantId;
            this.visibleScope = visibleScope;
        }
    }

    private static class Sql {
        private String cte;
        private String fromWhere;
        private final Map<String, Object> params = new HashMap<>();
    }
}
