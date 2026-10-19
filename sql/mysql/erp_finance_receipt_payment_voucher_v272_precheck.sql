-- v272 收款单手工凭证号只读预检；不包含 DDL/DML，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() target_schema, VERSION() mysql_version, @@hostname mysql_host;

SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_finance_receipt', 'erp_field_config',
                     'system_field_definition', 'system_role_field_permission')
ORDER BY table_name;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'erp_finance_receipt'
  AND column_name = 'payment_voucher_no';

SELECT COUNT(*) receipt_count
FROM erp_finance_receipt
WHERE deleted = b'0';

SELECT tenant_id, field_key, COUNT(*) row_count
FROM system_field_definition
WHERE module = 'erp_finance_receipt'
  AND field_key = 'paymentVoucherNo'
  AND deleted = b'0'
GROUP BY tenant_id, field_key
ORDER BY tenant_id;

SELECT tenant_id, field_name, COUNT(*) row_count
FROM erp_field_config
WHERE module_key = 'erp_finance_receipt'
  AND field_name = 'paymentVoucherNo'
  AND deleted = b'0'
GROUP BY tenant_id, field_name
ORDER BY tenant_id;

SELECT definition.tenant_id, definition.field_key,
       COUNT(permission_row.id) hidden_permission_count
FROM system_field_definition definition
LEFT JOIN system_role_field_permission permission_row
  ON permission_row.field_id = definition.id
 AND permission_row.tenant_id = definition.tenant_id
 AND permission_row.deleted = b'0'
WHERE definition.module = 'erp_finance_receipt'
  AND definition.field_key = 'paymentVoucherNo'
  AND definition.deleted = b'0'
GROUP BY definition.tenant_id, definition.field_key
ORDER BY definition.tenant_id;

