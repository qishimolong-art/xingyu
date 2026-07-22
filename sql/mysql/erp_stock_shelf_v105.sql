-- Add per-stock-row shelf location for product inventory.
-- Run this once in each target MySQL database before deploying code that reads erp_stock.shelf.

ALTER TABLE erp_stock
    ADD COLUMN shelf VARCHAR(64) DEFAULT NULL COMMENT '货架号' AFTER dept_id;
