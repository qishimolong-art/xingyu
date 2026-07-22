-- ERP 库存调拨菜单分组（v125）
-- 目标：
--   1. 在“库存管理”下新增“调拨管理”目录。
--   2. 将仓库移货单、调拨入库单、调拨出库单迁入该目录。
--   3. 子菜单顺序固定为：仓库移货单、调拨入库单、调拨出库单。
--
-- 安全说明：
--   - 不删除、不替换任何已有菜单或按钮权限。
--   - 仅修改三个目标页面菜单的父级与排序。
--   - 为已经拥有任一目标页面或按钮权限的角色补充分组目录权限。
--   - 所有写入均可重复执行，不会重复插入有效角色菜单关系。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

SET @stock_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND `name` = '库存管理'
    AND `path` = 'stock'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @transfer_management_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND `parent_id` = @stock_menu_id
    AND (`name` = '调拨管理' OR `path` = 'transfer-management')
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31350, '调拨管理', '', 1, 6, @stock_menu_id, 'transfer-management', 'ep:switch', '', '',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_menu_id IS NOT NULL
  AND @transfer_management_menu_id IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `id` = 31350
  );

SET @transfer_management_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND `parent_id` = @stock_menu_id
    AND (`name` = '调拨管理' OR `path` = 'transfer-management')
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @transfer_management_menu_id,
    `sort` = CASE `component`
      WHEN 'erp/stock/warehouse-move/index' THEN 1
      WHEN 'erp/stock/transfer-in/index' THEN 2
      WHEN 'erp/stock/transfer-out/index' THEN 3
      ELSE `sort`
    END,
    `updater` = '1',
    `update_time` = NOW()
WHERE @transfer_management_menu_id IS NOT NULL
  AND `deleted` = b'0'
  AND `type` = 2
  AND `component` IN (
    'erp/stock/warehouse-move/index',
    'erp/stock/transfer-in/index',
    'erp/stock/transfer-out/index'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT granted_role.`role_id`, @transfer_management_menu_id,
       '1', NOW(), '1', NOW(), b'0', granted_role.`tenant_id`
FROM `system_role_menu` granted_role
INNER JOIN (
  SELECT target_page.`id` AS `menu_id`
  FROM `system_menu` target_page
  WHERE target_page.`deleted` = b'0'
    AND target_page.`type` = 2
    AND target_page.`component` IN (
      'erp/stock/warehouse-move/index',
      'erp/stock/transfer-in/index',
      'erp/stock/transfer-out/index'
    )
  UNION ALL
  SELECT target_button.`id` AS `menu_id`
  FROM `system_menu` target_button
  INNER JOIN `system_menu` target_page
          ON target_page.`id` = target_button.`parent_id`
         AND target_page.`deleted` = b'0'
         AND target_page.`type` = 2
         AND target_page.`component` IN (
           'erp/stock/warehouse-move/index',
           'erp/stock/transfer-in/index',
           'erp/stock/transfer-out/index'
         )
  WHERE target_button.`deleted` = b'0'
    AND target_button.`type` = 3
) target_permission ON target_permission.`menu_id` = granted_role.`menu_id`
WHERE @transfer_management_menu_id IS NOT NULL
  AND granted_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_directory_permission
    WHERE existing_directory_permission.`role_id` = granted_role.`role_id`
      AND existing_directory_permission.`menu_id` = @transfer_management_menu_id
      AND existing_directory_permission.`tenant_id` = granted_role.`tenant_id`
      AND existing_directory_permission.`deleted` = b'0'
  );

COMMIT;

-- 部署后建议退出并重新登录，或刷新用户菜单缓存，使新目录立即生效。
