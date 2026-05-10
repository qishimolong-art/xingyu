-- 六期：采购订单赠品字段
-- 为采购订单子表增加"是否赠品"标记
ALTER TABLE `erp_purchase_order_items`
    ADD COLUMN `gift` TINYINT(1) DEFAULT 0 COMMENT '是否赠品（0=否，1=是）' AFTER `arrival_count`;
