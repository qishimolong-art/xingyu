-- ERP stock-in print permissions (v186).
-- Safe to rerun. This script only appends menu buttons and role-menu mappings:
-- 1) print permission inherits roles that already own stock-in query permission;
-- 2) print-template permission is granted only to super_admin by default.
-- It does not insert default print templates or overwrite existing data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_stock_in_print_permission_v186;

CREATE TEMPORARY TABLE tmp_erp_stock_in_print_permission_v186 (
  parent_permission varchar(128) NOT NULL,
  inherit_permission varchar(128) NOT NULL,
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_stock_in_print_permission_v186
(`parent_permission`, `inherit_permission`, `button_name`, `button_permission`, `sort`)
VALUES
('erp:stock-in:query', 'erp:stock-in:query', '其它入库单打印', 'erp:stock-in:print', 8),
('erp:stock-in:query', 'super_admin', '其它入库单打印模板', 'erp:stock-in:print-template', 9);

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT perm.button_name,
       perm.button_permission,
       3,
       perm.sort,
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
FROM tmp_erp_stock_in_print_permission_v186 perm
JOIN system_menu parent
  ON parent.permission = perm.parent_permission
 AND parent.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM system_menu exists_menu
     WHERE exists_menu.permission = perm.button_permission
       AND exists_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       button_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM tmp_erp_stock_in_print_permission_v186 perm
JOIN system_menu button_menu
  ON button_menu.permission = perm.button_permission
 AND button_menu.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.permission = perm.inherit_permission
 AND owned_menu.deleted = b'0'
JOIN system_role_menu owned_role
  ON owned_role.menu_id = owned_menu.id
 AND owned_role.deleted = b'0'
WHERE perm.inherit_permission <> 'super_admin'
  AND NOT EXISTS (
    SELECT 1
      FROM system_role_menu target
     WHERE target.role_id = owned_role.role_id
       AND target.menu_id = button_menu.id
       AND target.tenant_id = owned_role.tenant_id
       AND target.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       button_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM tmp_erp_stock_in_print_permission_v186 perm
JOIN system_menu button_menu
  ON button_menu.permission = perm.button_permission
 AND button_menu.deleted = b'0'
JOIN system_role super_role
  ON super_role.code = 'super_admin'
 AND super_role.deleted = b'0'
WHERE perm.inherit_permission = 'super_admin'
  AND NOT EXISTS (
      SELECT 1
        FROM system_role_menu target
       WHERE target.role_id = super_role.id
         AND target.menu_id = button_menu.id
         AND target.tenant_id = super_role.tenant_id
         AND target.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_stock_in_print_permission_v186;
