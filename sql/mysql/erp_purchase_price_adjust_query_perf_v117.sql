-- ERP purchase price adjust query performance indexes v117.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.
-- Scope: index-only patch for purchase price adjust list search.

DROP PROCEDURE IF EXISTS add_erp_purchase_price_adjust_index_if_missing;

DELIMITER //
CREATE PROCEDURE add_erp_purchase_price_adjust_index_if_missing(
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

-- Speeds up /erp/purchase-price-adjust/page when filtering by productId.
-- The list query joins erp_purchase_price_adjust_item by adjust_id and filters by product_id.
CALL add_erp_purchase_price_adjust_index_if_missing(
    'erp_purchase_price_adjust_item',
    'idx_ppa_item_product_tenant_deleted_adjust',
    '(`product_id`, `tenant_id`, `deleted`, `adjust_id`)'
);

-- Speeds up payment-status filtering and payment amount filling for purchase price adjust rows.
-- The query sums payment items by biz_type + biz_id under tenant/deleted guards.
CALL add_erp_purchase_price_adjust_index_if_missing(
    'erp_finance_payment_item',
    'idx_payment_item_biz_tenant_deleted',
    '(`biz_type`, `biz_id`, `tenant_id`, `deleted`)'
);

DROP PROCEDURE IF EXISTS add_erp_purchase_price_adjust_index_if_missing;
