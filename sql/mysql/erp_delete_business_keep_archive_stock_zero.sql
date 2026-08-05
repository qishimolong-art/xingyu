-- ERP business data cleanup script (MySQL)
-- Purpose:
--   Clear ERP business documents, vouchers, ledgers, histories and process data.
--   Keep archive/master data and personnel/system data.
--   Keep erp_stock rows, but reset all stock balance fields to 0.
--
-- Usage:
--   1. Backup the database first.
--   2. Execute once with @confirm_delete_business_keep_archive = 'NO' to review preview output.
--   3. Change @confirm_delete_business_keep_archive from 'NO' to 'YES'.
--   4. Execute:
--        mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < erp_delete_business_keep_archive_stock_zero.sql
--
-- Notes:
--   - This script operates on the current database and all tenants.
--   - It never truncates system_* tables, personnel tables, archive/master tables, or erp_stock.
--   - It uses TRUNCATE TABLE for business tables, so data cannot be recovered without a backup and
--     AUTO_INCREMENT values are reset.
--   - erp_stock.purchase_price, product_id, warehouse_id, dept_id and shelf are kept.

SET @confirm_delete_business_keep_archive = 'YES';

DROP PROCEDURE IF EXISTS clean_erp_business_keep_archive_stock_zero;

DELIMITER $$

CREATE PROCEDURE clean_erp_business_keep_archive_stock_zero()
clean_block: BEGIN
    DECLARE v_table_name VARCHAR(64);
    DECLARE v_target_count INT DEFAULT 0;
    DECLARE v_stock_table_exists INT DEFAULT 0;
    DECLARE v_stock_balance_columns INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        IF @old_foreign_key_checks IS NOT NULL THEN
            SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;
        END IF;
        DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_cleanup_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_cleanup_preserved;
        RESIGNAL;
    END;

    SET @old_foreign_key_checks = @@FOREIGN_KEY_CHECKS;

    DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_cleanup_targets;
    CREATE TEMPORARY TABLE tmp_erp_business_cleanup_targets (
        table_name VARCHAR(64) NOT NULL PRIMARY KEY,
        category VARCHAR(32) NOT NULL,
        sort_no INT NOT NULL
    ) ENGINE = MEMORY;

    INSERT INTO tmp_erp_business_cleanup_targets (table_name, category, sort_no) VALUES
        -- Purchase documents
        ('erp_purchase_order_items', 'purchase', 1010),
        ('erp_purchase_order', 'purchase', 1020),
        ('erp_purchase_in_items', 'purchase', 1030),
        ('erp_purchase_in', 'purchase', 1040),
        ('erp_purchase_return_items', 'purchase', 1050),
        ('erp_purchase_return', 'purchase', 1060),
        ('erp_purchase_invoice_item', 'purchase', 1070),
        ('erp_purchase_invoice', 'purchase', 1080),
        ('erp_purchase_price_adjust_item', 'purchase', 1090),
        ('erp_purchase_price_adjust', 'purchase', 1100),
        ('erp_purchase_suggestion_item', 'purchase', 1110),
        ('erp_purchase_suggestion', 'purchase', 1120),

        -- Sale documents
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

        -- Stock documents, locks and ledgers. erp_stock is intentionally not here.
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

        -- Finance business documents, write-offs and accounting vouchers
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

        -- Operation logs and derived business histories
        ('erp_import_export_record_detail', 'auxiliary', 5010),
        ('erp_import_export_record', 'auxiliary', 5020),
        ('erp_archive_merge_log', 'auxiliary', 5030),
        ('erp_price_history', 'auxiliary', 5040);

    DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_cleanup_preserved;
    CREATE TEMPORARY TABLE tmp_erp_business_cleanup_preserved (
        category VARCHAR(32) NOT NULL,
        scope_desc VARCHAR(128) NOT NULL
    ) ENGINE = MEMORY;

    INSERT INTO tmp_erp_business_cleanup_preserved (category, scope_desc) VALUES
        ('system/personnel', 'system_users, system_dept, system_post, system_role, system_menu, system_dict and related permission tables'),
        ('product archive', 'erp_product, erp_product_category, erp_product_unit, erp_product_dept, erp_product_universal'),
        ('party archive', 'erp_supplier*, erp_customer*'),
        ('warehouse archive', 'erp_warehouse, erp_warehouse_branch, erp_user_warehouse_permission, erp_warehouse_sale_dept_permission'),
        ('finance archive/config', 'erp_account, erp_accounting_subject, erp_subject_auxiliary, erp_voucher_word, erp_report_item_template'),
        ('price/config archive', 'erp_price_system, erp_product_price_system, erp_base_data, erp_*_config'),
        ('vehicle archive', 'erp_vehicle_brand, erp_vehicle_model, erp_vehicle_series, erp_vehicle_product_fit'),
        ('stock rows kept', 'erp_stock rows are kept; count/lock/occupied/pending/in-transit/cost fields are reset to 0');

    DELETE target
    FROM tmp_erp_business_cleanup_targets target
    LEFT JOIN information_schema.tables t
           ON t.table_schema = DATABASE()
          AND t.table_name = target.table_name
          AND t.table_type = 'BASE TABLE'
    WHERE t.table_name IS NULL;

    SELECT COUNT(*) INTO v_target_count FROM tmp_erp_business_cleanup_targets;
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

    SELECT
        'Business tables that will be truncated' AS message,
        v_target_count AS table_count;

    SELECT category, table_name
    FROM tmp_erp_business_cleanup_targets
    ORDER BY sort_no, table_name;

    SELECT
        'Preserved scopes' AS message,
        category,
        scope_desc
    FROM tmp_erp_business_cleanup_preserved
    ORDER BY category;

    SELECT
        'erp_stock reset plan' AS message,
        CASE
            WHEN v_stock_table_exists = 0 THEN 'erp_stock table not found; stock reset will be skipped'
            WHEN v_stock_balance_columns = 7 THEN 'erp_stock rows will be kept and stock balance fields will be reset to 0'
            ELSE 'erp_stock table exists but required stock balance columns are incomplete; execution will stop if confirmed'
        END AS plan;

    IF @confirm_delete_business_keep_archive <> 'YES' THEN
        SELECT 'Cleanup blocked: set @confirm_delete_business_keep_archive = ''YES'' after backup and preview review.' AS message;
        DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_cleanup_targets;
        DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_cleanup_preserved;
        LEAVE clean_block;
    END IF;

    IF v_stock_table_exists = 1 AND v_stock_balance_columns <> 7 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'erp_stock exists but required balance columns are incomplete';
    END IF;

    SET FOREIGN_KEY_CHECKS = 0;

    WHILE EXISTS (SELECT 1 FROM tmp_erp_business_cleanup_targets) DO
        SELECT table_name
        INTO v_table_name
        FROM tmp_erp_business_cleanup_targets
        ORDER BY sort_no, table_name
        LIMIT 1;

        SET @truncate_sql = CONCAT('TRUNCATE TABLE `', REPLACE(v_table_name, '`', '``'), '`');
        PREPARE truncate_stmt FROM @truncate_sql;
        EXECUTE truncate_stmt;
        DEALLOCATE PREPARE truncate_stmt;

        DELETE FROM tmp_erp_business_cleanup_targets
        WHERE table_name = v_table_name;
    END WHILE;

    IF v_stock_table_exists = 1 THEN
        UPDATE `erp_stock`
        SET `count` = 0,
            `lock_count` = 0,
            `occupied_count` = 0,
            `pending_in_count` = 0,
            `in_transit_count` = 0,
            `cost_price` = 0,
            `cost_amount` = 0;
    END IF;

    SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;

    DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_cleanup_targets;
    DROP TEMPORARY TABLE IF EXISTS tmp_erp_business_cleanup_preserved;

    SELECT 'ERP business data cleanup completed; archive/personnel data kept and erp_stock balances reset to 0.' AS message;
END clean_block$$

DELIMITER ;

CALL clean_erp_business_keep_archive_stock_zero();

DROP PROCEDURE IF EXISTS clean_erp_business_keep_archive_stock_zero;
