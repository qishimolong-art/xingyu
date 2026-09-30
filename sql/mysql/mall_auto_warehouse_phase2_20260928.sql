-- 小程序第二阶段：按收货位置自动分仓
-- 1. 订单保存下单时目的地坐标快照；2. 购物车按客户、销售部门、SKU 合并并建立活动记录唯一约束。
-- 脚本可重复执行。重复购物车会先备份到 trade_cart_auto_warehouse_backup_20260928。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_column_if_not_exists(
    IN tableName VARCHAR(64), IN columnName VARCHAR(64), IN columnSql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = columnName
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnSql);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_column_if_not_exists('trade_order', 'receiver_longitude',
    'DECIMAL(10,6) NULL COMMENT ''下单时目的地经度（GCJ-02）'' AFTER `receiver_detail_address`');
CALL add_column_if_not_exists('trade_order', 'receiver_latitude',
    'DECIMAL(10,6) NULL COMMENT ''下单时目的地纬度（GCJ-02）'' AFTER `receiver_longitude`');

-- 在生成列加入购物车前创建备份表，确保备份表可直接保存原始行。
CREATE TABLE IF NOT EXISTS `trade_cart_auto_warehouse_backup_20260928` LIKE `trade_cart`;

-- 只为非删除记录生成唯一键值；历史软删除记录不会相互冲突。
CALL add_column_if_not_exists('trade_cart', 'auto_warehouse_active_key',
    'TINYINT GENERATED ALWAYS AS (IF(`deleted` = b''0'', 1, NULL)) STORED COMMENT ''自动分仓活动购物车唯一键''');
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

DROP TEMPORARY TABLE IF EXISTS tmp_trade_cart_auto_warehouse_merge;
CREATE TEMPORARY TABLE tmp_trade_cart_auto_warehouse_merge AS
SELECT MIN(`id`) AS keeper_id,
       `tenant_id`, `user_id`, `customer_id`, `dept_id`, `sku_id`,
       SUM(`count`) AS merged_count,
       MIN(CAST(`selected` AS UNSIGNED)) AS merged_selected,
       COUNT(*) AS row_count
  FROM `trade_cart`
 WHERE `deleted` = b'0'
   AND `customer_id` IS NOT NULL
   AND `dept_id` IS NOT NULL
 GROUP BY `tenant_id`, `user_id`, `customer_id`, `dept_id`, `sku_id`
HAVING COUNT(*) > 1;

START TRANSACTION;

INSERT IGNORE INTO `trade_cart_auto_warehouse_backup_20260928`
    (`id`, `user_id`, `customer_id`, `dept_id`, `spu_id`, `sku_id`, `stock_id`, `erp_product_id`,
     `warehouse_id`, `count`, `selected`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT cart.`id`, cart.`user_id`, cart.`customer_id`, cart.`dept_id`, cart.`spu_id`, cart.`sku_id`,
       cart.`stock_id`, cart.`erp_product_id`, cart.`warehouse_id`, cart.`count`, cart.`selected`,
       cart.`creator`, cart.`create_time`, cart.`updater`, cart.`update_time`, cart.`deleted`, cart.`tenant_id`
  FROM `trade_cart` cart
  JOIN tmp_trade_cart_auto_warehouse_merge merge_row
    ON merge_row.`tenant_id` = cart.`tenant_id`
   AND merge_row.`user_id` = cart.`user_id`
   AND merge_row.`customer_id` = cart.`customer_id`
   AND merge_row.`dept_id` = cart.`dept_id`
   AND merge_row.`sku_id` = cart.`sku_id`
 WHERE cart.`deleted` = b'0';

UPDATE `trade_cart` cart
  JOIN tmp_trade_cart_auto_warehouse_merge merge_row ON merge_row.`keeper_id` = cart.`id`
   SET cart.`count` = merge_row.`merged_count`,
       cart.`selected` = IF(merge_row.`merged_selected` = 1, b'1', b'0'),
       cart.`stock_id` = NULL,
       cart.`erp_product_id` = NULL,
       cart.`warehouse_id` = NULL,
       cart.`updater` = IFNULL(cart.`updater`, '1'),
       cart.`update_time` = NOW();

UPDATE `trade_cart` cart
  JOIN tmp_trade_cart_auto_warehouse_merge merge_row
    ON merge_row.`tenant_id` = cart.`tenant_id`
   AND merge_row.`user_id` = cart.`user_id`
   AND merge_row.`customer_id` = cart.`customer_id`
   AND merge_row.`dept_id` = cart.`dept_id`
   AND merge_row.`sku_id` = cart.`sku_id`
   SET cart.`deleted` = b'1',
       cart.`updater` = IFNULL(cart.`updater`, '1'),
       cart.`update_time` = NOW()
 WHERE cart.`id` <> merge_row.`keeper_id`
   AND cart.`deleted` = b'0';

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_trade_cart_auto_warehouse_merge;

DROP PROCEDURE IF EXISTS add_unique_index_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_unique_index_if_not_exists(
    IN tableName VARCHAR(64), IN indexName VARCHAR(64), IN indexSql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND INDEX_NAME = indexName
    ) THEN
        SET @ddl = CONCAT('CREATE UNIQUE INDEX `', indexName, '` ON `', tableName, '` ', indexSql);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_unique_index_if_not_exists('trade_cart', 'uk_trade_cart_auto_warehouse_active',
    '(`tenant_id`, `user_id`, `customer_id`, `dept_id`, `sku_id`, `auto_warehouse_active_key`)');
DROP PROCEDURE IF EXISTS add_unique_index_if_not_exists;

-- 执行后核对：应为 0 行。
SELECT `tenant_id`, `user_id`, `customer_id`, `dept_id`, `sku_id`, COUNT(*) AS duplicate_count
  FROM `trade_cart`
 WHERE `deleted` = b'0'
 GROUP BY `tenant_id`, `user_id`, `customer_id`, `dept_id`, `sku_id`
HAVING COUNT(*) > 1;
