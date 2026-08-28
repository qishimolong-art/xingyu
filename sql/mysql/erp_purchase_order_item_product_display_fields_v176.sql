-- ERP purchase order detail product display fields (v176).
-- Registers product weight and package quantity as detail display/print/export fields.

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_purchase_order', 'item_weight', '重量', 'detail_item', 315, '1', NOW(), '1', NOW(), b'0', 1),
('erp_purchase_order', 'item_packageQty', '包装数', 'detail_item', 316, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
