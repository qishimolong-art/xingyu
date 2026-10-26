-- v284 precheck: 只读检查仓库移货下单确认与拣货自动审核迁移前状态。
SET @current_schema := DATABASE();

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @current_schema
  AND ((TABLE_NAME = 'erp_warehouse_move'
        AND COLUMN_NAME IN ('confirm_user_id','confirm_time'))
    OR (TABLE_NAME = 'erp_sale_pick_delivery_item'
        AND COLUMN_NAME = 'warehouse_move_item_id'))
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT status, COUNT(*) AS row_count
FROM erp_warehouse_move
WHERE deleted = b'0'
GROUP BY status
ORDER BY status;

SELECT source_type, source_id, COUNT(*) AS duplicate_count
FROM erp_sale_pick_delivery_order
WHERE deleted = b'0' AND source_type = 80 AND source_id IS NOT NULL
GROUP BY source_type, source_id
HAVING COUNT(*) > 1;

SELECT permission, COUNT(*) AS active_count
FROM system_menu
WHERE deleted = b'0'
  AND permission IN ('erp:warehouse-move:update-status',
                     'erp:warehouse-move:confirm',
                     'erp:warehouse-move:cancel-confirm')
GROUP BY permission;

SELECT rm.tenant_id, COUNT(DISTINCT rm.role_id) AS authorized_role_count
FROM system_role_menu rm
INNER JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = b'0'
WHERE rm.deleted = b'0' AND m.permission = 'erp:warehouse-move:update-status'
GROUP BY rm.tenant_id
ORDER BY rm.tenant_id;
