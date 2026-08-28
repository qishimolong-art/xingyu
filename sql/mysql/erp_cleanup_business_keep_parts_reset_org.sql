-- ERP business/personnel cleanup script (MySQL)
-- Purpose:
--   1. Clear ERP business documents, customer/supplier data, finance business data,
--      import/export logs, print records and record-level permissions.
--   2. Keep parts information: erp_product, erp_product_category, erp_product_unit,
--      erp_product_price_system, erp_product_universal, erp_price_system and vehicle fit data.
--   3. Keep warehouse master data because erp_stock rows reference warehouses.
--   4. Keep erp_stock rows, but reset all stock balance fields to 0.
--   5. Reset organization/personnel/roles to the minimum required records:
--      one department, two kept users and the first three required roles.
--   6. Normalize department/user/role IDs and AUTO_INCREMENT values so new records
--      continue without gaps after the kept records.
--
-- Usage:
--   1. Back up the database first.
--   2. Execute once with @confirm_cleanup = 'NO' to review the preview output.
--   3. If the preview is correct, change @confirm_cleanup to 'YES' and execute again.
--
-- Defaults for the current seed data:
--   - Keep one department and renumber it to 1. If @keep_dept_old_id is NULL,
--     the script automatically keeps the current department of user 1/2 first,
--     then falls back to the smallest existing department id.
--   - Keep system_users.id in (1, 2).
--   - Keep system_role.id in (1, 2, 3).
--
-- Important:
--   - This script operates on the current database and all tenants.
--   - TRUNCATE/DELETE/UPDATE operations are irreversible without a backup.
--   - If the customer's required kept department/user/role differ, edit the
--     variables below before execution.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @confirm_cleanup = 'YES';
SET @keep_dept_old_id = NULL;
SET @keep_dept_new_id = 1;
SET @keep_dept_name = '总公司';
SET @keep_user_id_1 = 1;
SET @keep_user_id_2 = 2;
SET @keep_role_id_1 = 1;
SET @keep_role_id_2 = 2;
SET @keep_role_id_3 = 3;
SET @keep_tenant_id = 1;

DROP PROCEDURE IF EXISTS clean_erp_business_keep_parts_reset_org;

DELIMITER $$

CREATE PROCEDURE clean_erp_business_keep_parts_reset_org()
clean_block: BEGIN
    DECLARE v_table_name VARCHAR(64);
    DECLARE v_column_name VARCHAR(64);
    DECLARE v_statement_sql VARCHAR(4000);
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_statement_count INT DEFAULT 0;
    DECLARE v_stock_table_exists INT DEFAULT 0;
    DECLARE v_stock_balance_columns INT DEFAULT 0;
    DECLARE v_keep_dept_count INT DEFAULT 0;
    DECLARE v_keep_user_count INT DEFAULT 0;
    DECLARE v_keep_role_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        IF @old_foreign_key_checks IS NOT NULL THEN
            SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;
        END IF;
        DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_statements;
        DROP TEMPORARY TABLE IF EXISTS tmp_dept_column_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_auto_increment_targets;
        RESIGNAL;
    END;

    SET @old_foreign_key_checks = @@FOREIGN_KEY_CHECKS;

    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_targets;
    CREATE TEMPORARY TABLE tmp_cleanup_targets (
        table_name VARCHAR(64) NOT NULL PRIMARY KEY,
        category VARCHAR(32) NOT NULL,
        sort_no INT NOT NULL
    ) ENGINE = MEMORY;

    INSERT INTO tmp_cleanup_targets (table_name, category, sort_no) VALUES
        -- Purchase documents and supplier archive data
        ('erp_purchase_order_items', 'purchase', 1010),
        ('erp_purchase_order', 'purchase', 1020),
        ('erp_purchase_in_items', 'purchase', 1030),
        ('erp_purchase_in', 'purchase', 1040),
        ('erp_purchase_return_items', 'purchase', 1050),
        ('erp_purchase_return', 'purchase', 1060),
        ('erp_purchase_invoice_ocr_item', 'purchase', 1070),
        ('erp_purchase_invoice_ocr_batch', 'purchase', 1080),
        ('erp_purchase_invoice_item', 'purchase', 1090),
        ('erp_purchase_invoice', 'purchase', 1100),
        ('erp_purchase_price_adjust_item', 'purchase', 1110),
        ('erp_purchase_price_adjust', 'purchase', 1120),
        ('erp_purchase_suggestion_item', 'purchase', 1130),
        ('erp_purchase_suggestion', 'purchase', 1140),
        ('erp_supplier_account', 'purchase', 1210),
        ('erp_supplier_bill', 'purchase', 1220),
        ('erp_supplier_business_info', 'purchase', 1230),
        ('erp_supplier_contact', 'purchase', 1240),
        ('erp_supplier_contract', 'purchase', 1250),
        ('erp_supplier_dept', 'purchase', 1260),
        ('erp_supplier_extend', 'purchase', 1270),
        ('erp_supplier_extend_info', 'purchase', 1280),
        ('erp_supplier_image', 'purchase', 1290),
        ('erp_supplier_task', 'purchase', 1300),
        ('erp_supplier', 'purchase', 1310),

        -- Sale documents and customer archive data
        ('erp_sale_quote_items', 'sale', 2010),
        ('erp_sale_quote', 'sale', 2020),
        ('erp_sale_cart_items', 'sale', 2030),
        ('erp_sale_cart', 'sale', 2040),
        ('erp_sale_order_items', 'sale', 2050),
        ('erp_sale_order', 'sale', 2060),
        ('erp_sale_out_items', 'sale', 2070),
        ('erp_sale_out', 'sale', 2080),
        ('erp_sale_return_items', 'sale', 2090),
        ('erp_sale_return', 'sale', 2100),
        ('erp_sale_price_adjust_item', 'sale', 2110),
        ('erp_sale_price_adjust', 'sale', 2120),
        ('erp_sale_convert_record', 'sale', 2130),
        ('erp_chain_order_item', 'sale', 2140),
        ('erp_chain_order', 'sale', 2150),
        ('erp_customer_business_info', 'sale', 2210),
        ('erp_customer_contact', 'sale', 2220),
        ('erp_customer_contract', 'sale', 2230),
        ('erp_customer_dept_credit', 'sale', 2240),
        ('erp_customer_dept', 'sale', 2250),
        ('erp_customer_extend', 'sale', 2260),
        ('erp_customer_extend_info', 'sale', 2270),
        ('erp_customer_image', 'sale', 2280),
        ('erp_customer_task', 'sale', 2290),
        ('erp_customer_area', 'sale', 2300),
        ('erp_customer', 'sale', 2310),

        -- Stock documents, locks and ledgers. erp_stock is reset, not truncated.
        ('erp_stock_record', 'stock', 3010),
        ('erp_stock_lock', 'stock', 3020),
        ('erp_stock_in_item', 'stock', 3030),
        ('erp_stock_in', 'stock', 3040),
        ('erp_stock_out_item', 'stock', 3050),
        ('erp_stock_out', 'stock', 3060),
        ('erp_stock_move_item', 'stock', 3070),
        ('erp_stock_move', 'stock', 3080),
        ('erp_stock_check_item', 'stock', 3090),
        ('erp_stock_check', 'stock', 3100),
        ('erp_stock_in_bill_pickup_record', 'stock', 3110),
        ('erp_stock_in_bill_item', 'stock', 3120),
        ('erp_stock_in_bill', 'stock', 3130),
        ('erp_stock_out_bill_pick_record', 'stock', 3140),
        ('erp_stock_out_bill_item', 'stock', 3150),
        ('erp_stock_out_bill', 'stock', 3160),
        ('erp_warehouse_move_item', 'stock', 3170),
        ('erp_warehouse_move', 'stock', 3180),
        ('erp_user_warehouse_permission', 'stock_permission', 3310),
        ('erp_warehouse_sale_dept_permission', 'stock_permission', 3320),

        -- Finance business data
        ('erp_finance_payment_item', 'finance', 4010),
        ('erp_finance_payment', 'finance', 4020),
        ('erp_finance_receipt_item', 'finance', 4030),
        ('erp_finance_receipt', 'finance', 4040),
        ('erp_finance_transfer', 'finance', 4050),
        ('erp_payable_expense_item', 'finance', 4060),
        ('erp_payable_expense', 'finance', 4070),
        ('erp_payable_other', 'finance', 4080),
        ('erp_payable_writeoff', 'finance', 4090),
        ('erp_receivable_other_item', 'finance', 4100),
        ('erp_receivable_other', 'finance', 4110),
        ('erp_receivable_other_income_item', 'finance', 4120),
        ('erp_receivable_other_income', 'finance', 4130),
        ('erp_receivable_writeoff', 'finance', 4140),
        ('erp_other_payable_item', 'finance', 4150),
        ('erp_other_payable', 'finance', 4160),
        ('erp_other_receivable_item', 'finance', 4170),
        ('erp_other_receivable', 'finance', 4180),
        ('erp_pre_payment_item', 'finance', 4190),
        ('erp_pre_payment', 'finance', 4200),
        ('erp_pre_receipt_item', 'finance', 4210),
        ('erp_pre_receipt', 'finance', 4220),
        ('erp_pre_receivable_item', 'finance', 4230),
        ('erp_pre_receivable', 'finance', 4240),
        ('erp_voucher_attribution', 'finance', 4250),
        ('erp_voucher_item', 'finance', 4260),
        ('erp_voucher', 'finance', 4270),
        ('erp_book_open_voucher_config', 'finance', 4280),
        ('erp_book_open', 'finance', 4290),
        ('erp_account', 'finance', 4300),

        -- Operational data and derived histories
        ('erp_import_export_record_detail', 'auxiliary', 5010),
        ('erp_import_export_record', 'auxiliary', 5020),
        ('erp_print_record', 'auxiliary', 5030),
        ('erp_archive_merge_log', 'auxiliary', 5040),
        ('erp_price_history', 'auxiliary', 5050),
        ('erp_auto_order_rule', 'auxiliary', 5060),
        ('erp_mall_product_mapping', 'auxiliary', 5070),
        ('erp_mall_category_mapping', 'auxiliary', 5080),
        ('form_data_permission', 'permission', 5090);

    DELETE target
    FROM tmp_cleanup_targets target
    LEFT JOIN information_schema.tables t
           ON t.table_schema = DATABASE()
          AND t.table_name = target.table_name
          AND t.table_type = 'BASE TABLE'
    WHERE t.table_name IS NULL;

    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_statements;
    CREATE TEMPORARY TABLE tmp_cleanup_statements (
        sort_no INT NOT NULL PRIMARY KEY,
        object_name VARCHAR(64) NOT NULL,
        action_desc VARCHAR(255) NOT NULL,
        statement_sql VARCHAR(4000) NOT NULL
    ) ENGINE = MEMORY;

    INSERT INTO tmp_cleanup_statements (sort_no, object_name, action_desc, statement_sql) VALUES
        (6010, 'system_oauth2_access_token', 'Clear login access tokens', 'TRUNCATE TABLE `system_oauth2_access_token`'),
        (6020, 'system_oauth2_refresh_token', 'Clear login refresh tokens', 'TRUNCATE TABLE `system_oauth2_refresh_token`'),
        (6030, 'system_oauth2_code', 'Clear oauth2 authorization codes', 'TRUNCATE TABLE `system_oauth2_code`'),
        (6040, 'system_oauth2_approve', 'Clear oauth2 approvals', 'TRUNCATE TABLE `system_oauth2_approve`'),
        (6050, 'system_user_session', 'Clear user sessions if the table exists', 'TRUNCATE TABLE `system_user_session`'),
        (6060, 'system_social_user_bind', 'Clear social user binds', 'TRUNCATE TABLE `system_social_user_bind`'),
        (6070, 'system_social_user', 'Clear social users', 'TRUNCATE TABLE `system_social_user`'),
        (6080, 'system_login_log', 'Clear login logs', 'TRUNCATE TABLE `system_login_log`'),
        (6090, 'system_operate_log', 'Clear operation logs', 'TRUNCATE TABLE `system_operate_log`'),
        (6100, 'system_user_permission_deny', 'Clear user-level deny permissions', 'TRUNCATE TABLE `system_user_permission_deny`'),
        (6110, 'system_user_price_field', 'Clear user price-field permissions', 'TRUNCATE TABLE `system_user_price_field`'),
        (6120, 'system_dept_price_field', 'Clear department price-field permissions', 'TRUNCATE TABLE `system_dept_price_field`'),
        (6130, 'system_role_field_permission', 'Clear role field restrictions', 'TRUNCATE TABLE `system_role_field_permission`'),
        (6140, 'system_role_form_data_scope', 'Clear role form data scopes', 'TRUNCATE TABLE `system_role_form_data_scope`'),
        (6150, 'system_user_role', 'Rebuild kept user-role relation from scratch', 'TRUNCATE TABLE `system_user_role`'),
        (6160, 'system_user_dept', 'Rebuild kept user-department relation from scratch', 'TRUNCATE TABLE `system_user_dept`'),
        (6170, 'system_user_post', 'Rebuild kept user-post relation from scratch', 'TRUNCATE TABLE `system_user_post`');

    DELETE s
    FROM tmp_cleanup_statements s
    LEFT JOIN information_schema.tables t
           ON t.table_schema = DATABASE()
          AND t.table_name = s.object_name
          AND t.table_type = 'BASE TABLE'
    WHERE t.table_name IS NULL;

    DROP TEMPORARY TABLE IF EXISTS tmp_dept_column_targets;
    CREATE TEMPORARY TABLE tmp_dept_column_targets (
        table_name VARCHAR(64) NOT NULL,
        column_name VARCHAR(64) NOT NULL,
        sort_no INT NOT NULL,
        PRIMARY KEY (table_name, column_name)
    ) ENGINE = MEMORY;

    INSERT INTO tmp_dept_column_targets (table_name, column_name, sort_no) VALUES
        ('erp_product', 'dept_id', 1010),
        ('erp_product', 'create_dept_id', 1020),
        ('erp_product_category', 'dept_id', 1030),
        ('erp_product_unit', 'dept_id', 1040),
        ('erp_product_universal', 'dept_id', 1050),
        ('erp_price_system', 'dept_id', 1060),
        ('erp_product_price_system', 'dept_id', 1070),
        ('erp_sale_config', 'dept_id', 1080),
        ('erp_warehouse', 'dept_id', 2010),
        ('erp_stock', 'dept_id', 2020);

    DELETE target
    FROM tmp_dept_column_targets target
    LEFT JOIN information_schema.columns c
           ON c.table_schema = DATABASE()
          AND c.table_name = target.table_name
          AND c.column_name = target.column_name
    WHERE c.column_name IS NULL;

    DROP TEMPORARY TABLE IF EXISTS tmp_auto_increment_targets;
    CREATE TEMPORARY TABLE tmp_auto_increment_targets (
        table_name VARCHAR(64) NOT NULL PRIMARY KEY,
        sort_no INT NOT NULL
    ) ENGINE = MEMORY;

    INSERT INTO tmp_auto_increment_targets (table_name, sort_no) VALUES
        ('system_dept', 1010),
        ('system_users', 1020),
        ('system_role', 1030),
        ('system_user_role', 1040),
        ('system_user_dept', 1050),
        ('system_user_post', 1060),
        ('erp_product_dept', 2010),
        ('erp_user_warehouse_permission', 2020),
        ('erp_warehouse_sale_dept_permission', 2030);

    DELETE target
    FROM tmp_auto_increment_targets target
    LEFT JOIN information_schema.tables t
           ON t.table_schema = DATABASE()
          AND t.table_name = target.table_name
          AND t.table_type = 'BASE TABLE'
    WHERE t.table_name IS NULL;

    SELECT COUNT(*) INTO v_target_count FROM tmp_cleanup_targets;
    SELECT COUNT(*) INTO v_statement_count FROM tmp_cleanup_statements;

    SELECT COUNT(*) INTO v_stock_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_stock'
      AND table_type = 'BASE TABLE';

    SELECT COUNT(*) INTO v_stock_balance_columns
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_stock'
      AND column_name IN (
          'count',
          'lock_count',
          'occupied_count',
          'pending_in_count',
          'in_transit_count',
          'cost_price',
          'cost_amount'
      );

    SELECT COUNT(*) INTO v_keep_user_count
    FROM system_users
    WHERE id IN (@keep_user_id_1, @keep_user_id_2);

    SELECT COUNT(*) INTO v_keep_role_count
    FROM system_role
    WHERE id IN (@keep_role_id_1, @keep_role_id_2, @keep_role_id_3);

    SET @keep_dept_old_id = COALESCE(
        @keep_dept_old_id,
        (
            SELECT d.id
            FROM system_dept d
            INNER JOIN system_users u ON u.dept_id = d.id
            WHERE u.id IN (@keep_user_id_1, @keep_user_id_2)
            ORDER BY
                CASE u.id
                    WHEN @keep_user_id_1 THEN 1
                    WHEN @keep_user_id_2 THEN 2
                    ELSE 3
                END,
                d.id
            LIMIT 1
        ),
        (
            SELECT MIN(id)
            FROM system_dept
        )
    );

    SELECT COUNT(*) INTO v_keep_dept_count
    FROM system_dept
    WHERE id = @keep_dept_old_id;

    SELECT
        'Cleanup preview' AS message,
        v_target_count AS truncate_table_count,
        v_statement_count AS system_statement_count,
        @keep_dept_old_id AS kept_dept_old_id,
        @keep_dept_new_id AS kept_dept_new_id,
        CONCAT(@keep_user_id_1, ',', @keep_user_id_2) AS kept_user_ids,
        CONCAT(@keep_role_id_1, ',', @keep_role_id_2, ',', @keep_role_id_3) AS kept_role_ids;

    SELECT category, table_name
    FROM tmp_cleanup_targets
    ORDER BY sort_no, table_name;

    SELECT object_name, action_desc
    FROM tmp_cleanup_statements
    ORDER BY sort_no;

    SELECT
        'Preserved scopes' AS message,
        'parts, product categories, product units, product prices, vehicle fit data, warehouse master data, ERP/system configuration, menus, dictionaries and print templates' AS scope_desc;

    SELECT
        'erp_stock reset plan' AS message,
        CASE
            WHEN v_stock_table_exists = 0 THEN 'erp_stock table not found; stock reset will be skipped'
            WHEN v_stock_balance_columns = 7 THEN 'erp_stock rows will be kept and stock balance fields will be reset to 0'
            ELSE 'erp_stock table exists but required balance columns are incomplete; execution will stop if confirmed'
        END AS plan;

    IF @confirm_cleanup <> 'YES' THEN
        SELECT 'Cleanup blocked: set @confirm_cleanup = ''YES'' after backup and preview review.' AS message;
        DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_statements;
        DROP TEMPORARY TABLE IF EXISTS tmp_dept_column_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_auto_increment_targets;
        LEAVE clean_block;
    END IF;

    IF v_keep_dept_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup aborted: kept department does not exist.';
    END IF;

    IF v_keep_user_count <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup aborted: one or more kept users do not exist.';
    END IF;

    IF v_keep_role_count <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup aborted: one or more kept roles do not exist.';
    END IF;

    IF v_stock_table_exists = 1 AND v_stock_balance_columns <> 7 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cleanup aborted: erp_stock exists but required balance columns are incomplete.';
    END IF;

    SET FOREIGN_KEY_CHECKS = 0;

    WHILE EXISTS (SELECT 1 FROM tmp_cleanup_targets) DO
        SELECT table_name
        INTO v_table_name
        FROM tmp_cleanup_targets
        ORDER BY sort_no, table_name
        LIMIT 1;

        SET @truncate_sql = CONCAT('TRUNCATE TABLE `', REPLACE(v_table_name, '`', '``'), '`');
        PREPARE truncate_stmt FROM @truncate_sql;
        EXECUTE truncate_stmt;
        DEALLOCATE PREPARE truncate_stmt;

        DELETE FROM tmp_cleanup_targets
        WHERE table_name = v_table_name;
    END WHILE;

    WHILE EXISTS (SELECT 1 FROM tmp_cleanup_statements) DO
        SELECT statement_sql
        INTO v_statement_sql
        FROM tmp_cleanup_statements
        ORDER BY sort_no
        LIMIT 1;

        SET @cleanup_sql = v_statement_sql;
        PREPARE cleanup_stmt FROM @cleanup_sql;
        EXECUTE cleanup_stmt;
        DEALLOCATE PREPARE cleanup_stmt;

        DELETE FROM tmp_cleanup_statements
        WHERE statement_sql = v_statement_sql;
    END WHILE;

    DELETE FROM system_users
    WHERE id NOT IN (@keep_user_id_1, @keep_user_id_2);

    DELETE FROM system_role
    WHERE id NOT IN (@keep_role_id_1, @keep_role_id_2, @keep_role_id_3);

    DELETE FROM system_dept
    WHERE id <> @keep_dept_old_id;

    UPDATE system_dept
    SET id = @keep_dept_new_id,
        name = @keep_dept_name,
        parent_id = 0,
        sort = 1,
        leader_user_id = @keep_user_id_1,
        status = 0,
        updater = CAST(@keep_user_id_1 AS CHAR),
        update_time = NOW(),
        deleted = b'0',
        tenant_id = @keep_tenant_id
    WHERE id = @keep_dept_old_id;

    UPDATE system_users
    SET dept_id = @keep_dept_new_id,
        status = 0,
        updater = CAST(@keep_user_id_1 AS CHAR),
        update_time = NOW(),
        deleted = b'0',
        tenant_id = @keep_tenant_id
    WHERE id IN (@keep_user_id_1, @keep_user_id_2);

    UPDATE system_role
    SET sort = CASE id
            WHEN @keep_role_id_1 THEN 1
            WHEN @keep_role_id_2 THEN 2
            WHEN @keep_role_id_3 THEN 3
            ELSE sort
        END,
        data_scope = 1,
        data_scope_dept_ids = '',
        status = 0,
        updater = CAST(@keep_user_id_1 AS CHAR),
        update_time = NOW(),
        deleted = b'0',
        tenant_id = @keep_tenant_id
    WHERE id IN (@keep_role_id_1, @keep_role_id_2, @keep_role_id_3);

    DELETE FROM system_role_menu
    WHERE role_id NOT IN (@keep_role_id_1, @keep_role_id_2, @keep_role_id_3);

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'system_user_role' AND table_type = 'BASE TABLE'
    ) THEN
        INSERT INTO system_user_role
        (`user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
        SELECT
            keep_users.kept_user_id,
            keep_roles.kept_role_id,
            CAST(@keep_user_id_1 AS CHAR),
            NOW(),
            CAST(@keep_user_id_1 AS CHAR),
            NOW(),
            b'0',
            @keep_tenant_id
        FROM (
            SELECT @keep_user_id_1 AS kept_user_id
            UNION ALL SELECT @keep_user_id_2
        ) keep_users
        CROSS JOIN (
            SELECT @keep_role_id_1 AS kept_role_id
            UNION ALL SELECT @keep_role_id_2
            UNION ALL SELECT @keep_role_id_3
        ) keep_roles
        ORDER BY keep_users.kept_user_id, keep_roles.kept_role_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'system_user_dept' AND table_type = 'BASE TABLE'
    ) THEN
        INSERT INTO system_user_dept
        (`user_id`, `dept_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
        SELECT
            kept_user_id,
            @keep_dept_new_id,
            CAST(@keep_user_id_1 AS CHAR),
            NOW(),
            CAST(@keep_user_id_1 AS CHAR),
            NOW(),
            b'0',
            @keep_tenant_id
        FROM (
            SELECT @keep_user_id_1 AS kept_user_id
            UNION ALL SELECT @keep_user_id_2
        ) keep_users
        ORDER BY kept_user_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'system_user_post' AND table_type = 'BASE TABLE'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'system_post' AND table_type = 'BASE TABLE'
    ) AND EXISTS (
        SELECT 1 FROM system_post WHERE id = 1
    ) THEN
        INSERT INTO system_user_post
        (`user_id`, `post_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
        SELECT
            kept_user_id,
            1,
            CAST(@keep_user_id_1 AS CHAR),
            NOW(),
            CAST(@keep_user_id_1 AS CHAR),
            NOW(),
            b'0',
            @keep_tenant_id
        FROM (
            SELECT @keep_user_id_1 AS kept_user_id
            UNION ALL SELECT @keep_user_id_2
        ) keep_users
        ORDER BY kept_user_id;
    END IF;

    WHILE EXISTS (SELECT 1 FROM tmp_dept_column_targets) DO
        SELECT table_name, column_name
        INTO v_table_name, v_column_name
        FROM tmp_dept_column_targets
        ORDER BY sort_no, table_name, column_name
        LIMIT 1;

        SET @update_dept_sql = CONCAT(
            'UPDATE `', REPLACE(v_table_name, '`', '``'),
            '` SET `', REPLACE(v_column_name, '`', '``'), '` = ', @keep_dept_new_id
        );
        PREPARE update_dept_stmt FROM @update_dept_sql;
        EXECUTE update_dept_stmt;
        DEALLOCATE PREPARE update_dept_stmt;

        DELETE FROM tmp_dept_column_targets
        WHERE table_name = v_table_name
          AND column_name = v_column_name;
    END WHILE;

    IF v_stock_table_exists = 1 THEN
        UPDATE erp_stock
        SET `count` = 0,
            `lock_count` = 0,
            `occupied_count` = 0,
            `pending_in_count` = 0,
            `in_transit_count` = 0,
            `cost_price` = 0,
            `cost_amount` = 0,
            `updater` = CAST(@keep_user_id_1 AS CHAR),
            `update_time` = NOW();
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'erp_product_dept' AND table_type = 'BASE TABLE'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'erp_product' AND table_type = 'BASE TABLE'
    ) THEN
        TRUNCATE TABLE erp_product_dept;
        INSERT INTO erp_product_dept
        (`product_id`, `dept_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
        SELECT id, @keep_dept_new_id, CAST(@keep_user_id_1 AS CHAR), NOW(), CAST(@keep_user_id_1 AS CHAR), NOW(), b'0', tenant_id
        FROM erp_product
        WHERE deleted = b'0';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'erp_user_warehouse_permission' AND table_type = 'BASE TABLE'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'erp_warehouse' AND table_type = 'BASE TABLE'
    ) THEN
        INSERT IGNORE INTO erp_user_warehouse_permission
        (`user_id`, `warehouse_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
        SELECT keep_users.kept_user_id, erp_warehouse.id, CAST(@keep_user_id_1 AS CHAR), NOW(), CAST(@keep_user_id_1 AS CHAR), NOW(), b'0', erp_warehouse.tenant_id
        FROM erp_warehouse
        CROSS JOIN (
            SELECT @keep_user_id_1 AS kept_user_id
            UNION ALL SELECT @keep_user_id_2
        ) keep_users
        WHERE deleted = b'0';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'erp_warehouse_sale_dept_permission' AND table_type = 'BASE TABLE'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'erp_warehouse' AND table_type = 'BASE TABLE'
    ) THEN
        INSERT IGNORE INTO erp_warehouse_sale_dept_permission
        (`warehouse_id`, `dept_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
        SELECT id, @keep_dept_new_id, CAST(@keep_user_id_1 AS CHAR), NOW(), CAST(@keep_user_id_1 AS CHAR), NOW(), b'0', tenant_id
        FROM erp_warehouse
        WHERE deleted = b'0';
    END IF;

    WHILE EXISTS (SELECT 1 FROM tmp_auto_increment_targets) DO
        SELECT table_name
        INTO v_table_name
        FROM tmp_auto_increment_targets
        ORDER BY sort_no, table_name
        LIMIT 1;

        SET @max_id_sql = CONCAT(
            'SELECT COALESCE(MAX(`id`), 0) + 1 INTO @next_auto_increment FROM `',
            REPLACE(v_table_name, '`', '``'),
            '`'
        );
        PREPARE max_id_stmt FROM @max_id_sql;
        EXECUTE max_id_stmt;
        DEALLOCATE PREPARE max_id_stmt;

        IF @next_auto_increment < 1 THEN
            SET @next_auto_increment = 1;
        END IF;

        SET @alter_auto_increment_sql = CONCAT(
            'ALTER TABLE `',
            REPLACE(v_table_name, '`', '``'),
            '` AUTO_INCREMENT = ',
            @next_auto_increment
        );
        PREPARE alter_ai_stmt FROM @alter_auto_increment_sql;
        EXECUTE alter_ai_stmt;
        DEALLOCATE PREPARE alter_ai_stmt;

        DELETE FROM tmp_auto_increment_targets
        WHERE table_name = v_table_name;
    END WHILE;

    SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;

    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_targets;
    DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_statements;
    DROP TEMPORARY TABLE IF EXISTS tmp_dept_column_targets;
    DROP TEMPORARY TABLE IF EXISTS tmp_auto_increment_targets;

    SELECT 'Cleanup completed.' AS message;

    SELECT 'system_dept' AS table_name, COUNT(*) AS row_count, MIN(id) AS min_id, MAX(id) AS max_id
    FROM system_dept
    UNION ALL
    SELECT 'system_users', COUNT(*), MIN(id), MAX(id)
    FROM system_users
    UNION ALL
    SELECT 'system_role', COUNT(*), MIN(id), MAX(id)
    FROM system_role;

    IF v_stock_table_exists = 1 THEN
        SELECT
            'erp_stock balance check' AS message,
            COALESCE(SUM(`count`), 0) AS total_count,
            COALESCE(SUM(`lock_count`), 0) AS total_lock_count,
            COALESCE(SUM(`occupied_count`), 0) AS total_occupied_count,
            COALESCE(SUM(`pending_in_count`), 0) AS total_pending_in_count,
            COALESCE(SUM(`in_transit_count`), 0) AS total_in_transit_count,
            COALESCE(SUM(`cost_amount`), 0) AS total_cost_amount
        FROM erp_stock;
    END IF;
END clean_block$$

DELIMITER ;

CALL clean_erp_business_keep_parts_reset_org();

DROP PROCEDURE IF EXISTS clean_erp_business_keep_parts_reset_org;
