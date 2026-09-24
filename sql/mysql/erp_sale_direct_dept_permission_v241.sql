-- v241: sale direct document forbidden department permission.
-- Purpose: configure departments that cannot directly create sale documents.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_sale_direct_forbidden_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `dept_id` bigint NOT NULL COMMENT 'Forbidden sale document department ID',
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_tenant_deleted` (`dept_id`, `tenant_id`, `deleted`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP sale direct document forbidden department';

-- ERP / 系统配置目录，按业务标识动态定位，不依赖环境中的固定菜单 ID。
SET @erp_root_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND `name` = 'ERP 系统'
  ORDER BY `id` DESC LIMIT 1
);

SET @erp_system_config_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND `name` = '系统配置'
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
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND `name` = '系统配置'
    AND `parent_id` = @erp_root_id
  ORDER BY `id` DESC LIMIT 1
);

SET @sale_dept_permission_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `component` = 'erp/sale/dept-permission/index'
      OR `component_name` = 'ErpSaleDeptPermission'
    )
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '销售开单部门权限', '', 2, 38, @erp_system_config_id, 'dept-permission', 'fa:users',
       'erp/sale/dept-permission/index', 'ErpSaleDeptPermission',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND @sale_dept_permission_id IS NULL;

SET @sale_dept_permission_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `component` = 'erp/sale/dept-permission/index'
      OR `component_name` = 'ErpSaleDeptPermission'
    )
  ORDER BY `id` DESC LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @erp_system_config_id,
    `sort` = 38,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = @sale_dept_permission_id
  AND @erp_system_config_id IS NOT NULL
  AND (`parent_id` <> @erp_system_config_id OR `sort` <> 38);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '销售开单部门权限查询', 'erp:sale-dept-permission:query', 3, 1, @sale_dept_permission_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @sale_dept_permission_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'erp:sale-dept-permission:query'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '销售开单部门权限更新', 'erp:sale-dept-permission:update', 3, 2, @sale_dept_permission_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @sale_dept_permission_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'erp:sale-dept-permission:update'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, m.`id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (
    m.`component_name` = 'ErpSaleDeptPermission'
    OR m.`permission` IN ('erp:sale-dept-permission:query', 'erp:sale-dept-permission:update')
  )
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.`role_id` = 1
      AND rm.`menu_id` = m.`id`
      AND rm.`tenant_id` = 1
      AND rm.`deleted` = b'0'
  );

-- Deployment check: should return the page menu and its two button permissions.
SELECT `id`, `name`, `parent_id`, `path`, `component`, `component_name`, `permission`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND (
    `component_name` = 'ErpSaleDeptPermission'
    OR `permission` IN ('erp:sale-dept-permission:query', 'erp:sale-dept-permission:update')
  )
ORDER BY `parent_id`, `sort`, `id`;
