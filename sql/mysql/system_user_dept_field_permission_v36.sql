-- System user/dept field permission definitions.
-- Scope: system_users and system_dept field definitions only.

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
('system_users', 'username', '用户名称', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'nickname', '用户昵称', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'deptIds', '归属部门', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'roleIds', '角色', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'email', '邮箱', 'main_form', 60, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'mobile', '手机号码', 'main_form', 70, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'sex', '用户性别', 'main_form', 80, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'status', '用户状态', 'main_form', 90, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'remark', '备注', 'main_form', 100, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_id', '用户编号', 'list_col', 110, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_username', '用户名称', 'list_col', 120, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_nickname', '用户昵称', 'list_col', 130, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_deptName', '部门', 'list_col', 140, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_roleNames', '角色', 'list_col', 150, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_email', '邮箱', 'list_col', 160, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_mobile', '手机号码', 'list_col', 170, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_sex', '用户性别', 'list_col', 180, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_status', '状态', 'list_col', 190, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_remark', '备注', 'list_col', 200, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'col_createTime', '创建时间', 'list_col', 210, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'search_username', '用户名称', 'search', 220, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'search_mobile', '手机号码', 'search', 230, '1', NOW(), '1', NOW(), b'0', 1),
('system_users', 'search_createTime', '创建时间', 'search', 240, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'parentId', '上级部门', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'name', '部门名称', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'sort', '显示顺序', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'leaderUserId', '负责人', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'phone', '联系电话', 'main_form', 50, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'email', '邮箱', 'main_form', 60, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'status', '状态', 'main_form', 70, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'col_name', '部门名称', 'list_col', 80, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'col_leaderUserId', '负责人', 'list_col', 90, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'col_sort', '显示顺序', 'list_col', 100, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'col_status', '部门状态', 'list_col', 110, '1', NOW(), '1', NOW(), b'0', 1),
('system_dept', 'col_createTime', '创建时间', 'list_col', 120, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
