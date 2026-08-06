-- ERP product field-config and custom-field standalone page fix (v84)
-- Safe to execute repeatedly.
--
-- Purpose:
--   1. Seed missing `erp_field_config` rows for module_key = 'erp_product',
--      so "ERP / System Config / Field Config / Product Info" has fields.
--   2. Keep product field-permission definitions in sync for role field hiding.
--   3. Ensure "Add Custom Field" is a standalone ERP system-config page and
--      that the page has both query and create permissions.
--
-- Notes:
--   - Existing active user-edited erp_field_config rows are not overwritten.
--   - No broad DELETE is used.
--   - Execute with utf8mb4, for example:
--     mysql --default-character-set=utf8mb4 -u root -p your_db < code/sql/mysql/erp_product_field_config_custom_field_fix_v84.sql

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `module_key` varchar(50) NOT NULL COMMENT 'Module key',
  `field_name` varchar(100) NOT NULL COMMENT 'Field name',
  `field_label` varchar(100) DEFAULT NULL COMMENT 'Field label',
  `required` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Required',
  `visible` bit(1) NOT NULL DEFAULT b'1' COMMENT 'Visible',
  `sort` int NOT NULL DEFAULT 0 COMMENT 'Sort',
  `field_group` varchar(64) DEFAULT NULL COMMENT 'Field group',
  `field_source` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'Field source: SYSTEM/CUSTOM',
  `physical_column` varchar(64) DEFAULT NULL COMMENT 'Custom physical column',
  `field_type` varchar(32) DEFAULT NULL COMMENT 'Field type',
  `component_type` varchar(64) DEFAULT NULL COMMENT 'Frontend component type',
  `max_length` int DEFAULT NULL COMMENT 'Max length',
  `decimal_precision` int DEFAULT NULL COMMENT 'Decimal precision',
  `decimal_scale` int DEFAULT NULL COMMENT 'Decimal scale',
  `default_value` varchar(500) DEFAULT NULL COMMENT 'Default value',
  `list_visible` bit(1) NOT NULL DEFAULT b'0' COMMENT 'List visible',
  `searchable` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Searchable',
  `readonly` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Readonly',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_module_field` (`tenant_id`, `module_key`, `field_name`, `deleted`),
  KEY `idx_module_key` (`tenant_id`, `module_key`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP field config';

DROP PROCEDURE IF EXISTS add_erp_product_field_config_columns_v84;

DELIMITER //
CREATE PROCEDURE add_erp_product_field_config_columns_v84()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'visible') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `visible` bit(1) NOT NULL DEFAULT b'1' COMMENT 'Visible' AFTER `required`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'field_group') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `field_group` varchar(64) NULL COMMENT 'Field group' AFTER `sort`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'field_source') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `field_source` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'Field source: SYSTEM/CUSTOM' AFTER `field_group`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'physical_column') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `physical_column` varchar(64) NULL COMMENT 'Custom physical column' AFTER `field_source`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'field_type') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `field_type` varchar(32) NULL COMMENT 'Field type' AFTER `physical_column`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'component_type') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `component_type` varchar(64) NULL COMMENT 'Frontend component type' AFTER `field_type`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'max_length') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `max_length` int NULL COMMENT 'Max length' AFTER `component_type`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'decimal_precision') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `decimal_precision` int NULL COMMENT 'Decimal precision' AFTER `max_length`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'decimal_scale') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `decimal_scale` int NULL COMMENT 'Decimal scale' AFTER `decimal_precision`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'default_value') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `default_value` varchar(500) NULL COMMENT 'Default value' AFTER `decimal_scale`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'list_visible') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `list_visible` bit(1) NOT NULL DEFAULT b'0' COMMENT 'List visible' AFTER `default_value`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'searchable') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `searchable` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Searchable' AFTER `list_visible`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config' AND COLUMN_NAME = 'readonly') THEN
        ALTER TABLE `erp_field_config` ADD COLUMN `readonly` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Readonly' AFTER `searchable`;
    END IF;
END //
DELIMITER ;

CALL add_erp_product_field_config_columns_v84();
DROP PROCEDURE IF EXISTS add_erp_product_field_config_columns_v84;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_field_config_seed;
CREATE TEMPORARY TABLE tmp_erp_product_field_config_seed (
  `module_key` varchar(50) NOT NULL,
  `field_name` varchar(100) NOT NULL,
  `field_label` varchar(100) NOT NULL,
  `required` bit(1) NOT NULL DEFAULT b'0',
  `sort` int NOT NULL DEFAULT 0,
  `field_group` varchar(64) NOT NULL,
  `readonly` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`module_key`, `field_name`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_erp_product_field_config_seed
(`module_key`, `field_name`, `field_label`, `required`, `sort`, `field_group`, `readonly`)
VALUES
('erp_product', 'code', '配件编码', b'0', 10, 'base_info', b'1'),
('erp_product', 'name', '配件名称', b'1', 20, 'base_info', b'0'),
('erp_product', 'unitId', '单位', b'1', 30, 'base_info', b'0'),
('erp_product', 'defaultWarehouseId', '默认仓库', b'1', 40, 'base_info', b'0'),
('erp_product', 'vehicleModel', '适用车型', b'0', 50, 'base_info', b'0'),
('erp_product', 'standard', '规格', b'0', 60, 'base_info', b'0'),
('erp_product', 'categoryId', '配件分类', b'1', 70, 'base_info', b'0'),
('erp_product', 'batchNoEnabled', '是否开启批次号管理', b'0', 80, 'base_info', b'0'),
('erp_product', 'deptIds', '所属部门', b'0', 90, 'base_info', b'0'),
('erp_product', 'barCode', '条形码', b'0', 100, 'base_info', b'0'),
('erp_product', 'factoryCode', '厂家编码', b'0', 110, 'base_info', b'0'),
('erp_product', 'status', '状态', b'0', 120, 'base_info', b'0'),
('erp_product', 'remark', '备注', b'0', 130, 'base_info', b'0'),
('erp_product', 'referencePrice', '参考价', b'0', 210, 'price_info', b'0'),
('erp_product', 'retailPrice', '零售价', b'0', 220, 'price_info', b'0'),
('erp_product', 'lastPurchasePrice', '最后采购入库价', b'0', 230, 'price_info', b'1'),
('erp_product', 'grossProfitRate', '毛利率（%）', b'0', 240, 'price_info', b'0'),
('erp_product', 'backupPrice1', '备用价1', b'0', 250, 'price_info', b'0'),
('erp_product', 'wholesalePrice', '批发价', b'0', 260, 'price_info', b'0'),
('erp_product', 'sharePrice', '股份价', b'0', 270, 'price_info', b'0'),
('erp_product', 'stockMax', '库存上限', b'0', 310, 'extend_info', b'0'),
('erp_product', 'stockMin', '库存下限', b'0', 320, 'extend_info', b'0'),
('erp_product', 'stockStandard', '标准库存', b'0', 330, 'extend_info', b'0'),
('erp_product', 'packageQty', '包装数', b'0', 340, 'extend_info', b'0'),
('erp_product', 'weight', '重量（kg）', b'0', 350, 'extend_info', b'0');

-- Restore a logically deleted product field config only when there is no active row.
-- Use a temporary id table to avoid MySQL 1093 when updating and reading erp_field_config.
DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_field_config_restore_ids;
CREATE TEMPORARY TABLE tmp_erp_product_field_config_restore_ids (
  `id` bigint NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT IGNORE INTO tmp_erp_product_field_config_restore_ids (`id`)
SELECT cfg.`id`
FROM `erp_field_config` cfg
JOIN tmp_erp_product_field_config_seed seed
  ON seed.`module_key` COLLATE utf8mb4_unicode_ci = cfg.`module_key` COLLATE utf8mb4_unicode_ci
 AND seed.`field_name` COLLATE utf8mb4_unicode_ci = cfg.`field_name` COLLATE utf8mb4_unicode_ci
LEFT JOIN `erp_field_config` active_cfg
  ON active_cfg.`tenant_id` = cfg.`tenant_id`
 AND active_cfg.`module_key` COLLATE utf8mb4_unicode_ci = cfg.`module_key` COLLATE utf8mb4_unicode_ci
 AND active_cfg.`field_name` COLLATE utf8mb4_unicode_ci = cfg.`field_name` COLLATE utf8mb4_unicode_ci
 AND active_cfg.`deleted` = b'0'
WHERE cfg.`tenant_id` = 1
  AND cfg.`deleted` = b'1'
  AND active_cfg.`id` IS NULL;

UPDATE `erp_field_config` cfg
JOIN tmp_erp_product_field_config_restore_ids restore_ids
  ON restore_ids.`id` = cfg.`id`
JOIN tmp_erp_product_field_config_seed seed
  ON seed.`module_key` COLLATE utf8mb4_unicode_ci = cfg.`module_key` COLLATE utf8mb4_unicode_ci
 AND seed.`field_name` COLLATE utf8mb4_unicode_ci = cfg.`field_name` COLLATE utf8mb4_unicode_ci
SET cfg.`field_label` = seed.`field_label`,
    cfg.`required` = seed.`required`,
    cfg.`visible` = b'1',
    cfg.`sort` = seed.`sort`,
    cfg.`field_group` = seed.`field_group`,
    cfg.`field_source` = 'SYSTEM',
    cfg.`readonly` = seed.`readonly`,
    cfg.`updater` = '1',
    cfg.`update_time` = NOW(),
    cfg.`deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_field_config_insert_rows;
CREATE TEMPORARY TABLE tmp_erp_product_field_config_insert_rows (
  `module_key` varchar(50) NOT NULL,
  `field_name` varchar(100) NOT NULL,
  `field_label` varchar(100) NOT NULL,
  `required` bit(1) NOT NULL DEFAULT b'0',
  `sort` int NOT NULL DEFAULT 0,
  `field_group` varchar(64) NOT NULL,
  `readonly` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`module_key`, `field_name`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO tmp_erp_product_field_config_insert_rows
(`module_key`, `field_name`, `field_label`, `required`, `sort`, `field_group`, `readonly`)
SELECT seed.`module_key`,
       seed.`field_name`,
       seed.`field_label`,
       seed.`required`,
       seed.`sort`,
       seed.`field_group`,
       seed.`readonly`
FROM tmp_erp_product_field_config_seed seed
LEFT JOIN `erp_field_config` cfg
  ON cfg.`tenant_id` = 1
 AND cfg.`module_key` COLLATE utf8mb4_unicode_ci = seed.`module_key` COLLATE utf8mb4_unicode_ci
 AND cfg.`field_name` COLLATE utf8mb4_unicode_ci = seed.`field_name` COLLATE utf8mb4_unicode_ci
 AND cfg.`deleted` = b'0'
WHERE cfg.`id` IS NULL;

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `field_type`, `readonly`, `list_visible`, `searchable`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT insert_rows.`module_key`,
       insert_rows.`field_name`,
       insert_rows.`field_label`,
       insert_rows.`required`,
       b'1',
       insert_rows.`sort`,
       insert_rows.`field_group`,
       'SYSTEM',
       'TEXT',
       insert_rows.`readonly`,
       b'0',
       b'0',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       1
FROM tmp_erp_product_field_config_insert_rows insert_rows;

CREATE TABLE IF NOT EXISTS `system_field_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `module` varchar(50) NOT NULL COMMENT 'Module key',
  `field_key` varchar(100) NOT NULL COMMENT 'Field key',
  `field_label` varchar(100) NOT NULL COMMENT 'Field label',
  `field_group` varchar(50) DEFAULT NULL COMMENT 'Field group',
  `sort` int NOT NULL DEFAULT 0 COMMENT 'Sort',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_field` (`module`, `field_key`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System field definition';

CREATE TABLE IF NOT EXISTS `system_role_field_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `role_id` bigint NOT NULL COMMENT 'Role id',
  `field_id` bigint NOT NULL COMMENT 'Field definition id',
  `hidden` bit(1) NOT NULL DEFAULT b'1' COMMENT 'Whether hidden',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_field` (`role_id`, `field_id`, `tenant_id`, `deleted`),
  KEY `idx_field_id` (`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System role field permission';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_field_definition_seed;
CREATE TEMPORARY TABLE tmp_erp_product_field_definition_seed (
  `module` varchar(50) NOT NULL,
  `field_key` varchar(100) NOT NULL,
  `field_label` varchar(100) NOT NULL,
  `field_group` varchar(50) NOT NULL,
  `sort` int NOT NULL,
  PRIMARY KEY (`module`, `field_key`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_erp_product_field_definition_seed
(`module`, `field_key`, `field_label`, `field_group`, `sort`)
VALUES
('erp_product', 'code', '配件编码', 'base_info', 10),
('erp_product', 'name', '配件名称', 'base_info', 20),
('erp_product', 'unitId', '单位', 'base_info', 30),
('erp_product', 'defaultWarehouseId', '默认仓库', 'base_info', 40),
('erp_product', 'vehicleModel', '适用车型', 'base_info', 50),
('erp_product', 'standard', '规格', 'base_info', 60),
('erp_product', 'categoryId', '配件分类', 'base_info', 70),
('erp_product', 'batchNoEnabled', '是否开启批次号管理', 'base_info', 80),
('erp_product', 'deptId', '所属部门', 'base_info', 90),
('erp_product', 'barCode', '条形码', 'base_info', 100),
('erp_product', 'factoryCode', '厂家编码', 'base_info', 110),
('erp_product', 'status', '状态', 'base_info', 120),
('erp_product', 'remark', '备注', 'base_info', 130),
('erp_product', 'referencePrice', '参考价', 'price_info', 210),
('erp_product', 'retailPrice', '零售价', 'price_info', 220),
('erp_product', 'lastPurchasePrice', '最后采购入库价', 'price_info', 230),
('erp_product', 'grossProfitRate', '毛利率（%）', 'price_info', 240),
('erp_product', 'backupPrice1', '备用价1', 'price_info', 250),
('erp_product', 'wholesalePrice', '批发价', 'price_info', 260),
('erp_product', 'sharePrice', '股份价', 'price_info', 270),
('erp_product', 'stockMax', '库存上限', 'extend_info', 310),
('erp_product', 'stockMin', '库存下限', 'extend_info', 320),
('erp_product', 'stockStandard', '标准库存', 'extend_info', 330),
('erp_product', 'packageQty', '包装数', 'extend_info', 340),
('erp_product', 'weight', '重量（kg）', 'extend_info', 350),
('erp_product', 'col_code', '列表-配件编码', 'list_col', 1010),
('erp_product', 'col_name', '列表-配件名称', 'list_col', 1020),
('erp_product', 'col_vehicleModel', '列表-适用车型', 'list_col', 1030),
('erp_product', 'col_standard', '列表-规格', 'list_col', 1040),
('erp_product', 'col_categoryName', '列表-配件分类', 'list_col', 1050),
('erp_product', 'col_unitName', '列表-单位', 'list_col', 1060),
('erp_product', 'col_barCode', '列表-条形码', 'list_col', 1070),
('erp_product', 'col_factoryCode', '列表-厂家编码', 'list_col', 1080),
('erp_product', 'col_deptName', '列表-所属部门', 'list_col', 1090),
('erp_product', 'col_retailPrice', '列表-零售价', 'list_col', 1100),
('erp_product', 'col_referencePrice', '列表-参考价', 'list_col', 1110),
('erp_product', 'col_sharePrice', '列表-股份价', 'list_col', 1120),
('erp_product', 'col_currentStock', '列表-当前库存', 'list_col', 1130),
('erp_product', 'col_status', '列表-状态', 'list_col', 1140),
('erp_product', 'col_createTime', '列表-创建时间', 'list_col', 1150),
('erp_product', 'col_creatorName', '列表-创建人', 'list_col', 1160),
('erp_product', 'col_updateTime', '列表-修改时间', 'list_col', 1170),
('erp_product', 'col_updaterName', '列表-修改人', 'list_col', 1180);

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module`,
       seed.`field_key`,
       seed.`field_label`,
       seed.`field_group`,
       seed.`sort`,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       1
FROM tmp_erp_product_field_definition_seed seed
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

-- Repair ERP / System Config / Field Config and Add Custom Field menus.
SET @erp_root_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      (`name` COLLATE utf8mb4_unicode_ci = 'ERP 系统' COLLATE utf8mb4_unicode_ci AND `type` = 1 AND `parent_id` = 0)
      OR `id` = 2563
    )
  ORDER BY CASE WHEN `name` COLLATE utf8mb4_unicode_ci = 'ERP 系统' COLLATE utf8mb4_unicode_ci THEN 0 ELSE 1 END, `id` DESC
  LIMIT 1
);

SET @erp_system_config_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` COLLATE utf8mb4_unicode_ci = '系统配置' COLLATE utf8mb4_unicode_ci
    AND `type` = 1
    AND `parent_id` = @erp_root_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '系统配置',
       '',
       1,
       90,
       @erp_root_id,
       'system',
       'ep:setting',
       '',
       '',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @erp_root_id IS NOT NULL
  AND @erp_system_config_id IS NULL;

SET @erp_system_config_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` COLLATE utf8mb4_unicode_ci = '系统配置' COLLATE utf8mb4_unicode_ci
    AND `type` = 1
    AND `parent_id` = @erp_root_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @field_config_page_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/index' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '字段配置',
    `permission` = '',
    `type` = 2,
    `sort` = 1,
    `parent_id` = @erp_system_config_id,
    `path` = 'fieldconfig',
    `icon` = 'fa:list-alt',
    `component` = 'erp/system/fieldconfig/index',
    `component_name` = 'ErpFieldConfig',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @erp_system_config_id IS NOT NULL
  AND `id` = @field_config_page_id
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '字段配置',
       '',
       2,
       1,
       @erp_system_config_id,
       'fieldconfig',
       'fa:list-alt',
       'erp/system/fieldconfig/index',
       'ErpFieldConfig',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND @field_config_page_id IS NULL;

SET @field_config_page_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/index' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @field_query_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `parent_id` = @field_config_page_id
    AND `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:query' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '字段配置查询',
       'erp:field-config:query',
       3,
       1,
       @field_config_page_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @field_config_page_id IS NOT NULL
  AND @field_query_button_id IS NULL;

SET @field_update_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `parent_id` = @field_config_page_id
    AND `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:update' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '字段配置更新',
       'erp:field-config:update',
       3,
       2,
       @field_config_page_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @field_config_page_id IS NOT NULL
  AND @field_update_button_id IS NULL;

SET @custom_field_page_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/custom-field' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY CASE WHEN `parent_id` = @erp_system_config_id THEN 0 ELSE 1 END, `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '新增自定义字段',
    `permission` = '',
    `type` = 2,
    `sort` = 2,
    `parent_id` = @erp_system_config_id,
    `path` = 'fieldconfig/custom-field',
    `icon` = 'ep:plus',
    `component` = 'erp/system/fieldconfig/custom-field',
    `component_name` = 'ErpProductCustomFieldCreate',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @erp_system_config_id IS NOT NULL
  AND `id` = @custom_field_page_id
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '新增自定义字段',
       '',
       2,
       2,
       @erp_system_config_id,
       'fieldconfig/custom-field',
       'ep:plus',
       'erp/system/fieldconfig/custom-field',
       'ErpProductCustomFieldCreate',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND @custom_field_page_id IS NULL;

SET @custom_field_page_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/custom-field' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY CASE WHEN `parent_id` = @erp_system_config_id THEN 0 ELSE 1 END, `id` DESC
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_duplicate_custom_field_pages;
CREATE TEMPORARY TABLE tmp_duplicate_custom_field_pages (
  `id` bigint NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT IGNORE INTO tmp_duplicate_custom_field_pages (`id`)
SELECT `id`
FROM `system_menu`
WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/custom-field' COLLATE utf8mb4_unicode_ci
  AND `deleted` = b'0'
  AND `id` <> @custom_field_page_id;

UPDATE `system_menu` menu
JOIN tmp_duplicate_custom_field_pages dup
  ON dup.`id` = menu.`id`
SET menu.`deleted` = b'1',
    menu.`visible` = b'0',
    menu.`updater` = '1',
    menu.`update_time` = NOW()
WHERE @custom_field_page_id IS NOT NULL;

SET @custom_field_query_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `parent_id` = @custom_field_page_id
    AND `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:query' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '查询已有字段',
       'erp:field-config:query',
       3,
       1,
       @custom_field_page_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @custom_field_page_id IS NOT NULL
  AND @custom_field_query_button_id IS NULL;

SET @custom_field_create_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:create-custom-field' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '新增自定义字段',
    `type` = 3,
    `sort` = 2,
    `parent_id` = @custom_field_page_id,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = NULL,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @custom_field_page_id IS NOT NULL
  AND `id` = @custom_field_create_button_id
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '新增自定义字段',
       'erp:field-config:create-custom-field',
       3,
       2,
       @custom_field_page_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @custom_field_page_id IS NOT NULL
  AND @custom_field_create_button_id IS NULL;

SET @field_query_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `parent_id` = @field_config_page_id
    AND `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:query' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @field_update_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `parent_id` = @field_config_page_id
    AND `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:update' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @custom_field_query_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `parent_id` = @custom_field_page_id
    AND `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:query' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @custom_field_create_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:create-custom-field' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_custom_field_roles;
CREATE TEMPORARY TABLE tmp_erp_product_custom_field_roles (
  `role_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `tenant_id`)
) ENGINE=MEMORY;

INSERT IGNORE INTO tmp_erp_product_custom_field_roles (`role_id`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, rm.`tenant_id`
FROM `system_role_menu` rm
JOIN `system_menu` m
  ON m.`id` = rm.`menu_id`
 AND m.`deleted` = b'0'
WHERE rm.`deleted` = b'0'
  AND m.`permission` COLLATE utf8mb4_unicode_ci IN (
      'erp:field-config:query' COLLATE utf8mb4_unicode_ci,
      'erp:field-config:update' COLLATE utf8mb4_unicode_ci,
      'erp:field-config:create-custom-field' COLLATE utf8mb4_unicode_ci
  );

INSERT IGNORE INTO tmp_erp_product_custom_field_roles (`role_id`, `tenant_id`)
VALUES (1, 1);

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT roles.`role_id`,
       target.`menu_id`,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       roles.`tenant_id`
FROM tmp_erp_product_custom_field_roles roles
JOIN (
    SELECT @erp_system_config_id AS `menu_id`
    UNION ALL SELECT @custom_field_page_id
    UNION ALL SELECT @custom_field_query_button_id
    UNION ALL SELECT @custom_field_create_button_id
) target
  ON target.`menu_id` IS NOT NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` exists_rm
    WHERE exists_rm.`role_id` = roles.`role_id`
      AND exists_rm.`tenant_id` = roles.`tenant_id`
      AND exists_rm.`menu_id` = target.`menu_id`
      AND exists_rm.`deleted` = b'0'
);

-- Ensure role_id=1 can still open and save the original field-config page.
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1,
       target.`menu_id`,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       1
FROM (
    SELECT @erp_system_config_id AS `menu_id`
    UNION ALL SELECT @field_config_page_id
    UNION ALL SELECT @field_query_button_id
    UNION ALL SELECT @field_update_button_id
) target
WHERE target.`menu_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` exists_rm
      WHERE exists_rm.`role_id` = 1
        AND exists_rm.`tenant_id` = 1
        AND exists_rm.`menu_id` = target.`menu_id`
        AND exists_rm.`deleted` = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_custom_field_roles;
DROP TEMPORARY TABLE IF EXISTS tmp_duplicate_custom_field_pages;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_field_definition_seed;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_field_config_insert_rows;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_field_config_restore_ids;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_field_config_seed;

SELECT 'erp_product_field_config_count' AS `check_item`, COUNT(*) AS `check_value`
FROM `erp_field_config`
WHERE `module_key` COLLATE utf8mb4_unicode_ci = 'erp_product' COLLATE utf8mb4_unicode_ci
  AND `tenant_id` = 1
  AND `deleted` = b'0';

SELECT 'erp_product_field_definition_count' AS `check_item`, COUNT(*) AS `check_value`
FROM `system_field_definition`
WHERE `module` COLLATE utf8mb4_unicode_ci = 'erp_product' COLLATE utf8mb4_unicode_ci
  AND `tenant_id` = 1
  AND `deleted` = b'0';

SELECT 'custom_field_page_count' AS `check_item`, COUNT(*) AS `check_value`
FROM `system_menu`
WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/custom-field' COLLATE utf8mb4_unicode_ci
  AND `deleted` = b'0';
