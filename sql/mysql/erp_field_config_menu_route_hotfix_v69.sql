-- ERP field config menu route hotfix v69.
--
-- Root cause:
-- erp_common_config_v42.sql previously changed menu id 2961 to
-- component = 'erp/config/search/index' / component_name = 'ErpSearchFieldConfig'.
-- The frontend has no matching view under ui/apps/web-antd/src/views/erp/config/search,
-- so opening ERP system config -> field config falls through to the 404 page.
--
-- Safe to execute repeatedly. This script only repairs the field-config menu route
-- and appends missing super-admin menu grants. It does not delete menus or overwrite
-- unrelated role permissions.

UPDATE `system_menu`
SET `name` = '字段配置',
    `permission` = '',
    `type` = 2,
    `path` = 'fieldconfig',
    `icon` = 'fa:list-alt',
    `component` = 'erp/system/fieldconfig/index',
    `component_name` = 'ErpFieldConfig',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 2961;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, menu_id, '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 2961 AS menu_id
    UNION ALL SELECT 2962
    UNION ALL SELECT 2963
) field_config_menus
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` srm
    WHERE srm.`role_id` = 1
      AND srm.`menu_id` = field_config_menus.menu_id
      AND srm.`tenant_id` = 1
      AND srm.`deleted` = b'0'
);

SELECT `id`, `name`, `path`, `component`, `component_name`, `permission`
FROM `system_menu`
WHERE `id` = 2961;
