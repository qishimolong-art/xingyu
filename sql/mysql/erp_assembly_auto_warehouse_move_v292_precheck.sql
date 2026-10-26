-- v292 precheck：套件组装跨仓自动生成并完成仓库移货单。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database;

SELECT table_name, COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_assembly_order', 'erp_warehouse_move',
                     'erp_warehouse_move_item', 'erp_stock_record')
GROUP BY table_name;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'erp_assembly_order'
  AND column_name IN ('warehouse_id', 'out_warehouse_id', 'warehouse_move_id');

SELECT COUNT(*) AS active_order_missing_in_warehouse
FROM erp_assembly_order
WHERE deleted = b'0' AND warehouse_id IS NULL;

SELECT source_id, COUNT(*) AS duplicate_count
FROM erp_warehouse_move
WHERE deleted = b'0' AND source_type = 30 AND source_id IS NOT NULL
GROUP BY source_id
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS assembly_move_orphan_count
FROM erp_warehouse_move wm
LEFT JOIN erp_assembly_order ao
  ON ao.id = wm.source_id AND ao.tenant_id = wm.tenant_id AND ao.deleted = b'0'
WHERE wm.deleted = b'0' AND wm.source_type = 30 AND ao.id IS NULL;

