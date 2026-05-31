-- ERP 供应商导入权限补丁
-- 说明:
--   1. 供应商信息增加导入/模板下载入口
--   2. 需要先执行后端与前端代码更新
--   3. 执行后请刷新菜单缓存或重启后端，并重新登录前端

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(9000, '供应商导入', 'erp:supplier:import', 3, 6, 2603, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT
       role_id, 9000, '1', NOW(), '1', NOW(), b'0', tenant_id
FROM `system_role_menu`
WHERE `menu_id` IN (2603, 2604, 2605, 2606, 2607, 2608)
  AND `deleted` = b'0';

-- 兜底授权超管角色
INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 9000, '1', NOW(), '1', NOW(), b'0', 1);
