-- Restore sale cart delivery method and VIN metadata.
-- Safe to execute repeatedly. It only adds missing physical columns, inserts
-- missing tenant-1 config rows, and corrects these two labels when an earlier
-- run was affected by client encoding.

DROP PROCEDURE IF EXISTS add_sale_cart_column_if_missing_v154;

DELIMITER //
CREATE PROCEDURE add_sale_cart_column_if_missing_v154(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_sale_cart_column_if_missing_v154('erp_sale_cart', 'delivery_method', '`delivery_method` VARCHAR(64) DEFAULT NULL COMMENT ''delivery method''');
CALL add_sale_cart_column_if_missing_v154('erp_sale_cart', 'vin', '`vin` VARCHAR(64) DEFAULT NULL COMMENT ''vin''');

DROP PROCEDURE IF EXISTS add_sale_cart_column_if_missing_v154;

DROP PROCEDURE IF EXISTS add_sale_cart_field_config_visible_v154;

DELIMITER //
CREATE PROCEDURE add_sale_cart_field_config_visible_v154()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'visible'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `visible` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否显示'
            AFTER `required`;
    END IF;
END //
DELIMITER ;

CALL add_sale_cart_field_config_visible_v154();
DROP PROCEDURE IF EXISTS add_sale_cart_field_config_visible_v154;

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module_key`, seed.`field_name`, seed.`field_label`, seed.`required`, b'1', seed.`sort`, '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 'sale_cart' module_key, 'deliveryMethod' field_name, _utf8mb4 0xE98081E8B4A7E696B9E5BC8F field_label, b'0' required, 115 sort UNION ALL
    SELECT 'sale_cart', 'vin', _utf8mb4 0x56494EE7A081, b'0', 135
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_field_config` cfg
    WHERE cfg.`tenant_id` = 1
      AND cfg.`module_key` = seed.`module_key`
      AND cfg.`field_name` = seed.`field_name`
      AND cfg.`deleted` = b'0'
);

UPDATE `erp_field_config`
SET `field_label` = _utf8mb4 0xE98081E8B4A7E696B9E5BC8F,
    `updater` = '1',
    `update_time` = NOW()
WHERE `tenant_id` = 1
  AND `module_key` = 'sale_cart'
  AND `field_name` = 'deliveryMethod'
  AND `deleted` = b'0'
  AND `field_label` <> _utf8mb4 0xE98081E8B4A7E696B9E5BC8F;

UPDATE `erp_field_config`
SET `field_label` = _utf8mb4 0x56494EE7A081,
    `updater` = '1',
    `update_time` = NOW()
WHERE `tenant_id` = 1
  AND `module_key` = 'sale_cart'
  AND `field_name` = 'vin'
  AND `deleted` = b'0'
  AND `field_label` <> _utf8mb4 0x56494EE7A081;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module`, seed.`field_key`, seed.`field_label`, seed.`field_group`, seed.`sort`, '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 'erp_sale_cart' module, 'deliveryMethod' field_key, _utf8mb4 0xE98081E8B4A7E696B9E5BC8F field_label, 'main_form' field_group, 115 sort UNION ALL
    SELECT 'erp_sale_cart', 'vin', _utf8mb4 0x56494EE7A081, 'main_form', 135
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_field_definition` perm
    WHERE perm.`tenant_id` = 1
      AND perm.`module` = seed.`module`
      AND perm.`field_key` = seed.`field_key`
      AND perm.`field_group` = seed.`field_group`
      AND perm.`deleted` = b'0'
);

UPDATE `system_field_definition`
SET `field_label` = _utf8mb4 0xE98081E8B4A7E696B9E5BC8F,
    `updater` = '1',
    `update_time` = NOW()
WHERE `tenant_id` = 1
  AND `module` = 'erp_sale_cart'
  AND `field_key` = 'deliveryMethod'
  AND `field_group` = 'main_form'
  AND `deleted` = b'0'
  AND `field_label` <> _utf8mb4 0xE98081E8B4A7E696B9E5BC8F;

UPDATE `system_field_definition`
SET `field_label` = _utf8mb4 0x56494EE7A081,
    `updater` = '1',
    `update_time` = NOW()
WHERE `tenant_id` = 1
  AND `module` = 'erp_sale_cart'
  AND `field_key` = 'vin'
  AND `field_group` = 'main_form'
  AND `deleted` = b'0'
  AND `field_label` <> _utf8mb4 0x56494EE7A081;
