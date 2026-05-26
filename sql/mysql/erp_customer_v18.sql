-- erp_customer: 列表占位列字段（v18），对齐客户v2 第九章
-- 兼容 MySQL 5.7 / 8.0.x 全版本（不使用 ADD COLUMN IF NOT EXISTS，改为基于 information_schema 判断的存储过程）
-- 幂等：重复执行安全

DROP PROCEDURE IF EXISTS erp_customer_v18_apply;

DELIMITER $$
CREATE PROCEDURE erp_customer_v18_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer' AND COLUMN_NAME = 'customer_tag') THEN
        ALTER TABLE erp_customer ADD COLUMN customer_tag VARCHAR(200) COMMENT '客户标签（逗号分隔）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer' AND COLUMN_NAME = 'wechat_service') THEN
        ALTER TABLE erp_customer ADD COLUMN wechat_service VARCHAR(64) COMMENT '微信客服账号/姓名';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer' AND COLUMN_NAME = 'credit_limit') THEN
        ALTER TABLE erp_customer ADD COLUMN credit_limit DECIMAL(18,2) COMMENT '白条授信额度';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer' AND COLUMN_NAME = 'data_center_audit_status') THEN
        ALTER TABLE erp_customer ADD COLUMN data_center_audit_status TINYINT COMMENT '数据中心审核状态 0未审核 1已审核 2驳回';
    END IF;
END$$
DELIMITER ;

CALL erp_customer_v18_apply();
DROP PROCEDURE IF EXISTS erp_customer_v18_apply;
