-- erp_customer_contact: 补齐 Tab 2 联系人字段（v16），对齐《客户v2》文档
-- 兼容 MySQL 5.7 / 8.0.x 全版本（不使用 ADD COLUMN IF NOT EXISTS，改为基于 information_schema 判断的存储过程）
-- 幂等：重复执行安全

DROP PROCEDURE IF EXISTS erp_customer_contact_v16_apply;

DELIMITER $$
CREATE PROCEDURE erp_customer_contact_v16_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'salesperson') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN salesperson TINYINT(1) DEFAULT 0 COMMENT '是否导购员';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'gender') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN gender TINYINT COMMENT '性别 1=男 2=女';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'company_id') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN company_id BIGINT COMMENT '所属公司（客户ID，自关联）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'fax') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN fax VARCHAR(32) COMMENT '传真';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'dept_id') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN dept_id BIGINT COMMENT '所属部门';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'importance') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN importance TINYINT DEFAULT 1 COMMENT '重要性 1=普通 2=重要 3=决策人';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'commission_rate') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN commission_rate DECIMAL(10, 2) COMMENT '提成率（百分比）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'fixed_commission') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN fixed_commission TINYINT(1) DEFAULT 0 COMMENT '固定提成';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'post_code') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN post_code VARCHAR(20) COMMENT '邮编';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'birthday') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN birthday DATE COMMENT '生日';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer_contact' AND COLUMN_NAME = 'last_contact_time') THEN
        ALTER TABLE erp_customer_contact ADD COLUMN last_contact_time DATETIME COMMENT '最后联系时间';
    END IF;
END$$
DELIMITER ;

CALL erp_customer_contact_v16_apply();
DROP PROCEDURE IF EXISTS erp_customer_contact_v16_apply;
