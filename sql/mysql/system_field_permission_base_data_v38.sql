-- System field permission base data v38
-- Blacklist mode: records in system_role_field_permission mean hidden fields.

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_base_data', 'name', '名称', 'base_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_base_data', 'sort', '排序', 'base_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_base_data', 'status', '状态', 'status_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_base_data', 'remark', '备注', 'base_info', 40, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
