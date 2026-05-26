-- =============================================
-- 报价订单试用问题修复 v23
-- 1. erp_sale_out.account_id 改为可空（修复审核报错）
-- 2. erp_sale_quote 新增 21 个字段
-- =============================================

-- 1. 修复 erp_sale_out 字段 NOT NULL 导致报价订单审核报错
ALTER TABLE erp_sale_out MODIFY COLUMN account_id bigint DEFAULT NULL COMMENT '结算账户编号';
ALTER TABLE erp_sale_out MODIFY COLUMN order_id bigint DEFAULT NULL COMMENT '销售订单编号';
ALTER TABLE erp_sale_out MODIFY COLUMN order_no varchar(255) DEFAULT NULL COMMENT '销售订单号';

-- 1.1 修复 erp_sale_out_items 子表关联字段 NOT NULL 导致报价订单审核报错
ALTER TABLE erp_sale_out_items MODIFY COLUMN order_item_id bigint DEFAULT NULL COMMENT '销售订单项编号';

-- 2. erp_sale_quote 新增字段（幂等）
DROP PROCEDURE IF EXISTS erp_sale_quote_fix_v23_apply;
DELIMITER $$
CREATE PROCEDURE erp_sale_quote_fix_v23_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'order_type') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN order_type varchar(64) DEFAULT NULL COMMENT '订单类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'settle_method') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN settle_method varchar(64) DEFAULT NULL COMMENT '结算方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'prepayment') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN prepayment tinyint(1) DEFAULT 0 COMMENT '先款后货';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'priority') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN priority varchar(64) DEFAULT '正常件' COMMENT '优先级';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'delivery_method') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN delivery_method varchar(64) DEFAULT '客户自提' COMMENT '送货方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'proxy_delivery') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN proxy_delivery tinyint(1) DEFAULT 0 COMMENT '代客户发货';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'delivery_address') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN delivery_address varchar(500) DEFAULT NULL COMMENT '收货地址';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'allowance_price') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN allowance_price decimal(24,6) DEFAULT 0 COMMENT '折让金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'ticket_no') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN ticket_no varchar(64) DEFAULT NULL COMMENT '票据号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'invoice_type') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN invoice_type varchar(64) DEFAULT '收据' COMMENT '开票类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'freight_type') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN freight_type varchar(64) DEFAULT NULL COMMENT '运费类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'freight_amount') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN freight_amount decimal(24,6) DEFAULT 0 COMMENT '费用金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'logistics_company') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN logistics_company varchar(128) DEFAULT NULL COMMENT '物流公司';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'receiver_name') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN receiver_name varchar(64) DEFAULT NULL COMMENT '收货人';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'receiver_phone') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN receiver_phone varchar(32) DEFAULT NULL COMMENT '收货电话';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'price_type') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN price_type varchar(64) DEFAULT NULL COMMENT '价格类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'branch_delivery') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN branch_delivery varchar(64) DEFAULT NULL COMMENT '分店发货';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'expected_delivery_time') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN expected_delivery_time datetime DEFAULT NULL COMMENT '预计发货时间';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'billing_method') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN billing_method varchar(64) DEFAULT '正常单' COMMENT '开单方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'vehicle_plate_no') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN vehicle_plate_no varchar(32) DEFAULT NULL COMMENT '车牌号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'business_type') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN business_type varchar(64) DEFAULT '普通销售' COMMENT '业务类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'developer_user_id') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN developer_user_id bigint DEFAULT NULL COMMENT '开发员编号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'vin') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN vin varchar(64) DEFAULT NULL COMMENT 'VIN车架号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_quote' AND COLUMN_NAME = 'internal_remark') THEN
        ALTER TABLE erp_sale_quote ADD COLUMN internal_remark varchar(500) DEFAULT NULL COMMENT '内部说明';
    END IF;
END$$
DELIMITER ;
CALL erp_sale_quote_fix_v23_apply();
DROP PROCEDURE IF EXISTS erp_sale_quote_fix_v23_apply;
