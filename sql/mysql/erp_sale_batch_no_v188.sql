-- 为销售订单、销售退货明细补充批次号字段，并补充字段权限配置。
-- 适用版本: MySQL 5.7+

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

CREATE TABLE IF NOT EXISTS `system_role_field_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `role_id` BIGINT NOT NULL COMMENT 'Role id',
  `field_id` BIGINT NOT NULL COMMENT 'Field definition id',
  `hidden` BIT(1) NOT NULL DEFAULT b'1' COMMENT 'Whether hidden',
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_field` (`role_id`, `field_id`, `tenant_id`, `deleted`),
  KEY `idx_field_id` (`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System role field permission';

DROP PROCEDURE IF EXISTS erp_sale_batch_no_v188_apply;
DELIMITER $$
CREATE PROCEDURE erp_sale_batch_no_v188_apply()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_sale_order_items'
          AND COLUMN_NAME = 'batch_no'
    ) THEN
        ALTER TABLE erp_sale_order_items
            ADD COLUMN batch_no VARCHAR(64) DEFAULT NULL COMMENT '批次号' AFTER dept_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_sale_return_items'
          AND COLUMN_NAME = 'batch_no'
    ) THEN
        ALTER TABLE erp_sale_return_items
            ADD COLUMN batch_no VARCHAR(64) DEFAULT NULL COMMENT '批次号' AFTER dept_id;
    END IF;
END$$
DELIMITER ;
CALL erp_sale_batch_no_v188_apply();
DROP PROCEDURE IF EXISTS erp_sale_batch_no_v188_apply;

INSERT INTO `system_field_definition` (
    `module`, `field_key`, `field_label`, `field_group`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 'erp_sale_order', 'item_batchNo', '批次号', 'detail_item', 445,
       '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `system_field_definition`
    WHERE `module` = 'erp_sale_order'
      AND `field_key` = 'item_batchNo'
      AND `deleted` = b'0'
      AND `tenant_id` = 1
);

INSERT INTO `system_field_definition` (
    `module`, `field_key`, `field_label`, `field_group`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 'erp_sale_return', 'item_batchNo', '批次号', 'detail_item', 415,
       '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `system_field_definition`
    WHERE `module` = 'erp_sale_return'
      AND `field_key` = 'item_batchNo'
      AND `deleted` = b'0'
      AND `tenant_id` = 1
);

INSERT INTO `system_field_definition` (
    `module`, `field_key`, `field_label`, `field_group`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 'erp_sale_price_adjust', 'item_batchNo', '批次号', 'detail_item', 465,
       '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `system_field_definition`
    WHERE `module` = 'erp_sale_price_adjust'
      AND `field_key` = 'item_batchNo'
      AND `deleted` = b'0'
      AND `tenant_id` = 1
);

INSERT INTO `system_field_definition` (
    `module`, `field_key`, `field_label`, `field_group`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 'erp_sale_price_adjust', 'select_col_batchNo', '批次号', 'select_modal', 735,
       '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `system_field_definition`
    WHERE `module` = 'erp_sale_price_adjust'
      AND `field_key` = 'select_col_batchNo'
      AND `deleted` = b'0'
      AND `tenant_id` = 1
);
