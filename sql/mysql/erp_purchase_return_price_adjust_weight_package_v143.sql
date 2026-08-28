-- 用途：补齐采购退货、采购调价明细的重量/包装数字段和字段权限
-- 日期：2026-08-18

DROP PROCEDURE IF EXISTS add_erp_purchase_detail_column_if_missing;

DELIMITER $$
CREATE PROCEDURE add_erp_purchase_detail_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
           AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_erp_purchase_detail_column_if_missing(
    'erp_purchase_return_items',
    'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''重量'''
);

CALL add_erp_purchase_detail_column_if_missing(
    'erp_purchase_price_adjust_item',
    'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''重量'''
);

CALL add_erp_purchase_detail_column_if_missing(
    'erp_purchase_price_adjust_item',
    'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数'''
);

DROP PROCEDURE IF EXISTS add_erp_purchase_detail_column_if_missing;

CREATE TABLE IF NOT EXISTS `system_field_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `module` VARCHAR(50) NOT NULL COMMENT 'Module key',
  `field_key` VARCHAR(100) NOT NULL COMMENT 'Field key',
  `field_label` VARCHAR(100) NOT NULL COMMENT 'Field label',
  `field_group` VARCHAR(50) DEFAULT NULL COMMENT 'Field group',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort',
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_field` (`module`, `field_key`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System field definition';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_purchase_return', 'item_weight', '重量', 'detail_item', 452, '1', NOW(), '1', NOW(), b'0', 1),
('erp_purchase_return', 'item_packageQty', '包装数', 'detail_item', 454, '1', NOW(), '1', NOW(), b'0', 1),
('erp_purchase_price_adjust', 'item_productUnitName', '单位', 'detail_item', 235, '1', NOW(), '1', NOW(), b'0', 1),
('erp_purchase_price_adjust', 'item_weight', '重量', 'detail_item', 236, '1', NOW(), '1', NOW(), b'0', 1),
('erp_purchase_price_adjust', 'item_packageQty', '包装数', 'detail_item', 237, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
