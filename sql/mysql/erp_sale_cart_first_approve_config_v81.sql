-- ERP sale cart first-approve switch and department authorization (v81)
-- Safety:
--   - Creates the page under ERP / system configuration, not under sale cart.
--   - Moves the save permission under the new page when it already exists.
--   - Appends role-menu relations only for roles already owning
--     first-approve-config permission, and grants role_id=1 as the default admin.
--     No DELETE and no blanket role grant.
--   - Inserts the default config only when it does not exist. No overwrite update.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_root_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` COLLATE utf8mb4_unicode_ci = CONVERT(0x45525020E7B3BBE7BB9F USING utf8mb4) COLLATE utf8mb4_unicode_ci
    AND `type` = 1
    AND `parent_id` = 0
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @erp_system_config_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` COLLATE utf8mb4_unicode_ci = CONVERT(0xE7B3BBE7BB9FE9858DE7BDAE USING utf8mb4) COLLATE utf8mb4_unicode_ci
    AND `type` = 1
    AND `parent_id` = @erp_root_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT CONVERT(0xE7B3BBE7BB9FE9858DE7BDAE USING utf8mb4),
       '',
       1,
       90,
       @erp_root_id,
       'system',
       'ep:setting',
       '',
       '',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @erp_root_id IS NOT NULL
  AND @erp_system_config_id IS NULL;

SET @erp_system_config_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` COLLATE utf8mb4_unicode_ci = CONVERT(0xE7B3BBE7BB9FE9858DE7BDAE USING utf8mb4) COLLATE utf8mb4_unicode_ci
    AND `type` = 1
    AND `parent_id` = @erp_root_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @sale_cart_first_approve_config_page_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/sale-cart-first-approve-config/index' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = CONVERT(0xE99480E594AEE6898BE68EA8E8BDA6E5889DE5AEA1E8AEBEE7BDAE USING utf8mb4),
    `permission` = '',
    `type` = 2,
    `sort` = 30,
    `parent_id` = @erp_system_config_id,
    `path` = 'sale-cart-first-approve-config',
    `icon` = 'ep:setting',
    `component` = 'erp/system/sale-cart-first-approve-config/index',
    `component_name` = 'ErpSaleCartFirstApproveConfig',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @erp_system_config_id IS NOT NULL
  AND `id` = @sale_cart_first_approve_config_page_id
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT CONVERT(0xE99480E594AEE6898BE68EA8E8BDA6E5889DE5AEA1E8AEBEE7BDAE USING utf8mb4),
       '',
       2,
       30,
       @erp_system_config_id,
       'sale-cart-first-approve-config',
       'ep:setting',
       'erp/system/sale-cart-first-approve-config/index',
       'ErpSaleCartFirstApproveConfig',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND @sale_cart_first_approve_config_page_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/sale-cart-first-approve-config/index' COLLATE utf8mb4_unicode_ci
        AND `deleted` = b'0'
  );

SET @sale_cart_first_approve_config_page_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/sale-cart-first-approve-config/index' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @sale_cart_first_approve_config_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` COLLATE utf8mb4_unicode_ci = 'erp:sale-cart:first-approve-config' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = CONVERT(0xE99480E594AEE6898BE68EA8E8BDA6E5889DE5AEA1E8AEBEE7BDAEE4BF9DE5AD98 USING utf8mb4),
    `type` = 3,
    `sort` = 1,
    `parent_id` = @sale_cart_first_approve_config_page_id,
    `path` = '',
    `icon` = '',
    `component` = '',
    `component_name` = NULL,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @sale_cart_first_approve_config_page_id IS NOT NULL
  AND `id` = @sale_cart_first_approve_config_button_id
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT CONVERT(0xE99480E594AEE6898BE68EA8E8BDA6E5889DE5AEA1E8AEBEE7BDAEE4BF9DE5AD98 USING utf8mb4),
       'erp:sale-cart:first-approve-config',
       3,
       1,
       @sale_cart_first_approve_config_page_id,
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
FROM DUAL
WHERE @sale_cart_first_approve_config_page_id IS NOT NULL
  AND @sale_cart_first_approve_config_button_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `permission` COLLATE utf8mb4_unicode_ci = 'erp:sale-cart:first-approve-config' COLLATE utf8mb4_unicode_ci
        AND `deleted` = b'0'
  );

SET @sale_cart_first_approve_config_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` COLLATE utf8mb4_unicode_ci = 'erp:sale-cart:first-approve-config' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_sale_cart_first_approve_config_roles;
CREATE TEMPORARY TABLE tmp_sale_cart_first_approve_config_roles (
  role_id bigint NOT NULL,
  tenant_id bigint NOT NULL,
  PRIMARY KEY (role_id, tenant_id)
) ENGINE = MEMORY;

INSERT IGNORE INTO tmp_sale_cart_first_approve_config_roles (role_id, tenant_id)
SELECT DISTINCT role_menu.`role_id`, role_menu.`tenant_id`
FROM `system_role_menu` role_menu
JOIN `system_menu` menu
  ON menu.`id` = role_menu.`menu_id`
 AND menu.`deleted` = b'0'
WHERE role_menu.`deleted` = b'0'
  AND menu.`permission` COLLATE utf8mb4_unicode_ci = 'erp:sale-cart:first-approve-config' COLLATE utf8mb4_unicode_ci;

INSERT IGNORE INTO tmp_sale_cart_first_approve_config_roles (role_id, tenant_id)
SELECT 1, 1
FROM DUAL;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT roles.`role_id`,
       target.`menu_id`,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       roles.`tenant_id`
FROM tmp_sale_cart_first_approve_config_roles roles
JOIN (
    SELECT @erp_system_config_id AS menu_id
    UNION ALL
    SELECT @sale_cart_first_approve_config_page_id
    UNION ALL
    SELECT @sale_cart_first_approve_config_button_id
) target
  ON target.`menu_id` IS NOT NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` exists_role_menu
    WHERE exists_role_menu.`role_id` = roles.`role_id`
      AND exists_role_menu.`tenant_id` = roles.`tenant_id`
      AND exists_role_menu.`menu_id` = target.`menu_id`
      AND exists_role_menu.`deleted` = b'0'
);

INSERT INTO erp_sale_config
(`config_type`, `code`, `name`, `config_value`, `status`, `dept_id`, `sort`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'SALE_CART_FIRST_APPROVE',
       'GLOBAL',
       CONVERT(0xE99480E594AEE6898BE68EA8E8BDA6E5889DE5AEA1E8AEBEE7BDAE USING utf8mb4),
       '{"enabled":true,"deptAuthEnabled":false,"includeChildDept":true}',
       0,
       NULL,
       0,
       CONVERT(0xE68EA7E588B6E99480E594AEE6898BE68EA8E8BDA6E698AFE590A6E99C80E8A681E5889DE5AEA1E58F8AE5889DE5AEA1E983A8E997A8E68E88E69D83 USING utf8mb4),
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       1
WHERE NOT EXISTS (
    SELECT 1
    FROM erp_sale_config
    WHERE config_type COLLATE utf8mb4_unicode_ci = 'SALE_CART_FIRST_APPROVE' COLLATE utf8mb4_unicode_ci
      AND code COLLATE utf8mb4_unicode_ci = 'GLOBAL' COLLATE utf8mb4_unicode_ci
      AND tenant_id = 1
      AND deleted = b'0'
);

DROP TEMPORARY TABLE IF EXISTS tmp_sale_cart_first_approve_config_roles;
