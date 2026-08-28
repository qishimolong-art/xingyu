-- ERP customer mini-app member authorization logical-delete unique key fix (v200).
-- Safe to rerun. This script only adjusts erp_customer_member indexes and
-- backfills a unique marker for logically deleted authorization rows.
-- It does not delete or overwrite customer, member, menu, role, or permission data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_customer_member_column_if_missing_v200;
DELIMITER //
CREATE PROCEDURE add_erp_customer_member_column_if_missing_v200()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_customer_member'
          AND COLUMN_NAME = 'active_key'
    ) THEN
        ALTER TABLE `erp_customer_member`
            ADD COLUMN `active_key` bigint NOT NULL DEFAULT 0 COMMENT '唯一约束标记：未删除为0，删除后为记录ID' AFTER `deleted`;
    END IF;
END //
DELIMITER ;

CALL add_erp_customer_member_column_if_missing_v200();
DROP PROCEDURE IF EXISTS add_erp_customer_member_column_if_missing_v200;

UPDATE `erp_customer_member`
   SET `active_key` = `id`,
       `updater` = IFNULL(`updater`, '1'),
       `update_time` = NOW()
 WHERE `deleted` = b'1'
   AND `active_key` = 0;

UPDATE `erp_customer_member`
   SET `active_key` = 0
 WHERE `deleted` = b'0'
   AND `active_key` <> 0;

DROP PROCEDURE IF EXISTS replace_erp_customer_member_unique_key_v200;
DELIMITER //
CREATE PROCEDURE replace_erp_customer_member_unique_key_v200()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_customer_member'
          AND INDEX_NAME = 'uk_customer_member'
          AND COLUMN_NAME = 'deleted'
    ) THEN
        ALTER TABLE `erp_customer_member` DROP INDEX `uk_customer_member`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_customer_member'
          AND INDEX_NAME = 'uk_customer_member'
          AND COLUMN_NAME = 'active_key'
    ) THEN
        ALTER TABLE `erp_customer_member`
            ADD UNIQUE KEY `uk_customer_member` (`customer_id`, `member_user_id`, `tenant_id`, `active_key`) USING BTREE;
    END IF;
END //
DELIMITER ;

CALL replace_erp_customer_member_unique_key_v200();
DROP PROCEDURE IF EXISTS replace_erp_customer_member_unique_key_v200;
