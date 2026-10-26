package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableExpenseDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableExpenseDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableExpenseDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 费用支付明细报表的显式租户、删除状态与数据权限查询。 */
@Repository
public class ErpPayableExpenseDetailReportRepository {

    @Resource private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        Long count = jdbc().queryForObject("SELECT COUNT(*)" + sql.fromWhere, sql.params, Long.class);
        return count == null ? 0L : count;
    }

    public List<Row> page(Filter filter, Scope scope, long offset, int limit) {
        Sql sql = reportSql(filter, scope);
        sql.params.put("offset", offset);
        sql.params.put("limit", limit);
        return jdbc().query(selectColumns() + sql.fromWhere + orderBy(filter) + " LIMIT :limit OFFSET :offset",
                sql.params, new BeanPropertyRowMapper<>(Row.class));
    }

    public Summary summary(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        return jdbc().queryForObject("SELECT COUNT(*) row_count,COALESCE(SUM(i.amount),0) amount"
                        + sql.fromWhere, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private Sql reportSql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        sql.fromWhere = " FROM erp_payable_expense e"
                + " INNER JOIN erp_payable_expense_item i ON i.expense_id=e.id AND i.tenant_id=e.tenant_id"
                + " AND i.deleted=b'0'"
                + " LEFT JOIN erp_account a ON a.id=e.account_id AND a.tenant_id=e.tenant_id AND a.deleted=b'0'"
                + " LEFT JOIN system_dept bd ON bd.id=e.dept_id AND bd.deleted=b'0'"
                + " LEFT JOIN system_dept idp ON idp.id=i.dept_id AND idp.deleted=b'0'"
                + " LEFT JOIN system_users hu ON hu.id=e.handler_id AND hu.tenant_id=e.tenant_id AND hu.deleted=b'0'"
                + " LEFT JOIN system_users bu ON bu.id=i.handler_id AND bu.tenant_id=e.tenant_id AND bu.deleted=b'0'"
                + " WHERE e.tenant_id=:tenant AND e.deleted=b'0' AND e.status=20 AND e.approve_time IS NOT NULL"
                + documentScope(scope.visibleScope, "e.dept_id", "e.handler_id", "main", sql.params)
                + documentScope(scope.visibleScope, "i.dept_id", "i.handler_id", "item", sql.params);
        applyFilters(sql, filter);
        return sql;
    }

    private String selectColumns() {
        return "SELECT CONCAT(e.id,':',i.id) row_key,e.id expense_id,i.id item_id,e.account_id,e.handler_id,"
                + "e.dept_id billing_dept_id,i.dept_id,i.handler_id business_user_id,"
                + "DATE_FORMAT(e.approve_time,'%Y-%m-%d') approve_date,a.name account_name,e.no document_no,"
                + "i.item_name,ROUND(i.amount,6) amount,e.voucher_no,e.remark summary,hu.nickname handler_name,"
                + "e.party,CAST(NULL AS CHAR) type,bd.name billing_dept_name,idp.name dept_name,"
                + "bu.nickname business_user_name,CAST(NULL AS CHAR) invoice,CAST(NULL AS CHAR) occurrence_date";
    }

    private void applyFilters(Sql sql, Filter filter) {
        between(sql, "e.approve_time", "approve", filter.getApproveDateFrom(), filter.getApproveDateTo());
        equal(sql, "e.account_id", "accountId", filter.getAccountId());
        like(sql, "e.no", "documentNo", filter.getDocumentNo());
        like(sql, "i.item_name", "itemName", filter.getItemName());
        if (filter.getAmountMin() != null) {
            sql.fromWhere += " AND i.amount>=:amountMin";
            sql.params.put("amountMin", filter.getAmountMin());
        }
        if (filter.getAmountMax() != null) {
            sql.fromWhere += " AND i.amount<=:amountMax";
            sql.params.put("amountMax", filter.getAmountMax());
        }
        like(sql, "e.voucher_no", "voucherNo", filter.getVoucherNo());
        like(sql, "e.remark", "summary", filter.getSummary());
        equal(sql, "e.handler_id", "handlerId", filter.getHandlerId());
        like(sql, "e.party", "party", filter.getParty());
        equal(sql, "e.dept_id", "billingDeptId", filter.getBillingDeptId());
        equal(sql, "i.dept_id", "deptId", filter.getDeptId());
        equal(sql, "i.handler_id", "businessUserId", filter.getBusinessUserId());
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("approveDate", "e.approve_time");
        fields.put("accountName", "a.name");
        fields.put("documentNo", "e.no");
        fields.put("itemName", "i.item_name");
        fields.put("amount", "i.amount");
        fields.put("voucherNo", "e.voucher_no");
        fields.put("summary", "e.remark");
        fields.put("handlerName", "hu.nickname");
        fields.put("party", "e.party");
        fields.put("billingDeptName", "bd.name");
        fields.put("deptName", "idp.name");
        fields.put("businessUserName", "bu.nickname");
        String requested = fields.get(filter.getOrderField());
        if (requested == null) return " ORDER BY e.approve_time DESC,e.no DESC,i.id ASC";
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + requested + direction + ",e.approve_time DESC,e.no DESC,i.id ASC";
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
        private String fromWhere;
        private final Map<String, Object> params = new HashMap<>();
    }
}
