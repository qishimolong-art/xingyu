-- 其他应收 / 其他应付：补充账户归属字段与字段权限定义
-- 安全性：
--   - 仅追加 nullable account_id 字段、索引和字段定义。
--   - 不回填历史业务数据，不修改账户余额，不产生资金流水。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @tenant_id := 1;

SET @receivable_account_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'erp_receivable_misc'
    AND COLUMN_NAME = 'account_id'
);
SET @sql := IF(@receivable_account_column_exists = 0,
  'ALTER TABLE `erp_receivable_misc` ADD COLUMN `account_id` bigint DEFAULT NULL COMMENT ''账户ID'' AFTER `customer_id`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @receivable_account_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'erp_receivable_misc'
    AND INDEX_NAME = 'idx_account_id'
);
SET @sql := IF(@receivable_account_index_exists = 0,
  'ALTER TABLE `erp_receivable_misc` ADD INDEX `idx_account_id` (`account_id`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payable_account_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'erp_payable_misc'
    AND COLUMN_NAME = 'account_id'
);
SET @sql := IF(@payable_account_column_exists = 0,
  'ALTER TABLE `erp_payable_misc` ADD COLUMN `account_id` bigint DEFAULT NULL COMMENT ''账户ID'' AFTER `supplier_id`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payable_account_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'erp_payable_misc'
    AND INDEX_NAME = 'idx_account_id'
);
SET @sql := IF(@payable_account_index_exists = 0,
  'ALTER TABLE `erp_payable_misc` ADD INDEX `idx_account_id` (`account_id`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `system_field_definition` (`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT module_name, field_key, field_label, field_group, sort_no, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
  SELECT 'erp_finance_receivable_misc' AS module_name, 'accountId' AS field_key, '账户' AS field_label, 'base_info' AS field_group, 4 AS sort_no
  UNION ALL SELECT 'erp_finance_payable_misc', 'accountId', '账户', 'base_info', 4
) fields
WHERE NOT EXISTS (
  SELECT 1 FROM `system_field_definition` d
  WHERE d.`deleted` = b'0'
    AND d.`tenant_id` = @tenant_id
    AND d.`module` = fields.module_name
    AND d.`field_key` = fields.field_key
);
