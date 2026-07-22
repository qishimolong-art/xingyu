-- ERP purchase order item dept field (v108).
-- Adds a detail-level dept_id after warehouse_id and backfills existing rows from the parent order.

DROP PROCEDURE IF EXISTS add_purchase_order_item_dept_column;
DELIMITER //
CREATE PROCEDURE add_purchase_order_item_dept_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_purchase_order_items'
           AND COLUMN_NAME = 'dept_id'
    ) THEN
        ALTER TABLE `erp_purchase_order_items`
            ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门' AFTER `warehouse_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_purchase_order_items'
           AND INDEX_NAME = 'idx_dept_id'
    ) THEN
        CREATE INDEX `idx_dept_id` ON `erp_purchase_order_items` (`dept_id`);
    END IF;
END//
DELIMITER ;
CALL add_purchase_order_item_dept_column();
DROP PROCEDURE IF EXISTS add_purchase_order_item_dept_column;

UPDATE `erp_purchase_order_items` item
JOIN `erp_purchase_order` orders
  ON orders.`id` = item.`order_id`
 AND orders.`tenant_id` = item.`tenant_id`
 AND orders.`deleted` = b'0'
SET item.`dept_id` = orders.`dept_id`
WHERE item.`dept_id` IS NULL
  AND orders.`dept_id` IS NOT NULL
  AND item.`deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_purchase_order', 'item_deptId', '所属部门', 'detail_item', 245, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
