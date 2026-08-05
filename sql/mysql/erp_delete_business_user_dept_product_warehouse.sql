-- ERP data cleanup script (MySQL)
-- Purpose:
--   Clear business data and also clear department, product and warehouse data.
--   Keep the super administrator account (system_users.id = 1), delete other users,
--   and reset AUTO_INCREMENT of system_users to 2 and system_dept to 1 after cleanup.
--
-- Usage:
--   1. Backup the database first.
--   2. Execute once with @confirm_delete_all_main_data = 'NO' to review preview output.
--   3. Change @confirm_delete_all_main_data from 'NO' to 'YES'.
--   4. Execute:
--        mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < erp_delete_business_user_dept_product_warehouse.sql
--
-- Notes:
--   - This script operates on the current database and all tenants.
--   - It uses TRUNCATE TABLE, so data cannot be recovered without a backup.
--   - Menus, roles, posts, dicts, tenants, field definitions and ERP config tables are not truncated.
--   - Because products and warehouses are cleared, erp_stock is also truncated to avoid stale product/warehouse references.
--   - The kept super administrator account is system_users.id = 1.

SET @confirm_delete_all_main_data = 'YES';
SET @keep_super_admin_user_id = 1;

DROP PROCEDURE IF EXISTS clean_erp_business_user_dept_product_warehouse;

DELIMITER $$

CREATE PROCEDURE clean_erp_business_user_dept_product_warehouse()
clean_block: BEGIN
    DECLARE v_table_name VARCHAR(64);
    DECLARE v_statement_sql VARCHAR(1000);
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_reset_count INT DEFAULT 0;
    DECLARE v_statement_count INT DEFAULT 0;
    DECLARE v_keep_user_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        IF @old_foreign_key_checks IS NOT NULL THEN
            SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;
        END IF;
        DROP TEMPORARY TABLE IF EXISTS tmp_main_data_cleanup_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_main_data_cleanup_statements;
        RESIGNAL;
    END;

    SET @old_foreign_key_checks = @@FOREIGN_KEY_CHECKS;

    DROP TEMPORARY TABLE IF EXISTS tmp_main_data_cleanup_targets;
    DROP TEMPORARY TABLE IF EXISTS tmp_main_data_cleanup_statements;

    CREATE TEMPORARY TABLE tmp_main_data_cleanup_targets (
        table_name VARCHAR(64) NOT NULL PRIMARY KEY,
        category VARCHAR(32) NOT NULL,
        sort_no INT NOT NULL,
        reset_auto_increment TINYINT(1) NOT NULL DEFAULT 0
    ) ENGINE = MEMORY;

    CREATE TEMPORARY TABLE tmp_main_data_cleanup_statements (
        sort_no INT NOT NULL PRIMARY KEY,
        object_name VARCHAR(64) NOT NULL,
        action_desc VARCHAR(255) NOT NULL,
        statement_sql VARCHAR(1000) NOT NULL
    ) ENGINE = MEMORY;

    INSERT INTO tmp_main_data_cleanup_targets (table_name, category, sort_no, reset_auto_increment) VALUES
        -- Purchase documents
        ('erp_purchase_order_items', 'purchase', 1010, 0),
        ('erp_purchase_order', 'purchase', 1020, 0),
        ('erp_purchase_in_items', 'purchase', 1030, 0),
        ('erp_purchase_in', 'purchase', 1040, 0),
        ('erp_purchase_return_items', 'purchase', 1050, 0),
        ('erp_purchase_return', 'purchase', 1060, 0),
        ('erp_purchase_invoice_item', 'purchase', 1070, 0),
        ('erp_purchase_invoice', 'purchase', 1080, 0),
        ('erp_purchase_price_adjust_item', 'purchase', 1090, 0),
        ('erp_purchase_price_adjust', 'purchase', 1100, 0),
        ('erp_purchase_suggestion_item', 'purchase', 1110, 0),
        ('erp_purchase_suggestion', 'purchase', 1120, 0),

        -- Sale documents
        ('erp_sale_quote_items', 'sale', 2010, 0),
        ('erp_sale_quote', 'sale', 2020, 0),
        ('erp_sale_cart_items', 'sale', 2030, 0),
        ('erp_sale_cart', 'sale', 2040, 0),
        ('erp_sale_order_items', 'sale', 2050, 0),
        ('erp_sale_order', 'sale', 2060, 0),
        ('erp_sale_out_items', 'sale', 2070, 0),
        ('erp_sale_out', 'sale', 2080, 0),
        ('erp_sale_return_items', 'sale', 2090, 0),
        ('erp_sale_return', 'sale', 2100, 0),
        ('erp_sale_price_adjust_item', 'sale', 2110, 0),
        ('erp_sale_price_adjust', 'sale', 2120, 0),
        ('erp_sale_convert_record', 'sale', 2130, 0),
        ('erp_chain_order_item', 'sale', 2140, 0),
        ('erp_chain_order', 'sale', 2150, 0),

        -- Stock documents, locks, ledgers and current stock
        ('erp_stock_record', 'stock', 3010, 0),
        ('erp_stock_lock', 'stock', 3020, 0),
        ('erp_stock_in_item', 'stock', 3030, 0),
        ('erp_stock_in', 'stock', 3040, 0),
        ('erp_stock_out_item', 'stock', 3050, 0),
        ('erp_stock_out', 'stock', 3060, 0),
        ('erp_stock_move_item', 'stock', 3070, 0),
        ('erp_stock_move', 'stock', 3080, 0),
        ('erp_stock_check_item', 'stock', 3090, 0),
        ('erp_stock_check', 'stock', 3100, 0),
        ('erp_stock_in_bill_pickup_record', 'stock', 3110, 0),
        ('erp_stock_in_bill_item', 'stock', 3120, 0),
        ('erp_stock_in_bill', 'stock', 3130, 0),
        ('erp_stock_out_bill_pick_record', 'stock', 3140, 0),
        ('erp_stock_out_bill_item', 'stock', 3150, 0),
        ('erp_stock_out_bill', 'stock', 3160, 0),
        ('erp_warehouse_move_item', 'stock', 3170, 0),
        ('erp_warehouse_move', 'stock', 3180, 0),
        ('erp_stock', 'stock', 3190, 0),

        -- Finance business documents, write-offs and accounting vouchers
        ('erp_finance_payment_item', 'finance', 4010, 0),
        ('erp_finance_payment', 'finance', 4020, 0),
        ('erp_finance_receipt_item', 'finance', 4030, 0),
        ('erp_finance_receipt', 'finance', 4040, 0),
        ('erp_finance_transfer', 'finance', 4050, 0),
        ('erp_payable_expense_item', 'finance', 4060, 0),
        ('erp_payable_expense', 'finance', 4070, 0),
        ('erp_payable_other', 'finance', 4080, 0),
        ('erp_payable_writeoff', 'finance', 4090, 0),
        ('erp_receivable_other_item', 'finance', 4100, 0),
        ('erp_receivable_other', 'finance', 4110, 0),
        ('erp_receivable_other_income_item', 'finance', 4120, 0),
        ('erp_receivable_other_income', 'finance', 4130, 0),
        ('erp_receivable_writeoff', 'finance', 4140, 0),
        ('erp_other_payable_item', 'finance', 4150, 0),
        ('erp_other_payable', 'finance', 4160, 0),
        ('erp_other_receivable_item', 'finance', 4170, 0),
        ('erp_other_receivable', 'finance', 4180, 0),
        ('erp_pre_payment_item', 'finance', 4190, 0),
        ('erp_pre_payment', 'finance', 4200, 0),
        ('erp_pre_receipt_item', 'finance', 4210, 0),
        ('erp_pre_receipt', 'finance', 4220, 0),
        ('erp_pre_receivable_item', 'finance', 4230, 0),
        ('erp_pre_receivable', 'finance', 4240, 0),
        ('erp_voucher_attribution', 'finance', 4250, 0),
        ('erp_voucher_item', 'finance', 4260, 0),
        ('erp_voucher', 'finance', 4270, 0),
        ('erp_book_open_voucher_config', 'finance', 4280, 0),
        ('erp_book_open', 'finance', 4290, 0),

        -- Operation logs and derived business histories
        ('erp_import_export_record_detail', 'auxiliary', 5010, 0),
        ('erp_import_export_record', 'auxiliary', 5020, 0),
        ('erp_archive_merge_log', 'auxiliary', 5030, 0),
        ('erp_price_history', 'auxiliary', 5040, 0),

        -- Product archive data
        ('erp_product_price_system', 'product', 6010, 0),
        ('erp_product_dept', 'product', 6020, 0),
        ('erp_product_universal', 'product', 6030, 0),
        ('erp_vehicle_product_fit', 'product', 6040, 0),
        ('erp_product', 'product', 6050, 0),
        ('erp_product_category', 'product', 6060, 0),
        ('erp_product_unit', 'product', 6070, 0),
        ('erp_price_system', 'product', 6080, 0),

        -- Warehouse archive data
        ('erp_user_warehouse_permission', 'warehouse', 7010, 0),
        ('erp_warehouse_sale_dept_permission', 'warehouse', 7020, 0),
        ('erp_warehouse_branch', 'warehouse', 7030, 0),
        ('erp_warehouse', 'warehouse', 7040, 0),

        -- User and department data
        ('system_oauth2_access_token', 'user_dept', 8010, 0),
        ('system_oauth2_refresh_token', 'user_dept', 8020, 0),
        ('system_oauth2_code', 'user_dept', 8030, 0),
        ('system_oauth2_approve', 'user_dept', 8040, 0),
        ('system_social_user_bind', 'user_dept', 8050, 0),
        ('system_social_user', 'user_dept', 8060, 0),
        ('system_user_permission_deny', 'user_dept', 8070, 0),
        ('system_user_price_field', 'user_dept', 8080, 0),
        ('system_dept_price_field', 'user_dept', 8090, 0),
        ('system_user_dept', 'user_dept', 8120, 0),
        ('erp_supplier_dept', 'user_dept', 8130, 0),
        ('erp_customer_dept', 'user_dept', 8140, 0),
        ('system_login_log', 'user_dept', 8150, 0),
        ('system_operate_log', 'user_dept', 8160, 0),
        ('system_dept', 'user_dept', 8180, 1);

    INSERT INTO tmp_main_data_cleanup_statements (sort_no, object_name, action_desc, statement_sql) VALUES
        (9010, 'system_user_role', 'Delete role relations of users except the kept super administrator',
         CONCAT('DELETE FROM `system_user_role` WHERE `user_id` <> ', @keep_super_admin_user_id)),
        (9020, 'system_user_post', 'Delete post relations of users except the kept super administrator',
         CONCAT('DELETE FROM `system_user_post` WHERE `user_id` <> ', @keep_super_admin_user_id)),
        (9030, 'system_users', 'Delete users except the kept super administrator',
         CONCAT('DELETE FROM `system_users` WHERE `id` <> ', @keep_super_admin_user_id)),
        (9040, 'system_users', 'Clear the kept super administrator department because department data is cleared',
         CONCAT('UPDATE `system_users` SET `dept_id` = NULL WHERE `id` = ', @keep_super_admin_user_id)),
        (9050, 'system_users', 'Reset user AUTO_INCREMENT to 2',
         'ALTER TABLE `system_users` AUTO_INCREMENT = 2');

    DELETE target
    FROM tmp_main_data_cleanup_targets target
    LEFT JOIN information_schema.tables t
           ON t.table_schema = DATABASE()
          AND t.table_name = target.table_name
          AND t.table_type = 'BASE TABLE'
    WHERE t.table_name IS NULL;

    DELETE s
    FROM tmp_main_data_cleanup_statements s
    LEFT JOIN information_schema.tables t
           ON t.table_schema = DATABASE()
          AND t.table_name = s.object_name
          AND t.table_type = 'BASE TABLE'
    WHERE t.table_name IS NULL;

    SELECT COUNT(*) INTO v_target_count FROM tmp_main_data_cleanup_targets;
    SELECT COUNT(*) INTO v_reset_count
    FROM tmp_main_data_cleanup_targets
    WHERE reset_auto_increment = 1;
    SELECT COUNT(*) INTO v_statement_count FROM tmp_main_data_cleanup_statements;

    SELECT
        'Tables that will be truncated' AS message,
        v_target_count AS table_count,
        v_reset_count AS auto_increment_reset_table_count;

    SELECT category, table_name,
           CASE WHEN reset_auto_increment = 1 THEN 'AUTO_INCREMENT will be reset to 1' ELSE '' END AS reset_plan
    FROM tmp_main_data_cleanup_targets
    ORDER BY sort_no, table_name;

    SELECT
        'Special user cleanup' AS message,
        @keep_super_admin_user_id AS kept_super_admin_user_id,
        v_statement_count AS statement_count;

    SELECT object_name, action_desc
    FROM tmp_main_data_cleanup_statements
    ORDER BY sort_no;

    SELECT 'Preserved scopes' AS message, 'system_menu, system_role, system_post, system_dict, system_tenant, ERP config and field definition tables' AS scope_desc;

    IF @confirm_delete_all_main_data <> 'YES' THEN
        SELECT 'Cleanup blocked: set @confirm_delete_all_main_data = ''YES'' after backup and preview review.' AS message;
        DROP TEMPORARY TABLE IF EXISTS tmp_main_data_cleanup_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_main_data_cleanup_statements;
        LEAVE clean_block;
    END IF;

    SELECT COUNT(*)
    INTO v_keep_user_count
    FROM system_users
    WHERE id = @keep_super_admin_user_id;

    IF v_keep_user_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup aborted: kept super administrator system_users.id = 1 does not exist.';
    END IF;

    SET FOREIGN_KEY_CHECKS = 0;

    WHILE EXISTS (SELECT 1 FROM tmp_main_data_cleanup_targets) DO
        SELECT table_name
        INTO v_table_name
        FROM tmp_main_data_cleanup_targets
        ORDER BY sort_no, table_name
        LIMIT 1;

        SET @truncate_sql = CONCAT('TRUNCATE TABLE `', REPLACE(v_table_name, '`', '``'), '`');
        PREPARE truncate_stmt FROM @truncate_sql;
        EXECUTE truncate_stmt;
        DEALLOCATE PREPARE truncate_stmt;

        IF EXISTS (
            SELECT 1
            FROM tmp_main_data_cleanup_targets
            WHERE table_name = v_table_name
              AND reset_auto_increment = 1
        ) THEN
            SET @alter_sql = CONCAT('ALTER TABLE `', REPLACE(v_table_name, '`', '``'), '` AUTO_INCREMENT = 1');
            PREPARE alter_stmt FROM @alter_sql;
            EXECUTE alter_stmt;
            DEALLOCATE PREPARE alter_stmt;
        END IF;

        DELETE FROM tmp_main_data_cleanup_targets
        WHERE table_name = v_table_name;
    END WHILE;

    WHILE EXISTS (SELECT 1 FROM tmp_main_data_cleanup_statements) DO
        SELECT statement_sql
        INTO v_statement_sql
        FROM tmp_main_data_cleanup_statements
        ORDER BY sort_no
        LIMIT 1;

        SET @cleanup_sql = v_statement_sql;
        PREPARE cleanup_stmt FROM @cleanup_sql;
        EXECUTE cleanup_stmt;
        DEALLOCATE PREPARE cleanup_stmt;

        DELETE FROM tmp_main_data_cleanup_statements
        WHERE statement_sql = v_statement_sql;
    END WHILE;

    SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;

    DROP TEMPORARY TABLE IF EXISTS tmp_main_data_cleanup_targets;
    DROP TEMPORARY TABLE IF EXISTS tmp_main_data_cleanup_statements;

    SELECT 'ERP business, user, department, product and warehouse data cleanup completed. Super administrator user is kept.' AS message;
END clean_block$$

DELIMITER ;

CALL clean_erp_business_user_dept_product_warehouse();

DROP PROCEDURE IF EXISTS clean_erp_business_user_dept_product_warehouse;
