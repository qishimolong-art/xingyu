-- 商城 SPU 配件参数字段（v192）
-- 安全说明：
--   - 仅给 product_spu 增加可空字段，不删除、不覆盖原有字段。
--   - 历史数据只从 ERP 同步映射回填当前为空的字段，并带 tenant_id + deleted 条件。
--   - 不在 SQL 中自动创建/重写品牌档案；真实品牌通过重新执行 ERP 商品同步写入 brand_id。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_mall_spu_part_param_column_v192;

DELIMITER //
CREATE PROCEDURE add_mall_spu_part_param_column_v192(IN columnName VARCHAR(64), IN columnDefinition VARCHAR(512))
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'product_spu'
          AND COLUMN_NAME = columnName
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `product_spu` ADD COLUMN `', columnName, '` ', columnDefinition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_mall_spu_part_param_column_v192('code', 'varchar(64) DEFAULT NULL COMMENT ''配件编码'' AFTER `brand_id`');
CALL add_mall_spu_part_param_column_v192('standard', 'varchar(128) DEFAULT NULL COMMENT ''配件规格'' AFTER `code`');
CALL add_mall_spu_part_param_column_v192('feature_code', 'varchar(64) DEFAULT NULL COMMENT ''配件特征码'' AFTER `standard`');
CALL add_mall_spu_part_param_column_v192('vehicle_model', 'varchar(128) DEFAULT NULL COMMENT ''适用车型'' AFTER `feature_code`');

DROP PROCEDURE IF EXISTS add_mall_spu_part_param_column_v192;

DROP PROCEDURE IF EXISTS backfill_mall_spu_part_params_v192;

DELIMITER //
CREATE PROCEDURE backfill_mall_spu_part_params_v192()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_mall_product_mapping')
       AND EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product')
       AND EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product' AND COLUMN_NAME = 'code')
       AND EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product' AND COLUMN_NAME = 'standard')
       AND EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product' AND COLUMN_NAME = 'feature_code')
       AND EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product' AND COLUMN_NAME = 'vehicle_model') THEN

        IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product' AND COLUMN_NAME = 'vehicle_model_text') THEN
            SET @vehicleModelExpr = 'COALESCE(NULLIF(TRIM(product.`vehicle_model`), ''''), NULLIF(TRIM(product.`vehicle_model_text`), ''''))';
        ELSE
            SET @vehicleModelExpr = 'NULLIF(TRIM(product.`vehicle_model`), '''')';
        END IF;

        SET @backfillSql = CONCAT(
            'UPDATE `product_spu` spu ',
            'JOIN `erp_mall_product_mapping` mapping ',
            '  ON mapping.`mall_spu_id` = spu.`id` ',
            ' AND mapping.`tenant_id` = spu.`tenant_id` ',
            ' AND mapping.`deleted` = b''0'' ',
            'JOIN `erp_product` product ',
            '  ON product.`id` = mapping.`erp_product_id` ',
            ' AND product.`tenant_id` = mapping.`tenant_id` ',
            ' AND product.`deleted` = b''0'' ',
            'SET spu.`code` = IF(NULLIF(TRIM(spu.`code`), '''') IS NULL, NULLIF(TRIM(product.`code`), ''''), spu.`code`), ',
            '    spu.`standard` = IF(NULLIF(TRIM(spu.`standard`), '''') IS NULL, NULLIF(TRIM(product.`standard`), ''''), spu.`standard`), ',
            '    spu.`feature_code` = IF(NULLIF(TRIM(spu.`feature_code`), '''') IS NULL, NULLIF(TRIM(product.`feature_code`), ''''), spu.`feature_code`), ',
            '    spu.`vehicle_model` = IF(NULLIF(TRIM(spu.`vehicle_model`), '''') IS NULL, ', @vehicleModelExpr, ', spu.`vehicle_model`), ',
            '    spu.`updater` = ''1'', ',
            '    spu.`update_time` = NOW() ',
            'WHERE spu.`deleted` = b''0'' ',
            '  AND (',
            '       (NULLIF(TRIM(spu.`code`), '''') IS NULL AND NULLIF(TRIM(product.`code`), '''') IS NOT NULL) ',
            '    OR (NULLIF(TRIM(spu.`standard`), '''') IS NULL AND NULLIF(TRIM(product.`standard`), '''') IS NOT NULL) ',
            '    OR (NULLIF(TRIM(spu.`feature_code`), '''') IS NULL AND NULLIF(TRIM(product.`feature_code`), '''') IS NOT NULL) ',
            '    OR (NULLIF(TRIM(spu.`vehicle_model`), '''') IS NULL AND ', @vehicleModelExpr, ' IS NOT NULL)',
            '  )'
        );
        PREPARE stmt FROM @backfillSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL backfill_mall_spu_part_params_v192();

DROP PROCEDURE IF EXISTS backfill_mall_spu_part_params_v192;
