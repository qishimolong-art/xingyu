-- ERP base data stable business code (v80).
-- Adds an optional code column for frontend option mapping without changing
-- legacy option values.

DROP PROCEDURE IF EXISTS add_erp_base_data_code_v80;

DELIMITER //
CREATE PROCEDURE add_erp_base_data_code_v80()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_base_data'
          AND COLUMN_NAME = 'code'
    ) THEN
        ALTER TABLE `erp_base_data`
            ADD COLUMN `code` varchar(64) DEFAULT NULL COMMENT '稳定业务编码' AFTER `name`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_base_data'
          AND INDEX_NAME = 'idx_type_code'
    ) THEN
        ALTER TABLE `erp_base_data`
            ADD INDEX `idx_type_code` (`type`, `code`);
    END IF;
END//
DELIMITER ;

CALL add_erp_base_data_code_v80();
DROP PROCEDURE IF EXISTS add_erp_base_data_code_v80;
