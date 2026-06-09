-- ERP data permission department columns.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

DROP PROCEDURE IF EXISTS add_erp_dept_column;

DELIMITER //
CREATE PROCEDURE add_erp_dept_column(IN tableName VARCHAR(64))
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
                                       '` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''所属部门''');
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

CALL add_erp_dept_column('erp_supplier');
CALL add_erp_dept_column('erp_purchase_suggestion');
CALL add_erp_dept_column('erp_sale_order');
CALL add_erp_dept_column('erp_finance_payment');
CALL add_erp_dept_column('erp_finance_receipt');
CALL add_erp_dept_column('erp_finance_transfer');
CALL add_erp_dept_column('erp_stock_check');
CALL add_erp_dept_column('erp_stock_in');
CALL add_erp_dept_column('erp_stock_move');
CALL add_erp_dept_column('erp_stock_out');
CALL add_erp_dept_column('erp_stock_record');
CALL add_erp_dept_column('erp_chain_order');
CALL add_erp_dept_column('erp_other_payable');
CALL add_erp_dept_column('erp_other_receivable');
CALL add_erp_dept_column('erp_pre_payment');
CALL add_erp_dept_column('erp_pre_receipt');
CALL add_erp_dept_column('erp_pre_receivable');

DROP PROCEDURE IF EXISTS add_erp_dept_column;

-- Historical data backfill.
-- Review these updates in a backup or staging database first. If historical rows were created by imports,
-- creator may not represent the real owning branch; correct those rows manually before production acceptance.

UPDATE erp_sale_order t
JOIN system_users u ON u.id = t.sale_user_id
SET t.dept_id = u.dept_id
WHERE t.dept_id IS NULL AND u.dept_id IS NOT NULL;

UPDATE erp_finance_payment t
JOIN system_users u ON u.id = t.finance_user_id
SET t.dept_id = u.dept_id
WHERE t.dept_id IS NULL AND u.dept_id IS NOT NULL;

UPDATE erp_finance_receipt t
JOIN system_users u ON u.id = t.finance_user_id
SET t.dept_id = u.dept_id
WHERE t.dept_id IS NULL AND u.dept_id IS NOT NULL;

UPDATE erp_finance_transfer t
JOIN system_users u ON u.id = t.finance_user_id
SET t.dept_id = u.dept_id
WHERE t.dept_id IS NULL AND u.dept_id IS NOT NULL;

DROP PROCEDURE IF EXISTS backfill_erp_dept_from_creator;

DELIMITER //
CREATE PROCEDURE backfill_erp_dept_from_creator(IN tableName VARCHAR(64))
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

CALL backfill_erp_dept_from_creator('erp_supplier');
CALL backfill_erp_dept_from_creator('erp_purchase_suggestion');
CALL backfill_erp_dept_from_creator('erp_sale_order');
CALL backfill_erp_dept_from_creator('erp_finance_payment');
CALL backfill_erp_dept_from_creator('erp_finance_receipt');
CALL backfill_erp_dept_from_creator('erp_finance_transfer');
CALL backfill_erp_dept_from_creator('erp_stock_check');
CALL backfill_erp_dept_from_creator('erp_stock_in');
CALL backfill_erp_dept_from_creator('erp_stock_move');
CALL backfill_erp_dept_from_creator('erp_stock_out');
CALL backfill_erp_dept_from_creator('erp_stock_record');
CALL backfill_erp_dept_from_creator('erp_chain_order');
CALL backfill_erp_dept_from_creator('erp_other_payable');
CALL backfill_erp_dept_from_creator('erp_other_receivable');
CALL backfill_erp_dept_from_creator('erp_pre_payment');
CALL backfill_erp_dept_from_creator('erp_pre_receipt');
CALL backfill_erp_dept_from_creator('erp_pre_receivable');

DROP PROCEDURE IF EXISTS backfill_erp_dept_from_creator;

-- Rows still missing dept_id after this query need manual branch attribution.
DROP TEMPORARY TABLE IF EXISTS erp_dept_missing_result;
CREATE TEMPORARY TABLE erp_dept_missing_result (
    table_name VARCHAR(64) NOT NULL,
    missing_dept_count BIGINT NOT NULL
);

DROP PROCEDURE IF EXISTS collect_erp_dept_missing_count;

DELIMITER //
CREATE PROCEDURE collect_erp_dept_missing_count(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) THEN
        SET @countSql = CONCAT(
            'INSERT INTO erp_dept_missing_result(table_name, missing_dept_count) ',
            'SELECT ''', tableName, ''', COUNT(*) FROM `', tableName, '` WHERE dept_id IS NULL'
        );
        PREPARE stmt FROM @countSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL collect_erp_dept_missing_count('erp_supplier');
CALL collect_erp_dept_missing_count('erp_purchase_suggestion');
CALL collect_erp_dept_missing_count('erp_sale_order');
CALL collect_erp_dept_missing_count('erp_finance_payment');
CALL collect_erp_dept_missing_count('erp_finance_receipt');
CALL collect_erp_dept_missing_count('erp_finance_transfer');
CALL collect_erp_dept_missing_count('erp_stock_check');
CALL collect_erp_dept_missing_count('erp_stock_in');
CALL collect_erp_dept_missing_count('erp_stock_move');
CALL collect_erp_dept_missing_count('erp_stock_out');
CALL collect_erp_dept_missing_count('erp_stock_record');
CALL collect_erp_dept_missing_count('erp_chain_order');
CALL collect_erp_dept_missing_count('erp_other_payable');
CALL collect_erp_dept_missing_count('erp_other_receivable');
CALL collect_erp_dept_missing_count('erp_pre_payment');
CALL collect_erp_dept_missing_count('erp_pre_receipt');
CALL collect_erp_dept_missing_count('erp_pre_receivable');

DROP PROCEDURE IF EXISTS collect_erp_dept_missing_count;

SELECT * FROM erp_dept_missing_result ORDER BY table_name;
