-- Product-stock department visibility and transfer-in pending linkage (v131)
--
-- Scope:
--   1. Align stock department snapshots with the warehouse that owns the stock.
--   2. Backfill one PROCESS transfer-in mirror for historical PROCESS transfer-outs.
--   3. Create missing zero-stock rows for target warehouses so pending quantities are visible.
--   4. Repoint uniquely matchable historical MOVE_IN records to the transfer-in mirror.
--
-- Safety:
--   - Every data change is tenant/deleted/status constrained.
--   - Inserts are guarded by NOT EXISTS and are safe to run repeatedly.
--   - No DELETE, menu, role, button, field-permission or user-warehouse assignment is changed.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Stock belongs to the warehouse department. Only NULL/mismatched snapshots are corrected.
UPDATE `erp_stock` stock
INNER JOIN `erp_warehouse` warehouse
        ON warehouse.`id` = stock.`warehouse_id`
       AND warehouse.`tenant_id` = stock.`tenant_id`
       AND warehouse.`deleted` = b'0'
SET stock.`dept_id` = warehouse.`dept_id`
WHERE stock.`deleted` = b'0'
  AND warehouse.`dept_id` IS NOT NULL
  AND (stock.`dept_id` IS NULL OR stock.`dept_id` <> warehouse.`dept_id`);

-- Create the read-only PROCESS transfer-in main document before the transfer-out is approved.
INSERT INTO `erp_stock_move`
(`no`, `dept_id`, `transfer_direction`, `related_move_id`, `related_move_no`,
 `from_dept_id`, `to_dept_id`, `move_time`, `source_type`, `source_id`, `source_no`,
 `total_count`, `total_price`, `status`, `approve_user_id`, `approve_time`, `remark`, `file_url`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT CONCAT('DCRKBF', transfer_out.`tenant_id`, '-', transfer_out.`id`),
       warehouse_snapshot.`to_dept_id`, 20, transfer_out.`id`, transfer_out.`no`,
       warehouse_snapshot.`from_dept_id`, warehouse_snapshot.`to_dept_id`, transfer_out.`move_time`,
       transfer_out.`source_type`, transfer_out.`source_id`, transfer_out.`source_no`,
       transfer_out.`total_count`, transfer_out.`total_price`, 10, NULL, NULL,
       transfer_out.`remark`, transfer_out.`file_url`, transfer_out.`creator`,
       COALESCE(transfer_out.`create_time`, NOW()), transfer_out.`updater`, NOW(), b'0', transfer_out.`tenant_id`
FROM `erp_stock_move` transfer_out
INNER JOIN (
    SELECT item.`move_id`, item.`tenant_id`,
           MIN(from_warehouse.`dept_id`) AS from_dept_id,
           MIN(to_warehouse.`dept_id`) AS to_dept_id
    FROM `erp_stock_move_item` item
    INNER JOIN `erp_warehouse` from_warehouse
            ON from_warehouse.`id` = item.`from_warehouse_id`
           AND from_warehouse.`tenant_id` = item.`tenant_id`
           AND from_warehouse.`deleted` = b'0'
    INNER JOIN `erp_warehouse` to_warehouse
            ON to_warehouse.`id` = item.`to_warehouse_id`
           AND to_warehouse.`tenant_id` = item.`tenant_id`
           AND to_warehouse.`deleted` = b'0'
    WHERE item.`deleted` = b'0'
    GROUP BY item.`move_id`, item.`tenant_id`
) warehouse_snapshot
        ON warehouse_snapshot.`move_id` = transfer_out.`id`
       AND warehouse_snapshot.`tenant_id` = transfer_out.`tenant_id`
WHERE transfer_out.`deleted` = b'0'
  AND transfer_out.`status` = 10
  AND (transfer_out.`transfer_direction` = 10 OR transfer_out.`transfer_direction` IS NULL)
  AND warehouse_snapshot.`to_dept_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `erp_stock_move` transfer_in
      WHERE transfer_in.`tenant_id` = transfer_out.`tenant_id`
        AND transfer_in.`deleted` = b'0'
        AND transfer_in.`transfer_direction` = 20
        AND transfer_in.`related_move_id` = transfer_out.`id`
  );

-- Copy items only for a mirror that has no active item yet; existing mirror data is not overwritten.
INSERT INTO `erp_stock_move_item`
(`move_id`, `from_warehouse_id`, `to_warehouse_id`, `from_dept_id`, `to_dept_id`,
 `product_id`, `product_unit_id`, `product_price`, `count`, `total_price`, `remark`, `from_shelf`,
 `batch_no`, `source_in_id`, `source_in_item_id`, `source_in_no`, `source_count`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT transfer_in.`id`, out_item.`from_warehouse_id`, out_item.`to_warehouse_id`,
       from_warehouse.`dept_id`, to_warehouse.`dept_id`, out_item.`product_id`, out_item.`product_unit_id`,
       out_item.`product_price`, out_item.`count`, out_item.`total_price`, out_item.`remark`, out_item.`from_shelf`,
       out_item.`batch_no`, out_item.`source_in_id`, out_item.`source_in_item_id`, out_item.`source_in_no`,
       out_item.`source_count`, out_item.`creator`, COALESCE(out_item.`create_time`, NOW()),
       out_item.`updater`, NOW(), b'0', out_item.`tenant_id`
FROM `erp_stock_move` transfer_in
INNER JOIN `erp_stock_move` transfer_out
        ON transfer_out.`id` = transfer_in.`related_move_id`
       AND transfer_out.`tenant_id` = transfer_in.`tenant_id`
       AND transfer_out.`deleted` = b'0'
       AND transfer_out.`status` = 10
       AND (transfer_out.`transfer_direction` = 10 OR transfer_out.`transfer_direction` IS NULL)
INNER JOIN `erp_stock_move_item` out_item
        ON out_item.`move_id` = transfer_out.`id`
       AND out_item.`tenant_id` = transfer_out.`tenant_id`
       AND out_item.`deleted` = b'0'
INNER JOIN `erp_warehouse` from_warehouse
        ON from_warehouse.`id` = out_item.`from_warehouse_id`
       AND from_warehouse.`tenant_id` = out_item.`tenant_id`
       AND from_warehouse.`deleted` = b'0'
INNER JOIN `erp_warehouse` to_warehouse
        ON to_warehouse.`id` = out_item.`to_warehouse_id`
       AND to_warehouse.`tenant_id` = out_item.`tenant_id`
       AND to_warehouse.`deleted` = b'0'
WHERE transfer_in.`deleted` = b'0'
  AND transfer_in.`transfer_direction` = 20
  AND transfer_in.`status` = 10
  AND NOT EXISTS (
      SELECT 1 FROM `erp_stock_move_item` existing_item
      WHERE existing_item.`move_id` = transfer_in.`id`
        AND existing_item.`tenant_id` = transfer_in.`tenant_id`
        AND existing_item.`deleted` = b'0'
  );

-- Keep both directions linked after a successful backfill.
UPDATE `erp_stock_move` transfer_out
INNER JOIN `erp_stock_move` transfer_in
        ON transfer_in.`related_move_id` = transfer_out.`id`
       AND transfer_in.`tenant_id` = transfer_out.`tenant_id`
       AND transfer_in.`transfer_direction` = 20
       AND transfer_in.`deleted` = b'0'
SET transfer_out.`related_move_id` = transfer_in.`id`,
    transfer_out.`related_move_no` = transfer_in.`no`
WHERE transfer_out.`deleted` = b'0'
  AND transfer_out.`status` = 10
  AND (transfer_out.`transfer_direction` = 10 OR transfer_out.`transfer_direction` IS NULL)
  AND (transfer_out.`related_move_id` IS NULL OR transfer_out.`related_move_id` = transfer_in.`id`);

-- A normal stock row is the display carrier for both ordinary and direct warehouses.
INSERT INTO `erp_stock`
(`product_id`, `warehouse_id`, `dept_id`, `count`, `lock_count`, `cost_price`, `cost_amount`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT in_item.`product_id`, in_item.`to_warehouse_id`, to_warehouse.`dept_id`,
       0, 0, 0, 0, MIN(transfer_in.`creator`), NOW(), MIN(transfer_in.`updater`), NOW(),
       b'0', transfer_in.`tenant_id`
FROM `erp_stock_move` transfer_in
INNER JOIN `erp_stock_move_item` in_item
        ON in_item.`move_id` = transfer_in.`id`
       AND in_item.`tenant_id` = transfer_in.`tenant_id`
       AND in_item.`deleted` = b'0'
INNER JOIN `erp_warehouse` to_warehouse
        ON to_warehouse.`id` = in_item.`to_warehouse_id`
       AND to_warehouse.`tenant_id` = transfer_in.`tenant_id`
       AND to_warehouse.`deleted` = b'0'
WHERE transfer_in.`deleted` = b'0'
  AND transfer_in.`transfer_direction` = 20
  AND transfer_in.`status` = 10
  AND NOT EXISTS (
      SELECT 1 FROM `erp_stock` stock
      WHERE stock.`tenant_id` = transfer_in.`tenant_id`
        AND stock.`deleted` = b'0'
        AND stock.`product_id` = in_item.`product_id`
        AND stock.`warehouse_id` = in_item.`to_warehouse_id`
  )
GROUP BY transfer_in.`tenant_id`, in_item.`product_id`, in_item.`to_warehouse_id`, to_warehouse.`dept_id`;

-- Historical approved MOVE_IN rows are changed only when one mirror item matches exactly.
UPDATE `erp_stock_record` stock_record
INNER JOIN `erp_stock_move` transfer_out
        ON transfer_out.`id` = stock_record.`biz_id`
       AND transfer_out.`tenant_id` = stock_record.`tenant_id`
       AND transfer_out.`deleted` = b'0'
       AND (transfer_out.`transfer_direction` = 10 OR transfer_out.`transfer_direction` IS NULL)
INNER JOIN `erp_stock_move_item` out_item
        ON out_item.`id` = stock_record.`biz_item_id`
       AND out_item.`move_id` = transfer_out.`id`
       AND out_item.`tenant_id` = stock_record.`tenant_id`
       AND out_item.`deleted` = b'0'
INNER JOIN `erp_stock_move` transfer_in
        ON transfer_in.`related_move_id` = transfer_out.`id`
       AND transfer_in.`tenant_id` = stock_record.`tenant_id`
       AND transfer_in.`transfer_direction` = 20
       AND transfer_in.`deleted` = b'0'
INNER JOIN (
    SELECT item.`tenant_id`, item.`move_id`, item.`product_id`, item.`from_warehouse_id`, item.`to_warehouse_id`,
           MIN(item.`id`) AS item_id, COUNT(*) AS match_count
    FROM `erp_stock_move_item` item
    WHERE item.`deleted` = b'0'
    GROUP BY item.`tenant_id`, item.`move_id`, item.`product_id`,
             item.`from_warehouse_id`, item.`to_warehouse_id`
) unique_in_item
        ON unique_in_item.`tenant_id` = transfer_in.`tenant_id`
       AND unique_in_item.`move_id` = transfer_in.`id`
       AND unique_in_item.`product_id` = out_item.`product_id`
       AND unique_in_item.`from_warehouse_id` = out_item.`from_warehouse_id`
       AND unique_in_item.`to_warehouse_id` = out_item.`to_warehouse_id`
       AND unique_in_item.`match_count` = 1
SET stock_record.`biz_id` = transfer_in.`id`,
    stock_record.`biz_item_id` = unique_in_item.`item_id`,
    stock_record.`biz_no` = transfer_in.`no`
WHERE stock_record.`deleted` = b'0'
  AND stock_record.`biz_type` = 30;

-- Deployment diagnostics: every result set should be empty after the application and migration are aligned.
SELECT stock.`id`, stock.`tenant_id`, stock.`product_id`, stock.`warehouse_id`,
       stock.`dept_id` AS stock_dept_id, warehouse.`dept_id` AS warehouse_dept_id
FROM `erp_stock` stock
INNER JOIN `erp_warehouse` warehouse
        ON warehouse.`id` = stock.`warehouse_id`
       AND warehouse.`tenant_id` = stock.`tenant_id`
       AND warehouse.`deleted` = b'0'
WHERE stock.`deleted` = b'0'
  AND NOT (stock.`dept_id` <=> warehouse.`dept_id`);

SELECT transfer_out.`id`, transfer_out.`tenant_id`, transfer_out.`no`
FROM `erp_stock_move` transfer_out
WHERE transfer_out.`deleted` = b'0'
  AND transfer_out.`status` = 10
  AND (transfer_out.`transfer_direction` = 10 OR transfer_out.`transfer_direction` IS NULL)
  AND NOT EXISTS (
      SELECT 1 FROM `erp_stock_move` transfer_in
      WHERE transfer_in.`tenant_id` = transfer_out.`tenant_id`
        AND transfer_in.`deleted` = b'0'
        AND transfer_in.`transfer_direction` = 20
        AND transfer_in.`related_move_id` = transfer_out.`id`
  );

SELECT transfer_in.`tenant_id`, transfer_in.`related_move_id`, COUNT(*) AS transfer_in_count
FROM `erp_stock_move` transfer_in
WHERE transfer_in.`deleted` = b'0'
  AND transfer_in.`transfer_direction` = 20
  AND transfer_in.`related_move_id` IS NOT NULL
GROUP BY transfer_in.`tenant_id`, transfer_in.`related_move_id`
HAVING COUNT(*) > 1;

SELECT transfer_out.`tenant_id`, transfer_out.`id` AS transfer_out_id,
       transfer_out.`related_move_id` AS linked_transfer_in_id, transfer_in.`id` AS expected_transfer_in_id
FROM `erp_stock_move` transfer_out
INNER JOIN `erp_stock_move` transfer_in
        ON transfer_in.`tenant_id` = transfer_out.`tenant_id`
       AND transfer_in.`related_move_id` = transfer_out.`id`
       AND transfer_in.`transfer_direction` = 20
       AND transfer_in.`deleted` = b'0'
WHERE transfer_out.`deleted` = b'0'
  AND (transfer_out.`transfer_direction` = 10 OR transfer_out.`transfer_direction` IS NULL)
  AND NOT (transfer_out.`related_move_id` <=> transfer_in.`id`);

SELECT transfer_out.`tenant_id`, transfer_out.`id` AS transfer_out_id,
       transfer_in.`id` AS transfer_in_id,
       (SELECT COUNT(*) FROM `erp_stock_move_item` out_item
         WHERE out_item.`tenant_id` = transfer_out.`tenant_id`
           AND out_item.`move_id` = transfer_out.`id` AND out_item.`deleted` = b'0') AS transfer_out_item_count,
       (SELECT COUNT(*) FROM `erp_stock_move_item` in_item
         WHERE in_item.`tenant_id` = transfer_in.`tenant_id`
           AND in_item.`move_id` = transfer_in.`id` AND in_item.`deleted` = b'0') AS transfer_in_item_count
FROM `erp_stock_move` transfer_out
INNER JOIN `erp_stock_move` transfer_in
        ON transfer_in.`tenant_id` = transfer_out.`tenant_id`
       AND transfer_in.`related_move_id` = transfer_out.`id`
       AND transfer_in.`transfer_direction` = 20
       AND transfer_in.`deleted` = b'0'
WHERE transfer_out.`deleted` = b'0'
  AND transfer_out.`status` = 10
  AND (transfer_out.`transfer_direction` = 10 OR transfer_out.`transfer_direction` IS NULL)
HAVING transfer_out_item_count <> transfer_in_item_count;

SELECT stock.`tenant_id`, stock.`product_id`, stock.`warehouse_id`, COUNT(*) AS stock_row_count
FROM `erp_stock` stock
WHERE stock.`deleted` = b'0'
GROUP BY stock.`tenant_id`, stock.`product_id`, stock.`warehouse_id`
HAVING COUNT(*) > 1;
