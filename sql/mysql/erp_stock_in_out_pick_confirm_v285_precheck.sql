-- v285 precheck: 只读检查其它入库/其它出库下单确认与拣货自动审批迁移前状态。
SET @current_schema := DATABASE();

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @current_schema
  AND ((TABLE_NAME IN ('erp_stock_in', 'erp_stock_out')
        AND COLUMN_NAME IN ('confirm_user_id', 'confirm_time', 'approve_user_id', 'approve_time'))
    OR (TABLE_NAME = 'erp_sale_pick_delivery_item'
        AND COLUMN_NAME IN ('stock_in_item_id', 'stock_out_item_id')))
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT 'erp_stock_in' AS table_name, status, COUNT(*) AS row_count
FROM erp_stock_in WHERE deleted = b'0' GROUP BY status
UNION ALL
SELECT 'erp_stock_out', status, COUNT(*)
FROM erp_stock_out WHERE deleted = b'0' GROUP BY status
ORDER BY table_name, status;

SELECT source_type, source_id, COUNT(*) AS duplicate_count
FROM erp_sale_pick_delivery_order
WHERE deleted = b'0' AND source_type IN (90, 100) AND source_id IS NOT NULL
GROUP BY source_type, source_id
HAVING COUNT(*) > 1;

SELECT permission, COUNT(*) AS active_count
FROM system_menu
WHERE deleted = b'0'
  AND permission IN ('erp:stock-in:update-status', 'erp:stock-in:confirm',
                     'erp:stock-in:cancel-confirm', 'erp:stock-out:update-status',
                     'erp:stock-out:confirm', 'erp:stock-out:cancel-confirm')
GROUP BY permission;

SELECT m.permission, rm.tenant_id, COUNT(DISTINCT rm.role_id) AS authorized_role_count
FROM system_role_menu rm
INNER JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = b'0'
WHERE rm.deleted = b'0'
  AND m.permission IN ('erp:stock-in:update-status', 'erp:stock-out:update-status')
GROUP BY m.permission, rm.tenant_id
ORDER BY m.permission, rm.tenant_id;
