-- ERP warehouse move independent module (v119)
-- Scope:
--   1. Create independent warehouse move tables.
--   2. Add independent menu/button permissions: erp:warehouse-move:*.
--   3. Add stock record business type dictionary values 34/35.
--   4. Register field-permission definitions for module erp_warehouse_move.
--
-- Safety:
--   - Does not alter, rename, delete, or reuse erp_stock_move / erp_stock_move_item.
--   - Idempotent inserts use ON DUPLICATE KEY UPDATE or WHERE NOT EXISTS.
--   - No broad DELETE statements.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_warehouse_move` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `no` varchar(64) NOT NULL COMMENT '移货单号',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `move_time` datetime NOT NULL COMMENT '移货日期',
  `from_warehouse_id` bigint NOT NULL COMMENT '移出仓库',
  `to_warehouse_id` bigint NOT NULL COMMENT '移入仓库',
  `handler_id` bigint DEFAULT NULL COMMENT '经办人',
  `source_type` int DEFAULT NULL COMMENT '来源类型',
  `source_id` bigint DEFAULT NULL COMMENT '来源单据 ID',
  `source_no` varchar(64) DEFAULT NULL COMMENT '来源单号',
  `total_count` decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT '移货数量合计',
  `total_price` decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT '移货金额合计',
  `total_cost_amount` decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT '成本金额合计',
  `status` tinyint NOT NULL COMMENT '审核状态',
  `approve_user_id` bigint DEFAULT NULL COMMENT '审核人',
  `approve_time` datetime DEFAULT NULL COMMENT '审核时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `file_url` varchar(512) DEFAULT NULL COMMENT '附件',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_erp_warehouse_move_no` (`no`, `deleted`, `tenant_id`),
  KEY `idx_erp_warehouse_move_dept_id` (`dept_id`),
  KEY `idx_erp_warehouse_move_time` (`move_time`),
  KEY `idx_erp_warehouse_move_status` (`status`),
  KEY `idx_erp_warehouse_move_from_warehouse_id` (`from_warehouse_id`),
  KEY `idx_erp_warehouse_move_to_warehouse_id` (`to_warehouse_id`),
  KEY `idx_erp_warehouse_move_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 仓库移货单';

CREATE TABLE IF NOT EXISTS `erp_warehouse_move_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `move_id` bigint NOT NULL COMMENT '移货单 ID',
  `from_warehouse_id` bigint NOT NULL COMMENT '移出仓库',
  `to_warehouse_id` bigint NOT NULL COMMENT '移入仓库',
  `product_id` bigint NOT NULL COMMENT '产品',
  `product_unit_id` bigint DEFAULT NULL COMMENT '单位',
  `product_price` decimal(24,6) DEFAULT NULL COMMENT '移货单价',
  `count` decimal(24,6) NOT NULL COMMENT '移货数量',
  `total_price` decimal(24,6) DEFAULT NULL COMMENT '移货金额',
  `from_shelf` varchar(64) DEFAULT NULL COMMENT '移出货架',
  `to_shelf` varchar(64) DEFAULT NULL COMMENT '移入货架',
  `cost_price` decimal(24,6) DEFAULT NULL COMMENT '成本单价',
  `cost_amount` decimal(24,6) DEFAULT NULL COMMENT '成本金额',
  `weight` decimal(24,6) DEFAULT NULL COMMENT '单重',
  `total_weight` decimal(24,6) DEFAULT NULL COMMENT '总重',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_erp_warehouse_move_item_move_id` (`move_id`),
  KEY `idx_erp_warehouse_move_item_product_id` (`product_id`),
  KEY `idx_erp_warehouse_move_item_from_warehouse_id` (`from_warehouse_id`),
  KEY `idx_erp_warehouse_move_item_to_warehouse_id` (`to_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 仓库移货单明细';

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 34, '仓库移货入库', '34', 'erp_stock_record_biz_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data`
  WHERE `dict_type` = 'erp_stock_record_biz_type'
    AND `value` = '34'
    AND `deleted` = b'0'
);

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 35, '仓库移货出库', '35', 'erp_stock_record_biz_type', 0, 'danger', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data`
  WHERE `dict_type` = 'erp_stock_record_biz_type'
    AND `value` = '35'
    AND `deleted` = b'0'
);

SET @warehouse_move_erp_root_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND `name` = 'ERP 系统'
    AND `path` = '/erp'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @warehouse_move_parent_id := (
  SELECT stock_menu.`id`
  FROM `system_menu` stock_menu
  WHERE stock_menu.`deleted` = b'0'
    AND stock_menu.`type` = 1
    AND stock_menu.`name` = '库存管理'
    AND stock_menu.`path` = 'stock'
    AND (@warehouse_move_erp_root_id IS NULL OR stock_menu.`parent_id` = @warehouse_move_erp_root_id)
  ORDER BY stock_menu.`id` DESC
  LIMIT 1
);

SET @warehouse_move_menu_id := 31190;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @warehouse_move_menu_id, '仓库移货单', '', 2, 6, @warehouse_move_parent_id, 'warehouse-move',
       'ep:rank', 'erp/stock/warehouse-move/index', 'ErpWarehouseMove',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_move_parent_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31191, '仓库移货单查询', 'erp:warehouse-move:query', 3, 1, @warehouse_move_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_move_parent_id IS NOT NULL
UNION ALL
SELECT 31192, '仓库移货单创建', 'erp:warehouse-move:create', 3, 2, @warehouse_move_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_move_parent_id IS NOT NULL
UNION ALL
SELECT 31193, '仓库移货单更新', 'erp:warehouse-move:update', 3, 3, @warehouse_move_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_move_parent_id IS NOT NULL
UNION ALL
SELECT 31194, '仓库移货单删除', 'erp:warehouse-move:delete', 3, 4, @warehouse_move_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_move_parent_id IS NOT NULL
UNION ALL
SELECT 31195, '仓库移货单导出', 'erp:warehouse-move:export', 3, 5, @warehouse_move_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_move_parent_id IS NOT NULL
UNION ALL
SELECT 31196, '仓库移货单审核', 'erp:warehouse-move:update-status', 3, 6, @warehouse_move_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_move_parent_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, `id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu`
WHERE `id` BETWEEN 31190 AND 31196
  AND `deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_warehouse_move', 'deptId', '所属部门', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'no', '移货单号', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'moveTime', '移货日期', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'fromWarehouseId', '移出仓库', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'toWarehouseId', '移入仓库', 'main_form', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'handlerId', '经办人', 'main_form', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'remark', '备注', 'main_form', 70, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'fileUrl', '附件', 'main_form', 80, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_productId', '产品名称', 'detail_item', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_productCode', '产品编码', 'detail_item', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_fromShelf', '移出货架', 'detail_item', 220, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_toShelf', '移入货架', 'detail_item', 230, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_fromStockCount', '移出库存', 'detail_item', 240, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_toStockCount', '移入库存', 'detail_item', 250, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_count', '移货数量', 'detail_item', 260, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_costPrice', '成本单价', 'detail_item', 270, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_costAmount', '成本金额', 'detail_item', 280, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_productPrice', '移货单价', 'detail_item', 290, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_totalPrice', '移货金额', 'detail_item', 300, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_weight', '单重', 'detail_item', 310, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_totalWeight', '总重', 'detail_item', 320, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'item_remark', '明细备注', 'detail_item', 330, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_fromWarehouseName', '移出仓库', 'list_col', 400, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_toWarehouseName', '移入仓库', 'list_col', 410, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_productNames', '产品信息', 'list_col', 420, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_productCodes', '产品编码', 'list_col', 430, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_handlerName', '经办人', 'list_col', 440, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_itemCount', '项数', 'list_col', 450, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_totalCount', '移货数量', 'list_col', 460, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_totalCostAmount', '成本金额', 'list_col', 470, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_totalPrice', '移货金额', 'list_col', 480, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_approveUserName', '审核人', 'list_col', 490, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'col_approveTime', '审核时间', 'list_col', 500, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'creatorName', '创建人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'createTime', '创建时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'updaterName', '修改人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'updateTime', '修改时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'approveUserName', '审核人', 'system_info', 940, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'approveTime', '审核时间', 'system_info', 950, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'query_fromWarehouseId', '查询-移出仓库', 'query', 1000, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'query_toWarehouseId', '查询-移入仓库', 'query', 1010, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'query_productId', '查询-产品', 'query', 1020, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'query_deptId', '查询-所属部门', 'query', 1030, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse_move', 'query_status', '查询-状态', 'query', 1040, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
