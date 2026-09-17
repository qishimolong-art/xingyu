-- ERP 自动核销配置：按部门关闭收款/付款审批后的系统自动核销
-- 可重复执行；只追加缺失菜单、权限和超级管理员授权，不覆盖普通角色权限。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_auto_write_off_dept_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dept_id` bigint NOT NULL COMMENT '关闭自动核销的部门编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_dept` (`tenant_id`, `dept_id`),
  KEY `idx_tenant_deleted` (`tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='ERP自动核销关闭部门配置';

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

SET @auto_write_off_config_page_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/auto-write-off-config/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '自动核销配置', '', 2, 37, @erp_system_config_id, 'auto-write-off-config', 'ep:operation',
       'erp/system/auto-write-off-config/index', 'ErpAutoWriteOffConfig',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND @auto_write_off_config_page_id IS NULL;

SET @auto_write_off_config_page_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/auto-write-off-config/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '查询自动核销配置', 'erp:auto-write-off-config:query', 3, 1, @auto_write_off_config_page_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @auto_write_off_config_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:auto-write-off-config:query' AND `deleted` = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '保存自动核销配置', 'erp:auto-write-off-config:update', 3, 2, @auto_write_off_config_page_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @auto_write_off_config_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:auto-write-off-config:update' AND `deleted` = b'0'
  );

-- 新菜单和按钮仅追加授予各租户超级管理员，不影响普通角色已有权限。
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT super_role.`id`, menu.`id`, '1', NOW(), '1', NOW(), b'0', super_role.`tenant_id`
FROM `system_role` super_role
JOIN `system_menu` menu
  ON (menu.`id` = @erp_system_config_id
      OR menu.`id` = @auto_write_off_config_page_id
      OR menu.`permission` IN (
        'erp:auto-write-off-config:query',
        'erp:auto-write-off-config:update'
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

-- 部署核对
SELECT `tenant_id`, `dept_id`, COUNT(*) AS `duplicate_count`
FROM `erp_auto_write_off_dept_config`
GROUP BY `tenant_id`, `dept_id`
HAVING COUNT(*) > 1;

SELECT `id`, `name`, `permission`, `component`
FROM `system_menu`
WHERE `component` = 'erp/system/auto-write-off-config/index'
   OR `permission` IN (
     'erp:auto-write-off-config:query',
     'erp:auto-write-off-config:update'
   )
ORDER BY `id`;
