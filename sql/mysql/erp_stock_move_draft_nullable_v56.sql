-- v56: allow stock move drafts to leave from warehouse empty.
-- Purpose: sale cart stock-shortage drafts are created before users complete the transfer warehouse.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

DROP PROCEDURE IF EXISTS modify_stock_move_item_from_warehouse_nullable;

DELIMITER //
CREATE PROCEDURE modify_stock_move_item_from_warehouse_nullable()
BEGIN
    DECLARE columnType TEXT;
    DECLARE columnComment TEXT;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move_item'
          AND COLUMN_NAME = 'from_warehouse_id'
          AND IS_NULLABLE = 'NO'
    ) THEN
        SELECT COLUMN_TYPE, COLUMN_COMMENT
        INTO columnType, columnComment
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move_item'
          AND COLUMN_NAME = 'from_warehouse_id';

        SET @modifyColumnSql = CONCAT(
            'ALTER TABLE `erp_stock_move_item` MODIFY COLUMN `from_warehouse_id` ',
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

CALL modify_stock_move_item_from_warehouse_nullable();

DROP PROCEDURE IF EXISTS modify_stock_move_item_from_warehouse_nullable;
