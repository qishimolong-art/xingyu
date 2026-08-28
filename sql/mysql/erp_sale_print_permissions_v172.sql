-- ERP sale module print permissions (v172).
-- Safe to rerun. This script only appends menu buttons and role-menu mappings:
-- 1) print permissions inherit roles that already own the matching query permission;
-- 2) print-template permissions are granted only to super_admin by default.
-- It does not insert default print templates or overwrite existing data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_sale_print_permission;

CREATE TEMPORARY TABLE tmp_erp_sale_print_permission (
  parent_permission varchar(128) NOT NULL,
  inherit_permission varchar(128) NOT NULL,
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_sale_print_permission
(`parent_permission`, `inherit_permission`, `button_name`, `button_permission`, `sort`)
VALUES
('erp:sale-order:query', 'erp:sale-order:query', '销售订单打印', 'erp:sale-order:print', 7),
('erp:sale-order:query', 'super_admin', '销售订单打印模板', 'erp:sale-order:print-template', 8),
('erp:sale-out:query', 'erp:sale-out:query', '销售出库打印', 'erp:sale-out:print', 7),
('erp:sale-out:query', 'super_admin', '销售出库打印模板', 'erp:sale-out:print-template', 8),
('erp:sale-return:query', 'erp:sale-return:query', '销售退货打印', 'erp:sale-return:print', 7),
('erp:sale-return:query', 'super_admin', '销售退货打印模板', 'erp:sale-return:print-template', 8),
('erp:sale-quote:query', 'erp:sale-quote:query', '销售报价打印', 'erp:sale-quote:print', 7),
('erp:sale-quote:query', 'super_admin', '销售报价打印模板', 'erp:sale-quote:print-template', 8),
('erp:sale-price-adjust:query', 'erp:sale-price-adjust:query', '销售调价打印', 'erp:sale-price-adjust:print', 7),
('erp:sale-price-adjust:query', 'super_admin', '销售调价打印模板', 'erp:sale-price-adjust:print-template', 8);

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
FROM tmp_erp_sale_print_permission perm
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
FROM tmp_erp_sale_print_permission perm
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
FROM tmp_erp_sale_print_permission perm
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

DROP TEMPORARY TABLE IF EXISTS tmp_erp_sale_print_permission;
