-- 销售退货草稿允许尚未选择客户；正式提交仍由 Service 做必填及来源校验。
ALTER TABLE `erp_sale_return`
    MODIFY COLUMN `customer_id` BIGINT DEFAULT NULL COMMENT '客户编号（草稿可为空）';
