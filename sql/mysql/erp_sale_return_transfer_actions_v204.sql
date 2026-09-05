-- ERP sale return transfer actions (v204).
-- Scope:
--   1. Add source sale-return fields to stock transfer-out and purchase-return items.
--   2. Add sale-return detail button permissions:
--      - erp:sale-return:transfer-out
--      - erp:sale-return:purchase-return
--
-- Safety:
--   - Idempotent DDL and menu inserts.
--   - Does not delete, overwrite, or grant role permissions.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_sale_return_source_column_if_missing_v204;
DROP PROCEDURE IF EXISTS add_erp_sale_return_source_index_if_missing_v204;

DELIMITER $$

CREATE PROCEDURE add_erp_sale_return_source_column_if_missing_v204(
    IN tableName VARCHAR(64),
    IN columnName VARCHAR(64),
    IN columnSql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND COLUMN_NAME = columnName
    ) THEN
        SET @addColumnSql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN ', columnSql);
        PREPARE stmt FROM @addColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_erp_sale_return_source_index_if_missing_v204(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64),
    IN indexSql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND INDEX_NAME = indexName
    ) THEN
        SET @addIndexSql = CONCAT('ALTER TABLE `', tableName, '` ADD INDEX `', indexName, '` ', indexSql);
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_erp_sale_return_source_column_if_missing_v204(
  'erp_stock_move_item',
  'source_sale_return_id',
  '`source_sale_return_id` BIGINT DEFAULT NULL COMMENT ''source sale return id'''
);
CALL add_erp_sale_return_source_column_if_missing_v204(
  'erp_stock_move_item',
  'source_sale_return_item_id',
  '`source_sale_return_item_id` BIGINT DEFAULT NULL COMMENT ''source sale return item id'''
);
CALL add_erp_sale_return_source_column_if_missing_v204(
  'erp_stock_move_item',
  'source_sale_return_no',
  '`source_sale_return_no` VARCHAR(64) DEFAULT NULL COMMENT ''source sale return no'''
);
CALL add_erp_sale_return_source_index_if_missing_v204(
  'erp_stock_move_item',
  'idx_erp_stock_move_item_source_sale_return_item_id',
  '(`source_sale_return_item_id`)'
);

CALL add_erp_sale_return_source_column_if_missing_v204(
  'erp_purchase_return_items',
  'source_sale_return_id',
  '`source_sale_return_id` BIGINT DEFAULT NULL COMMENT ''source sale return id'''
);
CALL add_erp_sale_return_source_column_if_missing_v204(
  'erp_purchase_return_items',
  'source_sale_return_item_id',
  '`source_sale_return_item_id` BIGINT DEFAULT NULL COMMENT ''source sale return item id'''
);
CALL add_erp_sale_return_source_column_if_missing_v204(
  'erp_purchase_return_items',
  'source_sale_return_no',
  '`source_sale_return_no` VARCHAR(64) DEFAULT NULL COMMENT ''source sale return no'''
);
CALL add_erp_sale_return_source_index_if_missing_v204(
  'erp_purchase_return_items',
  'idx_erp_purchase_return_items_source_sale_return_item_id',
  '(`source_sale_return_item_id`)'
);

DROP PROCEDURE IF EXISTS add_erp_sale_return_source_column_if_missing_v204;
DROP PROCEDURE IF EXISTS add_erp_sale_return_source_index_if_missing_v204;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_sale_return_transfer_permission_v204;

CREATE TEMPORARY TABLE tmp_erp_sale_return_transfer_permission_v204 (
  name varchar(64) NOT NULL,
  permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_sale_return_transfer_permission_v204
(`name`, `permission`, `sort`)
VALUES
('销售退货转调拨', 'erp:sale-return:transfer-out', 21),
('销售退货转采购退货', 'erp:sale-return:purchase-return', 22);

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT perm.name,
       perm.permission,
       3,
       perm.sort,
       query_menu.parent_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM tmp_erp_sale_return_transfer_permission_v204 perm
JOIN system_menu query_menu
  ON query_menu.permission = 'erp:sale-return:query'
 AND query_menu.deleted = b'0'
WHERE query_menu.parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.permission = perm.permission
      AND exists_menu.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_sale_return_transfer_permission_v204;
