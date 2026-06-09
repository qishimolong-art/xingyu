-- Add warehouse item fields to sale quote and sale cart field permissions.
-- Execute this if role field permission configuration is enabled.

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_sale_quote', 'item_warehouseId', '仓库', 'detail_item', 410, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_cart', 'item_warehouseId', '仓库', 'detail_item', 410, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
