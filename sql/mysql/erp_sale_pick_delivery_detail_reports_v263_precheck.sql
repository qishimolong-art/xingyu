-- v263 销售拣货、送货明细报表只读预检；不含 DDL/DML。
SELECT DATABASE() target_database,@@hostname database_host,@@version database_version;
SELECT table_name,table_rows FROM information_schema.tables WHERE table_schema=DATABASE()
  AND table_name IN ('erp_sale_pick_delivery_order','erp_sale_pick_delivery_pick_task',
    'erp_sale_pick_delivery_item','erp_sale_cart','erp_sale_out','system_menu','system_role_menu','system_tenant_package');
SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
FROM information_schema.statistics WHERE table_schema=DATABASE()
  AND table_name='erp_sale_pick_delivery_pick_task' GROUP BY index_name;
SELECT COUNT(*) active_tasks,SUM(source_type=30) cart_tasks,SUM(sale_out_id IS NOT NULL) bound_out_tasks
FROM erp_sale_pick_delivery_pick_task WHERE deleted=b'0';
SELECT COUNT(*) active_items,SUM(picked_count IS NULL AND pick_status=30) legacy_done_pick_items,
  SUM(delivered_count IS NULL AND delivery_status=30) legacy_done_delivery_items
FROM erp_sale_pick_delivery_item WHERE deleted=b'0';
SELECT permission,COUNT(*) active_count FROM system_menu WHERE deleted=b'0' AND permission IN
  ('erp:sale-pick:query','erp:sale-pick:export','erp:sale-delivery:query','erp:sale-delivery:export',
   'erp:sale-pick-detail-report:query','erp:sale-pick-detail-report:export',
   'erp:sale-delivery-detail-report:query','erp:sale-delivery-detail-report:export') GROUP BY permission;
SELECT child.id,child.parent_id,child.name,child.type,child.sort,child.path,child.component,child.component_name,child.permission
FROM system_menu child INNER JOIN system_menu sale ON sale.id=child.parent_id AND sale.deleted=b'0' AND sale.path='sale'
INNER JOIN system_menu report ON report.id=sale.parent_id AND report.deleted=b'0' AND report.path='business-report'
INNER JOIN system_menu erp ON erp.id=report.parent_id AND erp.deleted=b'0' AND erp.path='/erp'
WHERE child.deleted=b'0' AND child.path IN ('pick','delivery') ORDER BY child.sort,child.id;
SELECT COUNT(*) invalid_package_json FROM system_tenant_package
WHERE deleted=b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY');
