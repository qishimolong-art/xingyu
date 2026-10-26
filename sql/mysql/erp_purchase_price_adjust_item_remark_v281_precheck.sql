-- 采购调价明细备注迁移前检查（v281）。
-- 只读脚本；目标库、表结构或重复定义异常时不得执行正式迁移。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database;

SELECT COUNT(*) AS required_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name IN ('erp_purchase_price_adjust_item', 'system_field_definition');

SELECT column_name, column_type, is_nullable, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'erp_purchase_price_adjust_item'
  AND column_name = 'remark';

SELECT tenant_id, field_key, field_label, field_group, sort, deleted, COUNT(*) AS row_count
FROM system_field_definition
WHERE module = 'erp_purchase_price_adjust'
  AND field_key = 'item_remark'
GROUP BY tenant_id, field_key, field_label, field_group, sort, deleted
ORDER BY tenant_id, deleted;

SELECT tenant_id, COUNT(*) AS active_duplicate_count
FROM system_field_definition
WHERE module = 'erp_purchase_price_adjust'
  AND field_key = 'item_remark'
  AND deleted = b'0'
GROUP BY tenant_id
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS active_item_rows
FROM erp_purchase_price_adjust_item
WHERE deleted = b'0';
