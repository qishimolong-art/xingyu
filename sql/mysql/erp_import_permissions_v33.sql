-- ERP import permission patch (v33)
-- Adds missing import button menus and grants them to roles that already own
-- the corresponding ERP form/menu permissions.
--
-- Safety:
--   - Append-only for role-menu relations. No DELETE and no full replacement.
--   - Import menus are located by permission and parent menus are located from
--     existing query/create/update/export buttons, avoiding hard-coded menu ids.
--   - Explicit COLLATE is used on permission comparisons to avoid MySQL 1267.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_parent;
CREATE TEMPORARY TABLE tmp_erp_import_permissions (
  name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  sort int NOT NULL,
  query_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  create_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  update_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  export_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_import_permissions
(name, permission, sort, query_perm, create_perm, update_perm, export_perm)
VALUES
(CONVERT(0xE4BAA7E59381E5AFBCE585A5 USING utf8mb4), 'erp:product:import', 6, 'erp:product:query', 'erp:product:create', 'erp:product:update', 'erp:product:export'),
(CONVERT(0xE4BE9BE5BA94E59586E5AFBCE585A5 USING utf8mb4), 'erp:supplier:import', 6, 'erp:supplier:query', 'erp:supplier:create', 'erp:supplier:update', 'erp:supplier:export'),
(CONVERT(0xE5AEA2E688B7E5AFBCE585A5 USING utf8mb4), 'erp:customer:import', 6, 'erp:customer:query', 'erp:customer:create', 'erp:customer:update', 'erp:customer:export');

CREATE TEMPORARY TABLE tmp_erp_import_parent (
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  parent_id bigint NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_import_parent (permission, parent_id)
SELECT parent_source.permission, MIN(parent_source.parent_id) AS parent_id
FROM (
    SELECT source.permission, candidate.parent_id
    FROM tmp_erp_import_permissions source
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

-- Restore a previously soft-deleted import menu if one exists for the exact permission.
UPDATE system_menu m
JOIN tmp_erp_import_permissions source
  ON m.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
JOIN tmp_erp_import_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
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
WHERE m.deleted = b'1';

-- Create missing active import button menus.
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
FROM tmp_erp_import_permissions source
JOIN tmp_erp_import_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
      AND exists_menu.deleted = b'0'
);

-- Grant import buttons to roles that already own the corresponding ERP menu/button.
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
FROM tmp_erp_import_permissions source
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

-- Ensure active super-admin roles can see the import button permissions as menu nodes.
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
JOIN tmp_erp_import_permissions source ON 1 = 1
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

-- Verification queries.
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
FROM tmp_erp_import_permissions source
LEFT JOIN system_menu m
  ON m.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND m.deleted = b'0'
LEFT JOIN system_menu parent
  ON parent.id = m.parent_id
 AND parent.deleted = b'0'
ORDER BY source.permission, m.id;

SELECT source.permission,
       COUNT(DISTINCT rm.role_id) AS granted_role_count
FROM tmp_erp_import_permissions source
LEFT JOIN system_menu import_menu
  ON import_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND import_menu.deleted = b'0'
LEFT JOIN system_role_menu rm
  ON rm.menu_id = import_menu.id
 AND rm.deleted = b'0'
GROUP BY source.permission
ORDER BY source.permission;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_parent;
