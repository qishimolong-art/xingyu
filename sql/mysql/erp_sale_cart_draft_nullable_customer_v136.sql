-- 销售手推车允许在新增页关闭时保存尚未选择客户的草稿。
-- 正式提交仍由 ErpSaleCartSaveReqVO 的 @NotNull 校验保证客户必填。
ALTER TABLE `erp_sale_cart`
    MODIFY COLUMN `customer_id` BIGINT DEFAULT NULL COMMENT '客户编号（草稿可为空）';
