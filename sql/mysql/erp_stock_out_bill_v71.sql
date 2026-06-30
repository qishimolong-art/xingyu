-- ERP stock out bill readonly report.
-- Scope:
--   1. Create erp_stock_out_bill as an independent report table.
--   2. Add only the inventory menu and query permission for 出仓单.
--
-- Safety:
--   - Does not delete or update existing menu/role data.
--   - Does not create create/update/delete/audit/export permissions.
--   - Grants the new menu and query permission to super admin only.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_stock_out_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `priority` varchar(32) DEFAULT NULL COMMENT '优先级',
  `no` varchar(64) DEFAULT NULL COMMENT '出仓单单号',
  `bill_date` datetime DEFAULT NULL COMMENT '日期',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库编号',
  `warehouse_name` varchar(128) DEFAULT NULL COMMENT '仓库名称快照',
  `shipping_area` varchar(128) DEFAULT NULL COMMENT '发货区',
  `source_unit_name` varchar(128) DEFAULT NULL COMMENT '来源单位名称',
  `source_no` varchar(64) DEFAULT NULL COMMENT '来源单号',
  `status` int DEFAULT NULL COMMENT '状态',
  `creator_name` varchar(64) DEFAULT NULL COMMENT '创建人名称',
  `updater_name` varchar(64) DEFAULT NULL COMMENT '修改名称',
  `auditor` varchar(64) DEFAULT NULL COMMENT '审核人',
  `auditor_name` varchar(64) DEFAULT NULL COMMENT '审核人名称',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `print_time` datetime DEFAULT NULL COMMENT '打印时间',
  `print_count` int DEFAULT NULL COMMENT '打印次数',
  `source_remark` varchar(512) DEFAULT NULL COMMENT '来源单据备注',
  `total_weight` decimal(24,6) DEFAULT NULL COMMENT '总重',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `timeout_flag` bit(1) DEFAULT NULL COMMENT '超时',
  `whole_qty` decimal(24,6) DEFAULT NULL COMMENT '整件数',
  `loose_qty` decimal(24,6) DEFAULT NULL COMMENT '散件数',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_bill_date` (`tenant_id`, `bill_date`),
  KEY `idx_warehouse` (`tenant_id`, `warehouse_id`),
  KEY `idx_source_no` (`tenant_id`, `source_no`),
  KEY `idx_status` (`tenant_id`, `status`),
  KEY `idx_source_unit_name` (`tenant_id`, `source_unit_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 出仓单报表';

SET @stock_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '库存管理'
    AND `deleted` = b'0'
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '出仓单', '', 2, 6, @stock_menu_id, 'outbill', 'ep:document',
       'erp/stock/outbill/index', 'ErpStockOutBill', 0, b'1', b'1', b'1',
       '1', NOW(), '1', NOW(), b'0'
WHERE @stock_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `component` = 'erp/stock/outbill/index'
      AND `deleted` = b'0'
  );

SET @stock_out_bill_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/stock/outbill/index'
    AND `deleted` = b'0'
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '出仓单查询', 'erp:stock-out-bill:query', 3, 1, @stock_out_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_out_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-out-bill:query'
      AND `deleted` = b'0'
  );

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 1, menu.`id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` menu
WHERE (
    menu.`component` = 'erp/stock/outbill/index'
    OR menu.`permission` = 'erp:stock-out-bill:query'
  )
  AND menu.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` role_menu
    WHERE role_menu.`role_id` = 1
      AND role_menu.`menu_id` = menu.`id`
      AND role_menu.`deleted` = b'0'
  );
