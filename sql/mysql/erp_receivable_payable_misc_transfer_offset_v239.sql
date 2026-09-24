-- ERP 其他应收/其他应付转收付款审批后自动生成负数冲减单（v239）
-- MySQL 5.7 / 8.0 兼容，可重复执行。
-- 仅为 erp_receivable_misc / erp_payable_misc 补充来源追踪字段与幂等索引，不删除、不覆盖历史业务数据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_misc_transfer_offset_v239;

DELIMITER //
CREATE PROCEDURE add_erp_misc_transfer_offset_v239()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND COLUMN_NAME = 'source_type'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD COLUMN `source_type` VARCHAR(64) DEFAULT NULL COMMENT '来源类型'
                AFTER `file_url`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND COLUMN_NAME = 'source_id'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD COLUMN `source_id` BIGINT DEFAULT NULL COMMENT '来源收款单 ID'
                AFTER `source_type`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND COLUMN_NAME = 'source_no'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD COLUMN `source_no` VARCHAR(64) DEFAULT NULL COMMENT '来源收款单号'
                AFTER `source_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND COLUMN_NAME = 'source_item_id'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD COLUMN `source_item_id` BIGINT DEFAULT NULL COMMENT '来源收款核销明细 ID'
                AFTER `source_no`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND COLUMN_NAME = 'source_misc_id'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD COLUMN `source_misc_id` BIGINT DEFAULT NULL COMMENT '被冲减其他应收单 ID'
                AFTER `source_item_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND COLUMN_NAME = 'source_misc_no'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD COLUMN `source_misc_no` VARCHAR(64) DEFAULT NULL COMMENT '被冲减其他应收单号'
                AFTER `source_misc_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND INDEX_NAME = 'uk_receivable_misc_source_item'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD UNIQUE KEY `uk_receivable_misc_source_item`
                (`tenant_id`, `source_type`, `source_item_id`, `deleted`);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_misc'
          AND INDEX_NAME = 'idx_receivable_misc_source_misc'
    ) THEN
        ALTER TABLE `erp_receivable_misc`
            ADD KEY `idx_receivable_misc_source_misc`
                (`tenant_id`, `source_type`, `source_misc_id`, `status`);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND COLUMN_NAME = 'source_type'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD COLUMN `source_type` VARCHAR(64) DEFAULT NULL COMMENT '来源类型'
                AFTER `file_url`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND COLUMN_NAME = 'source_id'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD COLUMN `source_id` BIGINT DEFAULT NULL COMMENT '来源付款单 ID'
                AFTER `source_type`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND COLUMN_NAME = 'source_no'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD COLUMN `source_no` VARCHAR(64) DEFAULT NULL COMMENT '来源付款单号'
                AFTER `source_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND COLUMN_NAME = 'source_item_id'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD COLUMN `source_item_id` BIGINT DEFAULT NULL COMMENT '来源付款核销明细 ID'
                AFTER `source_no`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND COLUMN_NAME = 'source_misc_id'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD COLUMN `source_misc_id` BIGINT DEFAULT NULL COMMENT '被冲减其他应付单 ID'
                AFTER `source_item_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND COLUMN_NAME = 'source_misc_no'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD COLUMN `source_misc_no` VARCHAR(64) DEFAULT NULL COMMENT '被冲减其他应付单号'
                AFTER `source_misc_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND INDEX_NAME = 'uk_payable_misc_source_item'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD UNIQUE KEY `uk_payable_misc_source_item`
                (`tenant_id`, `source_type`, `source_item_id`, `deleted`);
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_misc'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_misc'
          AND INDEX_NAME = 'idx_payable_misc_source_misc'
    ) THEN
        ALTER TABLE `erp_payable_misc`
            ADD KEY `idx_payable_misc_source_misc`
                (`tenant_id`, `source_type`, `source_misc_id`, `status`);
    END IF;
END //
DELIMITER ;

CALL add_erp_misc_transfer_offset_v239();
DROP PROCEDURE IF EXISTS add_erp_misc_transfer_offset_v239;
