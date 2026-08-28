-- ERP 同步商城分类/商品（v190）
-- 安全说明：
--   - 仅新增映射表和同步按钮权限。
--   - 不删除 system_menu / system_role_menu，不覆盖已有 ERP 或商城权限。
--   - 执行后刷新菜单缓存，并重新登录前端。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_mall_category_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `erp_category_id` bigint NOT NULL COMMENT 'ERP 分类编号',
  `mall_category_id` bigint DEFAULT NULL COMMENT '商城分类编号',
  `sync_status` tinyint NOT NULL DEFAULT 1 COMMENT '同步状态：0 成功，1 失败',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `fail_reason` varchar(512) DEFAULT NULL COMMENT '失败原因/同步提示',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_erp_mall_category_mapping_erp` (`erp_category_id`, `tenant_id`),
  KEY `idx_erp_mall_category_mapping_mall` (`mall_category_id`),
  KEY `idx_erp_mall_category_mapping_status` (`sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 分类同步商城分类映射';

CREATE TABLE IF NOT EXISTS `erp_mall_product_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `erp_product_id` bigint NOT NULL COMMENT 'ERP 产品编号',
  `mall_spu_id` bigint DEFAULT NULL COMMENT '商城 SPU 编号',
  `mall_sku_id` bigint DEFAULT NULL COMMENT '商城 SKU 编号',
  `sync_status` tinyint NOT NULL DEFAULT 1 COMMENT '同步状态：0 成功，1 失败',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `fail_reason` varchar(512) DEFAULT NULL COMMENT '失败原因/同步提示',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_erp_mall_product_mapping_erp` (`erp_product_id`, `tenant_id`),
  KEY `idx_erp_mall_product_mapping_spu` (`mall_spu_id`),
  KEY `idx_erp_mall_product_mapping_sku` (`mall_sku_id`),
  KEY `idx_erp_mall_product_mapping_status` (`sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 产品同步商城商品映射';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_mall_sync_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_mall_sync_parent;

CREATE TEMPORARY TABLE tmp_erp_mall_sync_permissions (
  name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  sort int NOT NULL,
  query_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  grant_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_mall_sync_permissions
(name, permission, sort, query_perm, grant_perm)
VALUES
('同步商城分类', 'erp:mall-sync:category', 90, 'erp:product-category:query', 'erp:product-category:update'),
('同步商城商品', 'erp:mall-sync:product', 90, 'erp:product:query', 'erp:product:update');

CREATE TEMPORARY TABLE tmp_erp_mall_sync_parent (
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  parent_id bigint NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_mall_sync_parent (permission, parent_id)
SELECT source.permission, MIN(candidate.parent_id) AS parent_id
FROM tmp_erp_mall_sync_permissions source
JOIN system_menu candidate
  ON candidate.permission COLLATE utf8mb4_unicode_ci = source.query_perm COLLATE utf8mb4_unicode_ci
 AND candidate.deleted = b'0'
 AND candidate.parent_id > 0
GROUP BY source.permission;

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT source.name,
       source.permission,
       3,
       source.sort,
       parent.parent_id,
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
FROM tmp_erp_mall_sync_permissions source
JOIN tmp_erp_mall_sync_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
      AND exists_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       sync_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM tmp_erp_mall_sync_permissions source
JOIN system_menu sync_menu
  ON sync_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND sync_menu.deleted = b'0'
JOIN system_role_menu owned_role
  ON owned_role.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.id = owned_role.menu_id
 AND owned_menu.deleted = b'0'
 AND owned_menu.permission COLLATE utf8mb4_unicode_ci = source.grant_perm COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_menu target
    WHERE target.role_id = owned_role.role_id
      AND target.menu_id = sync_menu.id
      AND target.tenant_id = owned_role.tenant_id
      AND target.deleted = b'0'
);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_mall_sync_parent;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_mall_sync_permissions;
