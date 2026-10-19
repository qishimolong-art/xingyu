-- v267 调拨出库查询报表只读预检；不包含 DDL/DML，可在迁移前重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() target_database, @@hostname database_host, @@version database_version;

SELECT table_name, table_type, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_stock_move', 'erp_stock_move_item', 'erp_product', 'erp_warehouse',
                     'system_users', 'system_menu', 'system_role_menu', 'system_tenant_package')
ORDER BY table_name;

SELECT table_name, index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_stock_move', 'erp_stock_move_item')
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;

SELECT transfer_direction, status, deleted, COUNT(*) document_count
FROM erp_stock_move
GROUP BY transfer_direction, status, deleted
ORDER BY transfer_direction, status, deleted;

SELECT COUNT(*) approved_transfer_out_documents,
       COALESCE(SUM((SELECT COUNT(*) FROM erp_stock_move_item i
                     WHERE i.tenant_id = m.tenant_id AND i.move_id = m.id AND i.deleted = b'0')), 0)
         approved_transfer_out_items
FROM erp_stock_move m
WHERE m.deleted = b'0' AND m.transfer_direction = 10 AND m.status = 20;

SELECT child.id, child.parent_id, child.name, child.type, child.sort, child.path,
       child.component, child.component_name, child.permission
FROM system_menu child
INNER JOIN system_menu inventory ON inventory.id = child.parent_id
  AND inventory.deleted = b'0' AND inventory.type = 1 AND inventory.path = 'inventory'
INNER JOIN system_menu report ON report.id = inventory.parent_id
  AND report.deleted = b'0' AND report.type = 1 AND report.path = 'business-report'
INNER JOIN system_menu erp ON erp.id = report.parent_id
  AND erp.deleted = b'0' AND erp.type = 1 AND erp.path = '/erp'
WHERE child.deleted = b'0'
ORDER BY child.sort, child.type, child.id;

SELECT permission, COUNT(*) active_count,
       GROUP_CONCAT(CONCAT(id, ':', name, ':', type, ':', parent_id) ORDER BY id) matches
FROM system_menu
WHERE deleted = b'0'
  AND permission IN ('erp:stock-transfer-out:query', 'erp:stock-transfer-out:export',
                     'erp:stock-transfer-out-detail-report:query',
                     'erp:stock-transfer-out-detail-report:export')
GROUP BY permission
ORDER BY permission;

SELECT COUNT(*) conflicting_page_paths
FROM system_menu m
WHERE m.deleted = b'0' AND m.path = 'transfer-out-detail'
  AND NOT EXISTS (
    SELECT 1 FROM system_menu inventory
    INNER JOIN system_menu report ON report.id = inventory.parent_id
      AND report.deleted = b'0' AND report.path = 'business-report'
    INNER JOIN system_menu erp ON erp.id = report.parent_id
      AND erp.deleted = b'0' AND erp.path = '/erp'
    WHERE inventory.id = m.parent_id AND inventory.deleted = b'0' AND inventory.path = 'inventory'
  );

SELECT source.permission source_permission,
       COUNT(DISTINCT rm.role_id, rm.tenant_id) inherited_role_count
FROM system_menu source
LEFT JOIN system_role_menu rm ON rm.menu_id = source.id AND rm.deleted = b'0'
WHERE source.deleted = b'0'
  AND source.permission IN ('erp:stock-transfer-out:query', 'erp:stock-transfer-out:export')
GROUP BY source.permission
ORDER BY source.permission;

SELECT source.permission source_permission,
       COUNT(DISTINCT p.id) inherited_package_count
FROM system_menu source
LEFT JOIN system_tenant_package p ON p.deleted = b'0'
  AND JSON_VALID(p.menu_ids) AND JSON_TYPE(p.menu_ids) = 'ARRAY'
  AND JSON_CONTAINS(p.menu_ids, CAST(source.id AS CHAR), '$')
WHERE source.deleted = b'0'
  AND source.permission IN ('erp:stock-transfer-out:query', 'erp:stock-transfer-out:export')
GROUP BY source.permission
ORDER BY source.permission;

SELECT COUNT(*) invalid_package_json
FROM system_tenant_package
WHERE deleted = b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids) <> 'ARRAY');
