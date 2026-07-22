-- Add editable product-inventory fields on each stock row.
-- Run this once in each target MySQL database before using inline stock editing.

ALTER TABLE erp_stock
    ADD COLUMN purchase_price DECIMAL(24,6) DEFAULT NULL COMMENT '库存行进价' AFTER shelf,
    ADD COLUMN occupied_count DECIMAL(24,6) DEFAULT NULL COMMENT '库存行占用数' AFTER purchase_price,
    ADD COLUMN pending_in_count DECIMAL(24,6) DEFAULT NULL COMMENT '库存行未入数' AFTER occupied_count,
    ADD COLUMN in_transit_count DECIMAL(24,6) DEFAULT NULL COMMENT '库存行在途数' AFTER pending_in_count;
