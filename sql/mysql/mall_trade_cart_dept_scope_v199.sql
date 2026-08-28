-- 小程序购物车按客户与部门隔离（v199）
-- Safe to rerun. This script only adds columns/indexes and backfills rows whose
-- customer and sale department can be determined uniquely; it does not delete
-- or overwrite already scoped cart rows.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

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

CALL add_column_if_not_exists('trade_cart', 'customer_id',
    'BIGINT NULL COMMENT ''ERP 客户编号'' AFTER `user_id`');
CALL add_column_if_not_exists('trade_cart', 'dept_id',
    'BIGINT NULL COMMENT ''购物车所属部门编号'' AFTER `customer_id`');

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

DROP PROCEDURE IF EXISTS add_index_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_index_if_not_exists(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64),
    IN indexSql VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND INDEX_NAME = indexName
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX `', indexName, '` ON `', tableName, '` ', indexSql);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_index_if_not_exists('trade_cart', 'idx_trade_cart_scope',
    '(`tenant_id`, `user_id`, `customer_id`, `dept_id`)');
CALL add_index_if_not_exists('trade_cart', 'idx_trade_cart_scope_sku_stock',
    '(`tenant_id`, `user_id`, `customer_id`, `dept_id`, `sku_id`, `stock_id`)');

DROP PROCEDURE IF EXISTS add_index_if_not_exists;

DROP TEMPORARY TABLE IF EXISTS tmp_trade_cart_single_dept_scope;
CREATE TEMPORARY TABLE tmp_trade_cart_single_dept_scope (
    `member_user_id` BIGINT NOT NULL,
    `customer_id` BIGINT NOT NULL,
    `dept_id` BIGINT NOT NULL,
    `tenant_id` BIGINT NOT NULL,
    PRIMARY KEY (`member_user_id`, `tenant_id`)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_trade_cart_single_dept_scope (`member_user_id`, `customer_id`, `dept_id`, `tenant_id`)
SELECT cm.`member_user_id`,
       MAX(cm.`customer_id`) AS `customer_id`,
       MAX(available_dept.`dept_id`) AS `dept_id`,
       cm.`tenant_id`
  FROM `erp_customer_member` cm
  JOIN (
        SELECT c.`id` AS `customer_id`, c.`dept_id`, c.`tenant_id`
          FROM `erp_customer` c
         WHERE c.`deleted` = b'0'
           AND c.`dept_id` IS NOT NULL
        UNION ALL
        SELECT cd.`customer_id`, cd.`dept_id`, cd.`tenant_id`
          FROM `erp_customer_dept` cd
          JOIN `erp_customer` c ON c.`id` = cd.`customer_id`
                               AND c.`tenant_id` = cd.`tenant_id`
                               AND c.`deleted` = b'0'
                               AND c.`allow_multi_dept` = b'1'
         WHERE cd.`deleted` = b'0'
           AND cd.`dept_id` IS NOT NULL
       ) available_dept ON available_dept.`customer_id` = cm.`customer_id`
                       AND available_dept.`tenant_id` = cm.`tenant_id`
 WHERE cm.`deleted` = b'0'
   AND cm.`status` = 0
 GROUP BY cm.`member_user_id`, cm.`tenant_id`
HAVING COUNT(DISTINCT cm.`customer_id`) = 1
   AND COUNT(DISTINCT available_dept.`dept_id`) = 1;

UPDATE `trade_cart` cart
  JOIN tmp_trade_cart_single_dept_scope scope
    ON scope.`member_user_id` = cart.`user_id`
   AND scope.`tenant_id` = cart.`tenant_id`
   SET cart.`customer_id` = scope.`customer_id`,
       cart.`dept_id` = scope.`dept_id`,
       cart.`updater` = IFNULL(cart.`updater`, '1'),
       cart.`update_time` = NOW()
 WHERE cart.`deleted` = b'0'
   AND cart.`customer_id` IS NULL
   AND cart.`dept_id` IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_trade_cart_single_dept_scope;
