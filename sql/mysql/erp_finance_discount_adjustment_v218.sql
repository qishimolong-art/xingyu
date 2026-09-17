-- ERP 收/付款优惠自动生成调账单（v218）
-- MySQL 5.7 / 8.0 兼容，可重复执行。
-- 仅补齐来源追踪字段与幂等索引，不修改历史业务数据、不补历史调账单。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_finance_discount_adjustment_v218;

DELIMITER //
CREATE PROCEDURE add_erp_finance_discount_adjustment_v218()
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
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_other'
          AND COLUMN_NAME = 'source_no'
    ) THEN
        ALTER TABLE `erp_receivable_other`
            ADD COLUMN `source_no` VARCHAR(64) DEFAULT NULL COMMENT '来源单号'
                AFTER `source_id`;
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
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_other'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_other'
          AND COLUMN_NAME = 'source_id'
    ) THEN
        ALTER TABLE `erp_payable_other`
            ADD COLUMN `source_id` BIGINT DEFAULT NULL COMMENT '来源单据编号'
                AFTER `source_type`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_other'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_other'
          AND COLUMN_NAME = 'source_no'
    ) THEN
        ALTER TABLE `erp_payable_other`
            ADD COLUMN `source_no` VARCHAR(64) DEFAULT NULL COMMENT '来源单号'
                AFTER `source_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_other'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_other'
          AND INDEX_NAME = 'uk_tenant_source'
    ) THEN
        ALTER TABLE `erp_payable_other`
            ADD UNIQUE KEY `uk_tenant_source`
                (`tenant_id`, `source_type`, `source_id`, `deleted`);
    END IF;
END //
DELIMITER ;

CALL add_erp_finance_discount_adjustment_v218();
DROP PROCEDURE IF EXISTS add_erp_finance_discount_adjustment_v218;
