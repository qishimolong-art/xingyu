-- =============================================
-- 销售退货主表 + 子表字段扩展 v25
-- 适用 MySQL 5.7 / 8.0.x 全版本
-- =============================================

DROP PROCEDURE IF EXISTS erp_sale_return_v25_apply;
DELIMITER $$
CREATE PROCEDURE erp_sale_return_v25_apply()
BEGIN
    -- ========== 主表 erp_sale_return 新增字段 ==========
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'dept_id') THEN
        ALTER TABLE erp_sale_return ADD COLUMN dept_id BIGINT DEFAULT NULL COMMENT '部门编号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'priority') THEN
        ALTER TABLE erp_sale_return ADD COLUMN priority VARCHAR(64) DEFAULT NULL COMMENT '优先级别';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'invoice_type') THEN
        ALTER TABLE erp_sale_return ADD COLUMN invoice_type VARCHAR(64) DEFAULT NULL COMMENT '开票类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'bill_no') THEN
        ALTER TABLE erp_sale_return ADD COLUMN bill_no VARCHAR(128) DEFAULT NULL COMMENT '票据号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'delivery_method') THEN
        ALTER TABLE erp_sale_return ADD COLUMN delivery_method VARCHAR(64) DEFAULT NULL COMMENT '退货方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'reduction_amount') THEN
        ALTER TABLE erp_sale_return ADD COLUMN reduction_amount DECIMAL(24,6) DEFAULT NULL COMMENT '减收金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'freight_type') THEN
        ALTER TABLE erp_sale_return ADD COLUMN freight_type VARCHAR(64) DEFAULT NULL COMMENT '运费类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'freight_amount') THEN
        ALTER TABLE erp_sale_return ADD COLUMN freight_amount DECIMAL(24,6) DEFAULT NULL COMMENT '运费金额';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'settle_method') THEN
        ALTER TABLE erp_sale_return ADD COLUMN settle_method VARCHAR(64) DEFAULT NULL COMMENT '结算方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'logistics_company') THEN
        ALTER TABLE erp_sale_return ADD COLUMN logistics_company VARCHAR(128) DEFAULT NULL COMMENT '物流公司';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'vehicle_no') THEN
        ALTER TABLE erp_sale_return ADD COLUMN vehicle_no VARCHAR(64) DEFAULT NULL COMMENT '车牌号';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'branch_store') THEN
        ALTER TABLE erp_sale_return ADD COLUMN branch_store VARCHAR(128) DEFAULT NULL COMMENT '货到分店';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'branch_store_enabled') THEN
        ALTER TABLE erp_sale_return ADD COLUMN branch_store_enabled TINYINT(1) DEFAULT 0 COMMENT '货到分店启用';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'purchase_area') THEN
        ALTER TABLE erp_sale_return ADD COLUMN purchase_area VARCHAR(64) DEFAULT NULL COMMENT '进货区';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'business_type') THEN
        ALTER TABLE erp_sale_return ADD COLUMN business_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'order_method') THEN
        ALTER TABLE erp_sale_return ADD COLUMN order_method VARCHAR(64) DEFAULT NULL COMMENT '开单方式';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return' AND COLUMN_NAME = 'developer_user_id') THEN
        ALTER TABLE erp_sale_return ADD COLUMN developer_user_id BIGINT DEFAULT NULL COMMENT '开发员编号';
    END IF;

    -- ========== 子表 erp_sale_return_items 新增字段 ==========
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return_items' AND COLUMN_NAME = 'return_reason') THEN
        ALTER TABLE erp_sale_return_items ADD COLUMN return_reason VARCHAR(128) DEFAULT NULL COMMENT '退货原因';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_return_items' AND COLUMN_NAME = 'warehouse_position') THEN
        ALTER TABLE erp_sale_return_items ADD COLUMN warehouse_position VARCHAR(128) DEFAULT NULL COMMENT '货位/仓位';
    END IF;

    -- ========== 修复 account_id / order_id / order_no NOT NULL 问题（改为可空） ==========
    ALTER TABLE erp_sale_return MODIFY COLUMN account_id BIGINT DEFAULT NULL COMMENT '结算账户编号';
    ALTER TABLE erp_sale_return MODIFY COLUMN order_id BIGINT DEFAULT NULL COMMENT '销售订单编号';
    ALTER TABLE erp_sale_return MODIFY COLUMN order_no VARCHAR(255) DEFAULT NULL COMMENT '销售订单号';

    -- ========== 修复子表 order_item_id NOT NULL 问题（改为可空） ==========
    ALTER TABLE erp_sale_return_items MODIFY COLUMN order_item_id BIGINT DEFAULT NULL COMMENT '销售订单项编号';

END$$
DELIMITER ;
CALL erp_sale_return_v25_apply();
DROP PROCEDURE IF EXISTS erp_sale_return_v25_apply;
