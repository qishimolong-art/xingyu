package cn.iocoder.yudao.module.erp.dal.mysql.finance;

/** Shared settlement relations. Explicit tenant joins also protect correlated subqueries. */
public final class ErpMiscSettlementSql {
    private ErpMiscSettlementSql() {}

    public static final String RECEIVABLE_ROWS =
            "SELECT f.tenant_id, f.source_receivable_misc_id AS misc_id, f.id AS document_id, f.receipt_price AS amount "
            + "FROM erp_finance_receipt f WHERE f.deleted = 0 AND f.status = 20 AND f.source_receivable_misc_id IS NOT NULL "
            + "UNION ALL "
            + "SELECT i.tenant_id, i.biz_id AS misc_id, f.id AS document_id, SUM(i.receipt_price) AS amount "
            + "FROM erp_finance_receipt_item i INNER JOIN erp_finance_receipt f ON f.id = i.receipt_id AND f.tenant_id = i.tenant_id "
            + "WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 24 AND f.deleted = 0 AND f.status = 20 AND (f.source_receivable_misc_id IS NULL OR f.source_receivable_misc_id != i.biz_id) "
            + "GROUP BY i.tenant_id, i.biz_id, f.id "
            + "UNION ALL "
            + "SELECT o.tenant_id, o.source_misc_id AS misc_id, f.id AS document_id, SUM(CASE WHEN SIGN(m.amount) = -1 THEN o.amount ELSE -o.amount END) AS amount "
            + "FROM erp_receivable_misc o INNER JOIN erp_finance_receipt f ON f.id = o.source_id AND f.tenant_id = o.tenant_id "
            + "INNER JOIN erp_receivable_misc m ON m.id = o.source_misc_id AND m.tenant_id = o.tenant_id AND m.deleted = 0 "
            + "WHERE o.deleted = 0 AND o.status = 20 AND o.source_type = '收款单转其他应收冲减' AND SIGN(o.amount) = -1 "
            + "AND f.deleted = 0 AND f.status = 20 AND (f.source_receivable_misc_id IS NULL OR f.source_receivable_misc_id != o.source_misc_id) "
            + "AND NOT EXISTS (SELECT 1 FROM erp_finance_receipt_item old_item WHERE old_item.receipt_id = f.id AND old_item.tenant_id = f.tenant_id AND old_item.biz_type = 24 AND old_item.biz_id = o.source_misc_id) "
            + "GROUP BY o.tenant_id, o.source_misc_id, f.id ";

    public static final String RECEIVABLE_TOTALS = "(SELECT tenant_id, misc_id, SUM(amount) AS settled_amount FROM ("
            + RECEIVABLE_ROWS + ") settlement_rows GROUP BY tenant_id, misc_id)";

    public static final String RECEIVABLE_ORIGINAL =
            "SELECT m.id, m.tenant_id, m.deleted, m.status, m.customer_id, m.dept_id, m.handler_id, m.biz_time, m.amount, 0 AS settled_amount, m.no, m.remark, m.file_url, NULL AS document_id FROM erp_receivable_misc m WHERE (m.source_type IS NULL OR m.source_type != '收款单转其他应收冲减') ";

    public static final String RECEIVABLE_LEDGER = "(" + RECEIVABLE_ORIGINAL
            + " UNION ALL SELECT m.id, m.tenant_id, m.deleted, m.status, m.customer_id, f.dept_id, f.finance_user_id AS handler_id, f.receipt_time AS biz_time, -s.amount AS amount, s.amount AS settled_amount, f.no, f.remark, NULL AS file_url, f.id AS document_id"
            + " FROM (" + RECEIVABLE_ROWS + ") s INNER JOIN erp_receivable_misc m ON m.id = s.misc_id AND m.tenant_id = s.tenant_id"
            + " INNER JOIN erp_finance_receipt f ON f.id = s.document_id AND f.tenant_id = s.tenant_id WHERE m.deleted = 0 AND m.status = 20)";

    public static final String PAYABLE_ROWS =
            "SELECT f.tenant_id, f.source_payable_misc_id AS misc_id, f.id AS document_id, f.payment_price AS amount "
            + "FROM erp_finance_payment f WHERE f.deleted = 0 AND f.status = 20 AND f.source_payable_misc_id IS NOT NULL "
            + "UNION ALL "
            + "SELECT i.tenant_id, i.biz_id AS misc_id, f.id AS document_id, SUM(i.payment_price) AS amount "
            + "FROM erp_finance_payment_item i INNER JOIN erp_finance_payment f ON f.id = i.payment_id AND f.tenant_id = i.tenant_id "
            + "WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 14 AND f.deleted = 0 AND f.status = 20 AND (f.source_payable_misc_id IS NULL OR f.source_payable_misc_id != i.biz_id) "
            + "GROUP BY i.tenant_id, i.biz_id, f.id "
            + "UNION ALL "
            + "SELECT o.tenant_id, o.source_misc_id AS misc_id, f.id AS document_id, SUM(CASE WHEN SIGN(m.amount) = -1 THEN o.amount ELSE -o.amount END) AS amount "
            + "FROM erp_payable_misc o INNER JOIN erp_finance_payment f ON f.id = o.source_id AND f.tenant_id = o.tenant_id "
            + "INNER JOIN erp_payable_misc m ON m.id = o.source_misc_id AND m.tenant_id = o.tenant_id AND m.deleted = 0 "
            + "WHERE o.deleted = 0 AND o.status = 20 AND o.source_type = '付款单转其他应付冲减' AND SIGN(o.amount) = -1 "
            + "AND f.deleted = 0 AND f.status = 20 AND (f.source_payable_misc_id IS NULL OR f.source_payable_misc_id != o.source_misc_id) "
            + "AND NOT EXISTS (SELECT 1 FROM erp_finance_payment_item old_item WHERE old_item.payment_id = f.id AND old_item.tenant_id = f.tenant_id AND old_item.biz_type = 14 AND old_item.biz_id = o.source_misc_id) "
            + "GROUP BY o.tenant_id, o.source_misc_id, f.id ";

    public static final String PAYABLE_TOTALS = "(SELECT tenant_id, misc_id, SUM(amount) AS settled_amount FROM ("
            + PAYABLE_ROWS + ") settlement_rows GROUP BY tenant_id, misc_id)";

    public static final String PAYABLE_ORIGINAL =
            "SELECT m.id, m.tenant_id, m.deleted, m.status, m.supplier_id, m.dept_id, m.handler_id, m.biz_time, m.amount, 0 AS settled_amount, m.no, m.remark, m.file_url, NULL AS document_id FROM erp_payable_misc m WHERE (m.source_type IS NULL OR m.source_type != '付款单转其他应付冲减') ";

    public static final String PAYABLE_LEDGER = "(" + PAYABLE_ORIGINAL
            + " UNION ALL SELECT m.id, m.tenant_id, m.deleted, m.status, m.supplier_id, f.dept_id, f.finance_user_id AS handler_id, f.payment_time AS biz_time, -s.amount AS amount, s.amount AS settled_amount, f.no, f.remark, NULL AS file_url, f.id AS document_id"
            + " FROM (" + PAYABLE_ROWS + ") s INNER JOIN erp_payable_misc m ON m.id = s.misc_id AND m.tenant_id = s.tenant_id"
            + " INNER JOIN erp_finance_payment f ON f.id = s.document_id AND f.tenant_id = s.tenant_id WHERE m.deleted = 0 AND m.status = 20)";

    public static String totalExpression(boolean receivable, String alias) {
        return "COALESCE((SELECT SUM(s.amount) FROM (" + (receivable ? RECEIVABLE_ROWS : PAYABLE_ROWS)
                + ") s WHERE s.misc_id = " + alias + ".id AND s.tenant_id = " + alias + ".tenant_id), 0)";
    }
}
