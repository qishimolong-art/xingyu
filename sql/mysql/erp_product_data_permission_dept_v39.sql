-- ERP product management data permission department columns.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.
-- Scope: product, product category, product unit, price system, product price system, product universal.

DROP PROCEDURE IF EXISTS add_erp_product_dept_column;

DELIMITER //
CREATE PROCEDURE add_erp_product_dept_column(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
        ) THEN
            SET @addColumnSql = CONCAT('ALTER TABLE `', tableName,
                                       '` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''Department id''');
            PREPARE stmt FROM @addColumnSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND INDEX_NAME = 'idx_dept_id'
        ) THEN
            SET @addIndexSql = CONCAT('CREATE INDEX `idx_dept_id` ON `', tableName, '` (`dept_id`)');
            PREPARE stmt FROM @addIndexSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //
DELIMITER ;

CALL add_erp_product_dept_column('erp_product');
CALL add_erp_product_dept_column('erp_product_category');
CALL add_erp_product_dept_column('erp_product_unit');
CALL add_erp_product_dept_column('erp_price_system');
CALL add_erp_product_dept_column('erp_product_price_system');
CALL add_erp_product_dept_column('erp_product_universal');

DROP PROCEDURE IF EXISTS add_erp_product_dept_column;

-- Historical data backfill.
-- Review in a backup or staging database first. Rows left with dept_id = NULL after this script
-- must be assigned manually according to the real branch/company ownership.

DROP PROCEDURE IF EXISTS backfill_erp_product_dept_from_creator;

DELIMITER //
CREATE PROCEDURE backfill_erp_product_dept_from_creator(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'creator'
    ) THEN
        SET @backfillSql = CONCAT(
            'UPDATE `', tableName, '` t ',
            'JOIN system_users u ON t.creator REGEXP ''^[0-9]+$'' AND CAST(t.creator AS UNSIGNED) = u.id ',
            'SET t.dept_id = u.dept_id ',
            'WHERE t.dept_id IS NULL AND u.dept_id IS NOT NULL'
        );
        PREPARE stmt FROM @backfillSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL backfill_erp_product_dept_from_creator('erp_product');
CALL backfill_erp_product_dept_from_creator('erp_product_category');
CALL backfill_erp_product_dept_from_creator('erp_product_unit');
CALL backfill_erp_product_dept_from_creator('erp_price_system');
CALL backfill_erp_product_dept_from_creator('erp_product_price_system');
CALL backfill_erp_product_dept_from_creator('erp_product_universal');

DROP PROCEDURE IF EXISTS backfill_erp_product_dept_from_creator;

-- Product category inherits dept_id from parent category when creator backfill did not resolve it.
DROP PROCEDURE IF EXISTS backfill_erp_product_category_dept_from_parent;

DELIMITER //
CREATE PROCEDURE backfill_erp_product_category_dept_from_parent()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product_category'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product_category' AND COLUMN_NAME = 'dept_id'
    ) THEN
        UPDATE erp_product_category c
        JOIN erp_product_category p ON p.id = c.parent_id
        SET c.dept_id = p.dept_id
        WHERE c.dept_id IS NULL AND p.dept_id IS NOT NULL;
    END IF;
END //
DELIMITER ;

CALL backfill_erp_product_category_dept_from_parent();
CALL backfill_erp_product_category_dept_from_parent();
CALL backfill_erp_product_category_dept_from_parent();
CALL backfill_erp_product_category_dept_from_parent();
CALL backfill_erp_product_category_dept_from_parent();
CALL backfill_erp_product_category_dept_from_parent();
CALL backfill_erp_product_category_dept_from_parent();
CALL backfill_erp_product_category_dept_from_parent();

DROP PROCEDURE IF EXISTS backfill_erp_product_category_dept_from_parent;

-- Product inherits dept_id from category when creator backfill did not resolve it.
DROP PROCEDURE IF EXISTS backfill_erp_product_dept_from_category;

DELIMITER //
CREATE PROCEDURE backfill_erp_product_dept_from_category()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product_category'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product' AND COLUMN_NAME = 'dept_id'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product_category' AND COLUMN_NAME = 'dept_id'
    ) THEN
        UPDATE erp_product p
        JOIN erp_product_category c ON c.id = p.category_id
        SET p.dept_id = c.dept_id
        WHERE p.dept_id IS NULL AND c.dept_id IS NOT NULL;
    END IF;
END //
DELIMITER ;

CALL backfill_erp_product_dept_from_category();

DROP PROCEDURE IF EXISTS backfill_erp_product_dept_from_category;

-- Product child tables inherit dept_id from product.
DROP PROCEDURE IF EXISTS backfill_erp_product_child_dept_from_product;

DELIMITER //
CREATE PROCEDURE backfill_erp_product_child_dept_from_product(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'product_id'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_product' AND COLUMN_NAME = 'dept_id'
    ) THEN
        SET @backfillSql = CONCAT(
            'UPDATE `', tableName, '` t ',
            'JOIN erp_product p ON p.id = t.product_id ',
            'SET t.dept_id = p.dept_id ',
            'WHERE t.dept_id IS NULL AND p.dept_id IS NOT NULL'
        );
        PREPARE stmt FROM @backfillSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL backfill_erp_product_child_dept_from_product('erp_product_price_system');
CALL backfill_erp_product_child_dept_from_product('erp_product_universal');

DROP PROCEDURE IF EXISTS backfill_erp_product_child_dept_from_product;

-- Rows still missing dept_id after this query need manual branch attribution.
DROP TEMPORARY TABLE IF EXISTS erp_product_dept_missing_result;
CREATE TEMPORARY TABLE erp_product_dept_missing_result (
    table_name VARCHAR(64) NOT NULL,
    missing_dept_count BIGINT NOT NULL
);

DROP PROCEDURE IF EXISTS collect_erp_product_dept_missing_count;

DELIMITER //
CREATE PROCEDURE collect_erp_product_dept_missing_count(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) THEN
        SET @countSql = CONCAT(
            'INSERT INTO erp_product_dept_missing_result(table_name, missing_dept_count) ',
            'SELECT ''', tableName, ''', COUNT(*) FROM `', tableName, '` WHERE dept_id IS NULL'
        );
        PREPARE stmt FROM @countSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL collect_erp_product_dept_missing_count('erp_product');
CALL collect_erp_product_dept_missing_count('erp_product_category');
CALL collect_erp_product_dept_missing_count('erp_product_unit');
CALL collect_erp_product_dept_missing_count('erp_price_system');
CALL collect_erp_product_dept_missing_count('erp_product_price_system');
CALL collect_erp_product_dept_missing_count('erp_product_universal');

DROP PROCEDURE IF EXISTS collect_erp_product_dept_missing_count;

SELECT * FROM erp_product_dept_missing_result ORDER BY table_name;
