-- ERP 其他应付草稿：允许尚未正式提交的记录暂缺正式提交必填字段。
-- 正式提交时仍由后端执行完整必填及关联数据校验。
ALTER TABLE `erp_payable_other`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 10
        COMMENT '0=草稿 10=未审核 20=已审核 30=已反审（历史兼容）',
    MODIFY COLUMN `biz_time` date NULL COMMENT '开单日期',
    MODIFY COLUMN `supplier_id` bigint NULL COMMENT '供应商ID',
    MODIFY COLUMN `payable_amount` decimal(24,6) NULL COMMENT '应付金额（支持正负数）',
    MODIFY COLUMN `remark` varchar(512) NULL COMMENT '调账原因备注';
