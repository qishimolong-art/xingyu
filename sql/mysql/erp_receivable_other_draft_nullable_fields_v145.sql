-- ERP 其他应收草稿：允许尚未正式提交的记录暂缺业务日期和客户。
-- 正式提交时仍由后端执行完整必填校验。
ALTER TABLE `erp_receivable_other`
    MODIFY COLUMN `biz_time` date NULL COMMENT '业务日期',
    MODIFY COLUMN `customer_id` bigint NULL COMMENT '客户编号';
