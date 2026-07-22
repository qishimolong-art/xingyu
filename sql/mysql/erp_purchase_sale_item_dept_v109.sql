-- ERP purchase/sale detail dept fields (v109).
-- Adds detail-level dept_id to purchase/sale item tables. Tables with warehouse_id place it after warehouse_id;
-- sale price-adjust items place it after product_id because they reference sale-out items instead of warehouse_id.

DROP PROCEDURE IF EXISTS add_item_dept_column;
DELIMITER //
CREATE PROCEDURE add_item_dept_column(IN table_name_value VARCHAR(128))
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = table_name_value
           AND COLUMN_NAME = 'dept_id'
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', table_name_value,
            '` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''所属部门'' AFTER `warehouse_id`'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = table_name_value
           AND INDEX_NAME = 'idx_dept_id'
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX `idx_dept_id` ON `', table_name_value, '` (`dept_id`)');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS add_sale_order_item_warehouse_column;
DELIMITER //
CREATE PROCEDURE add_sale_order_item_warehouse_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_sale_order_items'
           AND COLUMN_NAME = 'warehouse_id'
    ) THEN
        ALTER TABLE `erp_sale_order_items`
            ADD COLUMN `warehouse_id` BIGINT DEFAULT NULL COMMENT '所属仓库' AFTER `product_unit_id`;
    END IF;
END//
DELIMITER ;

DROP PROCEDURE IF EXISTS add_sale_price_adjust_item_dept_column;
DELIMITER //
CREATE PROCEDURE add_sale_price_adjust_item_dept_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_sale_price_adjust_item'
           AND COLUMN_NAME = 'dept_id'
    ) THEN
        ALTER TABLE `erp_sale_price_adjust_item`
            ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门' AFTER `product_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_sale_price_adjust_item'
           AND INDEX_NAME = 'idx_dept_id'
    ) THEN
        CREATE INDEX `idx_dept_id` ON `erp_sale_price_adjust_item` (`dept_id`);
    END IF;
END//
DELIMITER ;

CALL add_item_dept_column('erp_purchase_in_items');
CALL add_item_dept_column('erp_purchase_return_items');
CALL add_item_dept_column('erp_purchase_price_adjust_item');
CALL add_item_dept_column('erp_sale_quote_items');
CALL add_item_dept_column('erp_sale_cart_items');
CALL add_sale_order_item_warehouse_column();
CALL add_item_dept_column('erp_sale_order_items');
CALL add_item_dept_column('erp_sale_out_items');
CALL add_item_dept_column('erp_sale_return_items');
CALL add_sale_price_adjust_item_dept_column();

DROP PROCEDURE IF EXISTS add_item_dept_column;
DROP PROCEDURE IF EXISTS add_sale_order_item_warehouse_column;
DROP PROCEDURE IF EXISTS add_sale_price_adjust_item_dept_column;

UPDATE `erp_purchase_in_items` item
JOIN `erp_warehouse` warehouse
  ON warehouse.`id` = item.`warehouse_id`
 AND warehouse.`tenant_id` = item.`tenant_id`
 AND warehouse.`deleted` = b'0'
SET item.`dept_id` = warehouse.`dept_id`
WHERE item.`dept_id` IS NULL
  AND warehouse.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_purchase_return_items` item
JOIN `erp_warehouse` warehouse
  ON warehouse.`id` = item.`warehouse_id`
 AND warehouse.`tenant_id` = item.`tenant_id`
 AND warehouse.`deleted` = b'0'
SET item.`dept_id` = warehouse.`dept_id`
WHERE item.`dept_id` IS NULL
  AND warehouse.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_purchase_price_adjust_item` item
JOIN `erp_warehouse` warehouse
  ON warehouse.`id` = item.`warehouse_id`
 AND warehouse.`tenant_id` = item.`tenant_id`
 AND warehouse.`deleted` = b'0'
SET item.`dept_id` = warehouse.`dept_id`
WHERE item.`dept_id` IS NULL
  AND warehouse.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_quote_items` item
JOIN `erp_warehouse` warehouse
  ON warehouse.`id` = item.`warehouse_id`
 AND warehouse.`tenant_id` = item.`tenant_id`
 AND warehouse.`deleted` = b'0'
SET item.`dept_id` = warehouse.`dept_id`
WHERE item.`dept_id` IS NULL
  AND warehouse.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_cart_items` item
JOIN `erp_warehouse` warehouse
  ON warehouse.`id` = item.`warehouse_id`
 AND warehouse.`tenant_id` = item.`tenant_id`
 AND warehouse.`deleted` = b'0'
SET item.`dept_id` = warehouse.`dept_id`
WHERE item.`dept_id` IS NULL
  AND warehouse.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_order_items` item
JOIN `erp_warehouse` warehouse
  ON warehouse.`id` = item.`warehouse_id`
 AND warehouse.`tenant_id` = item.`tenant_id`
 AND warehouse.`deleted` = b'0'
SET item.`dept_id` = warehouse.`dept_id`
WHERE item.`dept_id` IS NULL
  AND warehouse.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_out_items` item
JOIN `erp_warehouse` warehouse
  ON warehouse.`id` = item.`warehouse_id`
 AND warehouse.`tenant_id` = item.`tenant_id`
 AND warehouse.`deleted` = b'0'
SET item.`dept_id` = warehouse.`dept_id`
WHERE item.`dept_id` IS NULL
  AND warehouse.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_return_items` item
JOIN `erp_warehouse` warehouse
  ON warehouse.`id` = item.`warehouse_id`
 AND warehouse.`tenant_id` = item.`tenant_id`
 AND warehouse.`deleted` = b'0'
SET item.`dept_id` = warehouse.`dept_id`
WHERE item.`dept_id` IS NULL
  AND warehouse.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_price_adjust_item` item
JOIN `erp_sale_out_items` out_item
  ON out_item.`id` = item.`sale_out_item_id`
 AND out_item.`tenant_id` = item.`tenant_id`
 AND out_item.`deleted` = b'0'
SET item.`dept_id` = out_item.`dept_id`
WHERE item.`dept_id` IS NULL
  AND out_item.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_purchase_in_items` item
JOIN `erp_purchase_in` main
  ON main.`id` = item.`in_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_purchase_return_items` item
JOIN `erp_purchase_return` main
  ON main.`id` = item.`return_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_purchase_price_adjust_item` item
JOIN `erp_purchase_price_adjust` main
  ON main.`id` = item.`adjust_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_quote_items` item
JOIN `erp_sale_quote` main
  ON main.`id` = item.`quote_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_cart_items` item
JOIN `erp_sale_cart` main
  ON main.`id` = item.`cart_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_order_items` item
JOIN `erp_sale_order` main
  ON main.`id` = item.`order_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_out_items` item
JOIN `erp_sale_out` main
  ON main.`id` = item.`out_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_return_items` item
JOIN `erp_sale_return` main
  ON main.`id` = item.`return_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

UPDATE `erp_sale_price_adjust_item` item
JOIN `erp_sale_price_adjust` main
  ON main.`id` = item.`adjust_id`
 AND main.`tenant_id` = item.`tenant_id`
 AND main.`deleted` = b'0'
SET item.`dept_id` = main.`dept_id`
WHERE item.`dept_id` IS NULL
  AND main.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_purchase_in', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1),
('erp_purchase_return', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1),
('erp_purchase_price_adjust', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_quote', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_cart', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_order', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_out', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_return', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_price_adjust', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
