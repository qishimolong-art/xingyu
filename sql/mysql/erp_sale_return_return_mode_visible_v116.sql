-- Restore sale return mode visibility.
-- Safe to execute repeatedly. This only touches the sale_return.returnMode field.

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('sale_return', 'returnMode', '退货模式', b'1', b'1', 3, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `required` = b'1',
  `visible` = b'1',
  `sort` = VALUES(`sort`),
  `update_time` = NOW();

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_sale_return', 'returnMode', '退货模式', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `update_time` = NOW();
