-- ERP finance import permission patch (v101)
-- Adds import button menus for finance business forms and grants them to roles
-- that already own related finance permissions.
--
-- Safety:
--   - Append-only for role-menu relations. No DELETE and no full replacement.
--   - Parent menus are located from existing query/create/update/export buttons.
--   - Soft-deleted import menus for the same permission are restored.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_finance_import_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_finance_import_parent;

CREATE TEMPORARY TABLE tmp_erp_finance_import_permissions (
  name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  sort int NOT NULL,
  query_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  create_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  update_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  export_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_finance_import_permissions
(name, permission, sort, query_perm, create_perm, update_perm, export_perm)
VALUES
('结算账户导入', 'erp:account:import', 6, 'erp:account:query', 'erp:account:create', 'erp:account:update', 'erp:account:export'),
('银行转账导入', 'erp:finance-transfer:import', 6, 'erp:finance-transfer:query', 'erp:finance-transfer:create', 'erp:finance-transfer:update', 'erp:finance-transfer:export'),
('付款单导入', 'erp:finance-payment:import', 6, 'erp:finance-payment:query', 'erp:finance-payment:create', 'erp:finance-payment:update', 'erp:finance-payment:export'),
('收款单导入', 'erp:finance-receipt:import', 6, 'erp:finance-receipt:query', 'erp:finance-receipt:create', 'erp:finance-receipt:update', 'erp:finance-receipt:export'),
('费用支付导入', 'erp:payable-expense:import', 6, 'erp:payable-expense:query', 'erp:payable-expense:create', 'erp:payable-expense:update', 'erp:payable-expense:export'),
('其他应付导入', 'erp:payable-other:import', 6, 'erp:payable-other:query', 'erp:payable-other:create', 'erp:payable-other:update', 'erp:payable-other:export'),
('其他应收导入', 'erp:receivable-other:import', 6, 'erp:receivable-other:query', 'erp:receivable-other:create', 'erp:receivable-other:update', 'erp:receivable-other:export'),
('其他收入导入', 'erp:receivable-other-income:import', 6, 'erp:receivable-other-income:query', 'erp:receivable-other-income:create', 'erp:receivable-other-income:update', 'erp:receivable-other-income:export');

CREATE TEMPORARY TABLE tmp_erp_finance_import_parent (
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  parent_id bigint NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_finance_import_parent (permission, parent_id)
SELECT parent_source.permission, MIN(parent_source.parent_id) AS parent_id
FROM (
    SELECT source.permission, candidate.parent_id
    FROM tmp_erp_finance_import_permissions source
    JOIN system_menu candidate
      ON (
          candidate.permission COLLATE utf8mb4_unicode_ci = source.query_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.create_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.update_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.export_perm COLLATE utf8mb4_unicode_ci
      )
     AND candidate.deleted = b'0'
     AND candidate.parent_id > 0
) parent_source
GROUP BY parent_source.permission;

UPDATE system_menu menu
JOIN tmp_erp_finance_import_permissions source
  ON menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
JOIN tmp_erp_finance_import_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
SET menu.name = source.name,
    menu.type = 3,
    menu.sort = source.sort,
    menu.parent_id = parent.parent_id,
    menu.path = '',
    menu.icon = '',
    menu.component = '',
    menu.component_name = NULL,
    menu.status = 0,
    menu.visible = b'1',
    menu.keep_alive = b'1',
    menu.always_show = b'1',
    menu.updater = '1',
    menu.update_time = NOW(),
    menu.deleted = b'0'
WHERE menu.deleted = b'1';

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
FROM tmp_erp_finance_import_permissions source
JOIN tmp_erp_finance_import_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
      AND exists_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       import_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM tmp_erp_finance_import_permissions source
JOIN system_menu import_menu
  ON import_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND import_menu.deleted = b'0'
JOIN system_role_menu owned_role
  ON owned_role.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.id = owned_role.menu_id
 AND owned_menu.deleted = b'0'
 AND (
       owned_menu.permission COLLATE utf8mb4_unicode_ci = source.query_perm COLLATE utf8mb4_unicode_ci
    OR owned_menu.permission COLLATE utf8mb4_unicode_ci = source.create_perm COLLATE utf8mb4_unicode_ci
    OR owned_menu.permission COLLATE utf8mb4_unicode_ci = source.update_perm COLLATE utf8mb4_unicode_ci
    OR owned_menu.permission COLLATE utf8mb4_unicode_ci = source.export_perm COLLATE utf8mb4_unicode_ci
 )
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_menu target
    WHERE target.role_id = owned_role.role_id
      AND target.menu_id = import_menu.id
      AND target.tenant_id = owned_role.tenant_id
      AND target.deleted = b'0'
);

INSERT INTO system_role_menu
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
JOIN tmp_erp_finance_import_permissions source ON 1 = 1
JOIN system_menu import_menu
  ON import_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
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

SELECT source.permission,
       CASE
           WHEN menu.id IS NULL THEN 'MISSING_MENU'
           WHEN menu.type <> 3 THEN 'INVALID_MENU_TYPE'
           WHEN parent.id IS NULL THEN 'MISSING_PARENT'
           ELSE 'OK'
       END AS menu_status,
       menu.id AS menu_id,
       menu.name AS menu_name,
       menu.parent_id,
       parent.name AS parent_name
FROM tmp_erp_finance_import_permissions source
LEFT JOIN system_menu menu
  ON menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND menu.deleted = b'0'
LEFT JOIN system_menu parent
  ON parent.id = menu.parent_id
 AND parent.deleted = b'0'
ORDER BY source.permission, menu.id;

SELECT source.permission,
       COUNT(DISTINCT role_menu.role_id) AS granted_role_count
FROM tmp_erp_finance_import_permissions source
LEFT JOIN system_menu import_menu
  ON import_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND import_menu.deleted = b'0'
LEFT JOIN system_role_menu role_menu
  ON role_menu.menu_id = import_menu.id
 AND role_menu.deleted = b'0'
GROUP BY source.permission
ORDER BY source.permission;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_finance_import_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_finance_import_parent;
