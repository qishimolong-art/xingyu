-- ERP 配件品牌（v193）
-- 1. 新增独立配件品牌档案表。
-- 2. 确保 erp_product.brand 字段存在。
-- 3. 将历史配件品牌文本去重迁移为配件品牌档案。
-- 4. 新增配件品牌菜单、按钮权限和字段权限定义。

CREATE TABLE IF NOT EXISTS `erp_product_brand` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '品牌编号',
  `name` varchar(64) NOT NULL COMMENT '品牌名称',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '品牌状态',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_erp_product_brand_name` (`name`, `tenant_id`, `deleted`),
  KEY `idx_erp_product_brand_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 配件品牌';

DROP PROCEDURE IF EXISTS add_erp_product_brand_column_v193;

DELIMITER $$
CREATE PROCEDURE add_erp_product_brand_column_v193()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_product'
          AND COLUMN_NAME = 'brand'
    ) THEN
        ALTER TABLE `erp_product`
            ADD COLUMN `brand` varchar(64) DEFAULT NULL COMMENT '品牌' AFTER `vehicle_model`;
    END IF;
END$$
DELIMITER ;

CALL add_erp_product_brand_column_v193();

DROP PROCEDURE IF EXISTS add_erp_product_brand_column_v193;

INSERT INTO `erp_product_brand`
(`name`, `status`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT TRIM(p.`brand`) AS `name`,
       0 AS `status`,
       0 AS `sort`,
       '1' AS `creator`,
       NOW() AS `create_time`,
       '1' AS `updater`,
       NOW() AS `update_time`,
       b'0' AS `deleted`,
       p.`tenant_id`
FROM `erp_product` p
WHERE p.`deleted` = b'0'
  AND p.`brand` IS NOT NULL
  AND TRIM(p.`brand`) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM `erp_product_brand` b
      WHERE b.`deleted` = b'0'
        AND b.`tenant_id` = p.`tenant_id`
        AND b.`name` COLLATE utf8mb4_unicode_ci = TRIM(p.`brand`) COLLATE utf8mb4_unicode_ci
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_brand_parent;
CREATE TEMPORARY TABLE tmp_erp_product_brand_parent (
  parent_id bigint NOT NULL
) ENGINE=Memory;

INSERT INTO tmp_erp_product_brand_parent(parent_id)
SELECT candidate.parent_id
FROM (
    SELECT m.parent_id, 1 AS priority, m.id
    FROM system_menu m
    WHERE m.deleted = b'0'
      AND m.component = 'erp/product/unit/index'
    UNION ALL
    SELECT m.id AS parent_id, 2 AS priority, m.id
    FROM system_menu m
    WHERE m.deleted = b'0'
      AND m.name = '配件基本信息'
) candidate
ORDER BY candidate.priority, candidate.id DESC
LIMIT 1;

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '配件品牌',
       '',
       2,
       25,
       parent.parent_id,
       'product-brand',
       'ep:collection-tag',
       'erp/product/brand/index',
       'ErpProductBrand',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM tmp_erp_product_brand_parent parent
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.deleted = b'0'
      AND exists_menu.component = 'erp/product/brand/index'
);

UPDATE system_menu
SET `name` = '配件品牌',
    `permission` = '',
    `type` = 2,
    `sort` = 25,
    `path` = 'product-brand',
    `icon` = 'ep:collection-tag',
    `component_name` = 'ErpProductBrand',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `component` = 'erp/product/brand/index';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_brand_permission;
CREATE TEMPORARY TABLE tmp_erp_product_brand_permission (
  name varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  sort int NOT NULL
) ENGINE=Memory;

INSERT INTO tmp_erp_product_brand_permission(name, permission, sort)
VALUES
('品牌查询', 'erp:product-brand:query', 1),
('品牌创建', 'erp:product-brand:create', 2),
('品牌更新', 'erp:product-brand:update', 3),
('品牌删除', 'erp:product-brand:delete', 4),
('品牌导出', 'erp:product-brand:export', 5),
('品牌导入', 'erp:product-brand:import', 6);

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT source.name,
       source.permission,
       3,
       source.sort,
       parent_menu.id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM tmp_erp_product_brand_permission source
JOIN system_menu parent_menu
  ON parent_menu.deleted = b'0'
 AND parent_menu.component = 'erp/product/brand/index'
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.deleted = b'0'
      AND exists_menu.permission COLLATE utf8mb4_unicode_ci = source.permission
);

UPDATE system_menu m
JOIN tmp_erp_product_brand_permission source
  ON source.permission = m.permission COLLATE utf8mb4_unicode_ci
SET m.name = source.name,
    m.type = 3,
    m.sort = source.sort,
    m.path = '',
    m.icon = '',
    m.component = '',
    m.component_name = NULL,
    m.status = 0,
    m.visible = b'1',
    m.keep_alive = b'1',
    m.always_show = b'1',
    m.updater = '1',
    m.update_time = NOW(),
    m.deleted = b'0';

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned.role_id,
       brand_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned.tenant_id
FROM system_menu brand_menu
JOIN system_role_menu owned
  ON owned.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.id = owned.menu_id
 AND owned_menu.deleted = b'0'
WHERE brand_menu.deleted = b'0'
  AND (
      brand_menu.component = 'erp/product/brand/index'
      OR brand_menu.permission IN (
          'erp:product-brand:query',
          'erp:product-brand:create',
          'erp:product-brand:update',
          'erp:product-brand:delete',
          'erp:product-brand:export',
          'erp:product-brand:import'
      )
  )
  AND (
      owned_menu.component IN ('erp/product/product/index', 'erp/product/unit/index', 'erp/product/category/index')
      OR owned_menu.permission IN (
          'erp:product:query',
          'erp:product-unit:query',
          'erp:product-category:query'
      )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu existing
      WHERE existing.deleted = b'0'
        AND existing.role_id = owned.role_id
        AND existing.menu_id = brand_menu.id
        AND existing.tenant_id = owned.tenant_id
  );

CREATE TABLE IF NOT EXISTS `system_field_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `module` VARCHAR(50) NOT NULL COMMENT 'Module key, for example erp_product',
  `field_key` VARCHAR(100) NOT NULL COMMENT 'Field key',
  `field_label` VARCHAR(100) NOT NULL COMMENT 'Field label',
  `field_group` VARCHAR(50) DEFAULT NULL COMMENT 'Field group',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort',
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_field` (`module`, `field_key`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System field definition';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_product', 'brand', '品牌', 'base_info', 65, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_brand', 'name', '品牌名称', 'base_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_brand', 'status', '品牌状态', 'base_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_brand', 'sort', '排序', 'base_info', 30, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_brand_permission;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_brand_parent;
