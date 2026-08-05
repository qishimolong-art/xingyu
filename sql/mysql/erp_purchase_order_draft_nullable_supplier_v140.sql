-- ERP 采购订单草稿：允许在尚未选择供应商时保存未完成内容。
-- 正式提交仍由 ErpPurchaseOrderSaveReqVO 与 Service 严格校验 supplier_id。
ALTER TABLE `erp_purchase_order`
    MODIFY COLUMN `supplier_id` BIGINT NULL DEFAULT NULL COMMENT '供应商编号（草稿可空）';
