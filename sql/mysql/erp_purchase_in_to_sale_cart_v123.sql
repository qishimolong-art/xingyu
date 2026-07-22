-- ERP purchase in -> sale cart (v123)
-- Scope:
--   1. Add purchase-in detail button permission: erp:purchase-in:to-sale-cart.
-- Safety:
--   - Does not delete or replace existing purchase-in or sale-cart permissions.
--   - Uses NOT EXISTS to avoid duplicate menu permission inserts.

SET @purchase_in_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `permission` = 'erp:purchase-in:query'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @purchase_in_parent_id := (
  SELECT `parent_id`
  FROM `system_menu`
  WHERE `id` = @purchase_in_menu_id
  LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31341, '采购入库转销售手推车', 'erp:purchase-in:to-sale-cart', 3, 21, @purchase_in_parent_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @purchase_in_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` = 'erp:purchase-in:to-sale-cart'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, menu.`id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` menu
WHERE menu.`permission` = 'erp:purchase-in:to-sale-cart'
  AND menu.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` role_menu
      WHERE role_menu.`role_id` = 1
        AND role_menu.`menu_id` = menu.`id`
        AND role_menu.`deleted` = b'0'
  );

-- After execution: refresh menu cache or restart backend, then re-login.
