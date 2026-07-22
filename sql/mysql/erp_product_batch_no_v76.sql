-- ERP product batch number management v76
-- Adds product-level batch number management switch and field permission definition.

DROP PROCEDURE IF EXISTS add_erp_product_batch_no_enabled_v76;

CREATE PROCEDURE add_erp_product_batch_no_enabled_v76()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'erp_product'
          AND column_name = 'batch_no_enabled'
    ) THEN
        ALTER TABLE `erp_product`
            ADD COLUMN `batch_no_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启批次号管理' AFTER `category_id`;
    END IF;
END;

CALL add_erp_product_batch_no_enabled_v76();
DROP PROCEDURE IF EXISTS add_erp_product_batch_no_enabled_v76;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_product', 'batchNoEnabled', '是否开启批次号管理', 'base_info', 65, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';

UPDATE `system_field_definition`
SET `field_label` = '配件分类',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `module` = 'erp_product'
  AND `field_key` = 'categoryId'
  AND `deleted` = b'0';

UPDATE `system_field_definition`
SET `field_label` = '列表-配件分类',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `module` = 'erp_product'
  AND `field_key` = 'col_categoryName'
  AND `deleted` = b'0';
