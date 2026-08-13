-- ERP purchase order print template permission split (v170).
-- Safe to rerun. Keeps ordinary print separate from template maintenance:
-- only super_admin keeps default print-template authorization; later template
-- admins can be granted erp:purchase-order:print-template manually.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_purchase_order_print_template_menu;

CREATE TEMPORARY TABLE tmp_erp_purchase_order_print_template_menu (
  menu_id bigint NOT NULL,
  PRIMARY KEY (menu_id)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_purchase_order_print_template_menu (menu_id)
SELECT id
  FROM system_menu
 WHERE permission = 'erp:purchase-order:print-template'
   AND deleted = b'0';

DELETE role_menu
  FROM system_role_menu role_menu
  JOIN tmp_erp_purchase_order_print_template_menu template_menu
    ON template_menu.menu_id = role_menu.menu_id
  JOIN system_role role
    ON role.id = role_menu.role_id
   AND role.deleted = b'0'
 WHERE role_menu.deleted = b'0'
   AND role.code <> 'super_admin';

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       template_menu.menu_id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM system_role super_role
JOIN tmp_erp_purchase_order_print_template_menu template_menu
WHERE super_role.code = 'super_admin'
  AND super_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
        FROM system_role_menu target
       WHERE target.role_id = super_role.id
         AND target.menu_id = template_menu.menu_id
         AND target.tenant_id = super_role.tenant_id
         AND target.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_purchase_order_print_template_menu;
