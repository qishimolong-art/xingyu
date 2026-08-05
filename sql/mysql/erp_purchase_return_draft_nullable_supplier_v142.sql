-- ERP 采购退货草稿允许尚未选择供应商；正式提交仍由 Service 做必填校验。
ALTER TABLE `erp_purchase_return`
    MODIFY COLUMN `supplier_id` BIGINT NULL DEFAULT NULL COMMENT '供应商编号（草稿可空）';
