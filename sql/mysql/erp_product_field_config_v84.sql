-- ERP product field-config seed fix v84.
-- Fixes the field-config page showing no rows for module_key = 'erp_product'.
-- Safe to execute repeatedly: only missing active tenant-1 rows are inserted.

DROP PROCEDURE IF EXISTS add_erp_product_field_config_columns_v84;

DELIMITER //
CREATE PROCEDURE add_erp_product_field_config_columns_v84()
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
          AND COLUMN_NAME = 'field_source'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_source` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'field source: SYSTEM/CUSTOM'
            AFTER `sort`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'physical_column'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `physical_column` varchar(64) NULL COMMENT 'custom physical column'
            AFTER `field_source`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_type'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_type` varchar(32) NULL COMMENT 'field type'
            AFTER `physical_column`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_group'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_group` varchar(64) NULL COMMENT 'field group'
            AFTER `field_type`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'component_type'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `component_type` varchar(64) NULL COMMENT 'frontend component type'
            AFTER `field_group`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'max_length'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `max_length` int NULL COMMENT 'max length'
            AFTER `component_type`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'decimal_precision'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `decimal_precision` int NULL COMMENT 'decimal precision'
            AFTER `max_length`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'decimal_scale'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `decimal_scale` int NULL COMMENT 'decimal scale'
            AFTER `decimal_precision`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'default_value'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `default_value` varchar(500) NULL COMMENT 'default value'
            AFTER `decimal_scale`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'list_visible'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `list_visible` bit(1) NOT NULL DEFAULT b'0' COMMENT 'list visible'
            AFTER `default_value`;
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

CALL add_erp_product_field_config_columns_v84();
DROP PROCEDURE IF EXISTS add_erp_product_field_config_columns_v84;

UPDATE `erp_field_config`
   SET `field_source` = 'SYSTEM'
 WHERE (`field_source` IS NULL OR `field_source` = '')
   AND `deleted` = b'0';

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `list_visible`, `searchable`, `readonly`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module_key`, seed.`field_name`, seed.`field_label`, seed.`required`, b'1', seed.`sort`,
       seed.`field_group`, 'SYSTEM', b'0', b'0', seed.`readonly`, '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 'erp_product' module_key, 'code' field_name, '配件编码' field_label, b'0' required, 10 sort, 'base_info' field_group, b'0' readonly UNION ALL
    SELECT 'erp_product', 'manualInputCode', '手动输入', b'0', 20, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'name', '配件名称', b'1', 30, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'unitId', '单位', b'1', 40, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'defaultWarehouseId', '默认仓库', b'1', 50, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'vehicleModel', '适用车型', b'0', 60, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'standard', '规格', b'0', 70, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'categoryId', '配件分类', b'1', 80, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'batchNoEnabled', '是否开启批次号管理', b'0', 90, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'deptIds', '所属部门', b'0', 100, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'barCode', '条形码', b'0', 110, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'factoryCode', '厂家编码', b'0', 120, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'status', '状态', b'0', 130, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'remark', '备注', b'0', 140, 'base_info', b'0' UNION ALL
    SELECT 'erp_product', 'referencePrice', '参考价', b'0', 210, 'price_info', b'0' UNION ALL
    SELECT 'erp_product', 'retailPrice', '零售价', b'0', 220, 'price_info', b'0' UNION ALL
    SELECT 'erp_product', 'lastPurchasePrice', '最后采购入库价', b'0', 230, 'price_info', b'1' UNION ALL
    SELECT 'erp_product', 'grossProfitRate', '毛利率（%）', b'0', 240, 'price_info', b'0' UNION ALL
    SELECT 'erp_product', 'backupPrice1', '备用价', b'0', 250, 'price_info', b'0' UNION ALL
    SELECT 'erp_product', 'wholesalePrice', '批发价', b'0', 260, 'price_info', b'0' UNION ALL
    SELECT 'erp_product', 'sharePrice', '股份价', b'0', 270, 'price_info', b'0' UNION ALL
    SELECT 'erp_product', 'stockMax', '库存上限', b'0', 310, 'extend_info', b'0' UNION ALL
    SELECT 'erp_product', 'stockMin', '库存下限', b'0', 320, 'extend_info', b'0' UNION ALL
    SELECT 'erp_product', 'stockStandard', '标准库存', b'0', 330, 'extend_info', b'0' UNION ALL
    SELECT 'erp_product', 'packageQty', '包装数', b'0', 340, 'extend_info', b'0' UNION ALL
    SELECT 'erp_product', 'weight', '重量（kg）', b'0', 350, 'extend_info', b'0' UNION ALL
    SELECT 'erp_product', 'currentStock', '当前库存', b'0', 410, 'stock_info', b'1' UNION ALL
    SELECT 'erp_product', 'inTransitStock', '在途数量', b'0', 420, 'stock_info', b'1' UNION ALL
    SELECT 'erp_product', 'availableStock', '可用库存', b'0', 430, 'stock_info', b'1'
) seed
WHERE NOT EXISTS (
    SELECT 1
      FROM `erp_field_config` cfg
     WHERE cfg.`module_key` = seed.`module_key`
       AND cfg.`field_name` = seed.`field_name`
       AND cfg.`tenant_id` = 1
       AND cfg.`deleted` = b'0'
);
