-- ERP finance export menu permission patch (v64)
-- Scope:
--   Add missing export button permission records for finance receivable/payable pages.
--
-- Safety:
--   - Idempotent and append-only.
--   - Does not delete menus or replace role permissions.
--   - Existing role-menu permissions are not overwritten.
--
-- After execution:
--   Refresh menu/permission cache or let affected users re-login.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '其他应收导出',
  'erp:receivable-other:export',
  3,
  6,
  parent_menu.id,
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
FROM `system_menu` parent_menu
WHERE parent_menu.id = 3102
  AND parent_menu.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu` existing
      WHERE existing.permission = 'erp:receivable-other:export'
        AND existing.deleted = b'0'
  )
LIMIT 1;

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '其他收入导出',
  'erp:receivable-other-income:export',
  3,
  6,
  parent_menu.id,
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
FROM `system_menu` parent_menu
WHERE parent_menu.id = 3108
  AND parent_menu.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu` existing
      WHERE existing.permission = 'erp:receivable-other-income:export'
        AND existing.deleted = b'0'
  )
LIMIT 1;

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '其他应付导出',
  'erp:payable-other:export',
  3,
  6,
  parent_menu.id,
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
FROM `system_menu` parent_menu
WHERE parent_menu.id = 31102
  AND parent_menu.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu` existing
      WHERE existing.permission = 'erp:payable-other:export'
        AND existing.deleted = b'0'
  )
LIMIT 1;

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '费用支付导出',
  'erp:payable-expense:export',
  3,
  6,
  parent_menu.id,
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
FROM `system_menu` parent_menu
WHERE parent_menu.id = 31108
  AND parent_menu.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu` existing
      WHERE existing.permission = 'erp:payable-expense:export'
        AND existing.deleted = b'0'
  )
LIMIT 1;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  1,
  menu.id,
  '1',
  NOW(),
  '1',
  NOW(),
  b'0',
  1
FROM `system_menu` menu
WHERE menu.permission IN (
    'erp:receivable-other:export',
    'erp:receivable-other-income:export',
    'erp:payable-other:export',
    'erp:payable-expense:export'
)
  AND menu.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` role_menu
      WHERE role_menu.role_id = 1
        AND role_menu.menu_id = menu.id
        AND role_menu.tenant_id = 1
        AND role_menu.deleted = b'0'
  );
