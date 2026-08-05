-- ERP 采购入库草稿：允许未选择供应商时先保存草稿。
-- 正式创建、更新并提交、直接提交仍由 VO 与 Service 严格校验 supplier_id。
ALTER TABLE `erp_purchase_in`
    MODIFY COLUMN `supplier_id` BIGINT NULL DEFAULT NULL COMMENT '供应商编号（草稿可空）';
