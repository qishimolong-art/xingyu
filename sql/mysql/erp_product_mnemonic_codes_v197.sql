-- Add product mnemonic-code fields and configuration.
-- Safe to execute repeatedly: only missing columns/active config rows are added, and existing permissions are not deleted.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_product_mnemonic_code_columns_v197;

DELIMITER //
CREATE PROCEDURE add_erp_product_mnemonic_code_columns_v197()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_product'
          AND COLUMN_NAME = 'pinyin_code'
    ) THEN
        ALTER TABLE `erp_product`
            ADD COLUMN `pinyin_code` varchar(64) NULL COMMENT '拼音码' AFTER `name`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_product'
          AND COLUMN_NAME = 'wubi_code'
    ) THEN
        ALTER TABLE `erp_product`
            ADD COLUMN `wubi_code` varchar(32) NULL COMMENT '五笔码' AFTER `pinyin_code`;
    END IF;
END //
DELIMITER ;

CALL add_erp_product_mnemonic_code_columns_v197();
DROP PROCEDURE IF EXISTS add_erp_product_mnemonic_code_columns_v197;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_mnemonic_field_seed_v197;
CREATE TEMPORARY TABLE tmp_erp_product_mnemonic_field_seed_v197 (
  `field_name` varchar(100) NOT NULL PRIMARY KEY,
  `field_label` varchar(100) NOT NULL,
  `sort` int NOT NULL,
  `max_length` int NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_erp_product_mnemonic_field_seed_v197
(`field_name`, `field_label`, `sort`, `max_length`) VALUES
('pinyinCode', '拼音码', 31, 64),
('wubiCode', '五笔码', 32, 32);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_config_tenant_v197;
CREATE TEMPORARY TABLE tmp_erp_product_config_tenant_v197 (
  `tenant_id` bigint NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT IGNORE INTO tmp_erp_product_config_tenant_v197 (`tenant_id`)
SELECT DISTINCT `tenant_id`
FROM `erp_field_config`
WHERE `module_key` = 'erp_product'
  AND `deleted` = b'0';

INSERT IGNORE INTO tmp_erp_product_config_tenant_v197 (`tenant_id`) VALUES (1);

UPDATE `erp_field_config` cfg
JOIN tmp_erp_product_mnemonic_field_seed_v197 seed
  ON seed.`field_name` COLLATE utf8mb4_unicode_ci = cfg.`field_name` COLLATE utf8mb4_unicode_ci
JOIN tmp_erp_product_config_tenant_v197 tenant_seed
  ON tenant_seed.`tenant_id` = cfg.`tenant_id`
SET cfg.`field_label` = seed.`field_label`,
    cfg.`visible` = b'1',
    cfg.`sort` = seed.`sort`,
    cfg.`field_group` = 'base_info',
    cfg.`field_source` = 'SYSTEM',
    cfg.`field_type` = 'VARCHAR',
    cfg.`component_type` = 'Input',
    cfg.`max_length` = seed.`max_length`,
    cfg.`readonly` = b'0',
    cfg.`updater` = '1',
    cfg.`update_time` = NOW()
WHERE cfg.`module_key` = 'erp_product'
  AND cfg.`deleted` = b'0';

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `field_type`, `component_type`, `max_length`, `readonly`, `list_visible`, `searchable`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_product',
       seed.`field_name`,
       seed.`field_label`,
       b'0',
       b'1',
       seed.`sort`,
       'base_info',
       'SYSTEM',
       'VARCHAR',
       'Input',
       seed.`max_length`,
       b'0',
       b'0',
       b'0',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       tenant_seed.`tenant_id`
FROM tmp_erp_product_config_tenant_v197 tenant_seed
CROSS JOIN tmp_erp_product_mnemonic_field_seed_v197 seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_field_config` cfg
    WHERE cfg.`tenant_id` = tenant_seed.`tenant_id`
      AND cfg.`module_key` = 'erp_product'
      AND cfg.`field_name` COLLATE utf8mb4_unicode_ci = seed.`field_name` COLLATE utf8mb4_unicode_ci
      AND cfg.`deleted` = b'0'
);

UPDATE `system_field_definition` fd
JOIN tmp_erp_product_mnemonic_field_seed_v197 seed
  ON seed.`field_name` COLLATE utf8mb4_unicode_ci = fd.`field_key` COLLATE utf8mb4_unicode_ci
JOIN tmp_erp_product_config_tenant_v197 tenant_seed
  ON tenant_seed.`tenant_id` = fd.`tenant_id`
SET fd.`field_label` = seed.`field_label`,
    fd.`field_group` = 'base_info',
    fd.`sort` = seed.`sort`,
    fd.`updater` = '1',
    fd.`update_time` = NOW()
WHERE fd.`module` = 'erp_product'
  AND fd.`deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`,
 `deleted`, `tenant_id`)
SELECT 'erp_product',
       seed.`field_name`,
       seed.`field_label`,
       'base_info',
       seed.`sort`,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       tenant_seed.`tenant_id`
FROM tmp_erp_product_config_tenant_v197 tenant_seed
CROSS JOIN tmp_erp_product_mnemonic_field_seed_v197 seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_field_definition` fd
    WHERE fd.`tenant_id` = tenant_seed.`tenant_id`
      AND fd.`module` = 'erp_product'
      AND fd.`field_key` COLLATE utf8mb4_unicode_ci = seed.`field_name` COLLATE utf8mb4_unicode_ci
      AND fd.`deleted` = b'0'
);

SELECT cfg.`tenant_id`, cfg.`field_name`, cfg.`field_label`, cfg.`field_group`, cfg.`sort`
FROM `erp_field_config` cfg
WHERE cfg.`module_key` = 'erp_product'
  AND cfg.`field_name` IN ('pinyinCode', 'wubiCode')
  AND cfg.`deleted` = b'0'
ORDER BY cfg.`tenant_id`, cfg.`sort`;
