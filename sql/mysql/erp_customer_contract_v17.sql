-- erp_customer_contract: 补齐 Tab 3 合同字段（v17），对齐客户v2文档
-- 兼容 MySQL 5.7 / 8.0.x 全版本（不使用 ADD COLUMN IF NOT EXISTS，改为基于 information_schema 判断的存储过程）
-- 幂等：重复执行安全

DROP PROCEDURE IF EXISTS erp_customer_contract_v17_apply;

DELIMITER $$
CREATE PROCEDURE erp_customer_contract_v17_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contract' AND COLUMN_NAME = 'main_contract') THEN
        ALTER TABLE erp_customer_contract ADD COLUMN main_contract TINYINT(1) DEFAULT 0 COMMENT '是否主要合同';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contract' AND COLUMN_NAME = 'rebate_enabled') THEN
        ALTER TABLE erp_customer_contract ADD COLUMN rebate_enabled TINYINT(1) DEFAULT 0 COMMENT '返点证集（是否启用返点）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contract' AND COLUMN_NAME = 'freight_settle_method') THEN
        ALTER TABLE erp_customer_contract ADD COLUMN freight_settle_method TINYINT COMMENT '运费结算方式 1=我方承担 2=客户承担 3=双方平摊 4=月结';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contract' AND COLUMN_NAME = 'summary') THEN
        ALTER TABLE erp_customer_contract ADD COLUMN summary TEXT COMMENT '合同摘要';
    END IF;
END$$
DELIMITER ;

CALL erp_customer_contract_v17_apply();
DROP PROCEDURE IF EXISTS erp_customer_contract_v17_apply;
