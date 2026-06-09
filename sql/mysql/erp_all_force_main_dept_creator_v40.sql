-- ERP all tables force to main department and super admin creator.
-- MySQL 5.7 / 8.0 compatible.
--
-- What this script does:
-- 1. Finds every BASE TABLE whose name starts with erp_ in the current database.
-- 2. Adds dept_id when missing.
-- 3. Adds creator when missing.
-- 4. Adds idx_dept_id when missing.
-- 5. Updates every row to dept_id = 1 and creator = '1'.
--
-- Before executing in production, back up the database first.

SET @erp_target_dept_id = 1;
SET @erp_target_creator = '1';

SELECT
    'Target department id' AS check_item,
    @erp_target_dept_id AS check_value;

SELECT
    'Target creator user' AS check_item,
    id,
    username,
    nickname,
    dept_id
FROM system_users
WHERE id = CAST(@erp_target_creator AS UNSIGNED);

DROP TEMPORARY TABLE IF EXISTS erp_force_main_dept_result;
CREATE TEMPORARY TABLE erp_force_main_dept_result (
    table_name VARCHAR(128) NOT NULL,
    total_rows BIGINT NOT NULL,
    changed_rows BIGINT NOT NULL
);

DROP PROCEDURE IF EXISTS force_erp_main_dept_creator;

DELIMITER //
CREATE PROCEDURE force_erp_main_dept_creator()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table_name VARCHAR(128);
    DECLARE v_safe_table_name VARCHAR(260);
    DECLARE v_literal_table_name VARCHAR(260);
    DECLARE v_changed_rows BIGINT DEFAULT 0;

    DECLARE tableCursor CURSOR FOR
        SELECT TABLE_NAME
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_TYPE = 'BASE TABLE'
          AND TABLE_NAME REGEXP '^erp_'
        ORDER BY TABLE_NAME;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN tableCursor;

    tableLoop: LOOP
        FETCH tableCursor INTO v_table_name;
        IF done = 1 THEN
            LEAVE tableLoop;
        END IF;

        SET v_safe_table_name = REPLACE(v_table_name, '`', '``');
        SET v_literal_table_name = REPLACE(v_table_name, '''', '''''');

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = v_table_name
              AND COLUMN_NAME = 'dept_id'
        ) THEN
            SET @addDeptSql = CONCAT(
                'ALTER TABLE `', v_safe_table_name,
                '` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''dept id'''
            );
            PREPARE stmt FROM @addDeptSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = v_table_name
              AND COLUMN_NAME = 'creator'
        ) THEN
            SET @addCreatorSql = CONCAT(
                'ALTER TABLE `', v_safe_table_name,
                '` ADD COLUMN `creator` VARCHAR(64) DEFAULT '''' COMMENT ''creator'''
            );
            PREPARE stmt FROM @addCreatorSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = v_table_name
              AND INDEX_NAME = 'idx_dept_id'
        ) THEN
            SET @addDeptIndexSql = CONCAT(
                'CREATE INDEX `idx_dept_id` ON `', v_safe_table_name, '` (`dept_id`)'
            );
            PREPARE stmt FROM @addDeptIndexSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        SET @updateSql = CONCAT(
            'UPDATE `', v_safe_table_name,
            '` SET `dept_id` = ', @erp_target_dept_id,
            ', `creator` = ''', REPLACE(@erp_target_creator, '''', ''''''), ''''
        );
        PREPARE stmt FROM @updateSql;
        EXECUTE stmt;
        SET v_changed_rows = ROW_COUNT();
        DEALLOCATE PREPARE stmt;

        SET @resultSql = CONCAT(
            'INSERT INTO erp_force_main_dept_result(table_name, total_rows, changed_rows) ',
            'SELECT ''', v_literal_table_name, ''', COUNT(*), ', v_changed_rows,
            ' FROM `', v_safe_table_name, '`'
        );
        PREPARE stmt FROM @resultSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;

    CLOSE tableCursor;
END //
DELIMITER ;

CALL force_erp_main_dept_creator();

DROP PROCEDURE IF EXISTS force_erp_main_dept_creator;

SELECT *
FROM erp_force_main_dept_result
ORDER BY table_name;

SELECT
    COUNT(*) AS erp_table_count,
    SUM(total_rows) AS erp_total_rows,
    SUM(changed_rows) AS erp_changed_rows
FROM erp_force_main_dept_result;
