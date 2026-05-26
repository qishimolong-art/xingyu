-- erp_customer_extend_info: 客户拓展信息结构化表（v15），对齐客户v2文档Tab 6
-- 执行时机：erp_sale_customer_extend_v10.sql 之后

CREATE TABLE IF NOT EXISTS `erp_customer_extend_info` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `customer_id` bigint NOT NULL COMMENT '客户编号',

    -- 1) 财务与结算信息
    `advance_amount` decimal(18,2) DEFAULT NULL COMMENT '预收款金额',
    `base_amount` decimal(18,2) DEFAULT NULL COMMENT '铺底金额',
    `settle_account_id` bigint DEFAULT NULL COMMENT '结算账户编号',
    `bank_name2` varchar(128) DEFAULT NULL COMMENT '开户行2',
    `bank_account2` varchar(64) DEFAULT NULL COMMENT '账户2',
    `settle_day` int DEFAULT NULL COMMENT '结账日期（1-31）',
    `price_markup_rate` decimal(10,4) DEFAULT NULL COMMENT '售价上浮率（%）',
    `discount_price_level` int DEFAULT NULL COMMENT '优惠价格级别',

    -- 2) 销售与业绩信息
    `monthly_sale_target` decimal(18,2) DEFAULT NULL COMMENT '月销量目标',
    `monthly_business_scale` decimal(18,2) DEFAULT NULL COMMENT '月营业规模',
    `total_assets` decimal(18,2) DEFAULT NULL COMMENT '总资产',
    `points_rate` decimal(10,4) DEFAULT NULL COMMENT '积分系数',
    `delivery_fee` decimal(18,2) DEFAULT NULL COMMENT '送货费用',

    -- 3) 法人与企业信息
    `legal_person` varchar(64) DEFAULT NULL COMMENT '企业法人',
    `legal_person_phone` varchar(32) DEFAULT NULL COMMENT '法人电话',
    `boss` varchar(64) DEFAULT NULL COMMENT '老板',
    `boss_phone` varchar(32) DEFAULT NULL COMMENT '老板电话',
    `business_scope` varchar(1000) DEFAULT NULL COMMENT '经营项目',
    `id_card_no` varchar(32) DEFAULT NULL COMMENT '身份证号码',
    `ext_email` varchar(128) DEFAULT NULL COMMENT '电子邮件（拓展）',

    -- 4) 采购退货联系人信息
    `purchase_return_contact` varchar(64) DEFAULT NULL COMMENT '采购退货联系人',
    `purchase_return_phone` varchar(32) DEFAULT NULL COMMENT '采购退货电话',
    `purchase_return_address` varchar(500) DEFAULT NULL COMMENT '采购退货地址',

    -- 5) 系统与业务开关
    `chain_group_id` bigint DEFAULT NULL COMMENT '对应连锁（关联 erp_sale_config CHAIN_GROUP）',
    `alternate_code` varchar(64) DEFAULT NULL COMMENT '替代编码',
    `general_distributor_id` bigint DEFAULT NULL COMMENT '总经销',
    `generate_out_bill` bit(1) DEFAULT NULL COMMENT '是否生成出仓单',
    `generate_in_bill` bit(1) DEFAULT NULL COMMENT '是否生成入仓单',
    `auto_confirm_receive_days` int DEFAULT NULL COMMENT '自动确认收货：0=不自动 3=3天 7=7天 15=15天',
    `internal_description` varchar(1000) DEFAULT NULL COMMENT '内部说明',
    `exclude_stocking` bit(1) DEFAULT NULL COMMENT '不参与备货',
    `reconcile_service_user_id` bigint DEFAULT NULL COMMENT '对账客服编号（关联 system_user）',
    `yijia_wang_account` varchar(128) DEFAULT NULL COMMENT '易加网账号',

    -- BaseDO 标准字段
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_id` (`customer_id`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 客户拓展信息（结构化）';
