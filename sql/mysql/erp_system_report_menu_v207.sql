-- ERP 系统报表菜单权限 v207
--
-- Safety:
--   - No DELETE/TRUNCATE.
--   - Only creates or repairs the target "系统报表" menu and query/export permissions.
--   - Tenant packages and role permissions are appended only when they already own ERP root
--     permissions, or when the role is active super_admin.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (`id` = 2563 OR (`name` = 'ERP 系统' AND `path` = '/erp'))
  ORDER BY (`id` = 2563) DESC, `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '系统报表', '', 2, 2, @erp_parent_id, 'system-report', 'ep:data-analysis',
       'erp/report/system/index', 'ErpSystemReport',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `component` = 'erp/report/system/index'
      AND `deleted` = b'0'
  );

SET @system_report_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/report/system/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '系统报表',
    `parent_id` = @erp_parent_id,
    `sort` = 2,
    `path` = 'system-report',
    `icon` = 'ep:data-analysis',
    `component_name` = 'ErpSystemReport',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @erp_parent_id IS NOT NULL
  AND `id` = @system_report_menu_id
  AND `deleted` = b'0';

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '系统报表查询', 'erp:system-report:query', 3, 1, @system_report_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @system_report_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:system-report:query'
      AND `deleted` = b'0'
  );

SET @system_report_query_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:system-report:query'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @system_report_menu_id,
    `type` = 3,
    `sort` = 1,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @system_report_menu_id IS NOT NULL
  AND `id` = @system_report_query_menu_id
  AND `deleted` = b'0';

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '系统报表导出', 'erp:system-report:export', 3, 2, @system_report_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @system_report_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:system-report:export'
      AND `deleted` = b'0'
  );

SET @system_report_export_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:system-report:export'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @system_report_menu_id,
    `type` = 3,
    `sort` = 2,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @system_report_menu_id IS NOT NULL
  AND `id` = @system_report_export_menu_id
  AND `deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_system_report_targets_v207;
CREATE TEMPORARY TABLE tmp_erp_system_report_targets_v207 (
  menu_id bigint NOT NULL PRIMARY KEY
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO tmp_erp_system_report_targets_v207 (menu_id)
SELECT `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND (`id` = @system_report_menu_id OR `id` = @system_report_query_menu_id OR `id` = @system_report_export_menu_id);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_system_report_package_append_v207;
CREATE TEMPORARY TABLE tmp_erp_system_report_package_append_v207 (
  package_id bigint NOT NULL PRIMARY KEY,
  missing_menu_ids varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_system_report_package_append_v207 (package_id, missing_menu_ids)
SELECT package_menu.package_id, JSON_ARRAYAGG(package_menu.menu_id)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.menu_id
  FROM `system_tenant_package` tenant_package
  JOIN tmp_erp_system_report_targets_v207 target
    ON TRUE
  WHERE tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
    AND @erp_parent_id IS NOT NULL
    AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@erp_parent_id AS CHAR), '$')
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.menu_id AS CHAR), '$')
) package_menu
GROUP BY package_menu.package_id;

UPDATE `system_tenant_package` tenant_package
JOIN tmp_erp_system_report_package_append_v207 package_append
  ON package_append.package_id = tenant_package.`id`
SET tenant_package.`menu_ids` = JSON_MERGE_PRESERVE(tenant_package.`menu_ids`, package_append.missing_menu_ids)
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND JSON_VALID(package_append.missing_menu_ids)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) + CHAR_LENGTH(package_append.missing_menu_ids) < 4000;

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT source_role.`role_id`, target.`menu_id`, '1', NOW(), '1', NOW(), b'0', source_role.`tenant_id`
FROM (
  SELECT role_menu.`role_id`, role_menu.`tenant_id`
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (owned_menu.`id` = @erp_parent_id OR owned_menu.`parent_id` = @erp_parent_id)
  UNION
  SELECT role.`id`, role.`tenant_id`
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
) source_role
JOIN tmp_erp_system_report_targets_v207 target
  ON TRUE
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` existing
  WHERE existing.`role_id` = source_role.`role_id`
    AND existing.`menu_id` = target.`menu_id`
    AND existing.`tenant_id` = source_role.`tenant_id`
    AND existing.`deleted` = b'0'
);

SELECT m.`id`, m.`name`, m.`permission`, m.`type`, m.`parent_id`, m.`component`, m.`status`, m.`visible`, m.`deleted`
FROM `system_menu` m
WHERE m.`id` IN (@system_report_menu_id, @system_report_query_menu_id, @system_report_export_menu_id)
ORDER BY m.`parent_id`, m.`type`, m.`sort`, m.`id`;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_system_report_targets_v207;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_system_report_package_append_v207;
