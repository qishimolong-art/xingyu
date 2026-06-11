-- ERP business data cleanup script (MySQL)
-- Purpose:
--   Delete all ERP module business data in the current database while keeping
--   ERP/system configuration, menus, permissions, dictionaries, and seed data.
--
-- Usage:
--   1. Backup the database first.
--   2. Review the preview result printed by this script.
--   3. Change @confirm_delete_erp_business_data from 'NO' to 'YES'.
--   4. Execute:
--        mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < erp_delete_all_business_data.sql
--
-- Notes:
--   - This script only targets BASE TABLEs whose names start with erp_.
--   - It never deletes system_menu, system_role, system_permission, system_dict,
--     tenant, user, login, or other non-ERP system tables.
--   - It uses TRUNCATE TABLE, so data cannot be recovered without a backup and
--     AUTO_INCREMENT values are reset.

SET @confirm_delete_erp_business_data = 'NO';

DROP PROCEDURE IF EXISTS clean_erp_business_data;

DELIMITER $$

CREATE PROCEDURE clean_erp_business_data()
clean_block: BEGIN
    DECLARE v_table_name VARCHAR(64);
    DECLARE v_target_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;
        DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_data_cleanup_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_data_cleanup_excludes;
        RESIGNAL;
    END;

    SET @old_foreign_key_checks = @@FOREIGN_KEY_CHECKS;

    DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_data_cleanup_excludes;
    CREATE TEMPORARY TABLE tmp_erp_business_data_cleanup_excludes (
        table_name VARCHAR(64) NOT NULL PRIMARY KEY
    ) ENGINE = MEMORY;

    -- ERP configuration/seed tables kept by default.
    -- If you also need to remove master/config data, delete the corresponding
    -- rows from this exclusion list before executing the script.
    INSERT INTO tmp_erp_business_data_cleanup_excludes (table_name) VALUES
        ('erp_accounting_subject'),
        ('erp_base_data'),
        ('erp_field_config'),
        ('erp_report_item_template'),
        ('erp_sale_config'),
        ('erp_search_field_config'),
        ('erp_shortcut_config'),
        ('erp_subject_auxiliary'),
        ('erp_voucher_word');

    DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_data_cleanup_targets;
    CREATE TEMPORARY TABLE tmp_erp_business_data_cleanup_targets (
        table_name VARCHAR(64) NOT NULL PRIMARY KEY
    ) ENGINE = MEMORY;

    INSERT INTO tmp_erp_business_data_cleanup_targets (table_name)
    SELECT t.table_name
    FROM information_schema.tables t
    LEFT JOIN tmp_erp_business_data_cleanup_excludes e ON e.table_name = t.table_name
    WHERE t.table_schema = DATABASE()
      AND t.table_type = 'BASE TABLE'
      AND t.table_name LIKE 'erp!_%' ESCAPE '!'
      AND e.table_name IS NULL
    ORDER BY t.table_name;

    SELECT COUNT(*) INTO v_target_count FROM tmp_erp_business_data_cleanup_targets;

    SELECT
        'ERP business tables that will be truncated' AS message,
        v_target_count AS table_count;

    SELECT table_name
    FROM tmp_erp_business_data_cleanup_targets
    ORDER BY table_name;

    SELECT
        'ERP configuration/seed tables kept' AS message,
        table_name
    FROM tmp_erp_business_data_cleanup_excludes
    ORDER BY table_name;

    IF @confirm_delete_erp_business_data <> 'YES' THEN
        SELECT 'Cleanup blocked: set @confirm_delete_erp_business_data = ''YES'' after backup and review.' AS message;
        DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_data_cleanup_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_data_cleanup_excludes;
        LEAVE clean_block;
    END IF;

    SET FOREIGN_KEY_CHECKS = 0;

    WHILE EXISTS (SELECT 1 FROM tmp_erp_business_data_cleanup_targets) DO
        SELECT table_name
        INTO v_table_name
        FROM tmp_erp_business_data_cleanup_targets
        ORDER BY table_name
        LIMIT 1;

        SET @truncate_sql = CONCAT('TRUNCATE TABLE `', REPLACE(v_table_name, '`', '``'), '`');
        PREPARE truncate_stmt FROM @truncate_sql;
        EXECUTE truncate_stmt;
        DEALLOCATE PREPARE truncate_stmt;

        DELETE FROM tmp_erp_business_data_cleanup_targets
        WHERE table_name = v_table_name;
    END WHILE;

    SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;

    DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_data_cleanup_targets;
    DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_data_cleanup_excludes;

    SELECT 'ERP business data cleanup completed.' AS message;
END clean_block$$

DELIMITER ;

CALL clean_erp_business_data();

DROP PROCEDURE IF EXISTS clean_erp_business_data;
