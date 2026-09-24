-- ERP 其他应收/其他应付转收付款主单来源字段（v240）
-- MySQL 5.7 / 8.0 兼容，可重复执行。
-- 仅新增来源追踪字段与幂等索引，不删除、不覆盖历史业务数据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_finance_misc_transfer_source_v240;

DELIMITER //
CREATE PROCEDURE add_erp_finance_misc_transfer_source_v240()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_finance_receipt'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_finance_receipt'
          AND COLUMN_NAME = 'source_receivable_misc_id'
    ) THEN
        ALTER TABLE `erp_finance_receipt`
            ADD COLUMN `source_receivable_misc_id` BIGINT DEFAULT NULL COMMENT '来源其他应收单 ID'
                AFTER `account_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_finance_receipt'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_finance_receipt'
          AND COLUMN_NAME = 'source_receivable_misc_no'
    ) THEN
        ALTER TABLE `erp_finance_receipt`
            ADD COLUMN `source_receivable_misc_no` VARCHAR(64) DEFAULT NULL COMMENT '来源其他应收单号'
                AFTER `source_receivable_misc_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_finance_receipt'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_finance_receipt'
          AND INDEX_NAME = 'idx_finance_receipt_source_receivable_misc'
    ) THEN
        ALTER TABLE `erp_finance_receipt`
            ADD KEY `idx_finance_receipt_source_receivable_misc`
                (`tenant_id`, `source_receivable_misc_id`, `deleted`);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_finance_payment'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_finance_payment'
          AND COLUMN_NAME = 'source_payable_misc_id'
    ) THEN
        ALTER TABLE `erp_finance_payment`
            ADD COLUMN `source_payable_misc_id` BIGINT DEFAULT NULL COMMENT '来源其他应付单 ID'
                AFTER `account_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_finance_payment'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_finance_payment'
          AND COLUMN_NAME = 'source_payable_misc_no'
    ) THEN
        ALTER TABLE `erp_finance_payment`
            ADD COLUMN `source_payable_misc_no` VARCHAR(64) DEFAULT NULL COMMENT '来源其他应付单号'
                AFTER `source_payable_misc_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_finance_payment'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_finance_payment'
          AND INDEX_NAME = 'idx_finance_payment_source_payable_misc'
    ) THEN
        ALTER TABLE `erp_finance_payment`
            ADD KEY `idx_finance_payment_source_payable_misc`
                (`tenant_id`, `source_payable_misc_id`, `deleted`);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND INDEX_NAME = 'uk_receivable_misc_source_doc'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD UNIQUE KEY `uk_receivable_misc_source_doc`
                (`tenant_id`, `source_type`, `source_id`, `deleted`);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND INDEX_NAME = 'uk_payable_misc_source_doc'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD UNIQUE KEY `uk_payable_misc_source_doc`
                (`tenant_id`, `source_type`, `source_id`, `deleted`);
    END IF;
END //
DELIMITER ;

CALL add_erp_finance_misc_transfer_source_v240();
DROP PROCEDURE IF EXISTS add_erp_finance_misc_transfer_source_v240;
