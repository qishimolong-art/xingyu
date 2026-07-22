-- 销售审批与调拨出库联动：库存预占及来源方向查询索引（增量、可重复执行）
-- MySQL 5.7/8.0 均不支持通用的 ADD INDEX IF NOT EXISTS，因此通过 information_schema 判定。
SET @current_schema := DATABASE();

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'erp_stock_lock'
          AND index_name = 'idx_stock_lock_biz_active'
    ),
    'SELECT ''idx_stock_lock_biz_active already exists''',
    'ALTER TABLE erp_stock_lock ADD INDEX idx_stock_lock_biz_active (tenant_id, biz_type, biz_id, status, deleted)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'erp_stock_move'
          AND index_name = 'idx_stock_move_source_direction'
    ),
    'SELECT ''idx_stock_move_source_direction already exists''',
    'ALTER TABLE erp_stock_move ADD INDEX idx_stock_move_source_direction (tenant_id, source_type, source_id, transfer_direction, deleted, status)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = 'erp_stock_lock'
          AND index_name = 'idx_stock_lock_biz_item_active'
    ),
    'SELECT ''idx_stock_lock_biz_item_active already exists''',
    'ALTER TABLE erp_stock_lock ADD INDEX idx_stock_lock_biz_item_active (tenant_id, biz_type, biz_id, biz_item_id, status, deleted)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
