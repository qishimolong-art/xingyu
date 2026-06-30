-- ERP stock out bill picking workflow (v78)
-- 1. Extend erp_stock_out_bill from readonly report to sale-out picking workflow support.
-- 2. Add item and pick record tables for partial picking.
-- 3. Add pick button permission without replacing existing query permission.

DROP PROCEDURE IF EXISTS add_stock_out_bill_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_stock_out_bill_column_if_missing(
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_stock_out_bill'
           AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql := CONCAT('ALTER TABLE `erp_stock_out_bill` ADD COLUMN ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_stock_out_bill_column_if_missing('pick_flag', '`pick_flag` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''pick flag'' AFTER `no`');
CALL add_stock_out_bill_column_if_missing('pick', '`pick` varchar(64) DEFAULT NULL COMMENT ''pick'' AFTER `pick_flag`');
CALL add_stock_out_bill_column_if_missing('pick_user_name', '`pick_user_name` varchar(64) DEFAULT NULL COMMENT ''pick user name'' AFTER `pick`');
CALL add_stock_out_bill_column_if_missing('source_biz_type', '`source_biz_type` tinyint DEFAULT NULL COMMENT ''source biz type'' AFTER `source_no`');
CALL add_stock_out_bill_column_if_missing('source_id', '`source_id` bigint DEFAULT NULL COMMENT ''source id'' AFTER `source_biz_type`');
CALL add_stock_out_bill_column_if_missing('total_count', '`total_count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT ''total pick count'' AFTER `loose_qty`');
CALL add_stock_out_bill_column_if_missing('picked_count', '`picked_count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT ''picked count'' AFTER `total_count`');

DROP PROCEDURE IF EXISTS add_stock_out_bill_column_if_missing;

CREATE TABLE IF NOT EXISTS `erp_stock_out_bill_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `bill_id` bigint NOT NULL COMMENT 'bill id',
  `source_id` bigint DEFAULT NULL COMMENT 'source id',
  `source_item_id` bigint DEFAULT NULL COMMENT 'source item id',
  `source_no` varchar(64) DEFAULT NULL COMMENT 'source no',
  `warehouse_id` bigint NOT NULL COMMENT 'warehouse id',
  `product_id` bigint NOT NULL COMMENT 'product id',
  `product_unit_id` bigint DEFAULT NULL COMMENT 'product unit id',
  `product_price` decimal(24, 6) DEFAULT NULL COMMENT 'product price',
  `count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'count',
  `picked_count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'picked count',
  `status` tinyint NOT NULL DEFAULT 10 COMMENT '10 wait pick, 20 partial pick, 30 done',
  `package_qty` int DEFAULT NULL COMMENT 'package qty',
  `whole_qty` int DEFAULT NULL COMMENT 'whole qty',
  `warehouse_position` varchar(128) DEFAULT NULL COMMENT 'warehouse position',
  `drawing_no` varchar(128) DEFAULT NULL COMMENT 'drawing no',
  `batch_no` varchar(128) DEFAULT NULL COMMENT 'batch no',
  `bar_code` varchar(128) DEFAULT NULL COMMENT 'bar code',
  `brand` varchar(128) DEFAULT NULL COMMENT 'brand',
  `vehicle_model` varchar(128) DEFAULT NULL COMMENT 'vehicle model',
  `origin_place` varchar(128) DEFAULT NULL COMMENT 'origin place',
  `remark` varchar(512) DEFAULT NULL COMMENT 'remark',
  `creator` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_bill_id` (`tenant_id`, `bill_id`),
  KEY `idx_tenant_source` (`tenant_id`, `source_id`, `source_item_id`),
  KEY `idx_tenant_product_warehouse` (`tenant_id`, `product_id`, `warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP stock out bill item';

CREATE TABLE IF NOT EXISTS `erp_stock_out_bill_pick_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `bill_id` bigint NOT NULL COMMENT 'bill id',
  `bill_item_id` bigint NOT NULL COMMENT 'bill item id',
  `source_id` bigint DEFAULT NULL COMMENT 'source id',
  `source_item_id` bigint DEFAULT NULL COMMENT 'source item id',
  `product_id` bigint NOT NULL COMMENT 'product id',
  `warehouse_id` bigint NOT NULL COMMENT 'warehouse id',
  `pick_count` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'pick count',
  `pick_user_id` bigint DEFAULT NULL COMMENT 'pick user id',
  `pick_user_name` varchar(64) DEFAULT NULL COMMENT 'pick user name',
  `pick_time` datetime NOT NULL COMMENT 'pick time',
  `remark` varchar(512) DEFAULT NULL COMMENT 'remark',
  `creator` varchar(64) DEFAULT '' COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT '' COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_bill_id` (`tenant_id`, `bill_id`),
  KEY `idx_tenant_bill_item_id` (`tenant_id`, `bill_item_id`),
  KEY `idx_tenant_pick_time` (`tenant_id`, `pick_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP stock out bill pick record';

SET @stock_out_bill_menu_id := (
  SELECT `id`
    FROM `system_menu`
   WHERE `permission` = ''
     AND `path` = 'outbill'
     AND `component_name` = 'ErpStockOutBill'
     AND `deleted` = b'0'
   LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
                           `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`,
                           `create_time`, `updater`, `update_time`, `deleted`)
SELECT '出仓单拣货', 'erp:stock-out-bill:pick', 3, 3, @stock_out_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1',
       NOW(), '1', NOW(), b'0'
WHERE @stock_out_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
       WHERE `permission` = 'erp:stock-out-bill:pick'
         AND `deleted` = b'0'
  );

SET @stock_out_bill_pick_menu_id := (
  SELECT `id`
    FROM `system_menu`
   WHERE `permission` = 'erp:stock-out-bill:pick'
     AND `deleted` = b'0'
   LIMIT 1
);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT role_menu.`role_id`, @stock_out_bill_pick_menu_id, '1', NOW(), '1', NOW(), b'0', role_menu.`tenant_id`
  FROM `system_role_menu` role_menu
 WHERE @stock_out_bill_pick_menu_id IS NOT NULL
   AND role_menu.`menu_id` = @stock_out_bill_menu_id
   AND role_menu.`deleted` = b'0'
   AND NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` target
        WHERE target.`role_id` = role_menu.`role_id`
          AND target.`menu_id` = @stock_out_bill_pick_menu_id
          AND target.`tenant_id` = role_menu.`tenant_id`
          AND target.`deleted` = b'0'
   );

UPDATE `system_tenant_package` tenant_package
   SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_out_bill_pick_menu_id)
 WHERE @stock_out_bill_pick_menu_id IS NOT NULL
   AND tenant_package.`deleted` = b'0'
   AND JSON_VALID(tenant_package.`menu_ids`)
   AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
   AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_out_bill_menu_id AS CHAR), '$')
   AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_out_bill_pick_menu_id AS CHAR), '$');
