-- ERP approver role and approval/audit button permissions (v54)
-- Scope:
--   1. Create or reuse role ERP Approver / erp_approver for one tenant.
--   2. Add missing positive approval/audit button menu permissions.
--   3. Append positive approval/audit menus to the role.
--   4. Optionally append the role to fixed users by username placeholders.
--
-- Safety:
--   - Append-only for role-menu and user-role relations. No DELETE and no full replacement.
--   - Uses NOT EXISTS to avoid duplicate role, menu, role-menu, and user-role rows.
--   - Does not grant erp:voucher:process.
--   - Explicit COLLATE is used on string comparisons to avoid MySQL 1267.
--
-- Before execution:
--   - Replace @tenant_id with the target tenant id.
--   - Replace username placeholders in tmp_erp_approver_usernames with real usernames,
--     or keep placeholders to skip user binding and bind users manually in the UI.
--   - Re-login or clear permission cache after execution.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @tenant_id := 1;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_approver_usernames;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_approver_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_approver_menu_sources;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_approver_menu_parent;

CREATE TEMPORARY TABLE tmp_erp_approver_usernames (
  username varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Replace these placeholders with fixed approver usernames.
-- Add more rows when multiple accounts need the ERP approver role.
-- Leave placeholders unchanged when user-role binding will be done manually.
INSERT IGNORE INTO tmp_erp_approver_usernames (username)
VALUES
('__REPLACE_USERNAME_1__'),
('__REPLACE_USERNAME_2__');

CREATE TEMPORARY TABLE tmp_erp_approver_permissions (
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_approver_permissions (permission)
VALUES
('erp:purchase-order:update-status'),
('erp:purchase-in:update-status'),
('erp:purchase-return:update-status'),
('erp:purchase-price-adjust:update-status'),
('erp:purchase-invoice:update-status'),
('erp:sale-quote:approve'),
('erp:sale-cart:first-approve'),
('erp:sale-cart:final-approve'),
('erp:sale-order:update-status'),
('erp:sale-out:update-status'),
('erp:sale-return:update-status'),
('erp:sale-price-adjust:update-status'),
('erp:stock-in:update-status'),
('erp:stock-out:update-status'),
('erp:stock-move:update-status'),
('erp:stock-check:update-status'),
('erp:finance-payment:update-status'),
('erp:finance-receipt:update-status'),
('erp:finance-transfer:update-status'),
('erp:payable-other:update-status'),
('erp:payable-expense:update-status'),
('erp:receivable-other:update-status'),
('erp:receivable-other-income:update-status'),
('erp:pre-payment:update-status'),
('erp:pre-receipt:update-status'),
('erp:pre-receivable:update-status'),
('erp:other-payable:update-status'),
('erp:other-receivable:update-status'),
('erp:voucher:audit');

-- Missing accounting approval/audit button menu definitions.
-- parent_id is located from existing query/create/update/delete buttons of the same module.
-- If the parent cannot be found, the row is skipped instead of hard-coding a menu id.
CREATE TEMPORARY TABLE tmp_erp_approver_menu_sources (
  name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  sort int NOT NULL,
  query_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  create_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  update_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  delete_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_approver_menu_sources
(name, permission, sort, query_perm, create_perm, update_perm, delete_perm)
VALUES
(CONVERT(0xE9A284E4BB98E6ACBEE5AEA1E6A0B8 USING utf8mb4), 'erp:pre-payment:update-status', 5, 'erp:pre-payment:query', 'erp:pre-payment:create', 'erp:pre-payment:update', 'erp:pre-payment:delete'),
(CONVERT(0xE9A284E694B6E6ACBEE5AEA1E6A0B8 USING utf8mb4), 'erp:pre-receipt:update-status', 5, 'erp:pre-receipt:query', 'erp:pre-receipt:create', 'erp:pre-receipt:update', 'erp:pre-receipt:delete'),
(CONVERT(0xE9A284E694B6E8B4A6E6ACBEE5AEA1E6A0B8 USING utf8mb4), 'erp:pre-receivable:update-status', 5, 'erp:pre-receivable:query', 'erp:pre-receivable:create', 'erp:pre-receivable:update', 'erp:pre-receivable:delete'),
(CONVERT(0xE585B6E4BB96E5BA94E4BB98E5AEA1E6A0B8 USING utf8mb4), 'erp:other-payable:update-status', 5, 'erp:other-payable:query', 'erp:other-payable:create', 'erp:other-payable:update', 'erp:other-payable:delete'),
(CONVERT(0xE585B6E4BB96E5BA94E694B6E5AEA1E6A0B8 USING utf8mb4), 'erp:other-receivable:update-status', 5, 'erp:other-receivable:query', 'erp:other-receivable:create', 'erp:other-receivable:update', 'erp:other-receivable:delete');

CREATE TEMPORARY TABLE tmp_erp_approver_menu_parent (
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  parent_id bigint NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_approver_menu_parent (permission, parent_id)
SELECT parent_source.permission, MIN(parent_source.parent_id) AS parent_id
FROM (
    SELECT source.permission, candidate.parent_id
    FROM tmp_erp_approver_menu_sources source
    JOIN system_menu candidate
      ON (
          candidate.permission COLLATE utf8mb4_unicode_ci = source.query_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.create_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.update_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.delete_perm COLLATE utf8mb4_unicode_ci
      )
     AND candidate.deleted = b'0'
     AND candidate.parent_id > 0
) parent_source
GROUP BY parent_source.permission;

-- Restore a previously soft-deleted approval/audit menu if one exists for the exact permission.
UPDATE system_menu m
JOIN tmp_erp_approver_menu_sources source
  ON m.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
JOIN tmp_erp_approver_menu_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
LEFT JOIN system_menu active_menu
  ON active_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND active_menu.deleted = b'0'
SET m.name = source.name,
    m.type = 3,
    m.sort = source.sort,
    m.parent_id = parent.parent_id,
    m.path = '',
    m.icon = '',
    m.component = '',
    m.component_name = NULL,
    m.status = 0,
    m.visible = b'1',
    m.keep_alive = b'1',
    m.always_show = b'1',
    m.updater = '1',
    m.update_time = NOW(),
    m.deleted = b'0'
WHERE m.deleted = b'1'
  AND active_menu.id IS NULL;

-- Create missing active approval/audit button menus.
INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT source.name,
       source.permission,
       3,
       source.sort,
       parent.parent_id,
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
FROM tmp_erp_approver_menu_sources source
JOIN tmp_erp_approver_menu_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
      AND exists_menu.deleted = b'0'
);

-- Create ERP Approver role for the target tenant if absent.
-- New role uses DEPT_ONLY(3), matching the default role creation service.
-- Existing erp_approver roles keep their current data_scope/data_scope_dept_ids.
INSERT INTO system_role
(`name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT CONVERT(0x455250E5AEA1E689B9E59198 USING utf8mb4),
       'erp_approver',
       50,
       3,
       '',
       0,
       2,
       CONVERT(0x455250E6ADA3E59091E5AEA1E689B92FE5AEA1E6A0B8E4B893E794A8E8A792E889B2EFBC8CE4B88DE58C85E590ABE58F8DE5AEA1E69D83E99990 USING utf8mb4),
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       @tenant_id
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role r
    WHERE r.code COLLATE utf8mb4_unicode_ci = 'erp_approver' COLLATE utf8mb4_unicode_ci
      AND r.tenant_id = @tenant_id
      AND r.deleted = b'0'
);

-- Standardize only the visible role name/status for the target role.
-- This does not overwrite menu permissions, user bindings, or data scope settings.
UPDATE system_role r
SET r.name = CONVERT(0x455250E5AEA1E689B9E59198 USING utf8mb4),
    r.status = 0,
    r.updater = '1',
    r.update_time = NOW()
WHERE r.code COLLATE utf8mb4_unicode_ci = 'erp_approver' COLLATE utf8mb4_unicode_ci
  AND r.tenant_id = @tenant_id
  AND r.deleted = b'0';

-- Append positive approval/audit button menus to ERP Approver.
-- erp:voucher:process is intentionally not present in tmp_erp_approver_permissions.
INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT r.id,
       m.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       @tenant_id
FROM system_role r
JOIN tmp_erp_approver_permissions p ON 1 = 1
JOIN system_menu m
  ON m.permission COLLATE utf8mb4_unicode_ci = p.permission COLLATE utf8mb4_unicode_ci
 AND m.deleted = b'0'
WHERE r.code COLLATE utf8mb4_unicode_ci = 'erp_approver' COLLATE utf8mb4_unicode_ci
  AND r.tenant_id = @tenant_id
  AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
        AND rm.tenant_id = @tenant_id
        AND rm.deleted = b'0'
  );

-- Append ERP Approver role to fixed users by username.
-- Placeholder rows are ignored until replaced.
INSERT INTO system_user_role
(`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT u.id,
       r.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       @tenant_id
FROM tmp_erp_approver_usernames tu
JOIN system_users u
  ON u.username COLLATE utf8mb4_unicode_ci = tu.username COLLATE utf8mb4_unicode_ci
 AND u.tenant_id = @tenant_id
 AND u.deleted = b'0'
JOIN system_role r
  ON r.code COLLATE utf8mb4_unicode_ci = 'erp_approver' COLLATE utf8mb4_unicode_ci
 AND r.tenant_id = @tenant_id
 AND r.deleted = b'0'
WHERE tu.username COLLATE utf8mb4_unicode_ci NOT IN (
        '__REPLACE_USERNAME_1__' COLLATE utf8mb4_unicode_ci,
        '__REPLACE_USERNAME_2__' COLLATE utf8mb4_unicode_ci
      )
  AND NOT EXISTS (
      SELECT 1
      FROM system_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
        AND ur.tenant_id = @tenant_id
        AND ur.deleted = b'0'
  );

-- Verification queries.
SELECT r.id AS role_id, r.name, r.code, r.data_scope, r.data_scope_dept_ids, r.tenant_id
FROM system_role r
WHERE r.code COLLATE utf8mb4_unicode_ci = 'erp_approver' COLLATE utf8mb4_unicode_ci
  AND r.tenant_id = @tenant_id
  AND r.deleted = b'0';

SELECT p.permission,
       CASE
           WHEN m.id IS NULL THEN 'MISSING_MENU'
           WHEN m.type <> 3 THEN 'INVALID_MENU_TYPE'
           ELSE 'OK'
       END AS menu_status,
       m.id AS menu_id,
       m.name AS menu_name,
       m.type AS menu_type,
       m.parent_id,
       parent.name AS parent_name
FROM tmp_erp_approver_permissions p
LEFT JOIN system_menu m
  ON m.permission COLLATE utf8mb4_unicode_ci = p.permission COLLATE utf8mb4_unicode_ci
 AND m.deleted = b'0'
LEFT JOIN system_menu parent
  ON parent.id = m.parent_id
 AND parent.deleted = b'0'
ORDER BY p.permission, m.id;

SELECT source.permission,
       CASE
           WHEN m.id IS NULL THEN 'MISSING_MENU'
           WHEN m.type <> 3 THEN 'INVALID_MENU_TYPE'
           WHEN parent.id IS NULL THEN 'MISSING_PARENT'
           ELSE 'OK'
       END AS menu_status,
       m.id AS menu_id,
       m.name AS menu_name,
       m.parent_id,
       parent.name AS parent_name
FROM tmp_erp_approver_menu_sources source
LEFT JOIN system_menu m
  ON m.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND m.deleted = b'0'
LEFT JOIN system_menu parent
  ON parent.id = m.parent_id
 AND parent.deleted = b'0'
ORDER BY source.permission, m.id;

SELECT forbidden.permission AS forbidden_permission,
       COUNT(m.id) AS granted_count
FROM (
    SELECT 'erp:voucher:process' AS permission
) forbidden
LEFT JOIN system_role r
  ON r.code COLLATE utf8mb4_unicode_ci = 'erp_approver' COLLATE utf8mb4_unicode_ci
 AND r.tenant_id = @tenant_id
 AND r.deleted = b'0'
LEFT JOIN system_role_menu rm
  ON rm.role_id = r.id
 AND rm.tenant_id = @tenant_id
 AND rm.deleted = b'0'
LEFT JOIN system_menu m
  ON m.id = rm.menu_id
 AND m.permission COLLATE utf8mb4_unicode_ci = forbidden.permission COLLATE utf8mb4_unicode_ci
 AND m.deleted = b'0'
GROUP BY forbidden.permission;

SELECT u.username,
       COUNT(ur.id) AS erp_approver_role_count
FROM tmp_erp_approver_usernames tu
JOIN system_users u
  ON u.username COLLATE utf8mb4_unicode_ci = tu.username COLLATE utf8mb4_unicode_ci
 AND u.tenant_id = @tenant_id
 AND u.deleted = b'0'
LEFT JOIN system_role r
  ON r.code COLLATE utf8mb4_unicode_ci = 'erp_approver' COLLATE utf8mb4_unicode_ci
 AND r.tenant_id = @tenant_id
 AND r.deleted = b'0'
LEFT JOIN system_user_role ur
  ON ur.user_id = u.id
 AND ur.role_id = r.id
 AND ur.tenant_id = @tenant_id
 AND ur.deleted = b'0'
WHERE tu.username COLLATE utf8mb4_unicode_ci NOT IN (
        '__REPLACE_USERNAME_1__' COLLATE utf8mb4_unicode_ci,
        '__REPLACE_USERNAME_2__' COLLATE utf8mb4_unicode_ci
      )
GROUP BY u.username
ORDER BY u.username;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_approver_usernames;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_approver_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_approver_menu_sources;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_approver_menu_parent;
