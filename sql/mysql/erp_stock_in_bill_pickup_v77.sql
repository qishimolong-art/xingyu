-- ERP stock in bill pickup workflow (v77)
-- Scope:
--   1. Extend erp_stock_in_bill from read-only report to purchase-in pickup workflow support.
--   2. Add item and pickup record tables for partial pickup.
--   3. Add pickup button permission without replacing existing query/export permissions.
-- Safety:
--   - No broad DELETE.
--   - No overwrite update for existing roles or menus.
--   - Existing stock-in-bill query/export menus are kept.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_stock_in_bill_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_stock_in_bill_column_if_missing(
    IN p_column_name VARCHAR(64),
    IN p_column_def TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_in_bill'
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `erp_stock_in_bill` ADD COLUMN ', p_column_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_stock_in_bill_column_if_missing('source_biz_type', '`source_biz_type` tinyint DEFAULT NULL COMMENT ''source biz type'' AFTER `source_no`');
CALL add_stock_in_bill_column_if_missing('source_id', '`source_id` bigint DEFAULT NULL COMMENT ''source bill id'' AFTER `source_biz_type`');
CALL add_stock_in_bill_column_if_missing('total_count', '`total_count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT ''total pickup count'' AFTER `loose_qty`');
CALL add_stock_in_bill_column_if_missing('picked_count', '`picked_count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT ''picked count'' AFTER `total_count`');

DROP PROCEDURE IF EXISTS add_stock_in_bill_column_if_missing;

CREATE TABLE IF NOT EXISTS `erp_stock_in_bill_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `bill_id` bigint NOT NULL COMMENT 'stock in bill id',
  `source_id` bigint NOT NULL COMMENT 'source purchase in id',
  `source_item_id` bigint NOT NULL COMMENT 'source purchase in item id',
  `source_no` varchar(64) DEFAULT NULL COMMENT 'source bill no',
  `warehouse_id` bigint NOT NULL COMMENT 'warehouse id',
  `product_id` bigint NOT NULL COMMENT 'product id',
  `product_unit_id` bigint DEFAULT NULL COMMENT 'product unit id',
  `product_price` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'purchase price',
  `count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'total count',
  `picked_count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'picked count',
  `status` tinyint NOT NULL DEFAULT 10 COMMENT '10 wait pickup, 20 partial pickup, 30 done',
  `package_qty` int DEFAULT NULL COMMENT 'package qty',
  `whole_qty` int DEFAULT NULL COMMENT 'whole qty',
  `warehouse_position` varchar(64) DEFAULT NULL COMMENT 'warehouse position',
  `drawing_no` varchar(64) DEFAULT NULL COMMENT 'drawing no',
  `batch_no` varchar(64) DEFAULT NULL COMMENT 'batch no',
  `bar_code` varchar(64) DEFAULT NULL COMMENT 'bar code',
  `brand` varchar(64) DEFAULT NULL COMMENT 'brand',
  `vehicle_model` varchar(128) DEFAULT NULL COMMENT 'vehicle model',
  `origin_place` varchar(64) DEFAULT NULL COMMENT 'origin place',
  `remark` varchar(1024) DEFAULT NULL COMMENT 'remark',
  `creator` varchar(64) DEFAULT NULL COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT NULL COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'tenant id',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_bill` (`tenant_id`, `bill_id`),
  KEY `idx_tenant_source_item` (`tenant_id`, `source_item_id`),
  KEY `idx_tenant_product_warehouse` (`tenant_id`, `product_id`, `warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP stock in bill item';

CREATE TABLE IF NOT EXISTS `erp_stock_in_bill_pickup_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `bill_id` bigint NOT NULL COMMENT 'stock in bill id',
  `bill_item_id` bigint NOT NULL COMMENT 'stock in bill item id',
  `source_id` bigint NOT NULL COMMENT 'source purchase in id',
  `source_item_id` bigint NOT NULL COMMENT 'source purchase in item id',
  `product_id` bigint NOT NULL COMMENT 'product id',
  `warehouse_id` bigint NOT NULL COMMENT 'warehouse id',
  `pickup_count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'pickup count',
  `pickup_user_id` bigint DEFAULT NULL COMMENT 'pickup user id',
  `pickup_user_name` varchar(64) DEFAULT NULL COMMENT 'pickup user name',
  `pickup_time` datetime NOT NULL COMMENT 'pickup time',
  `remark` varchar(1024) DEFAULT NULL COMMENT 'remark',
  `creator` varchar(64) DEFAULT NULL COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT NULL COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'tenant id',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_bill` (`tenant_id`, `bill_id`),
  KEY `idx_tenant_bill_item` (`tenant_id`, `bill_item_id`),
  KEY `idx_tenant_pickup_time` (`tenant_id`, `pickup_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP stock in bill pickup record';

SET @stock_in_bill_stock_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/stock/inbill/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @stock_in_bill_purchase_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/purchase/inbill/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @stock_in_bill_menu_id := COALESCE(@stock_in_bill_stock_menu_id, @stock_in_bill_purchase_menu_id);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '入仓单提货', 'erp:stock-in-bill:pickup', 3, 3, @stock_in_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_in_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:stock-in-bill:pickup'
      AND `deleted` = b'0'
  );

SET @stock_in_bill_pickup_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:stock-in-bill:pickup'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @stock_in_bill_query_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:stock-in-bill:query'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT role_menu.`role_id`, @stock_in_bill_pickup_menu_id, '1', NOW(), '1', NOW(), b'0', role_menu.`tenant_id`
FROM `system_role_menu` role_menu
JOIN `system_menu` owned_menu
  ON owned_menu.`id` = role_menu.`menu_id`
 AND owned_menu.`deleted` = b'0'
WHERE @stock_in_bill_pickup_menu_id IS NOT NULL
  AND role_menu.`deleted` = b'0'
  AND (
    owned_menu.`permission` = 'erp:stock-in-bill:query'
    OR owned_menu.`id` IN (@stock_in_bill_stock_menu_id, @stock_in_bill_purchase_menu_id)
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` target
    WHERE target.`role_id` = role_menu.`role_id`
      AND target.`menu_id` = @stock_in_bill_pickup_menu_id
      AND target.`tenant_id` = role_menu.`tenant_id`
      AND target.`deleted` = b'0'
  );

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_in_bill_pickup_menu_id)
WHERE @stock_in_bill_pickup_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND (
    (@stock_in_bill_stock_menu_id IS NOT NULL
      AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_stock_menu_id AS CHAR), '$'))
    OR (@stock_in_bill_purchase_menu_id IS NOT NULL
      AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_purchase_menu_id AS CHAR), '$'))
    OR (@stock_in_bill_query_menu_id IS NOT NULL
      AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_query_menu_id AS CHAR), '$'))
  )
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_pickup_menu_id AS CHAR), '$');

SELECT `id`, `name`, `permission`, `parent_id`, `component`, `deleted`
FROM `system_menu`
WHERE `component` IN ('erp/stock/inbill/index', 'erp/purchase/inbill/index')
   OR `permission` IN ('erp:stock-in-bill:query', 'erp:stock-in-bill:export', 'erp:stock-in-bill:pickup')
ORDER BY `type`, `sort`, `id`;
