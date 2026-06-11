-- v55: stock move source tracking fields.
-- Purpose: support source traceability and idempotency for stock move drafts generated from sale carts.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

DROP PROCEDURE IF EXISTS add_erp_stock_move_column_if_missing;
DROP PROCEDURE IF EXISTS add_erp_stock_move_index_if_missing;

DELIMITER //
CREATE PROCEDURE add_erp_stock_move_column_if_missing(
    IN columnName VARCHAR(64),
    IN columnSql TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
          AND COLUMN_NAME = columnName
    ) THEN
        SET @addColumnSql = CONCAT('ALTER TABLE `erp_stock_move` ADD COLUMN ', columnSql);
        PREPARE stmt FROM @addColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

CREATE PROCEDURE add_erp_stock_move_index_if_missing(
    IN indexName VARCHAR(64),
    IN indexSql TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
          AND INDEX_NAME = indexName
    ) THEN
        SET @addIndexSql = CONCAT('ALTER TABLE `erp_stock_move` ADD INDEX `', indexName, '` ', indexSql);
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_erp_stock_move_column_if_missing('source_type', '`source_type` INT DEFAULT NULL COMMENT ''source type''');
CALL add_erp_stock_move_column_if_missing('source_id', '`source_id` BIGINT DEFAULT NULL COMMENT ''source id''');
CALL add_erp_stock_move_column_if_missing('source_no', '`source_no` VARCHAR(64) DEFAULT NULL COMMENT ''source no''');

CALL add_erp_stock_move_index_if_missing('idx_stock_move_source_type_id', '(`source_type`, `source_id`)');

DROP PROCEDURE IF EXISTS add_erp_stock_move_column_if_missing;
DROP PROCEDURE IF EXISTS add_erp_stock_move_index_if_missing;
