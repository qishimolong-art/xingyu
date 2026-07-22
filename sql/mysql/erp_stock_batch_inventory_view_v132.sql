-- ERP 产品库存按批次展开支持（v132）
-- 1. 确保库存流水保留批次号并补充批次余额查询索引。
-- 2. 仅对批次号为空的历史流水，从可追溯的来源明细回填批次号。
-- 3. 不删除、不覆盖已有批次号，不修改菜单、角色或权限数据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS upgrade_erp_stock_batch_inventory_v132;

DELIMITER //
CREATE PROCEDURE upgrade_erp_stock_batch_inventory_v132()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_record'
          AND COLUMN_NAME = 'batch_no'
    ) THEN
        ALTER TABLE `erp_stock_record`
            ADD COLUMN `batch_no` VARCHAR(128) DEFAULT NULL COMMENT '批次号'
            AFTER `dept_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_stock_record'
          AND INDEX_NAME = 'idx_tenant_stock_batch'
    ) THEN
        ALTER TABLE `erp_stock_record`
            ADD INDEX `idx_tenant_stock_batch`
                (`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `batch_no`);
    END IF;
END //
DELIMITER ;

CALL upgrade_erp_stock_batch_inventory_v132();
DROP PROCEDURE IF EXISTS upgrade_erp_stock_batch_inventory_v132;

-- 采购入库/作废流水（70/71）
UPDATE `erp_stock_record` sr
INNER JOIN `erp_purchase_in_items` source_item
        ON source_item.`id` = sr.`biz_item_id`
       AND source_item.`tenant_id` = sr.`tenant_id`
       AND source_item.`deleted` = b'0'
SET sr.`batch_no` = TRIM(source_item.`batch_no`)
WHERE sr.`deleted` = b'0'
  AND sr.`biz_type` IN (70, 71)
  AND (sr.`batch_no` IS NULL OR TRIM(sr.`batch_no`) = '')
  AND source_item.`batch_no` IS NOT NULL
  AND TRIM(source_item.`batch_no`) <> '';

-- 采购退货/作废流水（80/81）
UPDATE `erp_stock_record` sr
INNER JOIN `erp_purchase_return_items` source_item
        ON source_item.`id` = sr.`biz_item_id`
       AND source_item.`tenant_id` = sr.`tenant_id`
       AND source_item.`deleted` = b'0'
SET sr.`batch_no` = TRIM(source_item.`batch_no`)
WHERE sr.`deleted` = b'0'
  AND sr.`biz_type` IN (80, 81)
  AND (sr.`batch_no` IS NULL OR TRIM(sr.`batch_no`) = '')
  AND source_item.`batch_no` IS NOT NULL
  AND TRIM(source_item.`batch_no`) <> '';

-- 销售出库/作废流水（50/51）
UPDATE `erp_stock_record` sr
INNER JOIN `erp_sale_out_items` source_item
        ON source_item.`id` = sr.`biz_item_id`
       AND source_item.`tenant_id` = sr.`tenant_id`
       AND source_item.`deleted` = b'0'
SET sr.`batch_no` = TRIM(source_item.`batch_no`)
WHERE sr.`deleted` = b'0'
  AND sr.`biz_type` IN (50, 51)
  AND (sr.`batch_no` IS NULL OR TRIM(sr.`batch_no`) = '')
  AND source_item.`batch_no` IS NOT NULL
  AND TRIM(source_item.`batch_no`) <> '';

-- 调拨入库/出库及作废流水（30~33）
UPDATE `erp_stock_record` sr
INNER JOIN `erp_stock_move_item` source_item
        ON source_item.`id` = sr.`biz_item_id`
       AND source_item.`tenant_id` = sr.`tenant_id`
       AND source_item.`deleted` = b'0'
SET sr.`batch_no` = TRIM(source_item.`batch_no`)
WHERE sr.`deleted` = b'0'
  AND sr.`biz_type` IN (30, 31, 32, 33)
  AND (sr.`batch_no` IS NULL OR TRIM(sr.`batch_no`) = '')
  AND source_item.`batch_no` IS NOT NULL
  AND TRIM(source_item.`batch_no`) <> '';

-- 盘点盘盈/盘亏及作废流水（40~43）
UPDATE `erp_stock_record` sr
INNER JOIN `erp_stock_check_item` source_item
        ON source_item.`id` = sr.`biz_item_id`
       AND source_item.`tenant_id` = sr.`tenant_id`
       AND source_item.`deleted` = b'0'
SET sr.`batch_no` = TRIM(source_item.`batch_no`)
WHERE sr.`deleted` = b'0'
  AND sr.`biz_type` IN (40, 41, 42, 43)
  AND (sr.`batch_no` IS NULL OR TRIM(sr.`batch_no`) = '')
  AND source_item.`batch_no` IS NOT NULL
  AND TRIM(source_item.`batch_no`) <> '';
