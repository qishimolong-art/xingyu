-- ERP customer credit feature v104.
-- Adds white-credit fields and field permission definitions.
-- Safe to execute repeatedly; no broad deletes or overwrite-style updates.

DROP PROCEDURE IF EXISTS add_erp_customer_credit_columns_v104;

DELIMITER //
CREATE PROCEDURE add_erp_customer_credit_columns_v104()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_customer'
           AND COLUMN_NAME = 'credit_enabled'
    ) THEN
        ALTER TABLE `erp_customer`
            ADD COLUMN `credit_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用白条授信'
            AFTER `credit_limit`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_customer'
           AND COLUMN_NAME = 'credit_term_days'
    ) THEN
        ALTER TABLE `erp_customer`
            ADD COLUMN `credit_term_days` INT NULL COMMENT '白条授信期限（天）'
            AFTER `credit_enabled`;
    END IF;
END //
DELIMITER ;

CALL add_erp_customer_credit_columns_v104();
DROP PROCEDURE IF EXISTS add_erp_customer_credit_columns_v104;

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `list_visible`, `searchable`, `readonly`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module_key`, seed.`field_name`, seed.`field_label`, seed.`required`, b'1', seed.`sort`,
       seed.`field_group`, 'SYSTEM', b'0', b'0', b'0', '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 'customer' module_key, 'creditEnabled' field_name, '白条授信' field_label, b'0' required, 285 sort, 'finance_info' field_group
    UNION ALL
    SELECT 'customer', 'creditTermDays', '授信期限（天）', b'0', 295, 'finance_info'
) seed
WHERE EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'erp_field_config'
)
  AND NOT EXISTS (
    SELECT 1
      FROM `erp_field_config` cfg
     WHERE cfg.`module_key` = seed.`module_key`
       AND cfg.`field_name` = seed.`field_name`
       AND cfg.`tenant_id` = 1
       AND cfg.`deleted` = b'0'
);

DROP PROCEDURE IF EXISTS update_erp_customer_credit_field_config_v104;

DELIMITER //
CREATE PROCEDURE update_erp_customer_credit_field_config_v104()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_field_config'
    ) THEN
        UPDATE `erp_field_config`
           SET `field_label` = '授信期限（天）',
               `updater` = '1',
               `update_time` = NOW()
         WHERE `module_key` = 'customer'
           AND `field_name` = 'creditTermDays'
           AND `field_label` = '授信期限'
           AND `tenant_id` = 1
           AND `deleted` = b'0';
    END IF;
END //
DELIMITER ;

CALL update_erp_customer_credit_field_config_v104();
DROP PROCEDURE IF EXISTS update_erp_customer_credit_field_config_v104;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_customer', 'creditEnabled', '白条授信', 'finance_info', 285, '1', NOW(), '1', NOW(), b'0', 1),
('erp_customer', 'creditTermDays', '授信期限（天）', 'finance_info', 295, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
