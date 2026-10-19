-- v262 只读预检；不含 DDL/DML。
SELECT DATABASE() target_database,@@hostname database_host,@@version database_version;
SELECT table_name,table_rows FROM information_schema.tables WHERE table_schema=DATABASE()
  AND table_name IN ('erp_purchase_invoice','erp_purchase_invoice_item','erp_purchase_in','erp_purchase_return','erp_purchase_price_adjust','system_menu','system_role_menu','system_tenant_package');
SELECT column_name,column_type,is_nullable FROM information_schema.columns WHERE table_schema=DATABASE()
  AND table_name='erp_purchase_invoice' AND column_name='approve_time';
SELECT COUNT(*) active_invoices,SUM(status=20) approved_invoices,SUM(status=20 AND update_time IS NULL) approved_without_backfill_source
FROM erp_purchase_invoice WHERE deleted=b'0';
SELECT COUNT(*) active_items,SUM(source_type IS NULL) manual_items,SUM(source_type=10) inbound_items,
  SUM(source_type=20) adjust_items,SUM(source_type=30) return_items FROM erp_purchase_invoice_item WHERE deleted=b'0';
SELECT permission,COUNT(*) active_count FROM system_menu WHERE deleted=b'0' AND permission IN
  ('erp:purchase-invoice:query','erp:purchase-invoice:export','erp:purchase-invoice-detail-report:query','erp:purchase-invoice-detail-report:export') GROUP BY permission;
SELECT id,parent_id,name,type,sort,path,component,component_name,permission FROM system_menu WHERE deleted=b'0'
  AND (path='invoice-detail' OR permission IN ('erp:purchase-invoice-detail-report:query','erp:purchase-invoice-detail-report:export'));
SELECT child.id,child.name,child.sort,child.path,child.permission FROM system_menu child
INNER JOIN system_menu purchase ON purchase.id=child.parent_id AND purchase.deleted=b'0' AND purchase.path='purchase'
WHERE child.deleted=b'0' ORDER BY child.sort,child.id;
SELECT COUNT(*) invalid_package_json FROM system_tenant_package WHERE deleted=b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY');
