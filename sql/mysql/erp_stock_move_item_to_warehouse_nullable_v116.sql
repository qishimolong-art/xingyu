-- v116: allow stock move drafts to leave to warehouse empty.
-- Purpose: quote-to-cart cross-department sale flow creates a stock move draft before users choose the transfer-in warehouse.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

DROP PROCEDURE IF EXISTS modify_stock_move_item_to_warehouse_nullable;

DELIMITER //
CREATE PROCEDURE modify_stock_move_item_to_warehouse_nullable()
BEGIN
    DECLARE columnType TEXT;
    DECLARE columnComment TEXT;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move_item'
          AND COLUMN_NAME = 'to_warehouse_id'
          AND IS_NULLABLE = 'NO'
    ) THEN
        SELECT COLUMN_TYPE, COLUMN_COMMENT
        INTO columnType, columnComment
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move_item'
          AND COLUMN_NAME = 'to_warehouse_id';

        SET @modifyColumnSql = CONCAT(
            'ALTER TABLE `erp_stock_move_item` MODIFY COLUMN `to_warehouse_id` ',
            columnType,
            ' NULL COMMENT ',
            QUOTE(columnComment)
        );
        PREPARE stmt FROM @modifyColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL modify_stock_move_item_to_warehouse_nullable();

DROP PROCEDURE IF EXISTS modify_stock_move_item_to_warehouse_nullable;
