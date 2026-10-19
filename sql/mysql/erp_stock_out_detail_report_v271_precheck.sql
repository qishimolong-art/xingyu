-- v271 其他出库明细报表只读预检；不包含 DDL/DML，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() target_schema, VERSION() mysql_version, @@hostname mysql_host;

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_stock_out', 'erp_stock_out_item', 'erp_stock_record', 'erp_stock',
                     'erp_product', 'erp_product_category', 'erp_warehouse', 'erp_customer',
                     'system_users', 'system_dept', 'system_menu', 'system_role_menu',
                     'system_tenant_package', 'system_field_definition',
                     'system_role_field_permission')
ORDER BY table_name;

SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'erp_stock_out'
  AND column_name IN ('id', 'status', 'approve_time', 'update_time', 'tenant_id', 'deleted')
ORDER BY ordinal_position;

SELECT table_name, index_name,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_stock_out', 'erp_stock_out_item', 'erp_stock_record')
GROUP BY table_name, index_name
ORDER BY table_name, index_name;

SELECT COUNT(*) approved_documents
FROM erp_stock_out
WHERE deleted = b'0' AND status = 20;

SELECT COUNT(*) approved_items, COALESCE(SUM(item.count), 0) total_count,
       COALESCE(SUM(item.total_price), 0) total_amount
FROM erp_stock_out_item item
INNER JOIN erp_stock_out document ON document.id = item.out_id
  AND document.tenant_id = item.tenant_id AND document.deleted = b'0'
WHERE item.deleted = b'0' AND document.status = 20;

SELECT COUNT(*) approved_items_without_cost_record
FROM erp_stock_out_item item
INNER JOIN erp_stock_out document ON document.id = item.out_id
  AND document.tenant_id = item.tenant_id AND document.deleted = b'0'
LEFT JOIN erp_stock_record record ON record.id = (
  SELECT MAX(candidate.id) FROM erp_stock_record candidate
  WHERE candidate.tenant_id = item.tenant_id AND candidate.deleted = b'0'
    AND candidate.biz_type = 20 AND candidate.biz_id = document.id
    AND candidate.biz_item_id = item.id)
WHERE item.deleted = b'0' AND document.status = 20
  AND (record.id IS NULL OR record.unit_price IS NULL OR record.total_price IS NULL);

SELECT id, parent_id, name, type, sort, path, component, component_name, permission
FROM system_menu
WHERE deleted = b'0'
  AND (path IN ('/erp', 'business-report', 'inventory', 'other-out-detail')
       OR permission IN ('erp:stock-out:query', 'erp:stock-out:export',
                         'erp:stock-out-detail-report:query',
                         'erp:stock-out-detail-report:export'))
ORDER BY parent_id, type, sort, id;

SELECT permission, COUNT(*) row_count
FROM system_menu
WHERE deleted = b'0'
  AND permission IN ('erp:stock-out:query', 'erp:stock-out:export',
                     'erp:stock-out-detail-report:query',
                     'erp:stock-out-detail-report:export')
GROUP BY permission;

SELECT source.permission, COUNT(DISTINCT role_menu.role_id) role_count
FROM system_menu source
LEFT JOIN system_role_menu role_menu ON role_menu.menu_id = source.id AND role_menu.deleted = b'0'
WHERE source.deleted = b'0' AND source.permission IN ('erp:stock-out:query', 'erp:stock-out:export')
GROUP BY source.id, source.permission;

SELECT source.permission, COUNT(DISTINCT package_row.id) package_count
FROM system_menu source
LEFT JOIN system_tenant_package package_row ON package_row.deleted = b'0'
  AND JSON_VALID(package_row.menu_ids)
  AND JSON_CONTAINS(package_row.menu_ids, CAST(source.id AS CHAR), '$')
WHERE source.deleted = b'0' AND source.permission IN ('erp:stock-out:query', 'erp:stock-out:export')
GROUP BY source.id, source.permission;

SELECT tenant_id, field_key, COUNT(*) row_count
FROM system_field_definition
WHERE deleted = b'0' AND module = 'erp_stock_out'
  AND field_key IN ('approveTime', 'updateTime')
GROUP BY tenant_id, field_key
ORDER BY tenant_id, field_key;

SELECT COUNT(*) invalid_package_json
FROM system_tenant_package
WHERE deleted = b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids) <> 'ARRAY');
