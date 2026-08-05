-- Form data permission model.
-- Execute once, then refresh menu cache and re-login.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `form_data_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_type` varchar(64) NOT NULL COMMENT '业务表名',
  `form_id` bigint NOT NULL COMMENT '业务表单ID',
  `user_id` bigint NOT NULL COMMENT '授权用户ID',
  `relation` varchar(32) NOT NULL COMMENT '关系: creator/mentioned/approver/cc/shared',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_user_relation` (`tenant_id`, `form_type`, `form_id`, `user_id`, `relation`, `deleted`),
  KEY `idx_user_type` (`user_id`, `form_type`),
  KEY `idx_form` (`form_type`, `form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单数据权限关系表';

CREATE TABLE IF NOT EXISTS `form_permission_table_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_type` varchar(64) NOT NULL COMMENT '业务表名',
  `table_desc` varchar(128) DEFAULT '' COMMENT '表说明',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用权限过滤',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_type` (`form_type`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单权限接入表配置';

CREATE TABLE IF NOT EXISTS `form_permission_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_type` varchar(64) NOT NULL COMMENT '业务表名',
  `column_name` varchar(64) NOT NULL COMMENT '数据库列名',
  `column_desc` varchar(128) DEFAULT '' COMMENT '字段说明',
  `value_type` varchar(20) NOT NULL DEFAULT 'single_id' COMMENT 'single_id/csv_ids/json_ids',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_column` (`form_type`, `column_name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单权限相关人字段配置';

SET @erp_root_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = 'ERP 系统'
    AND `type` = 1
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @erp_system_config_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '系统配置'
    AND `type` = 1
    AND `parent_id` = @erp_root_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
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
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '系统配置'
    AND `type` = 1
    AND `parent_id` = @erp_root_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @form_data_permission_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` IN ('erp/system/form-data-permission/index', 'system/formDataPermission/index')
    AND `deleted` = b'0'
  ORDER BY FIELD(`component`, 'erp/system/form-data-permission/index', 'system/formDataPermission/index'), `id`
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '表单数据权限配置',
    `sort` = 45,
    `parent_id` = @erp_system_config_id,
    `path` = 'form-data-permission',
    `icon` = 'ep:lock',
    `component` = 'erp/system/form-data-permission/index',
    `component_name` = 'ErpFormDataPermission',
    `updater` = '1',
    `update_time` = NOW()
WHERE @erp_system_config_id IS NOT NULL
  AND @form_data_permission_menu_id IS NOT NULL
  AND `id` = @form_data_permission_menu_id
  AND `deleted` = b'0'
  AND (
    IFNULL(`name`, '') <> '表单数据权限配置'
    OR `sort` <> 45
    OR `parent_id` <> @erp_system_config_id
    OR IFNULL(`path`, '') <> 'form-data-permission'
    OR IFNULL(`icon`, '') <> 'ep:lock'
    OR IFNULL(`component`, '') <> 'erp/system/form-data-permission/index'
    OR IFNULL(`component_name`, '') <> 'ErpFormDataPermission'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '表单数据权限配置', '', 2, 45, @erp_system_config_id, 'form-data-permission', 'ep:lock',
       'erp/system/form-data-permission/index', 'ErpFormDataPermission',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `component` IN ('erp/system/form-data-permission/index', 'system/formDataPermission/index') AND `deleted` = b'0'
);

SET @form_data_permission_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/form-data-permission/index' AND `deleted` = b'0'
  ORDER BY `id` LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @form_data_permission_menu_id,
    `updater` = '1',
    `update_time` = NOW()
WHERE @form_data_permission_menu_id IS NOT NULL
  AND `permission` IN ('system:form-data-permission:query',
                       'system:form-data-permission:config',
                       'system:form-data-permission:rebuild')
  AND `deleted` = b'0'
  AND `parent_id` <> @form_data_permission_menu_id;

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '表单数据权限查询', 'system:form-data-permission:query', 3, 1,
       @form_data_permission_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @form_data_permission_menu_id IS NOT NULL
  AND NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'system:form-data-permission:query' AND `deleted` = b'0'
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '表单数据权限配置', 'system:form-data-permission:config', 3, 2,
       @form_data_permission_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @form_data_permission_menu_id IS NOT NULL
  AND NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'system:form-data-permission:config' AND `deleted` = b'0'
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '表单权限数据重建', 'system:form-data-permission:rebuild', 3, 3,
       @form_data_permission_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @form_data_permission_menu_id IS NOT NULL
  AND NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'system:form-data-permission:rebuild' AND `deleted` = b'0'
);

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, `id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu`
WHERE (`id` IN (@erp_system_config_id, @form_data_permission_menu_id)
   OR `permission` IN ('system:form-data-permission:query',
                       'system:form-data-permission:config',
                       'system:form-data-permission:rebuild'))
  AND `deleted` = b'0';
