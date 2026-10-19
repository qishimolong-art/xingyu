-- 收款单优惠自动生成非现金费用支付（v274）执行前检查
-- 只读检查，不修改结构、配置或业务数据。

SELECT DATABASE() AS target_database, VERSION() AS mysql_version;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_finance_receipt', 'erp_payable_expense', 'erp_payable_expense_item',
                     'erp_base_data', 'system_tenant')
ORDER BY table_name;

SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'erp_finance_receipt'
        AND column_name IN ('id', 'no', 'status', 'receipt_time', 'finance_user_id', 'dept_id',
                            'customer_id', 'account_id', 'discount_price', 'receipt_price'))
    OR (table_name = 'erp_payable_expense'
        AND column_name IN ('id', 'status', 'approve_time', 'account_id', 'total_amount',
                            'source_type', 'source_id', 'source_no', 'tenant_id', 'deleted'))
    OR (table_name = 'erp_payable_expense_item'
        AND column_name IN ('expense_id', 'item_name', 'amount', 'expense_category', 'tenant_id', 'deleted')))
ORDER BY table_name, ordinal_position;

SELECT tenant_id, source_type, source_id, deleted, COUNT(*) AS duplicate_count
FROM erp_payable_expense
WHERE source_type IS NOT NULL AND source_id IS NOT NULL
GROUP BY tenant_id, source_type, source_id, deleted
HAVING COUNT(*) > 1;

SELECT index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_list
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'erp_payable_expense'
GROUP BY index_name, non_unique
ORDER BY index_name;

SELECT t.id AS tenant_id, seed.type, seed.name,
       SUM(CASE WHEN d.id IS NOT NULL AND d.deleted = b'0' THEN 1 ELSE 0 END) AS active_count
FROM system_tenant t
CROSS JOIN (
    SELECT 'settle_method' AS type, '挂账' AS name
    UNION ALL SELECT 'payable_expense_biz_type', '一般费用'
    UNION ALL SELECT 'payable_expense_type', '其他'
    UNION ALL SELECT 'payable_expense_doc_type', '正常单据'
    UNION ALL SELECT 'payable_expense_item_project', '销售减收'
) seed
LEFT JOIN erp_base_data d ON d.tenant_id = t.id AND d.type = seed.type AND d.name = seed.name
WHERE t.deleted = b'0' AND t.status = 0
GROUP BY t.id, seed.type, seed.name
ORDER BY t.id, seed.type, seed.name;

SELECT COUNT(*) AS approved_discount_receipts_not_to_backfill,
       COALESCE(SUM(discount_price), 0) AS historical_discount_amount_not_to_backfill
FROM erp_finance_receipt
WHERE deleted = b'0' AND status = 20 AND discount_price > 0;
