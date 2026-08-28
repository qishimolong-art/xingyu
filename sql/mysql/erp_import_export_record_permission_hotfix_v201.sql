-- ERP import/export record query permission hotfix (v201).
--
-- Purpose:
--   Backfill missing query button permissions for roles and tenant packages that
--   already contain the import/export record pages. This fixes users who can
--   open the pages but receive 403 from the record query APIs.
--
-- Safety:
--   - No DELETE.
--   - No fixed menu ids.
--   - Grants only roles that already have the matching page, the ERP system
--     configuration parent, or super_admin.
--   - Does not overwrite tenant package menu_ids; only appends missing ids.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TEMPORARY TABLE tmp_erp_import_export_record_permission_hotfix (
  `name` varchar(100) NOT NULL,
  `component` varchar(200) NOT NULL,
  `query_permission` varchar(100) NOT NULL,
  `query_name` varchar(100) NOT NULL,
  PRIMARY KEY (`component`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_erp_import_export_record_permission_hotfix
(`name`, `component`, `query_permission`, `query_name`)
VALUES
('导入记录', 'erp/system/import-record/index', 'erp:import-record:query', '导入记录查询'),
('导出记录', 'erp/system/export-record/index', 'erp:export-record:query', '导出记录查询');

SET @erp_root_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = 'ERP 系统'
    AND `type` = 1
    AND `parent_id` = 0
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @erp_system_config_parent_id := (
  SELECT page.`parent_id`
  FROM tmp_erp_import_export_record_permission_hotfix t
  JOIN `system_menu` page
    ON page.`component` = t.`component`
   AND page.`deleted` = b'0'
  ORDER BY page.`id` DESC
  LIMIT 1
);

SET @erp_system_config_parent_id := COALESCE(@erp_system_config_parent_id, (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '系统配置'
    AND `type` = 1
    AND `parent_id` = @erp_root_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
));

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.`query_name`, t.`query_permission`, 3, 1, page.`id`, '', '', '', '',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM tmp_erp_import_export_record_permission_hotfix t
JOIN `system_menu` page
  ON page.`component` = t.`component`
 AND page.`deleted` = b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_menu` existing
  WHERE existing.`permission` = t.`query_permission`
    AND existing.`parent_id` = page.`id`
    AND existing.`deleted` = b'0'
);

CREATE TEMPORARY TABLE tmp_erp_import_export_record_role_grants (
  `role_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `tenant_id`, `menu_id`)
) ENGINE=MEMORY;

INSERT IGNORE INTO tmp_erp_import_export_record_role_grants (`role_id`, `tenant_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, rm.`tenant_id`, button.`id`
FROM tmp_erp_import_export_record_permission_hotfix t
JOIN `system_menu` page
  ON page.`component` = t.`component`
 AND page.`deleted` = b'0'
JOIN `system_menu` button
  ON button.`permission` = t.`query_permission`
 AND button.`deleted` = b'0'
JOIN `system_role_menu` rm
  ON rm.`menu_id` = page.`id`
 AND rm.`deleted` = b'0';

INSERT IGNORE INTO tmp_erp_import_export_record_role_grants (`role_id`, `tenant_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, rm.`tenant_id`, button.`id`
FROM tmp_erp_import_export_record_permission_hotfix t
JOIN `system_menu` button
  ON button.`permission` = t.`query_permission`
 AND button.`deleted` = b'0'
JOIN `system_role_menu` rm
  ON rm.`menu_id` = @erp_system_config_parent_id
 AND rm.`deleted` = b'0'
WHERE @erp_system_config_parent_id IS NOT NULL;

INSERT IGNORE INTO tmp_erp_import_export_record_role_grants (`role_id`, `tenant_id`, `menu_id`)
SELECT role.`id`, role.`tenant_id`, button.`id`
FROM `system_role` role
JOIN tmp_erp_import_export_record_permission_hotfix t
JOIN `system_menu` button
  ON button.`permission` = t.`query_permission`
 AND button.`deleted` = b'0'
WHERE role.`code` = 'super_admin'
  AND role.`deleted` = b'0'
  AND role.`status` = 0;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT grant_scope.`role_id`, grant_scope.`menu_id`, '1', NOW(), '1', NOW(), b'0', grant_scope.`tenant_id`
FROM tmp_erp_import_export_record_role_grants grant_scope
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` existing
  WHERE existing.`role_id` = grant_scope.`role_id`
    AND existing.`menu_id` = grant_scope.`menu_id`
    AND existing.`tenant_id` = grant_scope.`tenant_id`
    AND existing.`deleted` = b'0'
);

CREATE TEMPORARY TABLE tmp_erp_import_export_record_package_grants (
  `package_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`package_id`, `menu_id`)
) ENGINE=MEMORY;

INSERT IGNORE INTO tmp_erp_import_export_record_package_grants (`package_id`, `menu_id`)
SELECT tenant_package.`id`, button.`id`
FROM `system_tenant_package` tenant_package
JOIN tmp_erp_import_export_record_permission_hotfix t
JOIN `system_menu` page
  ON page.`component` = t.`component`
 AND page.`deleted` = b'0'
JOIN `system_menu` button
  ON button.`permission` = t.`query_permission`
 AND button.`deleted` = b'0'
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(page.`id` AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(button.`id` AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
JOIN tmp_erp_import_export_record_package_grants grant_scope
  ON grant_scope.`package_id` = tenant_package.`id`
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', grant_scope.`menu_id`)
WHERE tenant_package.`deleted` = b'0';

SELECT page.`id` AS `page_id`,
       page.`name` AS `page_name`,
       button.`id` AS `query_button_id`,
       button.`permission` AS `query_permission`,
       COUNT(DISTINCT rm.`role_id`) AS `granted_role_count`
FROM tmp_erp_import_export_record_permission_hotfix t
JOIN `system_menu` page
  ON page.`component` = t.`component`
 AND page.`deleted` = b'0'
LEFT JOIN `system_menu` button
  ON button.`permission` = t.`query_permission`
 AND button.`deleted` = b'0'
LEFT JOIN `system_role_menu` rm
  ON rm.`menu_id` = button.`id`
 AND rm.`deleted` = b'0'
GROUP BY page.`id`, page.`name`, button.`id`, button.`permission`
ORDER BY page.`id`;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_export_record_package_grants;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_export_record_role_grants;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_export_record_permission_hotfix;
