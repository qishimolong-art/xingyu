-- v24：销售手推车扩展字段 + 销售出库 order_id/order_item_id 改可空
-- 修复 Bug 5：终审生成销售单时 order_id 传 null 报错
-- MySQL 5.7 兼容，使用 information_schema 存储过程方案保幂等

DROP PROCEDURE IF EXISTS erp_sale_cart_fix_v24_apply;
DELIMITER $$
CREATE PROCEDURE erp_sale_cart_fix_v24_apply()
BEGIN
    -- ========================================
    -- 1. erp_sale_out.order_id 改为可空
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out'
               AND COLUMN_NAME = 'order_id' AND IS_NULLABLE = 'NO') THEN
        ALTER TABLE erp_sale_out MODIFY COLUMN order_id BIGINT DEFAULT NULL COMMENT '销售订单编号';
    END IF;

    -- ========================================
    -- 2. erp_sale_out_items.order_item_id 改为可空
    -- ========================================
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items'
               AND COLUMN_NAME = 'order_item_id' AND IS_NULLABLE = 'NO') THEN
        ALTER TABLE erp_sale_out_items MODIFY COLUMN order_item_id BIGINT DEFAULT NULL COMMENT '销售订单项编号';
    END IF;

    -- ========================================
    -- 3. erp_sale_cart 加 22 个扩展字段
    -- ========================================
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'business_type') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN business_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'order_type') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN order_type VARCHAR(64) DEFAULT NULL COMMENT '订单类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'billing_method') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN billing_method VARCHAR(64) DEFAULT NULL COMMENT '开单方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'settle_method') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN settle_method VARCHAR(64) DEFAULT NULL COMMENT '结算方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'invoice_type') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN invoice_type VARCHAR(64) DEFAULT NULL COMMENT '发票类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'delivery_method') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN delivery_method VARCHAR(64) DEFAULT NULL COMMENT '配送方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'freight_type') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN freight_type VARCHAR(64) DEFAULT NULL COMMENT '运费类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'priority') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN priority VARCHAR(64) DEFAULT NULL COMMENT '优先级';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'price_type') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN price_type VARCHAR(64) DEFAULT NULL COMMENT '价格类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'logistics_company') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN logistics_company VARCHAR(128) DEFAULT NULL COMMENT '物流公司';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'developer_user_id') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN developer_user_id BIGINT DEFAULT NULL COMMENT '开发人员编号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'contact_person') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN contact_person VARCHAR(128) DEFAULT NULL COMMENT '联系人';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'contact_phone') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN contact_phone VARCHAR(64) DEFAULT NULL COMMENT '联系电话';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'delivery_address') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN delivery_address VARCHAR(512) DEFAULT NULL COMMENT '送货地址';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'delivery_date') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN delivery_date DATETIME DEFAULT NULL COMMENT '送货日期';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'tax_rate') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN tax_rate DECIMAL(24,6) DEFAULT NULL COMMENT '税率';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'total_freight') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN total_freight DECIMAL(24,6) DEFAULT NULL COMMENT '运费合计';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'payment_date') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN payment_date DATETIME DEFAULT NULL COMMENT '付款日期';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'business_entity') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN business_entity VARCHAR(128) DEFAULT NULL COMMENT '经营主体';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'order_method') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN order_method VARCHAR(64) DEFAULT NULL COMMENT '订货方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'source_type2') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN source_type2 VARCHAR(64) DEFAULT NULL COMMENT '来源类型2';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_cart' AND COLUMN_NAME = 'remark2') THEN
        ALTER TABLE erp_sale_cart ADD COLUMN remark2 VARCHAR(512) DEFAULT NULL COMMENT '备注2';
    END IF;

END$$
DELIMITER ;
CALL erp_sale_cart_fix_v24_apply();
DROP PROCEDURE IF EXISTS erp_sale_cart_fix_v24_apply;
