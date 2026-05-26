-- =============================================
-- 销售出库表字段扩展 v24
-- 适用版本: MySQL 5.7+
-- 执行方式: mysql -u root -p --default-character-set=utf8mb4 ruoyi-vue-pro < erp_sale_out_v24.sql
-- =============================================

DROP PROCEDURE IF EXISTS erp_sale_out_v24_apply;
DELIMITER $$
CREATE PROCEDURE erp_sale_out_v24_apply()
BEGIN
    -- ========== erp_sale_out 主表扩展 ==========

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'settle_status') THEN
        ALTER TABLE erp_sale_out ADD COLUMN settle_status TINYINT DEFAULT 0 COMMENT '结算状态（0=未结算, 1=部分结算, 2=已结算）';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'order_type') THEN
        ALTER TABLE erp_sale_out ADD COLUMN order_type VARCHAR(64) DEFAULT NULL COMMENT '订单类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'extra_fee') THEN
        ALTER TABLE erp_sale_out ADD COLUMN extra_fee DECIMAL(24,6) DEFAULT NULL COMMENT '额外费用';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'priority') THEN
        ALTER TABLE erp_sale_out ADD COLUMN priority VARCHAR(64) DEFAULT NULL COMMENT '优先级';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'sign_status') THEN
        ALTER TABLE erp_sale_out ADD COLUMN sign_status TINYINT DEFAULT 0 COMMENT '客户签收状态（0=未签收, 1=已签收）';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'sign_image_url') THEN
        ALTER TABLE erp_sale_out ADD COLUMN sign_image_url VARCHAR(512) DEFAULT NULL COMMENT '签收图片';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'delivery_method') THEN
        ALTER TABLE erp_sale_out ADD COLUMN delivery_method VARCHAR(64) DEFAULT NULL COMMENT '送货方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'shipper') THEN
        ALTER TABLE erp_sale_out ADD COLUMN shipper VARCHAR(128) DEFAULT NULL COMMENT '发货方';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'receiver_name') THEN
        ALTER TABLE erp_sale_out ADD COLUMN receiver_name VARCHAR(128) DEFAULT NULL COMMENT '收货人';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'receiver_phone') THEN
        ALTER TABLE erp_sale_out ADD COLUMN receiver_phone VARCHAR(32) DEFAULT NULL COMMENT '收货电话';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'delivery_no') THEN
        ALTER TABLE erp_sale_out ADD COLUMN delivery_no VARCHAR(128) DEFAULT NULL COMMENT '配送单号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'logistics_no') THEN
        ALTER TABLE erp_sale_out ADD COLUMN logistics_no VARCHAR(128) DEFAULT NULL COMMENT '物流单号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'logistics_company') THEN
        ALTER TABLE erp_sale_out ADD COLUMN logistics_company VARCHAR(128) DEFAULT NULL COMMENT '物流公司';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'sender_name') THEN
        ALTER TABLE erp_sale_out ADD COLUMN sender_name VARCHAR(128) DEFAULT NULL COMMENT '发货人';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'insurance_company') THEN
        ALTER TABLE erp_sale_out ADD COLUMN insurance_company VARCHAR(128) DEFAULT NULL COMMENT '保险公司';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'third_party_no') THEN
        ALTER TABLE erp_sale_out ADD COLUMN third_party_no VARCHAR(128) DEFAULT NULL COMMENT '第三方单号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'third_party_upstream_no') THEN
        ALTER TABLE erp_sale_out ADD COLUMN third_party_upstream_no VARCHAR(128) DEFAULT NULL COMMENT '第三方上游单号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'settle_method') THEN
        ALTER TABLE erp_sale_out ADD COLUMN settle_method VARCHAR(64) DEFAULT NULL COMMENT '结算方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'invoice_amount') THEN
        ALTER TABLE erp_sale_out ADD COLUMN invoice_amount DECIMAL(24,6) DEFAULT NULL COMMENT '开票金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'reduction_amount') THEN
        ALTER TABLE erp_sale_out ADD COLUMN reduction_amount DECIMAL(24,6) DEFAULT NULL COMMENT '减收金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'after_reduction_amount') THEN
        ALTER TABLE erp_sale_out ADD COLUMN after_reduction_amount DECIMAL(24,6) DEFAULT NULL COMMENT '减后金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'bill_amount') THEN
        ALTER TABLE erp_sale_out ADD COLUMN bill_amount DECIMAL(24,6) DEFAULT NULL COMMENT '票据金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'freight') THEN
        ALTER TABLE erp_sale_out ADD COLUMN freight DECIMAL(24,6) DEFAULT NULL COMMENT '运费';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'bill_type') THEN
        ALTER TABLE erp_sale_out ADD COLUMN bill_type VARCHAR(64) DEFAULT NULL COMMENT '票据类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'bill_no') THEN
        ALTER TABLE erp_sale_out ADD COLUMN bill_no VARCHAR(128) DEFAULT NULL COMMENT '票据号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'cancel_count') THEN
        ALTER TABLE erp_sale_out ADD COLUMN cancel_count DECIMAL(24,6) DEFAULT NULL COMMENT '取消数量';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'cancel_amount') THEN
        ALTER TABLE erp_sale_out ADD COLUMN cancel_amount DECIMAL(24,6) DEFAULT NULL COMMENT '取消金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'after_cancel_amount') THEN
        ALTER TABLE erp_sale_out ADD COLUMN after_cancel_amount DECIMAL(24,6) DEFAULT NULL COMMENT '取消后金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'auditor_id') THEN
        ALTER TABLE erp_sale_out ADD COLUMN auditor_id BIGINT DEFAULT NULL COMMENT '审核人编号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'dept_id') THEN
        ALTER TABLE erp_sale_out ADD COLUMN dept_id BIGINT DEFAULT NULL COMMENT '部门编号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'total_weight') THEN
        ALTER TABLE erp_sale_out ADD COLUMN total_weight DECIMAL(24,6) DEFAULT NULL COMMENT '总重';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'approve_time') THEN
        ALTER TABLE erp_sale_out ADD COLUMN approve_time DATETIME DEFAULT NULL COMMENT '审核时间';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'print_time') THEN
        ALTER TABLE erp_sale_out ADD COLUMN print_time DATETIME DEFAULT NULL COMMENT '打印时间';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'confirm_time') THEN
        ALTER TABLE erp_sale_out ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT '确认时间';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'source_create_time') THEN
        ALTER TABLE erp_sale_out ADD COLUMN source_create_time DATETIME DEFAULT NULL COMMENT '来源单制单日期';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'internal_note') THEN
        ALTER TABLE erp_sale_out ADD COLUMN internal_note VARCHAR(512) DEFAULT NULL COMMENT '内部说明';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'vin') THEN
        ALTER TABLE erp_sale_out ADD COLUMN vin VARCHAR(64) DEFAULT NULL COMMENT 'VIN';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'print_count') THEN
        ALTER TABLE erp_sale_out ADD COLUMN print_count INT DEFAULT 0 COMMENT '打印次数';
    END IF;

    -- ========== erp_sale_out_items 子表扩展 ==========

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'vehicle_model') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN vehicle_model VARCHAR(128) DEFAULT NULL COMMENT '车型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'standard') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN standard VARCHAR(128) DEFAULT NULL COMMENT '规格';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'feature_code') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN feature_code VARCHAR(128) DEFAULT NULL COMMENT '特征码';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'brand') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN brand VARCHAR(128) DEFAULT NULL COMMENT '品牌';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'drawing_no') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN drawing_no VARCHAR(128) DEFAULT NULL COMMENT '图号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'batch_no') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN batch_no VARCHAR(128) DEFAULT NULL COMMENT '批次';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'warehouse_position') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN warehouse_position VARCHAR(128) DEFAULT NULL COMMENT '仓位';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'unit_weight') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN unit_weight DECIMAL(24,6) DEFAULT NULL COMMENT '单重';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'total_weight') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN total_weight DECIMAL(24,6) DEFAULT NULL COMMENT '总重';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'after_reduction_price') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN after_reduction_price DECIMAL(24,6) DEFAULT NULL COMMENT '减后价';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'after_reduction_amount') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN after_reduction_amount DECIMAL(24,6) DEFAULT NULL COMMENT '减后金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'actual_sale_amount') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN actual_sale_amount DECIMAL(24,6) DEFAULT NULL COMMENT '实际销售金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'origin_place') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN origin_place VARCHAR(128) DEFAULT NULL COMMENT '产地';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'supplier_name') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN supplier_name VARCHAR(128) DEFAULT NULL COMMENT '供应商名称';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'original_product_price') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN original_product_price DECIMAL(24,6) DEFAULT NULL COMMENT '浮动前价格（调价前原价）';
    END IF;

    -- ========== 销售模块逻辑对齐 v24 追加字段 ==========

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out' AND COLUMN_NAME = 'return_status') THEN
        ALTER TABLE erp_sale_out ADD COLUMN return_status TINYINT DEFAULT 0 COMMENT '退货状态（0=未退货, 1=部分退货, 2=已退货）';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'adjusted') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN adjusted TINYINT(1) DEFAULT 0 COMMENT '是否被调价';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items' AND COLUMN_NAME = 'adjust_id') THEN
        ALTER TABLE erp_sale_out_items ADD COLUMN adjust_id BIGINT DEFAULT NULL COMMENT '关联调价单编号';
    END IF;

END$$
DELIMITER ;
CALL erp_sale_out_v24_apply();
DROP PROCEDURE IF EXISTS erp_sale_out_v24_apply;
