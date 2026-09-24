-- ERP 销售拣货/送货支持销售手推车跨部门调拨来源任务（可重复执行版）
-- 说明：跨部门手推车先生成调拨出库来源的拣货/送货单，完成送货后自动审核调拨出库，再生成并绑定销售出库单。
-- 若线上出现 Unknown column 'source_type'，说明本脚本尚未执行或未执行完整。
-- 本脚本只调整拣货/送货履约相关表结构，不修改销售开单、审核、扣库存流程，不写入业务数据。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_sale_pick_delivery_transfer_source_v237_add_column`;
DROP PROCEDURE IF EXISTS `erp_sale_pick_delivery_transfer_source_v237_add_index`;
DROP PROCEDURE IF EXISTS `erp_sale_pick_delivery_transfer_source_v237_apply`;
DELIMITER $$
CREATE PROCEDURE `erp_sale_pick_delivery_transfer_source_v237_add_column`(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `erp_sale_pick_delivery_transfer_source_v237_add_index`(
  IN p_table VARCHAR(64),
  IN p_index VARCHAR(64),
  IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND INDEX_NAME = p_index
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

CREATE PROCEDURE `erp_sale_pick_delivery_transfer_source_v237_apply`()
BEGIN
  ALTER TABLE `erp_sale_pick_delivery_order`
    MODIFY COLUMN `sale_out_id` bigint DEFAULT NULL COMMENT '销售出库单编号',
    MODIFY COLUMN `sale_out_no` varchar(64) DEFAULT NULL COMMENT '销售出库单号';
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_column`('erp_sale_pick_delivery_order', 'source_type', 'tinyint DEFAULT NULL COMMENT ''来源类型：30销售手推车'' AFTER `sale_out_no`');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_column`('erp_sale_pick_delivery_order', 'source_id', 'bigint DEFAULT NULL COMMENT ''来源单据编号'' AFTER `source_type`');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_column`('erp_sale_pick_delivery_order', 'source_no', 'varchar(64) DEFAULT NULL COMMENT ''来源单据号'' AFTER `source_id`');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_index`('erp_sale_pick_delivery_order', 'idx_source', 'KEY `idx_source` (`tenant_id`, `source_type`, `source_id`, `deleted`)');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_index`('erp_sale_pick_delivery_order', 'idx_source_no', 'KEY `idx_source_no` (`tenant_id`, `source_no`)');

  ALTER TABLE `erp_sale_pick_delivery_pick_task`
    MODIFY COLUMN `sale_out_id` bigint DEFAULT NULL COMMENT '销售出库单编号',
    MODIFY COLUMN `sale_out_no` varchar(64) DEFAULT NULL COMMENT '销售出库单号';
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_column`('erp_sale_pick_delivery_pick_task', 'source_type', 'tinyint DEFAULT NULL COMMENT ''来源类型：30销售手推车'' AFTER `sale_out_no`');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_column`('erp_sale_pick_delivery_pick_task', 'source_id', 'bigint DEFAULT NULL COMMENT ''来源单据编号'' AFTER `source_type`');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_column`('erp_sale_pick_delivery_pick_task', 'source_no', 'varchar(64) DEFAULT NULL COMMENT ''来源单据号'' AFTER `source_id`');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_index`('erp_sale_pick_delivery_pick_task', 'idx_source', 'KEY `idx_source` (`tenant_id`, `source_type`, `source_id`, `deleted`)');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_index`('erp_sale_pick_delivery_pick_task', 'idx_source_no', 'KEY `idx_source_no` (`tenant_id`, `source_no`)');

  ALTER TABLE `erp_sale_pick_delivery_item`
    MODIFY COLUMN `sale_out_id` bigint DEFAULT NULL COMMENT '销售出库单编号',
    MODIFY COLUMN `sale_out_item_id` bigint DEFAULT NULL COMMENT '销售出库明细编号';
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_column`('erp_sale_pick_delivery_item', 'transfer_out_id', 'bigint DEFAULT NULL COMMENT ''调拨出库单编号'' AFTER `sale_out_item_id`');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_column`('erp_sale_pick_delivery_item', 'transfer_out_item_id', 'bigint DEFAULT NULL COMMENT ''调拨出库明细编号'' AFTER `transfer_out_id`');
  CALL `erp_sale_pick_delivery_transfer_source_v237_add_index`('erp_sale_pick_delivery_item', 'idx_transfer_out', 'KEY `idx_transfer_out` (`tenant_id`, `transfer_out_id`, `deleted`)');

  ALTER TABLE `erp_sale_pick_delivery_submit`
    MODIFY COLUMN `sale_out_id` bigint DEFAULT NULL COMMENT '销售出库单编号';
END$$
DELIMITER ;

CALL `erp_sale_pick_delivery_transfer_source_v237_apply`();

DROP PROCEDURE IF EXISTS `erp_sale_pick_delivery_transfer_source_v237_apply`;
DROP PROCEDURE IF EXISTS `erp_sale_pick_delivery_transfer_source_v237_add_index`;
DROP PROCEDURE IF EXISTS `erp_sale_pick_delivery_transfer_source_v237_add_column`;

SELECT table_name, column_name
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND table_name IN ('erp_sale_pick_delivery_order', 'erp_sale_pick_delivery_pick_task', 'erp_sale_pick_delivery_item')
  AND column_name IN ('source_type', 'source_id', 'source_no', 'transfer_out_id', 'transfer_out_item_id')
ORDER BY table_name, column_name;
