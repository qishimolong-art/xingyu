-- ERP stock data permission support (v57)
-- Scope:
--   1. Add erp_stock.dept_id when missing.
--   2. Backfill erp_stock.dept_id from erp_warehouse.dept_id.
--   3. Add an index for data-permission filtering.
--
-- Safety:
--   - Does not delete data.
--   - Only fills erp_stock.dept_id when it is NULL.
--   - Warehouse department remains the source of stock ownership.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_stock'
      AND COLUMN_NAME = 'dept_id'
);

SET @ddl := IF(@column_exists = 0,
               'ALTER TABLE `erp_stock` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''所属部门'' AFTER `warehouse_id`',
               'SELECT ''erp_stock.dept_id already exists''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE erp_stock s
JOIN erp_warehouse w ON w.id = s.warehouse_id AND w.deleted = b'0'
SET s.dept_id = w.dept_id
WHERE s.dept_id IS NULL
  AND w.dept_id IS NOT NULL;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_stock'
      AND INDEX_NAME = 'idx_dept_id'
);

SET @ddl := IF(@index_exists = 0,
               'CREATE INDEX `idx_dept_id` ON `erp_stock` (`dept_id`)',
               'SELECT ''erp_stock.idx_dept_id already exists''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) AS stock_missing_dept_id
FROM erp_stock
WHERE dept_id IS NULL;
