-- v254: 销售手推车开单确认状态、审计字段与按钮权限。
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.
-- 部署顺序：先执行本脚本，再发布读取新字段的后端代码。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @current_schema := DATABASE();

SET @add_billing_confirm_user_id_sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'erp_sale_cart'
      AND COLUMN_NAME = 'billing_confirm_user_id'
  ),
  'SELECT 1',
  'ALTER TABLE `erp_sale_cart` ADD COLUMN `billing_confirm_user_id` bigint DEFAULT NULL COMMENT ''开单确认人编号'' AFTER `cart_time`'
);
PREPARE add_billing_confirm_user_id_stmt FROM @add_billing_confirm_user_id_sql;
EXECUTE add_billing_confirm_user_id_stmt;
DEALLOCATE PREPARE add_billing_confirm_user_id_stmt;

SET @add_billing_confirm_time_sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @current_schema
      AND TABLE_NAME = 'erp_sale_cart'
      AND COLUMN_NAME = 'billing_confirm_time'
  ),
  'SELECT 1',
  'ALTER TABLE `erp_sale_cart` ADD COLUMN `billing_confirm_time` datetime DEFAULT NULL COMMENT ''开单确认时间'' AFTER `billing_confirm_user_id`'
);
PREPARE add_billing_confirm_time_stmt FROM @add_billing_confirm_time_sql;
EXECUTE add_billing_confirm_time_stmt;
DEALLOCATE PREPARE add_billing_confirm_time_stmt;

SET @sale_cart_menu_id := (
  SELECT `parent_id` FROM `system_menu`
  WHERE `permission` = 'erp:sale-cart:submit'
    AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

SET @active_billing_confirm_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:sale-cart:confirm'
    AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

UPDATE `system_menu`
SET `name` = '销售手推车开单确认',
    `type` = 3,
    `sort` = 6,
    `parent_id` = @sale_cart_menu_id,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = NULL,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = (
    SELECT `candidate`.`id`
    FROM (
      SELECT `id` FROM `system_menu`
      WHERE `permission` = 'erp:sale-cart:confirm'
        AND `deleted` = b'1'
      ORDER BY `id` DESC LIMIT 1
    ) `candidate`
  )
  AND @sale_cart_menu_id IS NOT NULL
  AND @active_billing_confirm_menu_id IS NULL;

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '销售手推车开单确认', 'erp:sale-cart:confirm', 3, 6, @sale_cart_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @sale_cart_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:sale-cart:confirm'
      AND `deleted` = b'0'
  );

SET @billing_confirm_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:sale-cart:confirm'
    AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

-- 只给默认超级管理员追加权限；开单员、部门主管由目标环境在角色管理中按实际角色授权。
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, @billing_confirm_menu_id, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE @billing_confirm_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu`
    WHERE `role_id` = 1
      AND `menu_id` = @billing_confirm_menu_id
      AND `tenant_id` = 1
      AND `deleted` = b'0'
  );

-- Post-checks: each column and active permission should return exactly one row.
SELECT `COLUMN_NAME`, `COLUMN_TYPE`, `IS_NULLABLE`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @current_schema
  AND TABLE_NAME = 'erp_sale_cart'
  AND COLUMN_NAME IN ('billing_confirm_user_id', 'billing_confirm_time')
ORDER BY `ORDINAL_POSITION`;

SELECT `id`, `name`, `parent_id`, `sort`, `permission`
FROM `system_menu`
WHERE `permission` = 'erp:sale-cart:confirm'
  AND `deleted` = b'0';

SELECT `role_id`, `menu_id`, `tenant_id`
FROM `system_role_menu`
WHERE `role_id` = 1
  AND `menu_id` = @billing_confirm_menu_id
  AND `tenant_id` = 1
  AND `deleted` = b'0';
