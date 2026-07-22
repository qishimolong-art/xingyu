-- ERP stock check batch number patch (v88)
-- Scope:
--   1. Add optional batch_no to erp_stock_check_item.
--   2. Add optional batch_no to erp_stock_record so approved stock check records keep batch traceability.
--   3. Register stock check item batch number field for ERP field permission.
-- Safety:
--   - Idempotent.
--   - Does not delete or overwrite role/menu/permission data.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_stock_check_batch_no_v88;

DELIMITER //
CREATE PROCEDURE add_erp_stock_check_batch_no_v88()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_check_item'
          AND COLUMN_NAME = 'batch_no'
    ) THEN
        ALTER TABLE `erp_stock_check_item`
            ADD COLUMN `batch_no` VARCHAR(128) DEFAULT NULL COMMENT '批次号'
            AFTER `product_unit_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_record'
          AND COLUMN_NAME = 'batch_no'
    ) THEN
        ALTER TABLE `erp_stock_record`
            ADD COLUMN `batch_no` VARCHAR(128) DEFAULT NULL COMMENT '批次号'
            AFTER `dept_id`;
    END IF;
END //
DELIMITER ;

CALL add_erp_stock_check_batch_no_v88();
DROP PROCEDURE IF EXISTS add_erp_stock_check_batch_no_v88;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_stock_check', 'item_batchNo', '批次号', 'detail_item', 225, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_record', 'col_batchNo', '列表-批次号', 'list_col', 285, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
