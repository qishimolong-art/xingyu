-- 其他收入草稿允许暂时缺少正式提交必填字段。
-- 正式提交仍由后端服务执行完整校验。
ALTER TABLE `erp_receivable_other_income`
    MODIFY COLUMN `biz_time` datetime NULL,
    MODIFY COLUMN `settle_method` varchar(64) NULL,
    MODIFY COLUMN `account_id` bigint NULL,
    MODIFY COLUMN `income_type` varchar(64) NULL,
    MODIFY COLUMN `handler_id` bigint NULL;

ALTER TABLE `erp_receivable_other_income_item`
    MODIFY COLUMN `item_name` varchar(128) NULL,
    MODIFY COLUMN `amount` decimal(24,6) NULL;
