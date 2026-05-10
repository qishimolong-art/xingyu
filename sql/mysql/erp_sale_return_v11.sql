-- ERP sale return v11: dual return mode support.
-- Execute after erp_sale_customer_extend_v10.sql.

ALTER TABLE `erp_sale_return`
    ADD COLUMN `return_mode` tinyint DEFAULT 0 COMMENT 'Return mode: 0 legacy order, 10 by sale out, 20 by stock' AFTER `sale_user_id`,
    ADD COLUMN `source_out_id` bigint DEFAULT NULL COMMENT 'Source sale out id' AFTER `order_no`,
    ADD COLUMN `source_out_no` varchar(64) DEFAULT NULL COMMENT 'Source sale out no' AFTER `source_out_id`;

ALTER TABLE `erp_sale_return_items`
    ADD COLUMN `source_out_item_id` bigint DEFAULT NULL COMMENT 'Source sale out item id' AFTER `order_item_id`;

ALTER TABLE `erp_sale_return`
    ADD INDEX `idx_sale_return_source_out_id` (`source_out_id`),
    ADD INDEX `idx_sale_return_return_mode` (`return_mode`);

ALTER TABLE `erp_sale_return_items`
    ADD INDEX `idx_sale_return_items_source_out_item_id` (`source_out_item_id`);
