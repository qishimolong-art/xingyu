-- ERP stock check department field permission supplement v117.
-- Safe to execute repeatedly. It only registers the detail display field
-- for warehouse owner department; it does not change business data.

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_stock_check', 'item_warehouseDeptName', '仓库所属部门', 'detail_item', 207, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
