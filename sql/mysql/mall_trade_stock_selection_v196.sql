-- 小程序普通购买选择 ERP 库存：购物车与订单项保存库存来源

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_column_if_not_exists(
    IN tableName VARCHAR(64),
    IN columnName VARCHAR(64),
    IN columnSql VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND COLUMN_NAME = columnName
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnSql);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_column_if_not_exists('trade_cart', 'stock_id',
    'BIGINT NULL COMMENT ''ERP 库存记录编号'' AFTER `sku_id`');
CALL add_column_if_not_exists('trade_cart', 'erp_product_id',
    'BIGINT NULL COMMENT ''ERP 产品编号'' AFTER `stock_id`');
CALL add_column_if_not_exists('trade_cart', 'warehouse_id',
    'BIGINT NULL COMMENT ''ERP 仓库编号'' AFTER `erp_product_id`');

CALL add_column_if_not_exists('trade_order_item', 'stock_id',
    'BIGINT NULL COMMENT ''ERP 库存记录编号'' AFTER `sku_id`');
CALL add_column_if_not_exists('trade_order_item', 'erp_product_id',
    'BIGINT NULL COMMENT ''ERP 产品编号'' AFTER `stock_id`');
CALL add_column_if_not_exists('trade_order_item', 'warehouse_id',
    'BIGINT NULL COMMENT ''ERP 仓库编号'' AFTER `erp_product_id`');

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
