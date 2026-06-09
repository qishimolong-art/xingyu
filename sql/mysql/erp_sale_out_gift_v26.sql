-- Add gift flag to sale out items.

ALTER TABLE erp_sale_out_items
    ADD COLUMN gift_flag TINYINT(1) DEFAULT 0 COMMENT '是否赠品';
