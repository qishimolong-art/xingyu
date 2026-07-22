-- ERP stock record department backfill (v110)
-- Scope:
--   1. Fill historical erp_stock_record.dept_id from erp_stock.dept_id.
--   2. Fallback to erp_warehouse.dept_id when no stock row exists.
--
-- Safety:
--   - Does not delete data.
--   - Only updates erp_stock_record rows whose dept_id is NULL.
--   - Does not overwrite manually corrected dept_id values.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE erp_stock_record r
JOIN erp_stock s
  ON s.product_id = r.product_id
 AND s.warehouse_id = r.warehouse_id
 AND s.deleted = b'0'
SET r.dept_id = s.dept_id
WHERE r.dept_id IS NULL
  AND s.dept_id IS NOT NULL
  AND r.deleted = b'0';

UPDATE erp_stock_record r
JOIN erp_warehouse w
  ON w.id = r.warehouse_id
 AND w.deleted = b'0'
SET r.dept_id = w.dept_id
WHERE r.dept_id IS NULL
  AND w.dept_id IS NOT NULL
  AND r.deleted = b'0';

SELECT COUNT(*) AS stock_record_missing_dept_id
FROM erp_stock_record
WHERE dept_id IS NULL
  AND deleted = b'0';
