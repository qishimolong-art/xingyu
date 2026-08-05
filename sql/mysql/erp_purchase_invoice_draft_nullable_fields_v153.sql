-- ERP 采购票据草稿允许在正式提交前暂缺供应商、开票日期和票据类型。
-- 正式创建、更新并提交、直接提交仍由 ErpPurchaseInvoiceSaveReqVO 和 Service 严格校验。
ALTER TABLE `erp_purchase_invoice`
    MODIFY COLUMN `supplier_id` bigint NULL DEFAULT NULL COMMENT '供应商ID（草稿可空）',
    MODIFY COLUMN `invoice_date` date NULL DEFAULT NULL COMMENT '开票日期（草稿可空）',
    MODIFY COLUMN `invoice_type` varchar(64) NULL DEFAULT NULL COMMENT '票据类型（草稿可空）';
