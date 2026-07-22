-- ERP stock transfer-in department permission (v130)
--
-- Scope:
--   1. Backfill the target-department snapshot used by transfer-in visibility.
--   2. Add an index for the explicit transfer-in target-department predicate.
--
-- Safety:
--   - Only fills NULL snapshot fields; existing non-NULL business data is preserved.
--   - Tenant and deleted guards are included in every backfill.
--   - No document status, stock record, menu, role or permission seed is changed.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE `erp_stock_move_item` item
INNER JOIN `erp_stock_move` move_main
        ON move_main.`id` = item.`move_id`
       AND move_main.`transfer_direction` = 20
       AND move_main.`deleted` = b'0'
INNER JOIN `erp_warehouse` warehouse
        ON warehouse.`id` = item.`to_warehouse_id`
       AND warehouse.`tenant_id` = move_main.`tenant_id`
       AND warehouse.`deleted` = b'0'
SET item.`to_dept_id` = warehouse.`dept_id`
WHERE item.`to_dept_id` IS NULL
  AND item.`deleted` = b'0'
  AND warehouse.`dept_id` IS NOT NULL;

UPDATE `erp_stock_move` move_main
INNER JOIN (
    SELECT item.`move_id`, MIN(item.`id`) AS first_item_id
    FROM `erp_stock_move_item` item
    INNER JOIN `erp_stock_move` move_filter
            ON move_filter.`id` = item.`move_id`
           AND move_filter.`transfer_direction` = 20
           AND move_filter.`deleted` = b'0'
    WHERE item.`deleted` = b'0'
      AND item.`to_dept_id` IS NOT NULL
    GROUP BY item.`move_id`
) first_item ON first_item.`move_id` = move_main.`id`
INNER JOIN `erp_stock_move_item` item
        ON item.`id` = first_item.`first_item_id`
       AND item.`move_id` = move_main.`id`
       AND item.`deleted` = b'0'
SET move_main.`to_dept_id` = item.`to_dept_id`
WHERE move_main.`to_dept_id` IS NULL
  AND move_main.`transfer_direction` = 20
  AND move_main.`deleted` = b'0';

UPDATE `erp_stock_move` transfer_in
INNER JOIN `erp_stock_move` transfer_out
        ON transfer_out.`id` = transfer_in.`related_move_id`
       AND transfer_out.`tenant_id` = transfer_in.`tenant_id`
       AND transfer_out.`deleted` = b'0'
SET transfer_in.`to_dept_id` = transfer_out.`to_dept_id`
WHERE transfer_in.`to_dept_id` IS NULL
  AND transfer_in.`transfer_direction` = 20
  AND transfer_in.`deleted` = b'0'
  AND transfer_out.`to_dept_id` IS NOT NULL;

DROP PROCEDURE IF EXISTS add_erp_stock_transfer_in_index_if_missing_v130;

DELIMITER $$

CREATE PROCEDURE add_erp_stock_transfer_in_index_if_missing_v130(
    IN indexName VARCHAR(64),
    IN indexColumns VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
          AND INDEX_NAME = indexName
    ) THEN
        SET @addIndexSql = CONCAT('ALTER TABLE `erp_stock_move` ADD INDEX `', indexName, '` ', indexColumns);
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_erp_stock_transfer_in_index_if_missing_v130(
    'idx_stock_transfer_in_to_dept',
    '(`tenant_id`, `transfer_direction`, `to_dept_id`, `deleted`)'
);

DROP PROCEDURE IF EXISTS add_erp_stock_transfer_in_index_if_missing_v130;
