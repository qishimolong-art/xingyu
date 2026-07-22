-- ERP stock transfer-out department permission (v129)
--
-- Scope:
--   1. Backfill immutable from/to department snapshots from warehouse data.
--   2. Add indexes for the explicit transfer-out visibility predicate.
--
-- Safety:
--   - Only fills NULL snapshot fields; existing non-NULL business data is preserved.
--   - Tenant and deleted guards are included in every backfill.
--   - No menu, role or permission seed is deleted or overwritten.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE `erp_stock_move_item` item
INNER JOIN `erp_stock_move` move_main
        ON move_main.`id` = item.`move_id`
       AND move_main.`deleted` = b'0'
INNER JOIN `erp_warehouse` warehouse
        ON warehouse.`id` = item.`from_warehouse_id`
       AND warehouse.`tenant_id` = move_main.`tenant_id`
       AND warehouse.`deleted` = b'0'
SET item.`from_dept_id` = warehouse.`dept_id`
WHERE item.`from_dept_id` IS NULL
  AND item.`deleted` = b'0'
  AND warehouse.`dept_id` IS NOT NULL;

UPDATE `erp_stock_move_item` item
INNER JOIN `erp_stock_move` move_main
        ON move_main.`id` = item.`move_id`
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
    WHERE item.`deleted` = b'0'
      AND item.`from_dept_id` IS NOT NULL
    GROUP BY item.`move_id`
) first_item ON first_item.`move_id` = move_main.`id`
INNER JOIN `erp_stock_move_item` item
        ON item.`id` = first_item.`first_item_id`
       AND item.`move_id` = move_main.`id`
       AND item.`deleted` = b'0'
SET move_main.`from_dept_id` = item.`from_dept_id`
WHERE move_main.`from_dept_id` IS NULL
  AND move_main.`deleted` = b'0'
  AND (move_main.`transfer_direction` = 10 OR move_main.`transfer_direction` IS NULL);

UPDATE `erp_stock_move` move_main
INNER JOIN (
    SELECT item.`move_id`, MIN(item.`id`) AS first_item_id
    FROM `erp_stock_move_item` item
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
  AND move_main.`deleted` = b'0'
  AND (move_main.`transfer_direction` = 10 OR move_main.`transfer_direction` IS NULL);

DROP PROCEDURE IF EXISTS add_erp_stock_transfer_out_index_if_missing_v129;

DELIMITER $$

CREATE PROCEDURE add_erp_stock_transfer_out_index_if_missing_v129(
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

CALL add_erp_stock_transfer_out_index_if_missing_v129(
    'idx_stock_transfer_out_owner_dept',
    '(`tenant_id`, `transfer_direction`, `dept_id`, `deleted`)'
);
CALL add_erp_stock_transfer_out_index_if_missing_v129(
    'idx_stock_transfer_out_from_dept',
    '(`tenant_id`, `transfer_direction`, `from_dept_id`, `deleted`)'
);

DROP PROCEDURE IF EXISTS add_erp_stock_transfer_out_index_if_missing_v129;
