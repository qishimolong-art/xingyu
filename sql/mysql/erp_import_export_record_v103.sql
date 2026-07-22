-- ERP import/export record tables and system-configuration menus.
--
-- Safety:
--   - No DELETE.
--   - No fixed menu ids.
--   - No broad role grants.
--   - No overwrite of tenant package menu_ids.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_import_export_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operation_type` varchar(20) NOT NULL COMMENT '操作类型：IMPORT/EXPORT',
  `module_key` varchar(64) NOT NULL COMMENT '模块编码',
  `module_name` varchar(64) NOT NULL COMMENT '模块名称',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `file_type` varchar(20) DEFAULT NULL COMMENT '文件类型',
  `status` varchar(30) NOT NULL COMMENT '执行状态',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总条数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功条数',
  `failure_count` int NOT NULL DEFAULT 0 COMMENT '失败条数',
  `create_count` int NOT NULL DEFAULT 0 COMMENT '新增条数',
  `update_count` int NOT NULL DEFAULT 0 COMMENT '更新条数',
  `query_params` text DEFAULT NULL COMMENT '查询条件 JSON',
  `export_fields` text DEFAULT NULL COMMENT '导出字段 JSON/文本',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '整体失败原因',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人编号',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人名称',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_type_module_time` (`tenant_id`, `operation_type`, `module_key`, `create_time`),
  KEY `idx_operator_time` (`tenant_id`, `operator_id`, `create_time`),
  KEY `idx_status_time` (`tenant_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 导入导出记录';

CREATE TABLE IF NOT EXISTS `erp_import_export_record_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_id` bigint NOT NULL COMMENT '主记录编号',
  `row_no` int DEFAULT NULL COMMENT '行号',
  `biz_key` varchar(128) DEFAULT NULL COMMENT '业务标识',
  `biz_name` varchar(255) DEFAULT NULL COMMENT '业务名称',
  `failure_reason` varchar(1000) NOT NULL COMMENT '失败原因',
  `raw_data` text DEFAULT NULL COMMENT '原始行数据 JSON',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`tenant_id`, `record_id`),
  KEY `idx_biz_key` (`tenant_id`, `biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 导入导出记录明细';

SET @erp_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = 'ERP 系统'
    AND `type` = 1
    AND `parent_id` = 0
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @system_config_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '系统配置'
    AND `type` = 1
    AND `parent_id` = @erp_parent_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '系统配置', '', 1, 90, @erp_parent_id, 'system', 'ep:setting',
  '', '',
  0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_parent_id IS NOT NULL
  AND @system_config_parent_id IS NULL;

SET @system_config_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '系统配置'
    AND `type` = 1
    AND `parent_id` = @erp_parent_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

CREATE TEMPORARY TABLE tmp_erp_import_export_record_menu (
  `name` varchar(100) NOT NULL,
  `path` varchar(100) NOT NULL,
  `component` varchar(200) NOT NULL,
  `component_name` varchar(100) NOT NULL,
  `icon` varchar(100) NOT NULL,
  `sort` int NOT NULL,
  `query_permission` varchar(100) NOT NULL,
  `query_name` varchar(100) NOT NULL,
  PRIMARY KEY (`component`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_erp_import_export_record_menu
(`name`, `path`, `component`, `component_name`, `icon`, `sort`, `query_permission`, `query_name`)
VALUES
('导入记录', 'import-record', 'erp/system/import-record/index', 'ErpImportRecord', 'ep:upload', 100, 'erp:import-record:query', '导入记录查询'),
('导出记录', 'export-record', 'erp/system/export-record/index', 'ErpExportRecord', 'ep:download', 110, 'erp:export-record:query', '导出记录查询');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.`name`, '', 2, t.`sort`, @system_config_parent_id, t.`path`, t.`icon`, t.`component`, t.`component_name`,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM tmp_erp_import_export_record_menu t
WHERE @system_config_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing
    WHERE existing.`component` = t.`component`
      AND existing.`deleted` = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.`query_name`, t.`query_permission`, 3, 1, page.`id`, '', '', '', '',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM tmp_erp_import_export_record_menu t
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

CREATE TEMPORARY TABLE tmp_erp_import_export_record_grant_menu_ids (
  `menu_id` bigint NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT IGNORE INTO tmp_erp_import_export_record_grant_menu_ids (`menu_id`)
SELECT @system_config_parent_id
FROM DUAL
WHERE @system_config_parent_id IS NOT NULL;

INSERT IGNORE INTO tmp_erp_import_export_record_grant_menu_ids (`menu_id`)
SELECT page.`id`
FROM tmp_erp_import_export_record_menu t
JOIN `system_menu` page
  ON page.`component` = t.`component`
 AND page.`deleted` = b'0';

INSERT IGNORE INTO tmp_erp_import_export_record_grant_menu_ids (`menu_id`)
SELECT button.`id`
FROM tmp_erp_import_export_record_menu t
JOIN `system_menu` button
  ON button.`permission` = t.`query_permission`
 AND button.`deleted` = b'0';

CREATE TEMPORARY TABLE tmp_erp_import_export_record_grant_roles (
  `role_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `tenant_id`)
) ENGINE=MEMORY;

INSERT IGNORE INTO tmp_erp_import_export_record_grant_roles (`role_id`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, rm.`tenant_id`
FROM `system_role_menu` rm
WHERE rm.`deleted` = b'0'
  AND @system_config_parent_id IS NOT NULL
  AND (
    rm.`menu_id` = @system_config_parent_id
    OR rm.`menu_id` IN (
      SELECT child.`id`
      FROM `system_menu` child
      WHERE child.`parent_id` = @system_config_parent_id
        AND child.`deleted` = b'0'
    )
  );

INSERT IGNORE INTO tmp_erp_import_export_record_grant_roles (`role_id`, `tenant_id`)
SELECT role.`id`, role.`tenant_id`
FROM `system_role` role
WHERE role.`code` = 'super_admin'
  AND role.`deleted` = b'0'
  AND role.`status` = 0;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT role_scope.`role_id`, menu_scope.`menu_id`, '1', NOW(), '1', NOW(), b'0', role_scope.`tenant_id`
FROM tmp_erp_import_export_record_grant_roles role_scope
JOIN tmp_erp_import_export_record_grant_menu_ids menu_scope
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` existing
  WHERE existing.`role_id` = role_scope.`role_id`
    AND existing.`menu_id` = menu_scope.`menu_id`
    AND existing.`tenant_id` = role_scope.`tenant_id`
    AND existing.`deleted` = b'0'
);

UPDATE `system_tenant_package` tenant_package
JOIN tmp_erp_import_export_record_grant_menu_ids menu_scope
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', menu_scope.`menu_id`)
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND @system_config_parent_id IS NOT NULL
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@system_config_parent_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(menu_scope.`menu_id` AS CHAR), '$');

SELECT child.`id`,
       child.`name`,
       parent.`name` AS `parent_name`,
       child.`permission`,
       child.`type`,
       child.`sort`,
       child.`component`,
       child.`component_name`,
       child.`status`,
       child.`visible`,
       child.`deleted`
FROM `system_menu` child
LEFT JOIN `system_menu` parent
  ON parent.`id` = child.`parent_id`
WHERE child.`component` IN ('erp/system/import-record/index', 'erp/system/export-record/index')
   OR child.`permission` IN ('erp:import-record:query', 'erp:export-record:query')
ORDER BY child.`parent_id`, child.`type`, child.`sort`, child.`id`;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_export_record_grant_roles;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_export_record_grant_menu_ids;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_import_export_record_menu;
