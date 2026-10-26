-- v291 precheck: 套件组装新增/详情字段与入、出货仓拆分。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database;

SELECT table_name, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_assembly_order', 'erp_assembly_order_item', 'erp_warehouse', 'erp_customer')
GROUP BY table_name;

SELECT COUNT(*) AS active_order_missing_in_warehouse
FROM erp_assembly_order
WHERE deleted = b'0' AND warehouse_id IS NULL;

SELECT COUNT(*) AS active_order_invalid_cost
FROM erp_assembly_order
WHERE deleted = b'0'
  AND (COALESCE(labor_cost_per_set, 0) < 0 OR COALESCE(total_cost, 0) < 0);

SELECT COUNT(*) AS legacy_invalid_assembly_time
FROM erp_assembly_order
WHERE (assembly_time IS NULL OR assembly_time < '2000-01-01 00:00:00')
  AND create_time IS NOT NULL;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'erp_assembly_order'
  AND column_name IN ('customer_id', 'handler_id', 'out_warehouse_id', 'shelf', 'other_cost');

