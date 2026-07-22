-- ERP purchase in -> stock transfer-out (v122)
-- Scope:
--   1. Add source purchase-in fields to erp_stock_move_item.
--   2. Add purchase-in detail button permission: erp:purchase-in:transfer-out.
--
-- Safety:
--   - Idempotent DDL and seed data.
--   - Does not delete or replace existing purchase-in or stock-transfer permissions.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_stock_move_item_column_if_missing_v122;
DROP PROCEDURE IF EXISTS add_erp_stock_move_item_index_if_missing_v122;

DELIMITER $$

CREATE PROCEDURE add_erp_stock_move_item_column_if_missing_v122(
    IN columnName VARCHAR(64),
    IN columnSql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move_item'
          AND COLUMN_NAME = columnName
    ) THEN
        SET @addColumnSql = CONCAT('ALTER TABLE `erp_stock_move_item` ADD COLUMN ', columnSql);
        PREPARE stmt FROM @addColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_erp_stock_move_item_index_if_missing_v122(
    IN indexName VARCHAR(64),
    IN indexSql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move_item'
          AND INDEX_NAME = indexName
    ) THEN
        SET @addIndexSql = CONCAT('ALTER TABLE `erp_stock_move_item` ADD INDEX `', indexName, '` ', indexSql);
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_erp_stock_move_item_column_if_missing_v122('source_in_id', '`source_in_id` BIGINT DEFAULT NULL COMMENT ''source purchase in id''');
CALL add_erp_stock_move_item_column_if_missing_v122('source_in_item_id', '`source_in_item_id` BIGINT DEFAULT NULL COMMENT ''source purchase in item id''');
CALL add_erp_stock_move_item_column_if_missing_v122('source_in_no', '`source_in_no` VARCHAR(64) DEFAULT NULL COMMENT ''source purchase in no''');
CALL add_erp_stock_move_item_column_if_missing_v122('source_count', '`source_count` DECIMAL(24,6) DEFAULT NULL COMMENT ''source purchase in count snapshot''');

CALL add_erp_stock_move_item_index_if_missing_v122('idx_erp_stock_move_item_source_in_item_id', '(`source_in_item_id`)');
CALL add_erp_stock_move_item_index_if_missing_v122('idx_erp_stock_move_item_source_in_id', '(`source_in_id`)');

DROP PROCEDURE IF EXISTS add_erp_stock_move_item_column_if_missing_v122;
DROP PROCEDURE IF EXISTS add_erp_stock_move_item_index_if_missing_v122;

SET @purchase_in_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `permission` = 'erp:purchase-in:query'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @purchase_in_parent_id := (
  SELECT `parent_id`
  FROM `system_menu`
  WHERE `id` = @purchase_in_menu_id
  LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31340, '采购入库转调拨出库', 'erp:purchase-in:transfer-out', 3, 20, @purchase_in_parent_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @purchase_in_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu`
      WHERE `deleted` = b'0'
        AND `permission` = 'erp:purchase-in:transfer-out'
  );

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, `id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu`
WHERE `permission` = 'erp:purchase-in:transfer-out'
  AND `deleted` = b'0';

-- After execution: refresh menu cache or restart backend, then re-login.
