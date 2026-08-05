-- ERP 付款单草稿允许在尚未选择供应商、付款账户时保存未完成内容。
-- 正式创建、更新并提交、直接提交仍由 VO 与 Service 严格校验。
ALTER TABLE `erp_finance_payment`
    MODIFY COLUMN `supplier_id` BIGINT NULL DEFAULT NULL COMMENT '供应商编号（草稿可空）',
    MODIFY COLUMN `account_id` BIGINT NULL DEFAULT NULL COMMENT '付款账户编号（草稿可空）';
