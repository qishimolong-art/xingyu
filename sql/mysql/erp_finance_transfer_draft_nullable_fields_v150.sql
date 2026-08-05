-- 银行转账草稿允许在正式提交前暂缺时间、转出/转入账户和转账金额。
-- 正式提交仍由 ErpFinanceTransferSaveReqVO 与 Service 执行严格校验。
ALTER TABLE `erp_finance_transfer`
    MODIFY COLUMN `transfer_time` datetime NULL COMMENT '转账时间',
    MODIFY COLUMN `out_account_id` bigint NULL COMMENT '转出账户ID',
    MODIFY COLUMN `in_account_id` bigint NULL COMMENT '转入账户ID',
    MODIFY COLUMN `transfer_price` decimal(24,6) NULL COMMENT '转账金额';
