-- ERP import permission patch (v33)
-- Adds missing import button menus and grants them to roles that already own
-- the corresponding ERP form/menu permissions.

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(9001, '产品导入', 'erp:product:import', 3, 6, 2565, '', '', '', '',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(9000, '供应商导入', 'erp:supplier:import', 3, 6, 2603, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(9002, '客户导入', 'erp:customer:import', 3, 6, 2618, '', '', '', NULL,
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

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source.role_id, source.import_menu_id, '1', NOW(), '1', NOW(), b'0', source.tenant_id
FROM (
    SELECT role_id, tenant_id, 9001 AS import_menu_id
    FROM `system_role_menu`
    WHERE `menu_id` IN (2565, 2566, 2567, 2568, 2569, 2570)
      AND `deleted` = b'0'

    UNION

    SELECT role_id, tenant_id, 9000 AS import_menu_id
    FROM `system_role_menu`
    WHERE `menu_id` IN (2603, 2604, 2605, 2606, 2607, 2608)
      AND `deleted` = b'0'

    UNION

    SELECT role_id, tenant_id, 9002 AS import_menu_id
    FROM `system_role_menu`
    WHERE `menu_id` IN (2618, 2619, 2620, 2621, 2622, 2623)
      AND `deleted` = b'0'
) source
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` target
    WHERE target.`role_id` = source.role_id
      AND target.`menu_id` = source.import_menu_id
      AND target.`tenant_id` = source.tenant_id
      AND target.`deleted` = b'0'
);

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT admin_role.role_id, import_menu.menu_id, '1', NOW(), '1', NOW(), b'0', admin_role.tenant_id
FROM (
    SELECT 1 AS role_id, 1 AS tenant_id
) admin_role
CROSS JOIN (
    SELECT 9001 AS menu_id
    UNION ALL SELECT 9000
    UNION ALL SELECT 9002
) import_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` target
    WHERE target.`role_id` = admin_role.role_id
      AND target.`menu_id` = import_menu.menu_id
      AND target.`tenant_id` = admin_role.tenant_id
      AND target.`deleted` = b'0'
);
