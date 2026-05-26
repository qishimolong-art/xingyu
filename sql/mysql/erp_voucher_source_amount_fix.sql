ALTER TABLE `erp_voucher`
    ADD COLUMN `source_biz_amount` decimal(24, 6) NULL DEFAULT NULL COMMENT '业务单据金额' AFTER `source_biz_no`;
