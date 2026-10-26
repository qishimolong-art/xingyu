-- v282 precheck: 只读检查销售退货下单确认与拣货自动审批迁移前状态。
SET @current_schema := DATABASE();

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @current_schema
  AND ((TABLE_NAME = 'erp_sale_return'
        AND COLUMN_NAME IN ('confirm_user_id','confirm_time','frozen_cost_basis_signature'))
    OR (TABLE_NAME = 'erp_sale_pick_delivery_item' AND COLUMN_NAME = 'sale_return_item_id')
    OR (TABLE_NAME = 'erp_sale_return_current_cost_confirmation' AND COLUMN_NAME = 'stock_basis'))
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT status, COUNT(*) AS row_count
FROM erp_sale_return
WHERE deleted = b'0'
GROUP BY status
ORDER BY status;

SELECT source_type, source_id, COUNT(*) AS duplicate_count
FROM erp_sale_pick_delivery_order
WHERE deleted = b'0' AND source_type IS NOT NULL AND source_id IS NOT NULL
GROUP BY source_type, source_id
HAVING COUNT(*) > 1;

SELECT permission, COUNT(*) AS active_count
FROM system_menu
WHERE deleted = b'0'
  AND permission IN ('erp:sale-return:update-status','erp:sale-return:confirm','erp:sale-return:cancel-confirm')
GROUP BY permission;

