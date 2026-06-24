-- ERP sale cart reject menu permission patch (v59)
-- Scope:
--   1. Add the missing sales cart reject button permission to the menu tree.
--   2. Append the permission to the ERP approver role when that role exists.
--
-- Safety:
--   - Idempotent and append-only.
--   - Does not delete menus or replace role permissions.
--   - Existing role-menu permissions are not overwritten.
--
-- After execution:
--   - Refresh menu/permission cache or let affected users re-login.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  CONVERT(0xE99480E594AEE6898BE68EA8E8BDA6E9A9B3E59B9E USING utf8mb4),
  'erp:sale-cart:reject',
  3,
  8,
  2980,
  '',
  '',
  '',
  NULL,
  0,
  b'1',
  b'1',
  b'1',
  '1',
  NOW(),
  '1',
  NOW(),
  b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:sale-cart:reject'
      AND `deleted` = b'0'
)
  AND EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `id` = 2980
        AND `deleted` = b'0'
  );

UPDATE `system_menu`
SET `sort` = 9,
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'erp:sale-cart:convert-quote'
  AND `parent_id` = 2980
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `sort` = 10,
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'erp:sale-cart:export'
  AND `parent_id` = 2980
  AND `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  r.id,
  m.id,
  '1',
  NOW(),
  '1',
  NOW(),
  b'0',
  r.tenant_id
FROM `system_role` r
JOIN `system_menu` m
  ON m.permission = 'erp:sale-cart:reject'
 AND m.deleted = b'0'
WHERE r.code = 'erp_approver'
  AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
        AND rm.tenant_id = r.tenant_id
        AND rm.deleted = b'0'
  );
