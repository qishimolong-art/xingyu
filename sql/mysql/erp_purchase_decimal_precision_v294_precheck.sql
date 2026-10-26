-- 采购单据六位小数迁移前检查（只读）
-- 目标：仅扩展既有 DECIMAL 字段为 DECIMAL(24,6)，不新增字段、不改数据。

SELECT DATABASE() AS target_database, NOW() AS checked_at;

SELECT expected.table_name,
       IF(t.table_name IS NULL, 'MISSING', 'OK') AS table_status
FROM (
    SELECT 'erp_purchase_order' table_name UNION ALL
    SELECT 'erp_purchase_order_items' UNION ALL
    SELECT 'erp_purchase_in' UNION ALL
    SELECT 'erp_purchase_in_items' UNION ALL
    SELECT 'erp_purchase_return' UNION ALL
    SELECT 'erp_purchase_return_items' UNION ALL
    SELECT 'erp_purchase_price_adjust' UNION ALL
    SELECT 'erp_purchase_price_adjust_item' UNION ALL
    SELECT 'erp_purchase_invoice' UNION ALL
    SELECT 'erp_purchase_invoice_item' UNION ALL
    SELECT 'erp_purchase_invoice_source_allocation'
) expected
LEFT JOIN information_schema.tables t
       ON t.table_schema = DATABASE()
      AND t.table_name = expected.table_name
ORDER BY expected.table_name;

SELECT table_name, column_name, column_type, is_nullable, column_default,
       column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN (
      'erp_purchase_order', 'erp_purchase_order_items',
      'erp_purchase_in', 'erp_purchase_in_items',
      'erp_purchase_return', 'erp_purchase_return_items',
      'erp_purchase_price_adjust', 'erp_purchase_price_adjust_item',
      'erp_purchase_invoice', 'erp_purchase_invoice_item',
      'erp_purchase_invoice_source_allocation'
  )
  AND data_type IN ('decimal', 'numeric')
ORDER BY table_name, ordinal_position;

-- 必须返回 0 行迁移才安全：既有小数位不能超过 6，且不能是生成列。
SELECT table_name, column_name, numeric_precision, numeric_scale, extra,
       'INCOMPATIBLE_WITH_DECIMAL_24_6' AS risk
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN (
      'erp_purchase_order', 'erp_purchase_order_items',
      'erp_purchase_in', 'erp_purchase_in_items',
      'erp_purchase_return', 'erp_purchase_return_items',
      'erp_purchase_price_adjust', 'erp_purchase_price_adjust_item',
      'erp_purchase_invoice', 'erp_purchase_invoice_item',
      'erp_purchase_invoice_source_allocation'
  )
  AND data_type IN ('decimal', 'numeric')
  AND (numeric_scale > 6 OR extra <> '');

-- 由 DECIMAL(24,2) 改为 DECIMAL(24,6) 时整数位从 22 位缩为 18 位；
-- 下列三项必须都小于 10^18，迁移过程还会逐列再次做同样的动态检查。
SELECT 'erp_purchase_order.tax_percent' AS column_name,
       MAX(ABS(tax_percent)) AS max_abs_value
FROM erp_purchase_order
UNION ALL
SELECT 'erp_purchase_price_adjust.total_adjust_price', MAX(ABS(total_adjust_price))
FROM erp_purchase_price_adjust
UNION ALL
SELECT 'erp_purchase_price_adjust_item.adjust_price', MAX(ABS(adjust_price))
FROM erp_purchase_price_adjust_item;

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
      'erp_purchase_order', 'erp_purchase_order_items',
      'erp_purchase_in', 'erp_purchase_in_items',
      'erp_purchase_return', 'erp_purchase_return_items',
      'erp_purchase_price_adjust', 'erp_purchase_price_adjust_item',
      'erp_purchase_invoice', 'erp_purchase_invoice_item',
      'erp_purchase_invoice_source_allocation'
  )
ORDER BY table_name;

