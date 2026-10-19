-- v269 调拨入库查询报表只读预检；不包含 DDL/DML，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() target_schema, VERSION() mysql_version, @@hostname mysql_host;

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_stock_move', 'erp_stock_move_item', 'erp_product', 'erp_warehouse',
                     'system_menu', 'system_role', 'system_role_menu', 'system_tenant_package')
ORDER BY table_name;

SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'erp_stock_move'
GROUP BY index_name
HAVING index_columns LIKE 'tenant_id,transfer_direction,status,deleted,move_time%';

SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'erp_stock_move_item'
GROUP BY index_name
HAVING index_columns LIKE 'tenant_id,move_id%';

SELECT id, parent_id, name, type, sort, path, component, component_name, permission
FROM system_menu
WHERE deleted = b'0'
  AND (path IN ('/erp', 'business-report', 'inventory', 'transfer-out-detail', 'transfer-in-detail')
       OR permission IN ('erp:stock-transfer-in:query', 'erp:stock-transfer-in:export',
                         'erp:stock-transfer-in-detail-report:query',
                         'erp:stock-transfer-in-detail-report:export'))
ORDER BY parent_id, type, sort, id;

SELECT permission, COUNT(*) duplicate_count
FROM system_menu
WHERE deleted = b'0'
  AND permission IN ('erp:stock-transfer-in:query', 'erp:stock-transfer-in:export',
                     'erp:stock-transfer-in-detail-report:query',
                     'erp:stock-transfer-in-detail-report:export')
GROUP BY permission;

SELECT source.permission, COUNT(DISTINCT rm.role_id) role_count
FROM system_menu source
LEFT JOIN system_role_menu rm ON rm.menu_id = source.id AND rm.deleted = b'0'
WHERE source.deleted = b'0'
  AND source.permission IN ('erp:stock-transfer-in:query', 'erp:stock-transfer-in:export')
GROUP BY source.id, source.permission;

SELECT source.permission, COUNT(DISTINCT package_row.id) package_count
FROM system_menu source
LEFT JOIN system_tenant_package package_row ON package_row.deleted = b'0'
  AND JSON_VALID(package_row.menu_ids)
  AND JSON_CONTAINS(package_row.menu_ids, CAST(source.id AS CHAR), '$')
WHERE source.deleted = b'0'
  AND source.permission IN ('erp:stock-transfer-in:query', 'erp:stock-transfer-in:export')
GROUP BY source.id, source.permission;

SELECT COUNT(*) invalid_package_json
FROM system_tenant_package
WHERE deleted = b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids) <> 'ARRAY');

SELECT COUNT(*) approved_transfer_in_documents
FROM erp_stock_move
WHERE deleted = b'0' AND transfer_direction = 20 AND status = 20;

SELECT COUNT(*) approved_transfer_in_items,
       COALESCE(SUM(smi.count), 0) total_count,
       COALESCE(SUM(ROUND(smi.count * smi.product_price, 2)), 0) total_amount
FROM erp_stock_move_item smi
INNER JOIN erp_stock_move sm ON sm.id = smi.move_id
  AND sm.tenant_id = smi.tenant_id AND sm.deleted = b'0'
WHERE smi.deleted = b'0' AND sm.transfer_direction = 20 AND sm.status = 20;
