-- ERP supplier field-config schema fix v168.
-- Aligns supplier field-config rows with the current frontend schema.
-- Safe to rerun: only supplier rows for tenant 1 are touched, and user visibility/required settings are preserved.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DROP PROCEDURE IF EXISTS add_erp_supplier_field_config_columns_v168;

DELIMITER //
CREATE PROCEDURE add_erp_supplier_field_config_columns_v168()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'visible'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `visible` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否显示'
            AFTER `required`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_group'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_group` varchar(64) NULL COMMENT '字段分组'
            AFTER `sort`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_source'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_source` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'field source: SYSTEM/CUSTOM'
            AFTER `field_group`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'list_visible'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `list_visible` bit(1) NOT NULL DEFAULT b'0' COMMENT 'list visible'
            AFTER `field_source`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'searchable'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `searchable` bit(1) NOT NULL DEFAULT b'0' COMMENT 'searchable'
            AFTER `list_visible`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'readonly'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `readonly` bit(1) NOT NULL DEFAULT b'0' COMMENT 'readonly'
            AFTER `searchable`;
    END IF;
END //
DELIMITER ;

CALL add_erp_supplier_field_config_columns_v168();
DROP PROCEDURE IF EXISTS add_erp_supplier_field_config_columns_v168;

DROP TEMPORARY TABLE IF EXISTS tmp_supplier_field_config_schema_fix_v168;

CREATE TEMPORARY TABLE tmp_supplier_field_config_schema_fix_v168 (
  old_field_name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL PRIMARY KEY,
  new_field_name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  new_field_label varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  new_sort int NOT NULL,
  field_group varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO tmp_supplier_field_config_schema_fix_v168
(`old_field_name`, `new_field_name`, `new_field_label`, `new_sort`, `field_group`)
VALUES
('invoiceTaxNo', 'taxpayerId', '纳税人识别号', 380, 'invoice_info'),
('invoiceTitle', 'invoiceCompany', '开票单位', 430, 'invoice_info'),
('financeMobile', 'financePhone', '财务联系电话', 500, 'finance_info');

UPDATE `erp_field_config` cfg
JOIN tmp_supplier_field_config_schema_fix_v168 fix
  ON fix.`old_field_name` COLLATE utf8mb4_0900_ai_ci = cfg.`field_name` COLLATE utf8mb4_0900_ai_ci
LEFT JOIN `erp_field_config` active_new
  ON active_new.`tenant_id` = cfg.`tenant_id`
 AND active_new.`module_key` COLLATE utf8mb4_0900_ai_ci = cfg.`module_key` COLLATE utf8mb4_0900_ai_ci
 AND active_new.`field_name` COLLATE utf8mb4_0900_ai_ci = fix.`new_field_name` COLLATE utf8mb4_0900_ai_ci
 AND active_new.`deleted` = b'0'
   SET cfg.`field_name` = fix.`new_field_name`,
       cfg.`field_label` = fix.`new_field_label`,
       cfg.`sort` = fix.`new_sort`,
       cfg.`field_group` = COALESCE(cfg.`field_group`, fix.`field_group`),
       cfg.`updater` = '1',
       cfg.`update_time` = NOW()
 WHERE cfg.`module_key` COLLATE utf8mb4_0900_ai_ci = 'supplier' COLLATE utf8mb4_0900_ai_ci
   AND cfg.`tenant_id` = 1
   AND cfg.`deleted` = b'0'
   AND active_new.`id` IS NULL;

UPDATE `erp_field_config` active_new
JOIN tmp_supplier_field_config_schema_fix_v168 fix
  ON fix.`new_field_name` COLLATE utf8mb4_0900_ai_ci = active_new.`field_name` COLLATE utf8mb4_0900_ai_ci
JOIN `erp_field_config` old_cfg
  ON old_cfg.`tenant_id` = active_new.`tenant_id`
 AND old_cfg.`module_key` COLLATE utf8mb4_0900_ai_ci = active_new.`module_key` COLLATE utf8mb4_0900_ai_ci
 AND old_cfg.`field_name` COLLATE utf8mb4_0900_ai_ci = fix.`old_field_name` COLLATE utf8mb4_0900_ai_ci
 AND old_cfg.`deleted` = b'0'
   SET active_new.`required` = old_cfg.`required`,
       active_new.`visible` = old_cfg.`visible`,
       active_new.`sort` = fix.`new_sort`,
       active_new.`field_label` = fix.`new_field_label`,
       active_new.`field_group` = COALESCE(active_new.`field_group`, fix.`field_group`),
       active_new.`updater` = '1',
       active_new.`update_time` = NOW(),
       old_cfg.`field_name` = CONCAT('__deprecated__', old_cfg.`id`, '_', old_cfg.`field_name`),
       old_cfg.`deleted` = b'1',
       old_cfg.`updater` = '1',
       old_cfg.`update_time` = NOW()
 WHERE active_new.`module_key` COLLATE utf8mb4_0900_ai_ci = 'supplier' COLLATE utf8mb4_0900_ai_ci
   AND active_new.`tenant_id` = 1
   AND active_new.`deleted` = b'0';

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `list_visible`, `searchable`, `readonly`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'supplier', fix.`new_field_name`, fix.`new_field_label`, b'0', b'1', fix.`new_sort`,
       fix.`field_group`, 'SYSTEM', b'0', b'0', b'0', '1', NOW(), '1', NOW(), b'0', 1
  FROM tmp_supplier_field_config_schema_fix_v168 fix
 WHERE NOT EXISTS (
       SELECT 1
         FROM `erp_field_config` cfg
        WHERE cfg.`module_key` COLLATE utf8mb4_0900_ai_ci = 'supplier' COLLATE utf8mb4_0900_ai_ci
          AND cfg.`field_name` COLLATE utf8mb4_0900_ai_ci = fix.`new_field_name` COLLATE utf8mb4_0900_ai_ci
          AND cfg.`tenant_id` = 1
          AND cfg.`deleted` = b'0'
 );

DROP TEMPORARY TABLE IF EXISTS tmp_supplier_field_config_schema_fix_v168;
