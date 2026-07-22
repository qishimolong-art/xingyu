-- ERP product custom-field standalone menu (v82)
-- Safe to execute repeatedly.
-- Purpose:
--   - Add "新增自定义字段" as a standalone page under ERP / 系统配置.
--   - Move the existing create-custom-field permission under that page.
--   - Grant the page to roles that already own ERP field-config query/update/create permissions,
--     plus role_id=1. No broad DELETE and no blanket role grant.

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

SET @custom_field_page_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/custom-field' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = CONVERT(0xE696B0E5A29EE887AAE5AE9AE4B989E5AD97E6AEB5 USING utf8mb4),
    `permission` = '',
    `type` = 2,
    `sort` = 2,
    `parent_id` = @erp_system_config_id,
    `path` = 'fieldconfig/custom-field',
    `icon` = 'ep:plus',
    `component` = 'erp/system/fieldconfig/custom-field',
    `component_name` = 'ErpProductCustomFieldCreate',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @erp_system_config_id IS NOT NULL
  AND `id` = @custom_field_page_id
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT CONVERT(0xE696B0E5A29EE887AAE5AE9AE4B989E5AD97E6AEB5 USING utf8mb4),
       '',
       2,
       2,
       @erp_system_config_id,
       'fieldconfig/custom-field',
       'ep:plus',
       'erp/system/fieldconfig/custom-field',
       'ErpProductCustomFieldCreate',
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
  AND @custom_field_page_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/custom-field' COLLATE utf8mb4_unicode_ci
        AND `deleted` = b'0'
  );

SET @custom_field_page_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` COLLATE utf8mb4_unicode_ci = 'erp/system/fieldconfig/custom-field' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @custom_field_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:create-custom-field' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = CONVERT(0xE696B0E5A29EE887AAE5AE9AE4B989E5AD97E6AEB5 USING utf8mb4),
    `type` = 3,
    `sort` = 1,
    `parent_id` = @custom_field_page_id,
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
WHERE @custom_field_page_id IS NOT NULL
  AND `id` = @custom_field_button_id
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT CONVERT(0xE696B0E5A29EE887AAE5AE9AE4B989E5AD97E6AEB5 USING utf8mb4),
       'erp:field-config:create-custom-field',
       3,
       1,
       @custom_field_page_id,
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
WHERE @custom_field_page_id IS NOT NULL
  AND @custom_field_button_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:create-custom-field' COLLATE utf8mb4_unicode_ci
        AND `deleted` = b'0'
  );

SET @custom_field_button_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` COLLATE utf8mb4_unicode_ci = 'erp:field-config:create-custom-field' COLLATE utf8mb4_unicode_ci
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_custom_field_menu_roles;
CREATE TEMPORARY TABLE tmp_erp_custom_field_menu_roles (
  role_id bigint NOT NULL,
  tenant_id bigint NOT NULL,
  PRIMARY KEY (role_id, tenant_id)
) ENGINE = MEMORY;

INSERT IGNORE INTO tmp_erp_custom_field_menu_roles (role_id, tenant_id)
SELECT DISTINCT role_menu.`role_id`, role_menu.`tenant_id`
FROM `system_role_menu` role_menu
JOIN `system_menu` menu
  ON menu.`id` = role_menu.`menu_id`
 AND menu.`deleted` = b'0'
WHERE role_menu.`deleted` = b'0'
  AND menu.`permission` COLLATE utf8mb4_unicode_ci IN (
      'erp:field-config:query' COLLATE utf8mb4_unicode_ci,
      'erp:field-config:update' COLLATE utf8mb4_unicode_ci,
      'erp:field-config:create-custom-field' COLLATE utf8mb4_unicode_ci
  );

INSERT IGNORE INTO tmp_erp_custom_field_menu_roles (role_id, tenant_id)
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
FROM tmp_erp_custom_field_menu_roles roles
JOIN (
    SELECT @erp_system_config_id AS menu_id
    UNION ALL
    SELECT @custom_field_page_id
    UNION ALL
    SELECT @custom_field_button_id
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

DROP TEMPORARY TABLE IF EXISTS tmp_erp_custom_field_menu_roles;
