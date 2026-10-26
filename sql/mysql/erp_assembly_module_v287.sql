-- v287：新增“模块管理”和“套件组装单”。
-- 安全约束：仅 CREATE IF NOT EXISTS、窄范围新增列及幂等 INSERT；不删除、不覆盖业务数据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_assembly_recipe` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配方编号',
  `no` varchar(64) NOT NULL COMMENT '配方单号',
  `product_id` bigint NOT NULL COMMENT '成品配件编号',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `version` int NOT NULL DEFAULT 1 COMMENT '配方版本',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0 停用，1 启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `active_product_id` bigint GENERATED ALWAYS AS (IF(`deleted` = b'0', `product_id`, NULL)) STORED COMMENT '未删除配方成品唯一键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_erp_assembly_recipe_no` (`tenant_id`, `no`),
  UNIQUE KEY `uk_erp_assembly_recipe_active_product` (`tenant_id`, `active_product_id`),
  KEY `idx_erp_assembly_recipe_product` (`product_id`),
  KEY `idx_erp_assembly_recipe_dept` (`dept_id`),
  KEY `idx_erp_assembly_recipe_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 组装配方';

CREATE TABLE IF NOT EXISTS `erp_assembly_recipe_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配方明细编号',
  `recipe_id` bigint NOT NULL COMMENT '配方编号',
  `product_id` bigint NOT NULL COMMENT '组成配件编号',
  `quantity_per_set` decimal(24,6) NOT NULL COMMENT '每套用量',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_erp_assembly_recipe_item_recipe` (`recipe_id`),
  KEY `idx_erp_assembly_recipe_item_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 组装配方明细';

CREATE TABLE IF NOT EXISTS `erp_assembly_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组装单编号',
  `no` varchar(64) NOT NULL COMMENT '组装单号',
  `recipe_id` bigint NOT NULL COMMENT '来源配方编号',
  `recipe_version` int NOT NULL COMMENT '配方版本快照',
  `product_id` bigint NOT NULL COMMENT '成品配件编号',
  `set_count` int NOT NULL COMMENT '组装套数及成品数量',
  `warehouse_id` bigint NOT NULL COMMENT '组装仓库编号',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `assembly_time` datetime NOT NULL COMMENT '组装业务时间',
  `finished_batch_no` varchar(64) DEFAULT NULL COMMENT '成品入库批次',
  `total_cost` decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT '最终总成本',
  `unit_cost` decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT '最终单位成本',
  `status` tinyint NOT NULL DEFAULT 10 COMMENT '状态：10 草稿，20 已完成，30 已作废',
  `complete_user_id` bigint DEFAULT NULL COMMENT '完成人',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `void_user_id` bigint DEFAULT NULL COMMENT '作废人',
  `void_time` datetime DEFAULT NULL COMMENT '作废时间',
  `void_reason` varchar(500) DEFAULT NULL COMMENT '作废原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_erp_assembly_order_no` (`tenant_id`, `no`),
  KEY `idx_erp_assembly_order_recipe` (`recipe_id`),
  KEY `idx_erp_assembly_order_product` (`product_id`),
  KEY `idx_erp_assembly_order_warehouse` (`warehouse_id`),
  KEY `idx_erp_assembly_order_dept` (`dept_id`),
  KEY `idx_erp_assembly_order_status_time` (`status`, `assembly_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 套件组装单';

CREATE TABLE IF NOT EXISTS `erp_assembly_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组装明细编号',
  `order_id` bigint NOT NULL COMMENT '组装单编号',
  `recipe_item_id` bigint NOT NULL COMMENT '来源配方明细编号',
  `product_id` bigint NOT NULL COMMENT '组成配件编号快照',
  `quantity_per_set` decimal(24,6) NOT NULL COMMENT '每套用量快照',
  `required_quantity` decimal(24,6) NOT NULL COMMENT '总需求数量',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '实际扣减批次',
  `consume_quantity` decimal(24,6) NOT NULL COMMENT '实际消耗数量',
  `cost_price` decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT '完成时成本单价',
  `cost_amount` decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT '完成时成本金额',
  `product_code` varchar(64) DEFAULT NULL COMMENT '配件编号快照',
  `product_name` varchar(255) DEFAULT NULL COMMENT '配件名称快照',
  `product_standard` varchar(255) DEFAULT NULL COMMENT '规格型号快照',
  `product_unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称快照',
  `product_factory_code` varchar(64) DEFAULT NULL COMMENT '工厂编号快照',
  `batch_no_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用批次快照',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_erp_assembly_order_item_order` (`order_id`),
  KEY `idx_erp_assembly_order_item_recipe_item` (`recipe_item_id`),
  KEY `idx_erp_assembly_order_item_product` (`product_id`),
  KEY `idx_erp_assembly_order_item_batch` (`product_id`, `batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 套件组装单明细';

SET @assembly_source_record_ddl := IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'erp_stock_record'
           AND column_name = 'source_record_id'),
  'SELECT 1',
  'ALTER TABLE `erp_stock_record` ADD COLUMN `source_record_id` bigint DEFAULT NULL COMMENT ''来源库存流水编号'' AFTER `biz_no`, ADD KEY `idx_erp_stock_record_source_record` (`source_record_id`)'
);
PREPARE assembly_source_record_stmt FROM @assembly_source_record_ddl;
EXECUTE assembly_source_record_stmt;
DEALLOCATE PREPARE assembly_source_record_stmt;

-- 字段和索引分别检查：兼容曾经只成功新增字段、但索引尚未创建的中断迁移状态。
SET @assembly_source_record_index_ddl := IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = 'erp_stock_record'
           AND index_name = 'idx_erp_stock_record_source_record'),
  'SELECT 1',
  'ALTER TABLE `erp_stock_record` ADD KEY `idx_erp_stock_record_source_record` (`source_record_id`)'
);
PREPARE assembly_source_record_index_stmt FROM @assembly_source_record_index_ddl;
EXECUTE assembly_source_record_index_stmt;
DEALLOCATE PREPARE assembly_source_record_index_stmt;

INSERT INTO system_dict_data
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 100, '组装配件出库', '100', 'erp_stock_record_biz_type', 0, 'warning', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_stock_record_biz_type' AND value = '100' AND deleted = b'0');

INSERT INTO system_dict_data
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 101, '组装配件出库（作废）', '101', 'erp_stock_record_biz_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_stock_record_biz_type' AND value = '101' AND deleted = b'0');

INSERT INTO system_dict_data
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 102, '组装成品入库', '102', 'erp_stock_record_biz_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_stock_record_biz_type' AND value = '102' AND deleted = b'0');

INSERT INTO system_dict_data
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 103, '组装成品入库（作废）', '103', 'erp_stock_record_biz_type', 0, 'danger', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_stock_record_biz_type' AND value = '103' AND deleted = b'0');

SET @assembly_stock_parent_id := (
  SELECT id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND name = '库存管理' AND path = 'stock'
  ORDER BY id DESC LIMIT 1
);

SET @assembly_recipe_menu_id := COALESCE(
  (SELECT id FROM system_menu WHERE deleted = b'0' AND component = 'erp/assembly/recipe/index' ORDER BY id DESC LIMIT 1),
  (SELECT GREATEST(COALESCE(MAX(id), 0) + 1, 32900) FROM system_menu)
);
INSERT INTO system_menu
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @assembly_recipe_menu_id, '模块管理', '', 2, 9, @assembly_stock_parent_id, 'assembly-recipe',
       'ep:set-up', 'erp/assembly/recipe/index', 'ErpAssemblyRecipe',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @assembly_stock_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND id = @assembly_recipe_menu_id);

SET @assembly_order_menu_id := COALESCE(
  (SELECT id FROM system_menu WHERE deleted = b'0' AND component = 'erp/assembly/order/index' ORDER BY id DESC LIMIT 1),
  (SELECT COALESCE(MAX(id), 0) + 1 FROM system_menu)
);
INSERT INTO system_menu
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @assembly_order_menu_id, '套件组装单', '', 2, 10, @assembly_stock_parent_id, 'assembly-order',
       'ep:box', 'erp/assembly/order/index', 'ErpAssemblyOrder',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @assembly_stock_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND id = @assembly_order_menu_id);

DROP TEMPORARY TABLE IF EXISTS tmp_assembly_permissions_v287;
CREATE TEMPORARY TABLE tmp_assembly_permissions_v287 (
  menu_name varchar(64) NOT NULL,
  permission_code varchar(128) NOT NULL,
  parent_id bigint NOT NULL,
  sort_no int NOT NULL,
  UNIQUE KEY uk_tmp_assembly_permission (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO tmp_assembly_permissions_v287 VALUES
('模块管理查询', 'erp:assembly-recipe:query', @assembly_recipe_menu_id, 1),
('模块管理创建', 'erp:assembly-recipe:create', @assembly_recipe_menu_id, 2),
('模块管理更新', 'erp:assembly-recipe:update', @assembly_recipe_menu_id, 3),
('模块管理删除', 'erp:assembly-recipe:delete', @assembly_recipe_menu_id, 4),
('套件组装单查询', 'erp:assembly-order:query', @assembly_order_menu_id, 1),
('套件组装单创建', 'erp:assembly-order:create', @assembly_order_menu_id, 2),
('套件组装单更新', 'erp:assembly-order:update', @assembly_order_menu_id, 3),
('套件组装单删除', 'erp:assembly-order:delete', @assembly_order_menu_id, 4),
('套件组装单完成', 'erp:assembly-order:complete', @assembly_order_menu_id, 5),
('套件组装单作废', 'erp:assembly-order:void', @assembly_order_menu_id, 6);

INSERT INTO system_menu
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT (@assembly_permission_base := @assembly_permission_base + 1), p.menu_name, p.permission_code,
       3, p.sort_no, p.parent_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM tmp_assembly_permissions_v287 p
CROSS JOIN (SELECT @assembly_permission_base := COALESCE(MAX(id), 0) FROM system_menu) seed
WHERE EXISTS (SELECT 1 FROM system_menu parent WHERE parent.deleted = b'0' AND parent.id = p.parent_id)
  AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0' AND m.permission = p.permission_code)
ORDER BY p.parent_id, p.sort_no;

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), b'0', r.tenant_id
FROM system_role r
INNER JOIN system_menu m ON m.deleted = b'0'
  AND (m.id IN (@assembly_recipe_menu_id, @assembly_order_menu_id)
       OR m.permission LIKE 'erp:assembly-%')
WHERE r.deleted = b'0' AND r.code = 'super_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM system_role_menu rm
    WHERE rm.role_id = r.id
      AND rm.menu_id = m.id
      AND rm.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_assembly_permissions_v287;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_assembly_recipe', 'erp_assembly_recipe_item',
                     'erp_assembly_order', 'erp_assembly_order_item')
ORDER BY table_name;

SELECT permission, parent_id, COUNT(*) AS row_count
FROM system_menu
WHERE deleted = b'0' AND permission LIKE 'erp:assembly-%'
GROUP BY permission, parent_id
ORDER BY permission;
