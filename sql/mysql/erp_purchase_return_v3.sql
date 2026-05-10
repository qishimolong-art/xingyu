-- ERP 采购退货 三期 双模式字段扩展脚本
-- 用途：为 erp_purchase_return 增加 return_mode；erp_purchase_return_items 增加原入库单追溯字段
-- 日期：2026-05-08

ALTER TABLE `erp_purchase_return`
    ADD COLUMN `return_mode` INT DEFAULT 10 COMMENT '退货模式（10=按单退货 / 20=按库存退货）';

ALTER TABLE `erp_purchase_return_items`
    ADD COLUMN `source_in_id` BIGINT DEFAULT NULL COMMENT '原采购入库单 ID',
    ADD COLUMN `source_in_item_id` BIGINT DEFAULT NULL COMMENT '原采购入库项 ID',
    ADD COLUMN `source_in_no` VARCHAR(64) DEFAULT NULL COMMENT '原采购入库单号（冗余）';
