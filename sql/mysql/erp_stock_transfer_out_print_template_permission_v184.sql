-- ERP stock transfer-out print template permission (v184).
-- Safe to rerun. This script only appends the print-template button permission
-- and grants it to super_admin by default. Existing print/query permissions are untouched.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_stock_transfer_out_print_template_permission_v184;

CREATE TEMPORARY TABLE tmp_erp_stock_transfer_out_print_template_permission_v184 (
  parent_permission varchar(128) NOT NULL,
  inherit_permission varchar(128) NOT NULL,
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_stock_transfer_out_print_template_permission_v184
(`parent_permission`, `inherit_permission`, `button_name`, `button_permission`, `sort`)
VALUES
('erp:stock-transfer-out:query', 'super_admin', '调拨出库单打印模板',
 'erp:stock-transfer-out:print-template', 10);

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
FROM tmp_erp_stock_transfer_out_print_template_permission_v184 perm
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
SELECT DISTINCT super_role.id,
       button_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM tmp_erp_stock_transfer_out_print_template_permission_v184 perm
JOIN system_menu button_menu
  ON button_menu.permission = perm.button_permission
 AND button_menu.deleted = b'0'
JOIN system_role super_role
  ON super_role.code = perm.inherit_permission
 AND super_role.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM system_role_menu target
     WHERE target.role_id = super_role.id
       AND target.menu_id = button_menu.id
       AND target.tenant_id = super_role.tenant_id
       AND target.deleted = b'0'
);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_stock_transfer_out_print_template_permission_v184;
