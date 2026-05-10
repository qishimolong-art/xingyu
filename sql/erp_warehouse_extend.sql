-- =============================================
-- 仓库管理模块扩展 - 数据库迁移脚本
-- =============================================

-- 1. ALTER TABLE erp_warehouse 新增字段（去掉 AFTER 子句，避免列顺序依赖报错）
ALTER TABLE `erp_warehouse`
  MODIFY COLUMN `warehouse_type` tinyint DEFAULT NULL COMMENT '仓库类型(1-正品仓库 2-废品仓库 3-待处理仓库 4-急件仓库 5-旧件仓库 6-寄售仓库 7-托管仓库 8-半成品仓)',
  ADD COLUMN `storage_center_id` bigint DEFAULT NULL COMMENT '仓储中心ID',
  ADD COLUMN `storage_warehouse_id` bigint DEFAULT NULL COMMENT '仓储对应仓库ID',
  ADD COLUMN `sale_enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '销售启用(0停用 1启用)',
  ADD COLUMN `purchase_enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '采购启用(0停用 1启用)',
  ADD COLUMN `stock_bill_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '入出仓单(0不生成 1生成)',
  ADD COLUMN `ecommerce_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '允许电商销售',
  ADD COLUMN `scan_control` bit(1) NOT NULL DEFAULT b'0' COMMENT '扫码管控',
  ADD COLUMN `sale_bill_control` bit(1) NOT NULL DEFAULT b'0' COMMENT '销售开单管控',
  ADD COLUMN `zero_stock_hide` bit(1) NOT NULL DEFAULT b'0' COMMENT '销售0库存不显示',
  ADD COLUMN `goods_to_branch` varchar(50) DEFAULT NULL COMMENT '货到分店(字典erp_warehouse_goods_branch)',
  ADD COLUMN `dept` varchar(50) DEFAULT NULL COMMENT '部门(字典erp_warehouse_dept)',
  ADD COLUMN `warehouse_location` varchar(100) DEFAULT NULL COMMENT '仓库地点',
  ADD COLUMN `out_packing` bit(1) NOT NULL DEFAULT b'0' COMMENT '出仓打包装箱',
  ADD COLUMN `auto_order` bit(1) NOT NULL DEFAULT b'1' COMMENT '自动订货',
  ADD COLUMN `max_pick_count` int NOT NULL DEFAULT 100000 COMMENT '允许同时拣货单数',
  ADD COLUMN `warehouse_code` varchar(50) DEFAULT NULL COMMENT '仓库编码',
  ADD COLUMN `split_order` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否拆单',
  ADD COLUMN `stock_group_type` tinyint NOT NULL DEFAULT 1 COMMENT '出入仓分组(1-全部 2-入仓单 3-出仓单 4-全部不分组)',
  ADD COLUMN `credit_control` decimal(12,2) DEFAULT NULL COMMENT '额度管控',
  ADD COLUMN `region_id` bigint DEFAULT NULL COMMENT '区域ID(关联erp_base_data type=region)';

-- 2. 新建关联表 erp_warehouse_branch
CREATE TABLE IF NOT EXISTS `erp_warehouse_branch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `branch_tenant_id` bigint NOT NULL COMMENT '分店租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_id` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 仓库分店关联表';

-- 3. 字典数据 - 货到分店
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('ERP 货到分店', 'erp_warehouse_goods_branch', 0, '', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '项目-四川路通源汽车服务有限公司', '1', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0'),
(2, '四川鸿利达科技服务有限公司新', '2', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0'),
(3, '项目-轮胎', '3', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0'),
(4, '项目-道达尔', '4', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0'),
(5, '项目-电瓶', '5', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0'),
(6, '项目-工业油', '6', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0'),
(7, '项目-康菲', '7', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0'),
(8, '项目-柴机油', '8', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0'),
(9, '项目-兴宇路通', '9', 'erp_warehouse_goods_branch', 0, '1', NOW(), '1', NOW(), b'0');

-- 4. 字典数据 - 仓库部门
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('ERP 仓库部门', 'erp_warehouse_dept', 0, '', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '销售部', '1', 'erp_warehouse_dept', 0, '1', NOW(), '1', NOW(), b'0'),
(2, '仓储部', '2', 'erp_warehouse_dept', 0, '1', NOW(), '1', NOW(), b'0'),
(3, '财务部', '3', 'erp_warehouse_dept', 0, '1', NOW(), '1', NOW(), b'0');

-- 5. 基础数据初始化（仓储中心 / 仓储对应仓库）
INSERT INTO `erp_base_data` (`type`, `name`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES
('storage_center', '默认仓储中心', 1, 0, '1', NOW(), '1', NOW(), b'0', 1),
('storage_warehouse', '默认仓储仓库', 1, 0, '1', NOW(), '1', NOW(), b'0', 1);

-- 6. 菜单配置（仓储中心、仓储对应仓库，挂在基础数据目录 parent_id=6400 下）
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6416, '仓储中心管理', 'erp:base-data:query', 2, 11, 6400, 'storage-center', 'ep:house', 'erp/base/storage-center/index', 'ErpBaseStorageCenter', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6417, '仓储对应仓库管理', 'erp:base-data:query', 2, 12, 6400, 'storage-warehouse', 'ep:box', 'erp/base/storage-warehouse/index', 'ErpBaseStorageWarehouse', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');
