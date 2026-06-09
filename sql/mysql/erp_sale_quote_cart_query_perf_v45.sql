-- ERP sale quote/cart query performance indexes v45.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

DROP PROCEDURE IF EXISTS add_erp_index_if_missing;

DELIMITER //
CREATE PROCEDURE add_erp_index_if_missing(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64),
    IN indexSql TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND INDEX_NAME = indexName
    ) THEN
        SET @addIndexSql = CONCAT('CREATE INDEX `', indexName, '` ON `', tableName, '` ', indexSql);
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- Sale quote list filters and id desc pagination.
CALL add_erp_index_if_missing('erp_sale_quote', 'idx_sale_quote_status_id', '(`status`, `id`)');
CALL add_erp_index_if_missing('erp_sale_quote', 'idx_sale_quote_quote_time_id', '(`quote_time`, `id`)');
CALL add_erp_index_if_missing('erp_sale_quote', 'idx_sale_quote_customer_id_id', '(`customer_id`, `id`)');
CALL add_erp_index_if_missing('erp_sale_quote', 'idx_sale_quote_sale_user_id_id', '(`sale_user_id`, `id`)');
CALL add_erp_index_if_missing('erp_sale_quote', 'idx_sale_quote_dept_id_id', '(`dept_id`, `id`)');

-- Sale cart list filters and id desc pagination.
CALL add_erp_index_if_missing('erp_sale_cart', 'idx_sale_cart_status_id', '(`status`, `id`)');
CALL add_erp_index_if_missing('erp_sale_cart', 'idx_sale_cart_cart_time_id', '(`cart_time`, `id`)');
CALL add_erp_index_if_missing('erp_sale_cart', 'idx_sale_cart_customer_id_id', '(`customer_id`, `id`)');
CALL add_erp_index_if_missing('erp_sale_cart', 'idx_sale_cart_sale_user_id_id', '(`sale_user_id`, `id`)');
CALL add_erp_index_if_missing('erp_sale_cart', 'idx_sale_cart_dept_id_id', '(`dept_id`, `id`)');

-- Reverse lookup generated sale out by business source.
CALL add_erp_index_if_missing('erp_sale_out', 'idx_sale_out_source_type_id', '(`source_type`, `source_id`)');

DROP PROCEDURE IF EXISTS add_erp_index_if_missing;
