-- ERP stock out bill export permission (v87)
-- Adds only the 出仓单 export button permission.
-- Safe to rerun: no broad deletes, no overwrite updates, and no role permission rebuild.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @stock_out_bill_menu_id := (
  SELECT `id`
    FROM `system_menu`
   WHERE `permission` = ''
     AND `path` = 'outbill'
     AND `component_name` = 'ErpStockOutBill'
     AND `deleted` = b'0'
   ORDER BY `id` DESC
   LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
                           `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`,
                           `create_time`, `updater`, `update_time`, `deleted`)
SELECT '出仓单导出', 'erp:stock-out-bill:export', 3, 2, @stock_out_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1',
       NOW(), '1', NOW(), b'0'
WHERE @stock_out_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
       WHERE `permission` = 'erp:stock-out-bill:export'
         AND `deleted` = b'0'
  );

SET @stock_out_bill_export_menu_id := (
  SELECT `id`
    FROM `system_menu`
   WHERE `permission` = 'erp:stock-out-bill:export'
     AND `deleted` = b'0'
   ORDER BY `id` DESC
   LIMIT 1
);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT role_menu.`role_id`, @stock_out_bill_export_menu_id, '1', NOW(), '1', NOW(), b'0', role_menu.`tenant_id`
  FROM `system_role_menu` role_menu
 WHERE @stock_out_bill_export_menu_id IS NOT NULL
   AND role_menu.`menu_id` = @stock_out_bill_menu_id
   AND role_menu.`deleted` = b'0'
   AND NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` target
        WHERE target.`role_id` = role_menu.`role_id`
          AND target.`menu_id` = @stock_out_bill_export_menu_id
          AND target.`tenant_id` = role_menu.`tenant_id`
          AND target.`deleted` = b'0'
   );

UPDATE `system_tenant_package` tenant_package
   SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_out_bill_export_menu_id)
 WHERE @stock_out_bill_export_menu_id IS NOT NULL
   AND tenant_package.`deleted` = b'0'
   AND JSON_VALID(tenant_package.`menu_ids`)
   AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
   AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_out_bill_menu_id AS CHAR), '$')
   AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_out_bill_export_menu_id AS CHAR), '$');
