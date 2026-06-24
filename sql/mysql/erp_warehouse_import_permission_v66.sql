-- ERP warehouse import button permission.
-- Safe incremental script: only inserts the missing button permission and grants it to super admin.

SET @warehouse_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = ''
    AND `component` = 'erp/stock/warehouse/index'
    AND `deleted` = b'0'
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '仓库导入', 'erp:warehouse:import', 3, 6, @warehouse_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:warehouse:import'
      AND `deleted` = b'0'
  );

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 1, `id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu`
WHERE `permission` = 'erp:warehouse:import'
  AND `deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu`
    WHERE `role_id` = 1
      AND `menu_id` = `system_menu`.`id`
      AND `deleted` = b'0'
  );
