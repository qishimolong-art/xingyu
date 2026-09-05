-- ERP warehouse location fields v209.
-- Adds optional GCJ-02 map location fields and field-permission definitions.
-- Safe to execute repeatedly; no broad deletes or role-permission overwrites.

DROP PROCEDURE IF EXISTS add_erp_warehouse_location_columns_v209;

DELIMITER //
CREATE PROCEDURE add_erp_warehouse_location_columns_v209()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_warehouse'
           AND COLUMN_NAME = 'longitude'
    ) THEN
        ALTER TABLE `erp_warehouse`
            ADD COLUMN `longitude` DECIMAL(10,6) NULL COMMENT '仓库经度（GCJ-02）'
            AFTER `address`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_warehouse'
           AND COLUMN_NAME = 'latitude'
    ) THEN
        ALTER TABLE `erp_warehouse`
            ADD COLUMN `latitude` DECIMAL(10,6) NULL COMMENT '仓库纬度（GCJ-02）'
            AFTER `longitude`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_warehouse'
           AND COLUMN_NAME = 'map_name'
    ) THEN
        ALTER TABLE `erp_warehouse`
            ADD COLUMN `map_name` VARCHAR(100) NULL COMMENT '地图显示名称'
            AFTER `latitude`;
    END IF;
END //
DELIMITER ;

CALL add_erp_warehouse_location_columns_v209();
DROP PROCEDURE IF EXISTS add_erp_warehouse_location_columns_v209;

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `list_visible`, `searchable`, `readonly`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module_key`, seed.`field_name`, seed.`field_label`, seed.`required`, b'1', seed.`sort`,
       seed.`field_group`, 'SYSTEM', seed.`list_visible`, b'0', b'0', '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 'erp_warehouse' module_key, 'address' field_name, '仓库地址' field_label, b'0' required, 45 sort, 'base_info' field_group, b'1' list_visible
    UNION ALL
    SELECT 'erp_warehouse', 'mapName', '地图名称', b'0', 46, 'base_info', b'1'
    UNION ALL
    SELECT 'erp_warehouse', 'longitude', '经度', b'0', 47, 'base_info', b'1'
    UNION ALL
    SELECT 'erp_warehouse', 'latitude', '纬度', b'0', 48, 'base_info', b'1'
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

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_warehouse', 'address', '仓库地址', 'base_info', 45, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'mapName', '地图名称', 'base_info', 46, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'longitude', '经度', 'base_info', 47, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'latitude', '纬度', 'base_info', 48, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'col_address', '列表-仓库地址', 'list_col', 410, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'col_mapName', '列表-地图名称', 'list_col', 420, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'col_longitude', '列表-经度', 'list_col', 430, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'col_latitude', '列表-纬度', 'list_col', 440, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
