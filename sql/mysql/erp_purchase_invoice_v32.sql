-- ERP purchase invoice (v32)
-- Execution order:
--   1. Execute table creation SQL
--   2. Execute menu and permission initialization SQL
--   3. Refresh menu cache or restart backend service
--
-- Notes:
--   1. This module belongs to ERP purchase management (`parent_id = 2602`)
--   2. Menu ids use 3291-3297 to avoid collision with accounting/voucher menus
--   3. If the old 3030-3036 purchase-invoice SQL was executed before, rerun
--      `erp_accounting_menu_v26.sql` first to restore accounting menus, then run this script

CREATE TABLE IF NOT EXISTS `erp_purchase_invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(64) NOT NULL COMMENT '票据单号（CGPJ 前缀）',
  `status` tinyint NOT NULL DEFAULT 10 COMMENT '10=未审核 20=已审核',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `invoice_date` date NOT NULL COMMENT '开票日期',
  `invoice_type` varchar(64) NOT NULL COMMENT '票据类型',
  `invoice_no` varchar(256) DEFAULT NULL COMMENT '发票号',
  `invoice_count` int NOT NULL DEFAULT 1 COMMENT '发票张数',
  `tax_exclusive_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '不含税金额',
  `tax_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '税额',
  `total_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '价税合计',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `handler_id` bigint DEFAULT NULL COMMENT '经手人用户ID',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `file_url` varchar(512) DEFAULT NULL COMMENT '附件URL',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购票据主表';

CREATE TABLE IF NOT EXISTS `erp_purchase_invoice_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL COMMENT '采购票据ID',
  `source_in_id` bigint DEFAULT NULL COMMENT '来源入库单ID',
  `source_in_no` varchar(64) DEFAULT NULL COMMENT '来源入库单号',
  `source_in_item_id` bigint DEFAULT NULL COMMENT '来源入库单明细ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `product_unit_name` varchar(32) DEFAULT NULL COMMENT '产品单位快照',
  `product_bar_code` varchar(64) DEFAULT NULL COMMENT '产品条码快照',
  `count` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '数量',
  `product_price` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '不含税单价',
  `tax_exclusive_price` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '不含税金额',
  `tax_percent` decimal(6,2) DEFAULT 0 COMMENT '税率(%)',
  `tax_price` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '税额',
  `total_price` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '价税合计',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_invoice_id` (`invoice_id`),
  KEY `idx_source_in_item_id` (`source_in_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购票据明细';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3291, '采购票据', '', 2, 5, 2602, 'invoice', 'fa:file-text-o',
 'erp/purchase/invoice/index', 'ErpPurchaseInvoice',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
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
VALUES
(3292, '采购票据查询', 'erp:purchase-invoice:query', 3, 1, 3291, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3293, '采购票据创建', 'erp:purchase-invoice:create', 3, 2, 3291, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3294, '采购票据更新', 'erp:purchase-invoice:update', 3, 3, 3291, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3295, '采购票据删除', 'erp:purchase-invoice:delete', 3, 4, 3291, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3296, '采购票据审批', 'erp:purchase-invoice:update-status', 3, 5, 3291, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3297, '采购票据导出', 'erp:purchase-invoice:export', 3, 6, 3291, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
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

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 3291, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3292, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3293, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3294, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3295, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3296, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3297, '1', NOW(), '1', NOW(), b'0', 1);
