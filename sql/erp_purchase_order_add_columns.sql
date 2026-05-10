-- =============================================
-- 采购订单新增字段 - 数据库表字段迁移
-- 注意：如果字段已存在会报错，可跳过
-- =============================================

ALTER TABLE erp_purchase_order ADD COLUMN purchaser BIGINT COMMENT '采购员（用户ID）';
ALTER TABLE erp_purchase_order ADD COLUMN dept_id BIGINT COMMENT '部门ID';
ALTER TABLE erp_purchase_order ADD COLUMN order_date DATE COMMENT '订货日期';
ALTER TABLE erp_purchase_order ADD COLUMN purchase_cycle INT COMMENT '采购周期(天)';
ALTER TABLE erp_purchase_order ADD COLUMN order_company VARCHAR(128) COMMENT '订货公司';
ALTER TABLE erp_purchase_order ADD COLUMN tax_percent DECIMAL(24,2) COMMENT '税率(%)';
