-- ERP stock transfer split phase 1 (v121)
-- Scope:
--   1. Add base split fields to erp_stock_move / erp_stock_move_item.
--   2. Add independent stock transfer-out / transfer-in menus and button permissions.
--   3. Register independent field-permission definitions.
--
-- Safety:
--   - Does not delete or replace existing erp:stock-move:* permissions.
--   - Transfer-in only gets query/export/print permissions in this phase.
--   - Idempotent DDL and seed data; no broad DELETE statements.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_stock_move_column_if_missing_v121;
DROP PROCEDURE IF EXISTS add_erp_stock_move_item_column_if_missing_v121;
DROP PROCEDURE IF EXISTS add_erp_stock_move_index_if_missing_v121;

DELIMITER $$

CREATE PROCEDURE add_erp_stock_move_column_if_missing_v121(
    IN columnName VARCHAR(64),
    IN columnSql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
          AND COLUMN_NAME = columnName
    ) THEN
        SET @addColumnSql = CONCAT('ALTER TABLE `erp_stock_move` ADD COLUMN ', columnSql);
        PREPARE stmt FROM @addColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_erp_stock_move_item_column_if_missing_v121(
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

CREATE PROCEDURE add_erp_stock_move_index_if_missing_v121(
    IN indexName VARCHAR(64),
    IN indexSql VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_move'
          AND INDEX_NAME = indexName
    ) THEN
        SET @addIndexSql = CONCAT('ALTER TABLE `erp_stock_move` ADD INDEX `', indexName, '` ', indexSql);
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_erp_stock_move_column_if_missing_v121('transfer_direction', '`transfer_direction` TINYINT DEFAULT NULL COMMENT ''transfer direction: 10 transfer out, 20 transfer in''');
CALL add_erp_stock_move_column_if_missing_v121('related_move_id', '`related_move_id` BIGINT DEFAULT NULL COMMENT ''related stock transfer id''');
CALL add_erp_stock_move_column_if_missing_v121('related_move_no', '`related_move_no` VARCHAR(64) DEFAULT NULL COMMENT ''related stock transfer no''');
CALL add_erp_stock_move_column_if_missing_v121('from_dept_id', '`from_dept_id` BIGINT DEFAULT NULL COMMENT ''transfer-out dept snapshot''');
CALL add_erp_stock_move_column_if_missing_v121('to_dept_id', '`to_dept_id` BIGINT DEFAULT NULL COMMENT ''transfer-in dept snapshot''');
CALL add_erp_stock_move_column_if_missing_v121('approve_user_id', '`approve_user_id` BIGINT DEFAULT NULL COMMENT ''approve user id''');
CALL add_erp_stock_move_column_if_missing_v121('approve_time', '`approve_time` DATETIME DEFAULT NULL COMMENT ''approve time''');

CALL add_erp_stock_move_item_column_if_missing_v121('from_dept_id', '`from_dept_id` BIGINT DEFAULT NULL COMMENT ''transfer-out dept snapshot''');
CALL add_erp_stock_move_item_column_if_missing_v121('to_dept_id', '`to_dept_id` BIGINT DEFAULT NULL COMMENT ''transfer-in dept snapshot''');
CALL add_erp_stock_move_item_column_if_missing_v121('from_shelf', '`from_shelf` VARCHAR(64) DEFAULT NULL COMMENT ''transfer-out shelf''');
CALL add_erp_stock_move_item_column_if_missing_v121('batch_no', '`batch_no` VARCHAR(64) DEFAULT NULL COMMENT ''batch no''');

UPDATE `erp_stock_move`
SET `transfer_direction` = 10
WHERE `transfer_direction` IS NULL
  AND `deleted` = b'0';

CALL add_erp_stock_move_index_if_missing_v121('idx_erp_stock_move_transfer_direction', '(`transfer_direction`)');
CALL add_erp_stock_move_index_if_missing_v121('idx_erp_stock_move_related_move_id', '(`related_move_id`)');
CALL add_erp_stock_move_index_if_missing_v121('idx_erp_stock_move_related_move_no', '(`related_move_no`)');
CALL add_erp_stock_move_index_if_missing_v121('idx_erp_stock_move_from_dept_id', '(`from_dept_id`)');
CALL add_erp_stock_move_index_if_missing_v121('idx_erp_stock_move_to_dept_id', '(`to_dept_id`)');

DROP PROCEDURE IF EXISTS add_erp_stock_move_column_if_missing_v121;
DROP PROCEDURE IF EXISTS add_erp_stock_move_item_column_if_missing_v121;
DROP PROCEDURE IF EXISTS add_erp_stock_move_index_if_missing_v121;

SET @erp_root_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND `name` = 'ERP 系统'
    AND `path` = '/erp'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @stock_parent_id := (
  SELECT stock_menu.`id`
  FROM `system_menu` stock_menu
  WHERE stock_menu.`deleted` = b'0'
    AND stock_menu.`type` = 1
    AND stock_menu.`name` = '库存管理'
    AND stock_menu.`path` = 'stock'
    AND (@erp_root_id IS NULL OR stock_menu.`parent_id` = @erp_root_id)
  ORDER BY stock_menu.`id` DESC
  LIMIT 1
);

SET @transfer_out_menu_id := 31210;
SET @transfer_in_menu_id := 31230;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @transfer_out_menu_id, '调拨出库单', '', 2, 7, @stock_parent_id, 'transfer-out',
       'ep:upload', 'erp/stock/transfer-out/index', 'ErpStockTransferOut',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @transfer_in_menu_id, '调拨入库单', '', 2, 8, @stock_parent_id, 'transfer-in',
       'ep:download', 'erp/stock/transfer-in/index', 'ErpStockTransferIn',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31211, '调拨出库单查询', 'erp:stock-transfer-out:query', 3, 1, @transfer_out_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
UNION ALL
SELECT 31212, '调拨出库单创建', 'erp:stock-transfer-out:create', 3, 2, @transfer_out_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
UNION ALL
SELECT 31213, '调拨出库单更新', 'erp:stock-transfer-out:update', 3, 3, @transfer_out_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
UNION ALL
SELECT 31214, '调拨出库单删除', 'erp:stock-transfer-out:delete', 3, 4, @transfer_out_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31231, '调拨入库单查询', 'erp:stock-transfer-in:query', 3, 1, @transfer_in_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
UNION ALL
SELECT 31232, '调拨入库单导出', 'erp:stock-transfer-in:export', 3, 2, @transfer_in_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
UNION ALL
SELECT 31233, '调拨入库单打印', 'erp:stock-transfer-in:print', 3, 3, @transfer_in_menu_id, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @stock_parent_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, `id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu`
WHERE (`id` BETWEEN 31210 AND 31214 OR `id` BETWEEN 31230 AND 31233)
  AND `deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_stock_transfer_out', 'deptId', '所属部门', 'main_form', 5, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'no', '调拨出库单号', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'moveTime', '开单日期', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'transferDirection', '调拨方向', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'relatedMoveNo', '关联调拨单号', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'fromDeptId', '调出部门', 'main_form', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'toDeptId', '调入部门', 'main_form', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'remark', '备注', 'main_form', 70, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'fileUrl', '附件', 'main_form', 80, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_fromWarehouseId', '调出仓库', 'detail_item', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_fromDeptId', '调出仓库部门', 'detail_item', 205, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_toWarehouseId', '调入仓库', 'detail_item', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_toDeptId', '调入仓库部门', 'detail_item', 215, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_productId', '产品名称', 'detail_item', 220, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_productCode', '产品编码', 'detail_item', 230, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_stockCount', '库存', 'detail_item', 240, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_productBarCode', '条码', 'detail_item', 250, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_productUnitName', '单位', 'detail_item', 260, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_fromShelf', '调出货架', 'detail_item', 270, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_batchNo', '批次号', 'detail_item', 280, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_count', '数量', 'detail_item', 290, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_productPrice', '产品单价', 'detail_item', 300, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_totalPrice', '金额', 'detail_item', 310, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'item_remark', '明细备注', 'detail_item', 320, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'col_sourceNo', '来源单号', 'list_col', 400, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'col_fromDeptName', '调出部门', 'list_col', 410, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'col_toDeptName', '调入部门', 'list_col', 420, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'col_productNames', '产品信息', 'list_col', 430, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'col_productCodes', '产品编码', 'list_col', 440, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'col_totalCount', '调出数量', 'list_col', 450, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'col_totalPrice', '调出金额', 'list_col', 460, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'approveUserName', '审核人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'approveTime', '审核时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'creatorName', '创建人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'createTime', '创建时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'updaterName', '修改人', 'system_info', 940, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'updateTime', '修改时间', 'system_info', 950, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'query_keyword', '查询-关键词', 'query', 1000, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'query_fromDeptId', '查询-调出部门', 'query', 1010, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'query_toDeptId', '查询-调入部门', 'query', 1020, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_out', 'query_status', '查询-状态', 'query', 1030, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'deptId', '所属部门', 'main_form', 5, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'no', '调拨入库单号', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'moveTime', '入库日期', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'transferDirection', '调拨方向', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'relatedMoveNo', '来源出库单号', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'fromDeptId', '调出部门', 'main_form', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'toDeptId', '调入部门', 'main_form', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'remark', '备注', 'main_form', 70, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'fileUrl', '附件', 'main_form', 80, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_fromWarehouseId', '调出仓库', 'detail_item', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_fromDeptId', '调出仓库部门', 'detail_item', 205, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_toWarehouseId', '调入仓库', 'detail_item', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_toDeptId', '调入仓库部门', 'detail_item', 215, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productId', '产品名称', 'detail_item', 220, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productCode', '产品编码', 'detail_item', 230, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_stockCount', '库存', 'detail_item', 240, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productBarCode', '条码', 'detail_item', 250, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productUnitName', '单位', 'detail_item', 260, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_fromShelf', '调出货架', 'detail_item', 270, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_batchNo', '批次号', 'detail_item', 280, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_count', '调入数量', 'detail_item', 290, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productPrice', '产品单价', 'detail_item', 300, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_totalPrice', '金额', 'detail_item', 310, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_remark', '明细备注', 'detail_item', 320, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_relatedMoveNo', '来源出库单号', 'list_col', 400, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_sourceNo', '来源单号', 'list_col', 410, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_fromDeptName', '调出部门', 'list_col', 420, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_toDeptName', '调入部门', 'list_col', 430, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_productNames', '产品信息', 'list_col', 440, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_productCodes', '产品编码', 'list_col', 450, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_totalCount', '调入数量', 'list_col', 460, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_totalPrice', '调入金额', 'list_col', 470, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'approveUserName', '审核人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'approveTime', '审核时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'creatorName', '创建人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'createTime', '创建时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'updaterName', '修改人', 'system_info', 940, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'updateTime', '修改时间', 'system_info', 950, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_fromDeptId', '查询-调出部门', 'query', 1000, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_toDeptId', '查询-调入部门', 'query', 1010, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_status', '查询-状态', 'query', 1020, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
