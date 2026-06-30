-- ERP product custom fields v71.
-- Safe to execute repeatedly. Adds metadata columns and the create-custom-field button permission.

DROP PROCEDURE IF EXISTS add_erp_product_custom_field_columns_v71;

DELIMITER //
CREATE PROCEDURE add_erp_product_custom_field_columns_v71()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'field_source') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `field_source` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'field source: SYSTEM/CUSTOM' AFTER `sort`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'physical_column') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `physical_column` varchar(64) NULL COMMENT 'custom physical column' AFTER `field_source`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'field_type') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `field_type` varchar(32) NULL COMMENT 'field type' AFTER `physical_column`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'field_group') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `field_group` varchar(64) NULL COMMENT 'field group' AFTER `field_type`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'component_type') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `component_type` varchar(64) NULL COMMENT 'frontend component type' AFTER `field_group`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'max_length') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `max_length` int NULL COMMENT 'max length' AFTER `component_type`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'decimal_precision') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `decimal_precision` int NULL COMMENT 'decimal precision' AFTER `max_length`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'decimal_scale') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `decimal_scale` int NULL COMMENT 'decimal scale' AFTER `decimal_precision`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'default_value') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `default_value` varchar(500) NULL COMMENT 'default value' AFTER `decimal_scale`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'list_visible') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `list_visible` bit(1) NOT NULL DEFAULT b'0' COMMENT 'list visible' AFTER `default_value`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'searchable') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `searchable` bit(1) NOT NULL DEFAULT b'0' COMMENT 'searchable' AFTER `list_visible`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'readonly') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `readonly` bit(1) NOT NULL DEFAULT b'0' COMMENT 'readonly' AFTER `searchable`;
    END IF;
END //
DELIMITER ;

CALL add_erp_product_custom_field_columns_v71();
DROP PROCEDURE IF EXISTS add_erp_product_custom_field_columns_v71;

UPDATE `erp_field_config`
SET `field_source` = 'SYSTEM'
WHERE (`field_source` IS NULL OR `field_source` = '')
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'Field Config Create Custom Field', 'erp:field-config:create-custom-field', 3, 4, parent.`id`, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`permission` = 'erp:field-config:update'
  AND parent.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu` m
      WHERE m.`permission` = 'erp:field-config:create-custom-field'
        AND m.`deleted` = b'0'
  )
LIMIT 1;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, m.`id`, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` m
WHERE m.`permission` = 'erp:field-config:create-custom-field'
  AND m.`deleted` = b'0';
