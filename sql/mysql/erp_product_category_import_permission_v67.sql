-- ERP product category import button permission.
-- Safe incremental script: inserts the missing button permission and grants it
-- to roles that already own product category permissions.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @product_category_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = ''
    AND `component` = 'erp/product/category/index'
    AND `deleted` = b'0'
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '产品分类导入', 'erp:product-category:import', 3, 6, @product_category_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @product_category_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:product-category:import'
      AND `deleted` = b'0'
  );

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT rm.`role_id`, import_menu.`id`, '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `system_menu` import_menu
JOIN `system_role_menu` rm
  ON rm.`deleted` = b'0'
JOIN `system_menu` owned_menu
  ON owned_menu.`id` = rm.`menu_id`
 AND owned_menu.`deleted` = b'0'
 AND owned_menu.`permission` IN (
   'erp:product-category:query',
   'erp:product-category:create',
   'erp:product-category:update',
   'erp:product-category:export'
 )
WHERE import_menu.`permission` = 'erp:product-category:import'
  AND import_menu.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` target
    WHERE target.`role_id` = rm.`role_id`
      AND target.`menu_id` = import_menu.`id`
      AND target.`tenant_id` = rm.`tenant_id`
      AND target.`deleted` = b'0'
  );

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT role.`id`, import_menu.`id`, '1', NOW(), '1', NOW(), b'0', role.`tenant_id`
FROM `system_role` role
JOIN `system_menu` import_menu
  ON import_menu.`permission` = 'erp:product-category:import'
 AND import_menu.`deleted` = b'0'
WHERE role.`code` = 'super_admin'
  AND role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` target
    WHERE target.`role_id` = role.`id`
      AND target.`menu_id` = import_menu.`id`
      AND target.`tenant_id` = role.`tenant_id`
      AND target.`deleted` = b'0'
  );
