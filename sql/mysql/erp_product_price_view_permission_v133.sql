-- 配件价格查看权限（v133）
-- 可重复执行：不删除、不覆盖现有菜单/角色权限；存量授权仅首次补齐，已撤销关系不会被重新放开。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_dept_price_field` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `field_key` varchar(100) NOT NULL COMMENT '配件价格逻辑字段编码',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_dept_field` (`tenant_id`, `dept_id`, `field_key`),
  KEY `idx_tenant_field` (`tenant_id`, `field_key`),
  KEY `idx_tenant_dept` (`tenant_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门可查看的配件价格字段';

-- 存量兼容：当前有效部门默认可查看当前所有配件价格字段。
-- INSERT IGNORE 会保留管理员之后逻辑删除的唯一键记录，重复执行不会把已收回权限重新放开。
INSERT IGNORE INTO `system_dept_price_field`
(`dept_id`, `field_key`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT dept.`id`, field_def.`field_key`, '1', NOW(), '1', NOW(), b'0', dept.`tenant_id`
FROM `system_dept` dept
JOIN `system_field_definition` field_def
  ON field_def.`tenant_id` = dept.`tenant_id`
 AND field_def.`module` = 'erp_product'
 AND field_def.`field_group` = 'price_info'
 AND field_def.`deleted` = b'0'
WHERE dept.`deleted` = b'0'
  AND dept.`status` = 0;

-- ERP / 系统配置目录，按业务标识动态定位，不依赖环境中的固定菜单 ID。
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

SET @price_permission_page_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/product-price-permission/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '配件价格查看权限', '', 2, 35, @erp_system_config_id, 'product-price-permission', 'ep:view',
       'erp/system/product-price-permission/index', 'ErpProductPricePermission',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND @price_permission_page_id IS NULL;

SET @price_permission_page_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/product-price-permission/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '查询配件价格查看权限', 'erp:product-price-permission:query', 3, 1, @price_permission_page_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @price_permission_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:product-price-permission:query' AND `deleted` = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '保存配件价格查看权限', 'erp:product-price-permission:update', 3, 2, @price_permission_page_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @price_permission_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:product-price-permission:update' AND `deleted` = b'0'
  );

-- 新权限只默认授予各租户超级管理员，不继承给普通角色。
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT super_role.`id`, menu.`id`, '1', NOW(), '1', NOW(), b'0', super_role.`tenant_id`
FROM `system_role` super_role
JOIN `system_menu` menu
  ON (menu.`id` = @erp_system_config_id
      OR menu.`id` = @price_permission_page_id
      OR menu.`permission` IN ('erp:product-price-permission:query', 'erp:product-price-permission:update'))
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
SELECT COUNT(*) AS `price_field_count`
FROM `system_field_definition`
WHERE `module` = 'erp_product' AND `field_group` = 'price_info' AND `deleted` = b'0';

SELECT `tenant_id`, COUNT(*) AS `active_authorization_count`
FROM `system_dept_price_field`
WHERE `deleted` = b'0'
GROUP BY `tenant_id`;
