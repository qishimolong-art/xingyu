-- 应收、应付及应收冲应付审批时间字段与历史回填（v279）。
-- MySQL 5.7 / 8.0 兼容，可重复执行。
-- 仅回填已审批、未删除且 approve_time 为空的记录，不覆盖已有审批时间。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_erp_finance_account_approval_time_v279;
DELIMITER //
CREATE PROCEDURE migrate_erp_finance_account_approval_time_v279()
BEGIN
    DECLARE finance_payment_expected BIGINT DEFAULT 0;
    DECLARE payable_other_expected BIGINT DEFAULT 0;
    DECLARE receivable_misc_expected BIGINT DEFAULT 0;
    DECLARE payable_misc_expected BIGINT DEFAULT 0;
    DECLARE remaining_rows BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF DATABASE() IS NULL OR DATABASE() = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v279: no target database selected';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_payable_other')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_misc')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_payable_misc') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v279: required finance table is missing';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment' AND column_name = 'approve_time') THEN
        ALTER TABLE erp_finance_payment ADD COLUMN approve_time DATETIME NULL COMMENT '审批时间' AFTER payment_time;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'erp_payable_other' AND column_name = 'approve_time') THEN
        ALTER TABLE erp_payable_other ADD COLUMN approve_time DATETIME NULL COMMENT '审批时间' AFTER biz_time;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_misc' AND column_name = 'approve_time') THEN
        ALTER TABLE erp_receivable_misc ADD COLUMN approve_time DATETIME NULL COMMENT '审批时间' AFTER biz_time;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'erp_payable_misc' AND column_name = 'approve_time') THEN
        ALTER TABLE erp_payable_misc ADD COLUMN approve_time DATETIME NULL COMMENT '审批时间' AFTER biz_time;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment' AND index_name = 'idx_finance_payment_approve_time') THEN
        ALTER TABLE erp_finance_payment ADD INDEX idx_finance_payment_approve_time (tenant_id, supplier_id, status, approve_time, deleted);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'erp_payable_other' AND index_name = 'idx_payable_other_approve_time') THEN
        ALTER TABLE erp_payable_other ADD INDEX idx_payable_other_approve_time (tenant_id, supplier_id, status, approve_time, deleted);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_misc' AND index_name = 'idx_receivable_misc_approve_time') THEN
        ALTER TABLE erp_receivable_misc ADD INDEX idx_receivable_misc_approve_time (tenant_id, customer_id, status, approve_time, deleted);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'erp_payable_misc' AND index_name = 'idx_payable_misc_approve_time') THEN
        ALTER TABLE erp_payable_misc ADD INDEX idx_payable_misc_approve_time (tenant_id, supplier_id, status, approve_time, deleted);
    END IF;

    START TRANSACTION;

    SELECT COUNT(*) INTO finance_payment_expected FROM erp_finance_payment
     WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;
    SELECT COUNT(*) INTO payable_other_expected FROM erp_payable_other
     WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;
    SELECT COUNT(*) INTO receivable_misc_expected FROM erp_receivable_misc
     WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;
    SELECT COUNT(*) INTO payable_misc_expected FROM erp_payable_misc
     WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;

    UPDATE erp_finance_payment SET approve_time = COALESCE(update_time, create_time)
     WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;
    UPDATE erp_payable_other SET approve_time = COALESCE(update_time, create_time)
     WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;
    UPDATE erp_receivable_misc SET approve_time = COALESCE(update_time, create_time)
     WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;
    UPDATE erp_payable_misc SET approve_time = COALESCE(update_time, create_time)
     WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;

    SELECT (SELECT COUNT(*) FROM erp_finance_payment WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL)
         + (SELECT COUNT(*) FROM erp_payable_other WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL)
         + (SELECT COUNT(*) FROM erp_receivable_misc WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL)
         + (SELECT COUNT(*) FROM erp_payable_misc WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL)
      INTO remaining_rows;

    IF remaining_rows <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v279: approval time backfill post-check failed';
    END IF;

    COMMIT;

    SELECT finance_payment_expected AS finance_payment_backfilled,
           payable_other_expected AS payable_other_backfilled,
           receivable_misc_expected AS receivable_misc_backfilled,
           payable_misc_expected AS payable_misc_backfilled,
           remaining_rows AS remaining_approved_null_rows;
END //
DELIMITER ;

CALL migrate_erp_finance_account_approval_time_v279();
DROP PROCEDURE IF EXISTS migrate_erp_finance_account_approval_time_v279;

SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_finance_payment', 'erp_payable_other', 'erp_receivable_misc', 'erp_payable_misc')
  AND column_name = 'approve_time'
ORDER BY table_name;

SELECT table_name, index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN ('idx_finance_payment_approve_time', 'idx_payable_other_approve_time',
                     'idx_receivable_misc_approve_time', 'idx_payable_misc_approve_time')
GROUP BY table_name, index_name
ORDER BY table_name;

