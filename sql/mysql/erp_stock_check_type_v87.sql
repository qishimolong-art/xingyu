-- ERP stock check type patch (v87)
-- Scope:
--   1. Add stock check type to erp_stock_check.
--   2. Register stock check type in ERP field config and field permission definitions.
-- Safety:
--   - Idempotent.
--   - Does not delete or overwrite role/menu/permission data.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_stock_check_type_column_v87;

DELIMITER //
CREATE PROCEDURE add_erp_stock_check_type_column_v87()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_check'
          AND COLUMN_NAME = 'check_type'
    ) THEN
        ALTER TABLE `erp_stock_check`
            ADD COLUMN `check_type` TINYINT NOT NULL DEFAULT 1 COMMENT '盘点类型：1盘数量 2盘成本'
            AFTER `check_time`;
    END IF;
END //
DELIMITER ;

CALL add_erp_stock_check_type_column_v87();
DROP PROCEDURE IF EXISTS add_erp_stock_check_type_column_v87;

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_stock_check', 'checkType', '盘点类型', b'1', b'1', 15, 'base_info', 'SYSTEM',
       '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `erp_field_config`
    WHERE `module_key` = 'erp_stock_check'
      AND `field_name` = 'checkType'
      AND `tenant_id` = 1
      AND `deleted` = b'0'
);

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_stock_check', 'checkType', '盘点类型', 'main_form', 15, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'col_checkType', '盘点类型', 'list_col', 15, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'query_checkType', '盘点类型', 'query', 15, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
