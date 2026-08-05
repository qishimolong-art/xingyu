-- ERP 调拨出库单草稿允许暂未填写调拨时间。
-- 正式创建、草稿提交仍由 VO / Service 执行严格必填校验。
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

DROP PROCEDURE IF EXISTS modify_stock_transfer_out_move_time_nullable_v144;

DELIMITER //
CREATE PROCEDURE modify_stock_transfer_out_move_time_nullable_v144()
BEGIN
    DECLARE columnType TEXT;
    DECLARE columnComment TEXT;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
          AND COLUMN_NAME = 'move_time'
          AND IS_NULLABLE = 'NO'
    ) THEN
        SELECT COLUMN_TYPE, COLUMN_COMMENT
        INTO columnType, columnComment
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
          AND COLUMN_NAME = 'move_time';

        SET @modifyColumnSql = CONCAT(
            'ALTER TABLE `erp_stock_move` MODIFY COLUMN `move_time` ',
            columnType,
            ' NULL DEFAULT NULL COMMENT ',
            QUOTE(columnComment)
        );
        PREPARE stmt FROM @modifyColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL modify_stock_transfer_out_move_time_nullable_v144();

DROP PROCEDURE IF EXISTS modify_stock_transfer_out_move_time_nullable_v144;
