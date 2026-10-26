package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPaymentDetailReportModels.Summary;
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
public class ErpPaymentDetailReportRepository {

    private static final int PAYMENT_VOUCHER_SOURCE_TYPE = 12;

    @Resource
    private JdbcTemplate jdbcTemplate;

    public long count(Filter filter, Scope scope) {
        Sql sql = reportSql(filter, scope);
        Long result = jdbc().queryForObject(sql.cte + " SELECT COUNT(*) FROM report WHERE 1=1" + sql.displayWhere,
                sql.params, Long.class);
        return result == null ? 0L : result;
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
        return jdbc().queryForObject(sql.cte + " SELECT COUNT(*) row_count,"
                        + "COALESCE(SUM(settlement_amount),0) settlement_amount,"
                        + "CASE WHEN COUNT(*)=0 THEN 0 WHEN COUNT(actual_payment_amount)=0 THEN NULL"
                        + " ELSE SUM(actual_payment_amount) END actual_payment_amount"
                        + " FROM report WHERE 1=1" + sql.displayWhere,
                sql.params, new BeanPropertyRowMapper<>(Summary.class));
    }

    private Sql reportSql(Filter filter, Scope scope) {
        Sql sql = new Sql();
        sql.params.put("tenant", scope.tenantId);
        String paymentScope = documentScope(scope.paymentScope, "p.dept_id", "p.finance_user_id",
                "payment", sql.params);
        sql.cte = "WITH vouchers AS (SELECT source_biz_id,GROUP_CONCAT(DISTINCT voucher_no ORDER BY id SEPARATOR '、') voucher_no"
                + " FROM erp_voucher WHERE tenant_id=:tenant AND deleted=b'0' AND source_biz_type="
                + PAYMENT_VOUCHER_SOURCE_TYPE + " GROUP BY source_biz_id),"
                + "raw AS (SELECT p.id payment_id,i.id payment_item_id,i.biz_id business_id,p.supplier_id,p.account_id,p.dept_id,"
                + "CASE WHEN CHAR_LENGTH(p.creator)>0 THEN CAST(p.creator AS UNSIGNED) END payer_id,"
                + sourceCreatorId() + " source_creator_id,i.biz_type business_type_code,"
                + collate("s.name") + " supplier_name,p.payment_time payment_date," + collate("p.no")
                + " payment_no," + collate("COALESCE(pin.no,pret.no,padj.no,pm.no,i.biz_no)") + " business_no,"
                + collate("CASE i.biz_type WHEN 11 THEN '采购入库' WHEN 12 THEN '采购退货' WHEN 13 THEN '采购调价'"
                + " WHEN 14 THEN '其他应付' END") + " business_type,"
                + "ROUND(COALESCE(i.payment_price,0),6) settlement_amount,"
                + "ROUND(COALESCE(p.payment_price,0),6) payment_actual," + collate("a.name")
                + " account_name," + collate("p.payment_voucher_no") + " payment_voucher_no,"
                + collate("v.voucher_no") + " voucher_no," + collate("pu.nickname")
                + " payer_name," + collate("su.nickname") + " purchaser_name," + collate("s.supplier_type")
                + " supplier_type,a.account_type payment_method_code,"
                + collate("CASE a.account_type WHEN 1 THEN '银行账户' WHEN 2 THEN '现金账户'"
                + " WHEN 3 THEN '其他账户' END") + " payment_method," + collate("su.nickname")
                + " creator_name," + collate("CASE i.biz_type WHEN 11 THEN pin.settle_method"
                + " WHEN 12 THEN pret.settle_method END") + " settlement_method," + collate("d.name")
                + " dept_name,SUM(COALESCE(i.payment_price,0)) OVER(PARTITION BY p.id) allocation_total,"
                + "ROW_NUMBER() OVER(PARTITION BY p.id ORDER BY i.id DESC) allocation_rank"
                + " FROM erp_finance_payment p INNER JOIN erp_finance_payment_item i ON i.payment_id=p.id"
                + " AND i.tenant_id=p.tenant_id AND i.deleted=b'0' AND i.write_off_status=1"
                + " INNER JOIN erp_supplier s ON s.id=p.supplier_id AND s.tenant_id=p.tenant_id AND s.deleted=b'0'"
                + " LEFT JOIN erp_account a ON a.id=p.account_id AND a.tenant_id=p.tenant_id AND a.deleted=b'0'"
                + " LEFT JOIN system_dept d ON d.id=p.dept_id AND d.tenant_id=p.tenant_id AND d.deleted=b'0'"
                + " LEFT JOIN system_users pu ON pu.id=CASE WHEN CHAR_LENGTH(p.creator)>0"
                + " THEN CAST(p.creator AS UNSIGNED) END"
                + " AND pu.tenant_id=p.tenant_id AND pu.deleted=b'0'"
                + " LEFT JOIN erp_purchase_in pin ON i.biz_type=11 AND pin.id=i.biz_id"
                + " AND pin.tenant_id=i.tenant_id AND pin.deleted=b'0'"
                + " LEFT JOIN erp_purchase_return pret ON i.biz_type=12 AND pret.id=i.biz_id"
                + " AND pret.tenant_id=i.tenant_id AND pret.deleted=b'0'"
                + " LEFT JOIN erp_purchase_price_adjust padj ON i.biz_type=13 AND padj.id=i.biz_id"
                + " AND padj.tenant_id=i.tenant_id AND padj.deleted=b'0'"
                + " LEFT JOIN erp_payable_misc pm ON i.biz_type=14 AND pm.id=i.biz_id"
                + " AND pm.tenant_id=i.tenant_id AND pm.deleted=b'0'"
                + " LEFT JOIN system_users su ON su.id=" + sourceCreatorId()
                + " AND su.tenant_id=p.tenant_id AND su.deleted=b'0'"
                + " LEFT JOIN vouchers v ON v.source_biz_id=p.id"
                + " WHERE p.tenant_id=:tenant AND p.deleted=b'0' AND p.status=20" + paymentScope + "),"
                + "allocated AS (SELECT raw.*,CASE WHEN allocation_total=0 THEN NULL"
                + " WHEN allocation_rank=1 THEN ROUND(payment_actual-COALESCE(SUM(CASE WHEN allocation_rank>1"
                + " THEN ROUND(payment_actual*settlement_amount/allocation_total,6) ELSE 0 END)"
                + " OVER(PARTITION BY payment_id),0),6)"
                + " ELSE ROUND(payment_actual*settlement_amount/allocation_total,6) END actual_payment_amount FROM raw),"
                + "report AS (SELECT * FROM allocated WHERE business_type_code IN (11,12,13,14))";
        applyDisplayFilters(sql, filter);
        return sql;
    }

    private String sourceCreatorId() {
        String creator = "CASE i.biz_type WHEN 11 THEN pin.creator WHEN 12 THEN pret.creator"
                + " WHEN 13 THEN padj.creator WHEN 14 THEN pm.creator END";
        return "CASE WHEN CHAR_LENGTH(" + creator + ")>0 THEN CAST(" + creator + " AS UNSIGNED) END";
    }

    private String collate(String expression) {
        return "CONVERT(" + expression + " USING utf8mb4) COLLATE utf8mb4_unicode_ci";
    }

    private void applyDisplayFilters(Sql sql, Filter filter) {
        between(sql, "report.payment_date", "paymentDate", filter.getPaymentDateFrom(), filter.getPaymentDateTo());
        equal(sql, "report.supplier_id", "supplierId", filter.getSupplierId());
        like(sql, "report.payment_no", "paymentNo", filter.getPaymentNo());
        like(sql, "report.business_no", "businessNo", filter.getBusinessNo());
        if (filter.getBusinessTypes() != null && !filter.getBusinessTypes().isEmpty()) {
            sql.displayWhere += " AND report.business_type_code IN (:businessTypes)";
            sql.params.put("businessTypes", filter.getBusinessTypes());
        }
        equal(sql, "report.account_id", "accountId", filter.getAccountId());
        equal(sql, "report.payer_id", "payerId", filter.getPayerId());
        equal(sql, "report.source_creator_id", "sourceCreatorId", filter.getSourceCreatorId());
        equal(sql, "report.supplier_type", "supplierType", filter.getSupplierType());
        equal(sql, "report.payment_method_code", "paymentMethod", filter.getPaymentMethod());
        equal(sql, "report.settlement_method", "settlementMethod", filter.getSettlementMethod());
        equal(sql, "report.dept_id", "deptId", filter.getDeptId());
        like(sql, "report.payment_voucher_no", "paymentVoucherNo", filter.getPaymentVoucherNo());
        like(sql, "report.voucher_no", "voucherNo", filter.getVoucherNo());
    }

    private String orderBy(Filter filter) {
        Map<String, String> fields = new HashMap<>();
        fields.put("supplierName", "report.supplier_name");
        fields.put("paymentDate", "report.payment_date");
        fields.put("paymentNo", "report.payment_no");
        fields.put("businessNo", "report.business_no");
        fields.put("businessType", "report.business_type_code");
        fields.put("settlementAmount", "report.settlement_amount");
        fields.put("actualPaymentAmount", "report.actual_payment_amount");
        fields.put("accountName", "report.account_name");
        fields.put("paymentVoucherNo", "report.payment_voucher_no");
        fields.put("voucherNo", "report.voucher_no");
        fields.put("payerName", "report.payer_name");
        fields.put("purchaserName", "report.purchaser_name");
        fields.put("supplierType", "report.supplier_type");
        fields.put("paymentMethod", "report.payment_method_code");
        fields.put("creatorName", "report.creator_name");
        fields.put("settlementMethod", "report.settlement_method");
        fields.put("deptName", "report.dept_name");
        String field = fields.getOrDefault(filter.getOrderField(), "report.payment_date");
        String direction = "asc".equalsIgnoreCase(filter.getOrderDirection()) ? " ASC" : " DESC";
        return " ORDER BY " + field + direction + ",report.payment_no" + direction
                + ",report.payment_item_id" + direction;
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
        private final ErpFinanceVisibleScope paymentScope;

        public Scope(long tenantId, ErpFinanceVisibleScope paymentScope) {
            this.tenantId = tenantId;
            this.paymentScope = paymentScope;
        }
    }

    private static class Sql {
        private String cte;
        private String displayWhere = "";
        private final Map<String, Object> params = new HashMap<>();
    }
}
