-- Add handler user field for sale return maker filtering.
DROP PROCEDURE IF EXISTS erp_sale_return_handler_v81_apply;

DELIMITER $$
CREATE PROCEDURE erp_sale_return_handler_v81_apply()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_sale_return'
          AND COLUMN_NAME = 'handler'
    ) THEN
        ALTER TABLE erp_sale_return
            ADD COLUMN handler BIGINT DEFAULT NULL COMMENT '制单人（用户ID）';
    END IF;
END $$
DELIMITER ;

CALL erp_sale_return_handler_v81_apply();
DROP PROCEDURE IF EXISTS erp_sale_return_handler_v81_apply;

UPDATE erp_sale_return
SET handler = CAST(creator AS UNSIGNED)
WHERE handler IS NULL
  AND deleted = b'0'
  AND creator REGEXP '^[0-9]+$';
