-- ERP 销售拣货/送货履约功能
-- 说明：拣货单按“销售单 + 仓库”拆分，送货单按销售单汇总；提交凭证绑定到每次提交批次。

CREATE TABLE IF NOT EXISTS `erp_sale_pick_delivery_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `sale_out_id` bigint NOT NULL COMMENT '销售出库单编号',
    `sale_out_no` varchar(64) NOT NULL COMMENT '销售出库单号',
    `customer_id` bigint DEFAULT NULL COMMENT '客户编号',
    `customer_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
    `dept_id` bigint DEFAULT NULL COMMENT '部门编号',
    `pick_status` tinyint NOT NULL COMMENT '拣货状态：10待拣货 20部分拣货 30已拣货',
    `delivery_status` tinyint NOT NULL COMMENT '送货状态：5待拣货完成 10待送货 20部分送货 30已送货',
    `total_item_count` int NOT NULL DEFAULT 0 COMMENT '总明细数',
    `picked_item_count` int NOT NULL DEFAULT 0 COMMENT '已拣货明细数',
    `delivered_item_count` int NOT NULL DEFAULT 0 COMMENT '已送货明细数',
    `latest_pick_time` datetime DEFAULT NULL COMMENT '最近拣货时间',
    `latest_delivery_time` datetime DEFAULT NULL COMMENT '最近送货时间',
    `pick_complete_time` datetime DEFAULT NULL COMMENT '拣货完成时间',
    `delivery_complete_time` datetime DEFAULT NULL COMMENT '送货完成时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sale_out` (`tenant_id`, `sale_out_id`, `deleted`),
    KEY `idx_sale_out_no` (`tenant_id`, `sale_out_no`),
    KEY `idx_delivery_status` (`tenant_id`, `delivery_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 销售拣货送货单';

CREATE TABLE IF NOT EXISTS `erp_sale_pick_delivery_pick_task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `order_id` bigint NOT NULL COMMENT '履约单编号',
    `sale_out_id` bigint NOT NULL COMMENT '销售出库单编号',
    `sale_out_no` varchar(64) NOT NULL COMMENT '销售出库单号',
    `customer_id` bigint DEFAULT NULL COMMENT '客户编号',
    `customer_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
    `warehouse_id` bigint NOT NULL COMMENT '仓库编号',
    `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
    `status` tinyint NOT NULL COMMENT '拣货状态：10待拣货 20部分拣货 30已拣货',
    `total_item_count` int NOT NULL DEFAULT 0 COMMENT '总明细数',
    `picked_item_count` int NOT NULL DEFAULT 0 COMMENT '已拣货明细数',
    `latest_pick_time` datetime DEFAULT NULL COMMENT '最近拣货时间',
    `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_warehouse` (`tenant_id`, `order_id`, `warehouse_id`, `deleted`),
    KEY `idx_warehouse_status` (`tenant_id`, `warehouse_id`, `status`),
    KEY `idx_sale_out_no` (`tenant_id`, `sale_out_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 销售拣货任务';

CREATE TABLE IF NOT EXISTS `erp_sale_pick_delivery_item` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `order_id` bigint NOT NULL COMMENT '履约单编号',
    `pick_task_id` bigint NOT NULL COMMENT '拣货任务编号',
    `sale_out_id` bigint NOT NULL COMMENT '销售出库单编号',
    `sale_out_item_id` bigint NOT NULL COMMENT '销售出库明细编号',
    `warehouse_id` bigint NOT NULL COMMENT '仓库编号',
    `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓库名称',
    `product_id` bigint NOT NULL COMMENT '产品编号',
    `product_code` varchar(64) DEFAULT NULL COMMENT '产品编码',
    `product_name` varchar(255) DEFAULT NULL COMMENT '产品名称',
    `standard` varchar(255) DEFAULT NULL COMMENT '规格',
    `count` decimal(24, 6) NOT NULL COMMENT '数量',
    `pick_status` tinyint NOT NULL COMMENT '拣货状态：10待拣货 30已拣货',
    `pick_user_id` bigint DEFAULT NULL COMMENT '拣货人编号',
    `pick_time` datetime DEFAULT NULL COMMENT '拣货时间',
    `delivery_status` tinyint NOT NULL COMMENT '送货状态：5待拣货完成 10待送货 30已送货',
    `delivery_user_id` bigint DEFAULT NULL COMMENT '送货人编号',
    `delivery_time` datetime DEFAULT NULL COMMENT '送货时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sale_out_item` (`tenant_id`, `sale_out_item_id`, `deleted`),
    KEY `idx_pick_task` (`tenant_id`, `pick_task_id`, `pick_status`),
    KEY `idx_order_delivery` (`tenant_id`, `order_id`, `delivery_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 销售拣货送货明细';

CREATE TABLE IF NOT EXISTS `erp_sale_pick_delivery_submit` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `order_id` bigint NOT NULL COMMENT '履约单编号',
    `pick_task_id` bigint DEFAULT NULL COMMENT '拣货任务编号',
    `sale_out_id` bigint NOT NULL COMMENT '销售出库单编号',
    `type` tinyint NOT NULL COMMENT '提交类型：10拣货 20送货',
    `submit_user_id` bigint NOT NULL COMMENT '提交人编号',
    `submit_time` datetime NOT NULL COMMENT '提交时间',
    `item_count` int NOT NULL COMMENT '提交明细数',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_order_type` (`tenant_id`, `order_id`, `type`),
    KEY `idx_pick_task_type` (`tenant_id`, `pick_task_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 销售拣货送货提交批次';

CREATE TABLE IF NOT EXISTS `erp_sale_pick_delivery_submit_file` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `submit_id` bigint NOT NULL COMMENT '提交批次编号',
    `file_url` varchar(512) NOT NULL COMMENT '文件地址',
    `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
    `file_type` varchar(64) DEFAULT NULL COMMENT '文件类型',
    `sort` int NOT NULL DEFAULT 1 COMMENT '排序',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_submit` (`tenant_id`, `submit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 销售拣货送货提交凭证';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(32000, '销售拣货单', '', 2, 8, 2617, 'pick', 'ep:box', 'erp/sale/pick/index', 'ErpSalePick', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(32001, '销售拣货单查询', 'erp:sale-pick:query', 3, 1, 32000, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(32002, '销售拣货', 'erp:sale-pick:pick', 3, 2, 32000, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(32003, '销售拣货单导出', 'erp:sale-pick:export', 3, 3, 32000, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(32010, '销售送货单', '', 2, 9, 2617, 'delivery', 'ep:van', 'erp/sale/delivery/index', 'ErpSaleDelivery', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(32011, '销售送货单查询', 'erp:sale-delivery:query', 3, 1, 32010, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(32012, '销售送货', 'erp:sale-delivery:delivery', 3, 2, 32010, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(32013, '销售送货单导出', 'erp:sale-delivery:export', 3, 3, 32010, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
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

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, menu.`id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` menu
WHERE menu.`id` IN (32000, 32001, 32002, 32003, 32010, 32011, 32012, 32013)
  AND NOT EXISTS (
      SELECT 1 FROM `system_role_menu` rm
      WHERE rm.`role_id` = 1 AND rm.`menu_id` = menu.`id` AND rm.`tenant_id` = 1 AND rm.`deleted` = b'0'
  );
