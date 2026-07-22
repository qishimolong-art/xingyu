-- ERP purchase invoice import permission patch (v102)
-- Adds the missing import button permission for purchase invoice.
--
-- Safety:
--   - Append-only for active role-menu relations. No DELETE and no full replacement.
--   - Existing menu id 3298 is reused only for the exact import permission.
--   - Existing roles that already own purchase invoice query/create/update/export are granted import.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3298, '采购票据导入', 'erp:purchase-invoice:import', 3, 7, 3291, '', '', '', NULL,
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
SELECT DISTINCT owned_role.role_id,
       import_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM system_menu import_menu
JOIN system_role_menu owned_role
  ON owned_role.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.id = owned_role.menu_id
 AND owned_menu.deleted = b'0'
 AND owned_menu.permission COLLATE utf8mb4_unicode_ci IN (
       'erp:purchase-invoice:query' COLLATE utf8mb4_unicode_ci,
       'erp:purchase-invoice:create' COLLATE utf8mb4_unicode_ci,
       'erp:purchase-invoice:update' COLLATE utf8mb4_unicode_ci,
       'erp:purchase-invoice:export' COLLATE utf8mb4_unicode_ci
 )
WHERE import_menu.permission COLLATE utf8mb4_unicode_ci = 'erp:purchase-invoice:import' COLLATE utf8mb4_unicode_ci
  AND import_menu.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu target
      WHERE target.role_id = owned_role.role_id
        AND target.menu_id = import_menu.id
        AND target.tenant_id = owned_role.tenant_id
        AND target.deleted = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       import_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM system_role super_role
JOIN system_menu import_menu
  ON import_menu.permission COLLATE utf8mb4_unicode_ci = 'erp:purchase-invoice:import' COLLATE utf8mb4_unicode_ci
 AND import_menu.deleted = b'0'
WHERE super_role.code COLLATE utf8mb4_unicode_ci = 'super_admin' COLLATE utf8mb4_unicode_ci
  AND super_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu target
      WHERE target.role_id = super_role.id
        AND target.menu_id = import_menu.id
        AND target.tenant_id = super_role.tenant_id
        AND target.deleted = b'0'
  );

SELECT m.id,
       m.name,
       m.permission,
       m.parent_id,
       m.sort,
       COUNT(DISTINCT rm.role_id) AS granted_role_count
FROM system_menu m
LEFT JOIN system_role_menu rm
  ON rm.menu_id = m.id
 AND rm.deleted = b'0'
WHERE m.permission COLLATE utf8mb4_unicode_ci = 'erp:purchase-invoice:import' COLLATE utf8mb4_unicode_ci
  AND m.deleted = b'0'
GROUP BY m.id, m.name, m.permission, m.parent_id, m.sort;
