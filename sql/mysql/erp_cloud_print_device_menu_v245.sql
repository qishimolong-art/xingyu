-- 云打印设备管理页菜单与权限（v245）。
-- 可重复执行；只追加/修正 ERP / 系统配置 / 云打印设备菜单及超级管理员授权，
-- 不修改云打印设备、仓库绑定、普通角色权限或历史打印任务。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_root_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = 'ERP 系统' AND `type` = 1 AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

SET @erp_system_config_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = '系统配置' AND `type` = 1 AND `deleted` = b'0'
    AND `parent_id` = @erp_root_id
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '系统配置', '', 1, 90, @erp_root_id, 'system', 'ep:setting', '', '',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_root_id IS NOT NULL
  AND @erp_system_config_id IS NULL;

SET @erp_system_config_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = '系统配置' AND `type` = 1 AND `deleted` = b'0'
    AND `parent_id` = @erp_root_id
  ORDER BY `id` DESC LIMIT 1
);

SET @cloud_print_device_page_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/cloud-print-device/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '云打印设备', '', 2, 38, @erp_system_config_id, 'cloud-print-device', 'ep:printer',
       'erp/system/cloud-print-device/index', 'ErpCloudPrintDevice',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND @cloud_print_device_page_id IS NULL;

SET @cloud_print_device_page_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/cloud-print-device/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

UPDATE `system_menu`
SET `name` = '云打印设备',
    `permission` = '',
    `type` = 2,
    `sort` = 38,
    `parent_id` = @erp_system_config_id,
    `path` = 'cloud-print-device',
    `icon` = 'ep:printer',
    `component_name` = 'ErpCloudPrintDevice',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = @cloud_print_device_page_id
  AND @erp_system_config_id IS NOT NULL;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_cloud_print_device_permissions_v245 (
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `permission` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort` int NOT NULL,
  PRIMARY KEY (`permission`)
) ENGINE=Memory;

TRUNCATE TABLE tmp_cloud_print_device_permissions_v245;

INSERT INTO tmp_cloud_print_device_permissions_v245 (`name`, `permission`, `sort`) VALUES
('云打印设备查询', 'erp:cloud-print-device:query', 1),
('云打印设备新增', 'erp:cloud-print-device:create', 2),
('云打印设备修改', 'erp:cloud-print-device:update', 3),
('云打印设备删除', 'erp:cloud-print-device:delete', 4),
('云打印设备状态修改', 'erp:cloud-print-device:status', 5),
('设置默认云打印设备', 'erp:cloud-print-device:default', 6),
('刷新云打印设备状态', 'erp:cloud-print-device:refresh', 7);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT perm.`name`, perm.`permission`, 3, perm.`sort`, @cloud_print_device_page_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM tmp_cloud_print_device_permissions_v245 perm
WHERE @cloud_print_device_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu` menu
    WHERE menu.`permission` = perm.`permission` AND menu.`deleted` = b'0'
  );

UPDATE `system_menu` menu
JOIN tmp_cloud_print_device_permissions_v245 perm ON perm.`permission` = menu.`permission`
SET menu.`name` = perm.`name`,
    menu.`type` = 3,
    menu.`sort` = perm.`sort`,
    menu.`parent_id` = @cloud_print_device_page_id,
    menu.`status` = 0,
    menu.`visible` = b'1',
    menu.`updater` = '1',
    menu.`update_time` = NOW()
WHERE menu.`deleted` = b'0'
  AND @cloud_print_device_page_id IS NOT NULL;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT super_role.`id`, menu.`id`, '1', NOW(), '1', NOW(), b'0', super_role.`tenant_id`
FROM `system_role` super_role
JOIN `system_menu` menu
  ON (menu.`id` IN (@erp_system_config_id, @cloud_print_device_page_id)
      OR menu.`permission` IN (
        'erp:cloud-print-device:query',
        'erp:cloud-print-device:create',
        'erp:cloud-print-device:update',
        'erp:cloud-print-device:delete',
        'erp:cloud-print-device:status',
        'erp:cloud-print-device:default',
        'erp:cloud-print-device:refresh'
      ))
 AND menu.`deleted` = b'0'
WHERE super_role.`code` = 'super_admin'
  AND super_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` existing
    WHERE existing.`role_id` = super_role.`id`
      AND existing.`menu_id` = menu.`id`
      AND existing.`tenant_id` = super_role.`tenant_id`
      AND existing.`deleted` = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_cloud_print_device_permissions_v245;

SELECT m.`id`, m.`name`, m.`path`, m.`component`, m.`permission`, m.`parent_id`, m.`sort`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (m.`component` = 'erp/system/cloud-print-device/index'
       OR m.`permission` LIKE 'erp:cloud-print-device:%')
ORDER BY m.`parent_id`, m.`sort`, m.`id`;
