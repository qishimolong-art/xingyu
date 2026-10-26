package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
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
public class ErpPayableDetailReportRepository {

    private static final String PAYMENT_DISCOUNT_SOURCE_TYPE = "付款单折让";

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
                + ", matched_suppliers AS (SELECT DISTINCT supplier_id FROM matched)"
                + ", ending_ranked AS (SELECT report.supplier_id,report.ending_payable,ROW_NUMBER() OVER("
                + "PARTITION BY report.supplier_id ORDER BY report.document_date DESC,report.event_order DESC,"
                + "report.document_no DESC,report.business_id DESC) rn FROM report INNER JOIN matched_suppliers ms "
                + "ON ms.supplier_id=report.supplier_id"
                + (filter.getDocumentDateTo() == null ? "" : " WHERE report.document_date<=:documentTo") + ")"
                + " SELECT COUNT(*) row_count,COALESCE(SUM(increase_payable),0) increase_payable,"
                + "COALESCE(SUM(payment_amount),0) payment_amount,"
                + "COALESCE((SELECT SUM(ending_payable) FROM ending_ranked WHERE rn=1),0) ending_payable FROM matched";
        return jdbc().queryForObject(query, sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private Sql reportSql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        String accountIn = documentScope(scope.accountScope, "t.dept_id", "t.creator", "pin", sql.params, true);
        String accountReturn = documentScope(scope.accountScope, "t.dept_id", "t.handler", "pret", sql.params, false);
        String accountAdjust = documentScope(scope.accountScope, "t.dept_id", "t.adjuster", "padj", sql.params, false);
        String accountOther = documentScope(scope.accountScope, "t.dept_id", "t.handler_id", "pother", sql.params, false);
        String accountPayment = documentScope(scope.accountScope, "t.dept_id", "t.finance_user_id", "payment", sql.params, false);
        String supplierScope = supplierScope(scope.supplierScope, "s", "supplier", sql.params);

        String rawEvents = String.join(" UNION ALL ",
                purchaseIn(accountIn, supplierScope),
                purchaseReturn(accountReturn, supplierScope),
                purchaseAdjust(accountAdjust, supplierScope),
                payableAdjust(accountOther, supplierScope),
                payment(accountPayment, supplierScope));
        sql.cte = "WITH raw_events AS (" + rawEvents + "), ledger AS (SELECT raw_events.*,(increase_payable-payment_amount) change_amount FROM raw_events),"
                + "report AS (SELECT ledger.*,"
                + "COALESCE(SUM(change_amount) OVER(PARTITION BY supplier_id ORDER BY document_date,event_order,document_no,business_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING),0) previous_balance,"
                + "COALESCE(SUM(change_amount) OVER(PARTITION BY supplier_id ORDER BY document_date,event_order,document_no,business_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW),0) ending_payable "
                + "FROM ledger)";
        applyDisplayFilters(sql, filter);
        return sql;
    }

    private String purchaseIn(String scope, String supplierScope) {
        return selectPrefix("PURCHASE_IN", "采购入库", "10", "t.in_time",
                ErpPurchaseInMapper.ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION, "0", voucher(8))
                + " FROM erp_purchase_in t INNER JOIN erp_supplier s ON s.id=t.supplier_id AND s.tenant_id=t.tenant_id AND s.deleted=b'0'"
                + creator() + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + supplierScope;
    }

    private String purchaseReturn(String scope, String supplierScope) {
        return selectPrefix("PURCHASE_RETURN", "采购退货", "20", "t.return_time",
                "-ABS(COALESCE(t.total_price,0))", "0", voucher(9))
                + " FROM erp_purchase_return t INNER JOIN erp_supplier s ON s.id=t.supplier_id AND s.tenant_id=t.tenant_id AND s.deleted=b'0'"
                + creator() + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + supplierScope;
    }

    private String purchaseAdjust(String scope, String supplierScope) {
        return selectPrefix("PURCHASE_ADJUST", "采购调价", "30", "t.adjust_time",
                "COALESCE(t.total_adjust_price,0)", "0", voucher(19))
                + " FROM erp_purchase_price_adjust t INNER JOIN erp_supplier s ON s.id=t.supplier_id AND s.tenant_id=t.tenant_id AND s.deleted=b'0'"
                + creator() + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + supplierScope;
    }

    private String payableAdjust(String scope, String supplierScope) {
        String date = "CASE WHEN t.source_type='" + PAYMENT_DISCOUNT_SOURCE_TYPE + "' AND t.source_id IS NOT NULL "
                + "THEN COALESCE((SELECT fp.payment_time FROM erp_finance_payment fp WHERE fp.tenant_id=t.tenant_id "
                + "AND fp.deleted=b'0' AND fp.id=t.source_id LIMIT 1),TIMESTAMP(t.biz_time),t.create_time) "
                + "ELSE COALESCE(TIMESTAMP(t.biz_time),t.create_time) END";
        String order = "CASE WHEN t.source_type='" + PAYMENT_DISCOUNT_SOURCE_TYPE + "' THEN 40 ELSE 35 END";
        String voucher = "COALESCE(NULLIF(t.voucher_no,'')," + voucher(10) + ")";
        return selectPrefix("PAYABLE_ADJUST", "应付调账", order, date,
                "COALESCE(t.payable_amount,0)", "0", voucher)
                + " FROM erp_payable_other t INNER JOIN erp_supplier s ON s.id=t.supplier_id AND s.tenant_id=t.tenant_id AND s.deleted=b'0'"
                + creator() + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + supplierScope;
    }

    private String payment(String scope, String supplierScope) {
        String paid = "CASE WHEN EXISTS (SELECT 1 FROM erp_payable_other dpo WHERE dpo.tenant_id=t.tenant_id "
                + "AND dpo.deleted=b'0' AND dpo.status=20 AND dpo.source_type='" + PAYMENT_DISCOUNT_SOURCE_TYPE
                + "' AND dpo.source_id=t.id) THEN COALESCE(t.payment_price,0) ELSE COALESCE(t.total_price,0) END";
        return selectPrefix("FINANCE_PAYMENT", "付款单", "50", "t.payment_time", "0", paid, voucher(12))
                + " FROM erp_finance_payment t INNER JOIN erp_supplier s ON s.id=t.supplier_id AND s.tenant_id=t.tenant_id AND s.deleted=b'0'"
                + creator() + " WHERE t.tenant_id=:tenant AND t.deleted=b'0' AND t.status=20" + scope + supplierScope;
    }

    private String selectPrefix(String code, String name, String eventOrder, String documentDate,
                                String increasePayable, String paymentAmount, String voucherNo) {
        return "SELECT " + collate("CONCAT('" + code + ":',t.id)") + " row_key,t.id business_id,t.supplier_id,t.dept_id,"
                + "CAST(NULLIF(t.creator,'') AS UNSIGNED) creator_id," + collate("'" + code + "'")
                + " document_type_code," + collate("'" + name + "'") + " document_type," + eventOrder
                + " event_order," + documentDate + " document_date," + collate("s.code") + " supplier_code,"
                + collate("s.name") + " supplier_name," + collate("s.supplier_type") + " supplier_type,"
                + collate("t.no") + " document_no," + collate("cu.nickname") + " creator_name,"
                + "ROUND(" + increasePayable + ",6) increase_payable,ROUND(" + paymentAmount + ",6) payment_amount,"
                + collate(voucherNo) + " voucher_no," + collate("t.remark") + " remark";
    }

    private String creator() {
        return " LEFT JOIN system_users cu ON cu.id=CAST(NULLIF(t.creator,'') AS UNSIGNED) "
                + "AND cu.tenant_id=t.tenant_id AND cu.deleted=b'0'";
    }

    private String voucher(int sourceType) {
        return "(SELECT GROUP_CONCAT(DISTINCT v.voucher_no ORDER BY v.id SEPARATOR '、') FROM erp_voucher v WHERE "
                + "v.tenant_id=t.tenant_id AND v.deleted=b'0' AND v.source_biz_type=" + sourceType
                + " AND v.source_biz_id=t.id)";
    }

    private String collate(String expression) {
        return "CONVERT(" + expression + " USING utf8mb4) COLLATE utf8mb4_unicode_ci";
    }

    private void applyDisplayFilters(Sql sql, Filter filter) {
        between(sql, "report.document_date", "document", filter.getDocumentDateFrom(), filter.getDocumentDateTo());
        if (filter.getDocumentTypes() != null && !filter.getDocumentTypes().isEmpty()) {
            sql.displayWhere += " AND report.document_type_code IN (:documentTypes)";
            sql.params.put("documentTypes", filter.getDocumentTypes());
        }
        equal(sql, "report.supplier_id", "supplierId", filter.getSupplierId());
        equal(sql, "report.supplier_code", "supplierCode", filter.getSupplierCode());
        equal(sql, "report.supplier_type", "supplierType", filter.getSupplierType());
        like(sql, "report.document_no", "documentNo", filter.getDocumentNo());
        equal(sql, "report.creator_id", "creatorId", filter.getCreatorId());
        like(sql, "report.voucher_no", "voucherNo", filter.getVoucherNo());
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("supplierCode", "report.supplier_code");
        fields.put("supplierName", "report.supplier_name");
        fields.put("documentDate", "report.document_date");
        fields.put("documentType", "report.document_type");
        fields.put("documentNo", "report.document_no");
        fields.put("creatorName", "report.creator_name");
        fields.put("increasePayable", "report.increase_payable");
        fields.put("paymentAmount", "report.payment_amount");
        fields.put("previousBalance", "report.previous_balance");
        fields.put("endingPayable", "report.ending_payable");
        fields.put("supplierType", "report.supplier_type");
        String field = fields.getOrDefault(filter.getOrderField(), "report.document_date");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",report.event_order" + direction + ",report.document_no" + direction
                + ",report.business_id" + direction;
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
            params.put(prefix + "Self", stringUserColumn ? String.valueOf(scope.getSelfUserId()) : scope.getSelfUserId());
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
                    .append(alias).append(".allow_multi_dept=b'1' AND EXISTS (SELECT 1 FROM erp_supplier_dept sd WHERE sd.tenant_id=")
                    .append(alias).append(".tenant_id AND sd.supplier_id=").append(alias)
                    .append(".id AND sd.deleted=b'0' AND sd.dept_id IN (:").append(prefix).append("DeptIds)))");
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

    private void between(Sql sql, String column, String prefix, Object from, Object to) {
        if (from != null) {
            sql.displayWhere += " AND " + column + ">=:" + prefix + "From";
            sql.params.put(prefix + "From", from);
        }
        if (to != null) {
            sql.displayWhere += " AND " + column + "<=:" + prefix + "To";
            sql.params.put(prefix + "To", to);
        }
    }

    private void like(Sql sql, String column, String name, String value) {
        if (!hasText(value)) return;
        sql.displayWhere += " AND " + column + " LIKE :" + name;
        sql.params.put(name, "%" + value.trim() + "%");
    }

    private void equal(Sql sql, String column, String name, Object value) {
        if (value == null || value instanceof String && !hasText((String) value)) return;
        sql.displayWhere += " AND " + column + "=:" + name;
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
        private final ErpFinanceVisibleScope accountScope;
        private final ErpFinanceVisibleScope supplierScope;

        public Scope(long tenantId, ErpFinanceVisibleScope accountScope, ErpFinanceVisibleScope supplierScope) {
            this.tenantId = tenantId;
            this.accountScope = accountScope;
            this.supplierScope = supplierScope;
        }
    }

    private static class Sql {
        private String cte;
        private String displayWhere = "";
        private final Map<String, Object> params = new HashMap<>();
    }
}
