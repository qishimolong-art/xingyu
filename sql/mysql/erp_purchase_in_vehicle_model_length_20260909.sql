-- ============================================================
-- 采购入库明细适用车型字段长度修复
-- 日期：2026-09-09
-- 背景：erp_product.vehicle_model 为 varchar(128)，采购入库导入会将商品车型带入
--       erp_purchase_in_items.vehicle_model。历史脚本中该字段为 varchar(64)，
--       当商品车型超过 64 字符时会触发 Data too long for column 'vehicle_model'。
-- ============================================================

SET @erp_purchase_in_vehicle_model_sql := (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN
            'ALTER TABLE `erp_purchase_in_items` ADD COLUMN `vehicle_model` VARCHAR(128) DEFAULT NULL COMMENT ''适用车型（从商品资料带出）'' AFTER `brand`'
        WHEN MAX(DATA_TYPE = 'varchar' AND CHARACTER_MAXIMUM_LENGTH < 128) = 1 THEN
            'ALTER TABLE `erp_purchase_in_items` MODIFY COLUMN `vehicle_model` VARCHAR(128) DEFAULT NULL COMMENT ''适用车型（从商品资料带出）'''
        ELSE
            'SELECT ''erp_purchase_in_items.vehicle_model length already >= 128'''
        END
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_purchase_in_items'
      AND COLUMN_NAME = 'vehicle_model'
);

PREPARE stmt FROM @erp_purchase_in_vehicle_model_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
