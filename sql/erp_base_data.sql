-- ERP 基础数据表
CREATE TABLE IF NOT EXISTS `erp_base_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `type` varchar(50) NOT NULL COMMENT '数据类型（region/category/supplier_type/logistics_company）',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0启用 1禁用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 基础数据表';

-- 初始化区域数据
INSERT INTO `erp_base_data` (`type`, `name`, `sort`, `status`) VALUES
('region', '华东', 1, 0),
('region', '华南', 2, 0),
('region', '华北', 3, 0),
('region', '华中', 4, 0),
('region', '西南', 5, 0),
('region', '西北', 6, 0),
('region', '东北', 7, 0);

-- 初始化往来类别数据
INSERT INTO `erp_base_data` (`type`, `name`, `sort`, `status`) VALUES
('category', '原材料供应商', 1, 0),
('category', '设备供应商', 2, 0),
('category', '服务供应商', 3, 0),
('category', '贸易商', 4, 0);

-- 初始化供应商类型数据
INSERT INTO `erp_base_data` (`type`, `name`, `sort`, `status`) VALUES
('supplier_type', '生产厂家', 1, 0),
('supplier_type', '代理商', 2, 0),
('supplier_type', '经销商', 3, 0),
('supplier_type', '贸易商', 4, 0);

-- 初始化物流公司数据
INSERT INTO `erp_base_data` (`type`, `name`, `sort`, `status`) VALUES
('logistics_company', '顺丰速运', 1, 0),
('logistics_company', '中通快递', 2, 0),
('logistics_company', '圆通速递', 3, 0),
('logistics_company', '韵达快递', 4, 0),
('logistics_company', '德邦物流', 5, 0);
