-- 销售手推车运费自动生成财务草稿（v137）
-- MySQL 5.7 / 8.0 兼容，可重复执行。
-- 仅增加来源追踪字段和幂等索引，不修改历史单据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_sale_cart_freight_finance_v137;

DELIMITER //
CREATE PROCEDURE add_erp_sale_cart_freight_finance_v137()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_other'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_other'
          AND COLUMN_NAME = 'source_id'
    ) THEN
        ALTER TABLE `erp_receivable_other`
            ADD COLUMN `source_id` BIGINT DEFAULT NULL COMMENT '来源单据编号'
                AFTER `source_type`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_other'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_other'
          AND INDEX_NAME = 'uk_tenant_source'
    ) THEN
        ALTER TABLE `erp_receivable_other`
            ADD UNIQUE KEY `uk_tenant_source`
                (`tenant_id`, `source_type`, `source_id`, `deleted`);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_expense'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_expense'
          AND COLUMN_NAME = 'source_type'
    ) THEN
        ALTER TABLE `erp_payable_expense`
            ADD COLUMN `source_type` VARCHAR(64) DEFAULT NULL COMMENT '来源类型'
                AFTER `related_biz`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_expense'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_expense'
          AND COLUMN_NAME = 'source_id'
    ) THEN
        ALTER TABLE `erp_payable_expense`
            ADD COLUMN `source_id` BIGINT DEFAULT NULL COMMENT '来源单据编号'
                AFTER `source_type`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_expense'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_expense'
          AND COLUMN_NAME = 'source_no'
    ) THEN
        ALTER TABLE `erp_payable_expense`
            ADD COLUMN `source_no` VARCHAR(64) DEFAULT NULL COMMENT '来源单号'
                AFTER `source_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_expense'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_expense'
          AND INDEX_NAME = 'uk_tenant_source'
    ) THEN
        ALTER TABLE `erp_payable_expense`
            ADD UNIQUE KEY `uk_tenant_source`
                (`tenant_id`, `source_type`, `source_id`, `deleted`);
    END IF;
END //
DELIMITER ;

CALL add_erp_sale_cart_freight_finance_v137();
DROP PROCEDURE IF EXISTS add_erp_sale_cart_freight_finance_v137;
