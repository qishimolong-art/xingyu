-- 采购订单子表字段扩展（四期）
-- 为 erp_purchase_order_items 表新增 12 个字段，对齐汽配云子表

ALTER TABLE `erp_purchase_order_items`
    ADD COLUMN `warehouse_id` BIGINT DEFAULT NULL COMMENT '仓库编号' AFTER `return_count`,
    ADD COLUMN `warehouse_position` VARCHAR(64) DEFAULT NULL COMMENT '货架位' AFTER `warehouse_id`,
    ADD COLUMN `vehicle_model` VARCHAR(64) DEFAULT NULL COMMENT '适用车型（产品带出）' AFTER `warehouse_position`,
    ADD COLUMN `origin_place` VARCHAR(64) DEFAULT NULL COMMENT '产地（产品带出）' AFTER `vehicle_model`,
    ADD COLUMN `standard` VARCHAR(64) DEFAULT NULL COMMENT '规格（产品带出）' AFTER `origin_place`,
    ADD COLUMN `feature_code` VARCHAR(64) DEFAULT NULL COMMENT '特征码（产品带出）' AFTER `standard`,
    ADD COLUMN `drawing_no` VARCHAR(64) DEFAULT NULL COMMENT '图号（产品带出）' AFTER `feature_code`,
    ADD COLUMN `batch_no` VARCHAR(64) DEFAULT NULL COMMENT '批次' AFTER `drawing_no`,
    ADD COLUMN `factory_code` VARCHAR(64) DEFAULT NULL COMMENT '厂家编码（产品带出）' AFTER `batch_no`,
    ADD COLUMN `brand` VARCHAR(64) DEFAULT NULL COMMENT '品牌（产品带出）' AFTER `factory_code`,
    ADD COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT '默认供应商编号' AFTER `brand`,
    ADD COLUMN `arrival_count` DECIMAL(24,6) DEFAULT 0 COMMENT '到货数量' AFTER `supplier_id`;
