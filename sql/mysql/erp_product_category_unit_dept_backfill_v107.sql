-- ERP product category/unit dept_id repair (v107).
-- Safe to execute repeatedly on MySQL 5.7 / 8.0.x.
--
-- Scope:
--   1. Ensure erp_product_category.dept_id and erp_product_unit.dept_id exist.
--   2. Fill only rows where dept_id is NULL, using the creator user's current dept.
--   3. Keep existing non-null dept_id values unchanged.
--
-- Rows that still have NULL dept_id after this script need manual branch attribution.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_product_category_unit_dept_v107;

DELIMITER //
CREATE PROCEDURE add_erp_product_category_unit_dept_v107(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tableName
    ) THEN
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = tableName
               AND COLUMN_NAME = 'dept_id'
        ) THEN
            SET @addColumnSql = CONCAT('ALTER TABLE `', tableName,
                                       '` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''Department id''');
            PREPARE stmt FROM @addColumnSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = tableName
               AND INDEX_NAME = 'idx_dept_id'
        ) THEN
            SET @addIndexSql = CONCAT('CREATE INDEX `idx_dept_id` ON `', tableName, '` (`dept_id`)');
            PREPARE stmt FROM @addIndexSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //
DELIMITER ;

CALL add_erp_product_category_unit_dept_v107('erp_product_category');
CALL add_erp_product_category_unit_dept_v107('erp_product_unit');

DROP PROCEDURE IF EXISTS add_erp_product_category_unit_dept_v107;

DROP PROCEDURE IF EXISTS backfill_erp_product_category_unit_dept_v107;

DELIMITER //
CREATE PROCEDURE backfill_erp_product_category_unit_dept_v107(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tableName
           AND COLUMN_NAME = 'dept_id'
    ) AND EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tableName
           AND COLUMN_NAME = 'creator'
    ) AND EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tableName
           AND COLUMN_NAME = 'deleted'
    ) AND EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tableName
           AND COLUMN_NAME = 'tenant_id'
    ) THEN
        SET @backfillSql = CONCAT(
            'UPDATE `', tableName, '` t ',
            'JOIN `system_users` u ',
            '  ON t.`creator` REGEXP ''^[0-9]+$'' ',
            ' AND CAST(t.`creator` AS UNSIGNED) = u.`id` ',
            ' AND t.`tenant_id` = u.`tenant_id` ',
            ' AND u.`deleted` = b''0'' ',
            'SET t.`dept_id` = u.`dept_id` ',
            'WHERE t.`dept_id` IS NULL ',
            '  AND t.`deleted` = b''0'' ',
            '  AND u.`dept_id` IS NOT NULL'
        );
        PREPARE stmt FROM @backfillSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL backfill_erp_product_category_unit_dept_v107('erp_product_category');
CALL backfill_erp_product_category_unit_dept_v107('erp_product_unit');

DROP PROCEDURE IF EXISTS backfill_erp_product_category_unit_dept_v107;

DROP TEMPORARY TABLE IF EXISTS erp_product_category_unit_dept_missing_result;
CREATE TEMPORARY TABLE erp_product_category_unit_dept_missing_result (
    table_name VARCHAR(64) NOT NULL,
    missing_dept_count BIGINT NOT NULL
);

DROP PROCEDURE IF EXISTS collect_erp_product_category_unit_dept_missing_v107;

DELIMITER //
CREATE PROCEDURE collect_erp_product_category_unit_dept_missing_v107(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tableName
           AND COLUMN_NAME = 'dept_id'
    ) THEN
        SET @countSql = CONCAT(
            'INSERT INTO erp_product_category_unit_dept_missing_result(table_name, missing_dept_count) ',
            'SELECT ''', tableName, ''', COUNT(*) FROM `', tableName, '` WHERE dept_id IS NULL'
        );
        PREPARE stmt FROM @countSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL collect_erp_product_category_unit_dept_missing_v107('erp_product_category');
CALL collect_erp_product_category_unit_dept_missing_v107('erp_product_unit');

DROP PROCEDURE IF EXISTS collect_erp_product_category_unit_dept_missing_v107;

SELECT * FROM erp_product_category_unit_dept_missing_result ORDER BY table_name;
