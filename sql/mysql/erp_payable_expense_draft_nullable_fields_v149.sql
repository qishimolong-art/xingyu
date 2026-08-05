-- 费用支付草稿允许在正式提交前暂缺日期、账户和申请人。
-- 正式提交仍由 ErpPayableExpenseSaveReqVO 与 Service 执行严格校验。
ALTER TABLE `erp_payable_expense`
    MODIFY COLUMN `biz_time` date NULL COMMENT '单据日期',
    MODIFY COLUMN `account_id` bigint NULL COMMENT '结算账户ID',
    MODIFY COLUMN `handler_id` bigint NULL COMMENT '申请人用户ID';
