-- ERP 调拨出库单“解锁手推车”权限（v126）
-- 仅新增独立按钮权限，并默认授权租户 1 的管理员角色（role_id = 1）。
-- 不删除、不覆盖任何已有菜单、角色或权限数据；脚本可重复执行。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @transfer_out_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 2
    AND `component` = 'erp/stock/transfer-out/index'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '调拨出库单解锁手推车', 'erp:stock-transfer-out:unlock-cart', 3, 9, @transfer_out_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @transfer_out_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing_menu
    WHERE existing_menu.`deleted` = b'0'
      AND existing_menu.`permission` = 'erp:stock-transfer-out:unlock-cart'
  );

SET @unlock_cart_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `permission` = 'erp:stock-transfer-out:unlock-cart'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, @unlock_cart_menu_id, '1', NOW(), '1', NOW(), b'0', 1
WHERE @unlock_cart_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = 1
      AND existing_role_menu.`menu_id` = @unlock_cart_menu_id
      AND existing_role_menu.`tenant_id` = 1
      AND existing_role_menu.`deleted` = b'0'
  );

-- 执行后请刷新菜单权限缓存或重新登录，使新权限进入登录用户权限集合。
