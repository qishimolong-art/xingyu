-- ERP 销售管理 - 客户企业地区、拓展信息、工商信息占位表
-- 执行时机：erp_sale_customer_tabs_v9.sql 之后

CREATE TABLE IF NOT EXISTS `erp_customer_area` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `customer_id` bigint NOT NULL COMMENT '客户编号',
    `longitude` decimal(18,10) DEFAULT NULL COMMENT '经度',
    `latitude` decimal(18,10) DEFAULT NULL COMMENT '纬度',
    `map_address` varchar(255) DEFAULT NULL COMMENT '地图地址',
    `detail_address` varchar(255) DEFAULT NULL COMMENT '详细地址',
    `defaulted` bit(1) DEFAULT b'0' COMMENT '默认地址',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_customer_area_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 客户企业地区';

CREATE TABLE IF NOT EXISTS `erp_customer_extend` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `customer_id` bigint NOT NULL COMMENT '客户编号',
    `extend_key` varchar(64) NOT NULL COMMENT '字段标识',
    `extend_name` varchar(128) DEFAULT NULL COMMENT '字段名称',
    `extend_value` text DEFAULT NULL COMMENT '字段值',
    `extend_type` varchar(64) DEFAULT NULL COMMENT '字段类型',
    `sort` int DEFAULT 0 COMMENT '排序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_customer_extend_customer_id` (`customer_id`),
    KEY `idx_customer_extend_key` (`extend_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 客户拓展信息';

CREATE TABLE IF NOT EXISTS `erp_customer_business_info` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `customer_id` bigint NOT NULL COMMENT '客户编号',
    `credit_code` varchar(64) DEFAULT NULL COMMENT '统一社会信用代码',
    `legal_person` varchar(64) DEFAULT NULL COMMENT '法人',
    `registered_capital` varchar(64) DEFAULT NULL COMMENT '注册资本',
    `establish_date` varchar(64) DEFAULT NULL COMMENT '成立日期',
    `business_status` varchar(64) DEFAULT NULL COMMENT '经营状态',
    `business_scope` text DEFAULT NULL COMMENT '经营范围',
    `raw_data` longtext DEFAULT NULL COMMENT '原始数据',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_customer_business_info_customer_id` (`customer_id`),
    KEY `idx_customer_business_info_credit_code` (`credit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 客户工商信息';
