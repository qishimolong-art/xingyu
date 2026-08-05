-- 报价订单草稿允许尚未选择客户；正式提交仍由 VO 与 Service 做必填校验。
ALTER TABLE `erp_sale_quote`
    MODIFY COLUMN `customer_id` BIGINT DEFAULT NULL COMMENT '客户编号（草稿可为空）';
