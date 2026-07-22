-- ERP stock transfer-in document support (v120)
-- Scope:
--   - Reuse erp_stock_move / erp_stock_move_item for transfer-in mirror documents.
--   - Add read-only stock-transfer-in menu and field-permission definitions.
-- Safety:
--   - Idempotent column/index/menu/field inserts.
--   - Compatible with MySQL 5.7 / 8.0.x.
--   - No broad DELETE.
--   - Existing stock-move permissions are not replaced.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS erp_stock_transfer_in_v120_apply;

DELIMITER $$
CREATE PROCEDURE erp_stock_transfer_in_v120_apply()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_stock_move' AND COLUMN_NAME = 'transfer_direction') THEN
    ALTER TABLE `erp_stock_move`
      ADD COLUMN `transfer_direction` TINYINT NULL DEFAULT 10 COMMENT '调拨方向：10调拨出库 20调拨入库' AFTER `dept_id`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_stock_move' AND COLUMN_NAME = 'related_move_id') THEN
    ALTER TABLE `erp_stock_move`
      ADD COLUMN `related_move_id` BIGINT NULL COMMENT '关联调拨单ID' AFTER `transfer_direction`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_stock_move' AND COLUMN_NAME = 'related_move_no') THEN
    ALTER TABLE `erp_stock_move`
      ADD COLUMN `related_move_no` VARCHAR(64) NULL COMMENT '关联调拨单号' AFTER `related_move_id`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_stock_move' AND COLUMN_NAME = 'from_dept_id') THEN
    ALTER TABLE `erp_stock_move`
      ADD COLUMN `from_dept_id` BIGINT NULL COMMENT '调出部门ID' AFTER `related_move_no`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_stock_move' AND COLUMN_NAME = 'to_dept_id') THEN
    ALTER TABLE `erp_stock_move`
      ADD COLUMN `to_dept_id` BIGINT NULL COMMENT '调入部门ID' AFTER `from_dept_id`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_stock_move' AND INDEX_NAME = 'idx_stock_move_transfer_direction') THEN
    CREATE INDEX `idx_stock_move_transfer_direction` ON `erp_stock_move` (`transfer_direction`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_stock_move' AND INDEX_NAME = 'idx_stock_move_related_move') THEN
    CREATE INDEX `idx_stock_move_related_move` ON `erp_stock_move` (`related_move_id`, `transfer_direction`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_stock_move' AND INDEX_NAME = 'idx_stock_move_from_to_dept') THEN
    CREATE INDEX `idx_stock_move_from_to_dept` ON `erp_stock_move` (`from_dept_id`, `to_dept_id`);
  END IF;
END$$
DELIMITER ;

CALL erp_stock_transfer_in_v120_apply();
DROP PROCEDURE IF EXISTS erp_stock_transfer_in_v120_apply;

UPDATE `erp_stock_move`
SET `transfer_direction` = 10
WHERE `transfer_direction` IS NULL
  AND `deleted` = b'0';

SET @stock_parent_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `id` = 2583 AND `deleted` = b'0'
  LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31220, '调拨入库单', '', 2, 6, @stock_parent_menu_id, 'transfer-in', 'ep:folder-add',
       'erp/stock/transfer-in/index', 'ErpStockTransferIn',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_parent_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
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
VALUES
(31221, '调拨入库单查询', 'erp:stock-transfer-in:query', 3, 1, 31220, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31222, '调拨入库单导出', 'erp:stock-transfer-in:export', 3, 2, 31220, '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
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
VALUES
(1, 31220, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31221, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31222, '1', NOW(), '1', NOW(), b'0', 1);

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_stock_transfer_in', 'no', '调拨入库单号', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'relatedMoveNo', '来源出库单号', 'main_form', 15, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'sourceNo', '来源单号', 'main_form', 16, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'moveTime', '入库日期', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'deptId', '所属部门', 'main_form', 25, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'fromDeptName', '调出部门', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'toDeptName', '调入部门', 'main_form', 35, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'remark', '备注', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'fileUrl', '附件', 'main_form', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'creatorName', '创建人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'createTime', '创建时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'updaterName', '修改人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'updateTime', '修改时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'approveUserName', '审核人', 'system_info', 940, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'approveTime', '审核时间', 'system_info', 950, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productCode', '编码', 'detail_item', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productName', '配件名称', 'detail_item', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_count', '调入数量', 'detail_item', 220, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productPrice', '价格', 'detail_item', 230, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_totalPrice', '金额', 'detail_item', 240, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_fromWarehouseName', '调出仓库', 'detail_item', 250, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_toWarehouseName', '调入仓库', 'detail_item', 260, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productBarCode', '条码', 'detail_item', 270, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_productUnitName', '单位', 'detail_item', 280, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'item_remark', '备注', 'detail_item', 290, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_no', '调拨入库单号', 'query', 1000, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_relatedMoveNo', '来源出库单号', 'query', 1010, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_sourceNo', '来源单号', 'query', 1020, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_moveTime', '入库日期', 'query', 1030, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_fromDeptId', '调出部门', 'query', 1040, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_toDeptId', '调入部门', 'query', 1050, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_fromWarehouseId', '调出仓库', 'query', 1060, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_toWarehouseId', '调入仓库', 'query', 1070, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_productId', '产品', 'query', 1080, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_status', '状态', 'query', 1090, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_creator', '创建人', 'query', 1100, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'query_remark', '备注', 'query', 1110, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_no', '调拨入库单号', 'list_col', 2000, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_relatedMoveNo', '来源出库单号', 'list_col', 2010, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_sourceNo', '来源单号', 'list_col', 2020, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_moveTime', '入库日期', 'list_col', 2030, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_fromDeptName', '调出部门', 'list_col', 2040, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_toDeptName', '调入部门', 'list_col', 2050, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_productNames', '产品信息', 'list_col', 2060, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_productCodes', '产品编码', 'list_col', 2070, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_totalCount', '调入数量', 'list_col', 2080, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_totalPrice', '调入金额', 'list_col', 2090, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_status', '状态', 'list_col', 2100, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_remark', '备注', 'list_col', 2110, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_creatorName', '创建人', 'list_col', 2120, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_transfer_in', 'col_createTime', '创建时间', 'list_col', 2130, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
