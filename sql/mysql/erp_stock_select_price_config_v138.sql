-- 价格名称管理：配置销售/采购选择库存时允许出现的价格字段
-- 可重复执行；不删除、不覆盖现有菜单、角色及管理员已经关闭的配置。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_stock_select_price_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `field_key` varchar(100) NOT NULL COMMENT 'erp_product 价格字段编码',
  `biz_type` varchar(20) NOT NULL COMMENT '业务场景：sale/purchase',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_field_biz` (`tenant_id`, `field_key`, `biz_type`),
  KEY `idx_tenant_biz` (`tenant_id`, `biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='库存选择价格字段场景配置';

-- 首次上线保持存量行为：当前已有且启用的价格字段默认在销售、采购场景均显示。
-- INSERT IGNORE 不会恢复管理员后续逻辑删除（关闭）的唯一键记录。
INSERT IGNORE INTO `erp_stock_select_price_config`
(`field_key`, `biz_type`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT field_def.`field_key`, biz.`biz_type`, '1', NOW(), '1', NOW(), b'0', field_def.`tenant_id`
FROM `system_field_definition` field_def
CROSS JOIN (
  SELECT 'sale' AS `biz_type`
  UNION ALL
  SELECT 'purchase' AS `biz_type`
) biz
WHERE field_def.`module` = 'erp_product'
  AND field_def.`field_group` = 'price_info'
  AND field_def.`deleted` = b'0';

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

SET @price_name_page_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/stock-select-price-config/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '价格名称管理', '', 2, 36, @erp_system_config_id, 'stock-select-price-config', 'ep:price-tag',
       'erp/system/stock-select-price-config/index', 'ErpStockSelectPriceConfig',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_system_config_id IS NOT NULL
  AND @price_name_page_id IS NULL;

SET @price_name_page_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/system/stock-select-price-config/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '查询价格名称管理', 'erp:stock-select-price-config:query', 3, 1, @price_name_page_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @price_name_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-select-price-config:query' AND `deleted` = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '保存价格名称管理', 'erp:stock-select-price-config:update', 3, 2, @price_name_page_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @price_name_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-select-price-config:update' AND `deleted` = b'0'
  );

-- 新菜单和按钮仅追加授予各租户超级管理员，不影响普通角色已有权限。
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT super_role.`id`, menu.`id`, '1', NOW(), '1', NOW(), b'0', super_role.`tenant_id`
FROM `system_role` super_role
JOIN `system_menu` menu
  ON (menu.`id` = @erp_system_config_id
      OR menu.`id` = @price_name_page_id
      OR menu.`permission` IN (
        'erp:stock-select-price-config:query',
        'erp:stock-select-price-config:update'
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
SELECT `tenant_id`, COUNT(*) AS `price_field_count`
FROM `system_field_definition`
WHERE `module` = 'erp_product'
  AND `field_group` = 'price_info'
  AND `deleted` = b'0'
GROUP BY `tenant_id`;

SELECT `tenant_id`, `biz_type`, COUNT(*) AS `enabled_count`
FROM `erp_stock_select_price_config`
WHERE `deleted` = b'0'
GROUP BY `tenant_id`, `biz_type`;

SELECT `tenant_id`, `field_key`, `biz_type`, COUNT(*) AS `duplicate_count`
FROM `erp_stock_select_price_config`
GROUP BY `tenant_id`, `field_key`, `biz_type`
HAVING COUNT(*) > 1;
