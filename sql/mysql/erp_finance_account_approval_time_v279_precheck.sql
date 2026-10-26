-- 应收、应付及应收冲应付审批时间迁移前检查（v279）。
-- 只读脚本：确认目标库、依赖字段、现有索引及预计回填范围，不修改数据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database;

SELECT required_object, object_exists
FROM (
    SELECT 'erp_finance_payment' AS required_object,
           EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment') AS object_exists
    UNION ALL SELECT 'erp_payable_other',
           EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_payable_other')
    UNION ALL SELECT 'erp_receivable_misc',
           EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_misc')
    UNION ALL SELECT 'erp_payable_misc',
           EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_payable_misc')
) dependency_check;

SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_finance_payment', 'erp_payable_other', 'erp_receivable_misc', 'erp_payable_misc')
  AND column_name IN ('id', 'tenant_id', 'status', 'deleted', 'create_time', 'update_time', 'approve_time')
ORDER BY table_name, ordinal_position;

SELECT table_name, index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_finance_payment', 'erp_payable_other', 'erp_receivable_misc', 'erp_payable_misc')
GROUP BY table_name, index_name
ORDER BY table_name, index_name;

SELECT 'erp_finance_payment' AS table_name, COUNT(*) AS approved_active_rows
FROM erp_finance_payment WHERE deleted = b'0' AND status = 20
UNION ALL
SELECT 'erp_payable_other', COUNT(*) FROM erp_payable_other WHERE deleted = b'0' AND status = 20
UNION ALL
SELECT 'erp_receivable_misc', COUNT(*) FROM erp_receivable_misc WHERE deleted = b'0' AND status = 20
UNION ALL
SELECT 'erp_payable_misc', COUNT(*) FROM erp_payable_misc WHERE deleted = b'0' AND status = 20;

