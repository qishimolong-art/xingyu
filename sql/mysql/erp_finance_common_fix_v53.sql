-- ERP finance common fixes v53
-- 1. Add missing dept_id indexes for finance tables that already have dept_id.
-- 2. Add dept_id and idx_dept_id for receivable/payable writeoff tables, then backfill from operator/creator user dept.
-- 3. Add finance field-permission definitions for dept/audit/list columns.

DROP PROCEDURE IF EXISTS add_erp_finance_dept_index_v53;
DELIMITER //
CREATE PROCEDURE add_erp_finance_dept_index_v53(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND INDEX_NAME = 'idx_dept_id'
    ) THEN
        SET @addIndexSql = CONCAT('CREATE INDEX `idx_dept_id` ON `', tableName, '` (`dept_id`)');
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_erp_finance_dept_index_v53('erp_receivable_other');
CALL add_erp_finance_dept_index_v53('erp_receivable_other_income');
CALL add_erp_finance_dept_index_v53('erp_receivable_other_income_item');
CALL add_erp_finance_dept_index_v53('erp_payable_other');
CALL add_erp_finance_dept_index_v53('erp_payable_expense');
CALL add_erp_finance_dept_index_v53('erp_payable_expense_item');

DROP PROCEDURE IF EXISTS add_erp_finance_dept_index_v53;

DROP PROCEDURE IF EXISTS add_erp_writeoff_dept_v53;
DELIMITER //
CREATE PROCEDURE add_erp_writeoff_dept_v53(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) THEN
        SET @addColumnSql = CONCAT(
            'ALTER TABLE `', tableName, '` ',
            'ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''所属部门'' AFTER `operator_user_id`'
        );
        PREPARE stmt FROM @addColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND INDEX_NAME = 'idx_dept_id'
    ) THEN
        SET @addIndexSql = CONCAT('CREATE INDEX `idx_dept_id` ON `', tableName, '` (`dept_id`)');
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_erp_writeoff_dept_v53('erp_receivable_writeoff');
CALL add_erp_writeoff_dept_v53('erp_payable_writeoff');

DROP PROCEDURE IF EXISTS add_erp_writeoff_dept_v53;

DROP PROCEDURE IF EXISTS backfill_erp_writeoff_dept_v53;
DELIMITER //
CREATE PROCEDURE backfill_erp_writeoff_dept_v53(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_users'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'operator_user_id'
    ) THEN
        SET @backfillOperatorSql = CONCAT(
            'UPDATE `', tableName, '` t ',
            'JOIN `system_users` u ON t.`operator_user_id` = u.`id` ',
            'SET t.`dept_id` = u.`dept_id` ',
            'WHERE t.`dept_id` IS NULL AND u.`dept_id` IS NOT NULL'
        );
        PREPARE stmt FROM @backfillOperatorSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_users'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'creator'
    ) THEN
        SET @backfillCreatorSql = CONCAT(
            'UPDATE `', tableName, '` t ',
            'JOIN `system_users` u ON t.`creator` REGEXP ''^[0-9]+$'' AND CAST(t.`creator` AS UNSIGNED) = u.`id` ',
            'SET t.`dept_id` = u.`dept_id` ',
            'WHERE t.`dept_id` IS NULL AND u.`dept_id` IS NOT NULL'
        );
        PREPARE stmt FROM @backfillCreatorSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL backfill_erp_writeoff_dept_v53('erp_receivable_writeoff');
CALL backfill_erp_writeoff_dept_v53('erp_payable_writeoff');

DROP PROCEDURE IF EXISTS backfill_erp_writeoff_dept_v53;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_account', 'deptId', '所属部门', 'base_info', 15, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'creatorName', '创建人', 'audit_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'createTime', '创建时间', 'audit_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'updaterName', '修改人', 'audit_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'updateTime', '修改时间', 'audit_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'col_deptName', '列表-所属部门', 'list_col', 115, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'col_deptId', '列表-所属部门', 'list_col', 116, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'col_creatorName', '列表-创建人', 'list_col', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'col_createTime', '列表-创建时间', 'list_col', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'col_updaterName', '列表-修改人', 'list_col', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_account', 'col_updateTime', '列表-修改时间', 'list_col', 930, '1', NOW(), '1', NOW(), b'0', 1),

('erp_finance_receipt', 'deptId', '所属部门', 'main_form', 45, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'creatorName', '创建人', 'audit_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'createTime', '创建时间', 'audit_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'updaterName', '修改人', 'audit_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'updateTime', '修改时间', 'audit_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'col_deptName', '列表-所属部门', 'list_col', 115, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'col_deptId', '列表-所属部门', 'list_col', 116, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'col_creatorName', '列表-创建人', 'list_col', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'col_createTime', '列表-创建时间', 'list_col', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'col_updaterName', '列表-修改人', 'list_col', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receipt', 'col_updateTime', '列表-修改时间', 'list_col', 930, '1', NOW(), '1', NOW(), b'0', 1),

('erp_finance_payment', 'deptId', '所属部门', 'main_form', 45, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'creatorName', '创建人', 'audit_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'createTime', '创建时间', 'audit_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'updaterName', '修改人', 'audit_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'updateTime', '修改时间', 'audit_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'col_deptName', '列表-所属部门', 'list_col', 115, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'col_deptId', '列表-所属部门', 'list_col', 116, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'col_creatorName', '列表-创建人', 'list_col', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'col_createTime', '列表-创建时间', 'list_col', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'col_updaterName', '列表-修改人', 'list_col', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payment', 'col_updateTime', '列表-修改时间', 'list_col', 930, '1', NOW(), '1', NOW(), b'0', 1),

('erp_finance_transfer', 'deptId', '所属部门', 'main_form', 75, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'creatorName', '创建人', 'audit_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'createTime', '创建时间', 'audit_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'updaterName', '修改人', 'audit_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'updateTime', '修改时间', 'audit_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'col_deptName', '列表-所属部门', 'list_col', 115, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'col_deptId', '列表-所属部门', 'list_col', 116, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'col_creatorName', '列表-创建人', 'list_col', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'col_createTime', '列表-创建时间', 'list_col', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'col_updaterName', '列表-修改人', 'list_col', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_transfer', 'col_updateTime', '列表-修改时间', 'list_col', 930, '1', NOW(), '1', NOW(), b'0', 1),

('erp_finance_receivable_other', 'deptId', '部门', 'main_form', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'creatorName', '创建人', 'audit_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'createTime', '创建时间', 'audit_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'updaterName', '修改人', 'audit_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'updateTime', '修改时间', 'audit_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'col_deptName', '列表-所属部门', 'list_col', 115, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'col_deptId', '列表-所属部门', 'list_col', 116, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'col_creatorName', '列表-创建人', 'list_col', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'col_createTime', '列表-创建时间', 'list_col', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'col_updaterName', '列表-修改人', 'list_col', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other', 'col_updateTime', '列表-修改时间', 'list_col', 930, '1', NOW(), '1', NOW(), b'0', 1),

('erp_finance_receivable_other_income', 'deptId', '部门', 'main_form', 80, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'creatorName', '创建人', 'audit_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'createTime', '创建时间', 'audit_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'updaterName', '修改人', 'audit_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'updateTime', '修改时间', 'audit_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'col_deptName', '列表-所属部门', 'list_col', 115, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'col_deptId', '列表-所属部门', 'list_col', 116, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'col_creatorName', '列表-创建人', 'list_col', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'col_createTime', '列表-创建时间', 'list_col', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'col_updaterName', '列表-修改人', 'list_col', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_receivable_other_income', 'col_updateTime', '列表-修改时间', 'list_col', 930, '1', NOW(), '1', NOW(), b'0', 1),

('erp_finance_payable_other', 'deptId', '部门', 'main_form', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'creatorName', '创建人', 'audit_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'createTime', '创建时间', 'audit_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'updaterName', '修改人', 'audit_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'updateTime', '修改时间', 'audit_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'col_deptName', '列表-所属部门', 'list_col', 115, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'col_deptId', '列表-所属部门', 'list_col', 116, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'col_creatorName', '列表-创建人', 'list_col', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'col_createTime', '列表-创建时间', 'list_col', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'col_updaterName', '列表-修改人', 'list_col', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_other', 'col_updateTime', '列表-修改时间', 'list_col', 930, '1', NOW(), '1', NOW(), b'0', 1),

('erp_finance_payable_expense', 'deptId', '申请部门', 'main_form', 80, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'creatorName', '创建人', 'audit_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'createTime', '创建时间', 'audit_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'updaterName', '修改人', 'audit_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'updateTime', '修改时间', 'audit_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'col_deptName', '列表-所属部门', 'list_col', 115, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'col_deptId', '列表-所属部门', 'list_col', 116, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'col_creatorName', '列表-创建人', 'list_col', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'col_createTime', '列表-创建时间', 'list_col', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'col_updaterName', '列表-修改人', 'list_col', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_finance_payable_expense', 'col_updateTime', '列表-修改时间', 'list_col', 930, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
