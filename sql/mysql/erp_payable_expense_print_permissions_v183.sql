-- ERP payable expense print permissions (v183).
-- Safe to rerun. This script only appends menu buttons and super-admin role mappings.
-- It does not insert default print templates or overwrite existing data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_payable_expense_print_permission;

CREATE TEMPORARY TABLE tmp_erp_payable_expense_print_permission (
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_payable_expense_print_permission
(`button_name`, `button_permission`, `sort`)
VALUES
('费用支付打印', 'erp:payable-expense:print', 7),
('费用支付打印模板', 'erp:payable-expense:print-template', 8);

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT perm.button_name,
       perm.button_permission,
       3,
       perm.sort,
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
FROM tmp_erp_payable_expense_print_permission perm
JOIN system_menu parent_menu
  ON parent_menu.id = 31108
 AND parent_menu.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM system_menu existing_menu
     WHERE existing_menu.permission = perm.button_permission
       AND existing_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1,
       button_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       1
FROM tmp_erp_payable_expense_print_permission perm
JOIN system_menu button_menu
  ON button_menu.permission = perm.button_permission
 AND button_menu.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM system_role_menu target
     WHERE target.role_id = 1
       AND target.menu_id = button_menu.id
       AND target.tenant_id = 1
       AND target.deleted = b'0'
);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_payable_expense_print_permission;
