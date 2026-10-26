-- v293 precheck：套件组装正反向状态流程。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_assembly_order', 'erp_assembly_order_item',
                     'erp_stock_record', 'erp_warehouse_move',
                     'system_menu', 'system_role_menu', 'system_tenant_package')
ORDER BY table_name;

SELECT status, COUNT(*) AS row_count
FROM erp_assembly_order
WHERE deleted = b'0'
GROUP BY status
ORDER BY status;

SELECT COUNT(*) AS invalid_status_count
FROM erp_assembly_order
WHERE deleted = b'0' AND status NOT IN (10, 15, 18, 20, 30);

SELECT permission, COUNT(*) AS active_count
FROM system_menu
WHERE deleted = b'0'
  AND permission IN ('erp:assembly-order:void',
                     'erp:assembly-order:reverse-assembly',
                     'erp:assembly-order:confirm',
                     'erp:assembly-order:reverse-confirm',
                     'erp:assembly-order:start',
                     'erp:assembly-order:cancel-start',
                     'erp:assembly-order:complete')
GROUP BY permission
ORDER BY permission;

SELECT tenant_id, source_id, COUNT(*) AS move_count,
       SUM(status = 20) AS active_move_count,
       SUM(status = 30) AS reversed_move_count
FROM erp_warehouse_move
WHERE deleted = b'0' AND source_type = 30 AND source_id IS NOT NULL
GROUP BY tenant_id, source_id
HAVING SUM(status = 20) > 1;

