-- ERP 收款单草稿允许在尚未选择客户、收款账户时保存未完成内容。
-- 正式创建、更新并提交、直接提交仍由后端执行完整必填校验。
ALTER TABLE `erp_finance_receipt`
    MODIFY COLUMN `customer_id` bigint NULL COMMENT '客户编号',
    MODIFY COLUMN `account_id` bigint NULL COMMENT '收款账户编号';
