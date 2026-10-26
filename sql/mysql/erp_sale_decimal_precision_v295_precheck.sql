-- v295 只读预检；明确目标环境、列定义、范围和历史数据，不写业务数据。
SELECT DATABASE() AS target_database, VERSION() AS mysql_version;
SELECT table_name,column_name,column_type,is_nullable,column_default,column_comment,extra
FROM information_schema.columns WHERE table_schema=DATABASE() AND ((table_name = 'erp_sale_price_adjust' AND column_name = 'total_adjust_price')
          OR (table_name = 'erp_sale_price_adjust_item' AND column_name IN ('old_price', 'new_price', 'adjust_price'))
          OR (table_name = 'erp_stock' AND column_name = 'cost_amount')
          OR (table_name = 'erp_stock_record' AND column_name IN ('total_price', 'cost_amount')))
ORDER BY table_name,column_name;
SELECT 'erp_sale_price_adjust' AS table_name,COUNT(*) AS row_count,MIN(total_adjust_price) AS min_value,MAX(total_adjust_price) AS max_value,SUM(total_adjust_price) AS amount_sum FROM erp_sale_price_adjust
UNION ALL SELECT 'erp_sale_price_adjust_item',COUNT(*),MIN(old_price),MAX(new_price),SUM(adjust_price) FROM erp_sale_price_adjust_item
UNION ALL SELECT 'erp_stock',COUNT(*),MIN(cost_amount),MAX(cost_amount),SUM(cost_amount) FROM erp_stock
UNION ALL SELECT 'erp_stock_record',COUNT(*),MIN(cost_amount),MAX(total_price),SUM(cost_amount) FROM erp_stock_record;
SELECT COUNT(*) AS incompatible_values FROM (
SELECT total_adjust_price AS value FROM erp_sale_price_adjust
UNION ALL SELECT old_price FROM erp_sale_price_adjust_item
UNION ALL SELECT new_price FROM erp_sale_price_adjust_item
UNION ALL SELECT adjust_price FROM erp_sale_price_adjust_item
UNION ALL SELECT cost_amount FROM erp_stock
UNION ALL SELECT total_price FROM erp_stock_record
UNION ALL SELECT cost_amount FROM erp_stock_record
) history_values WHERE ABS(value)>=1000000000000000000 OR value<>ROUND(value,6);
SELECT COUNT(*) AS purchase_decimal_columns, SUM(numeric_precision<>24 OR numeric_scale<>6) AS incompatible_purchase_columns
FROM information_schema.columns WHERE table_schema=DATABASE() AND data_type='decimal'
AND table_name IN ('erp_purchase_order','erp_purchase_order_items','erp_purchase_in','erp_purchase_in_items','erp_purchase_return','erp_purchase_return_items','erp_purchase_price_adjust','erp_purchase_price_adjust_item','erp_purchase_invoice','erp_purchase_invoice_item','erp_purchase_invoice_source_allocation');
