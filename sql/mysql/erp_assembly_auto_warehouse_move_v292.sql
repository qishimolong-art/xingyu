-- v292：套件组装跨仓自动生成并完成仓库移货单。
-- 只新增关联/审计字段、放宽出货仓为空及补充库存流水字典；不回填历史移货单。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_column_v292`;
DROP PROCEDURE IF EXISTS `erp_migrate_assembly_auto_move_v292`;

DELIMITER $$

CREATE PROCEDURE `erp_add_column_v292`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name
  ) THEN
    SET @v292_ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `',
                           p_column_name, '` ', p_definition);
    PREPARE v292_stmt FROM @v292_ddl;
    EXECUTE v292_stmt;
    DEALLOCATE PREPARE v292_stmt;
  END IF;
END$$

CREATE PROCEDURE `erp_migrate_assembly_auto_move_v292`()
BEGIN
  IF DATABASE() IS NULL OR DATABASE() = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v292: 未选择目标数据库';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name IN ('erp_assembly_order', 'erp_warehouse_move',
                           'erp_warehouse_move_item', 'erp_stock_record')) <> 4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v292: 组装、移货或库存流水表不存在';
  END IF;
  IF EXISTS (
    SELECT 1 FROM erp_assembly_order
    WHERE deleted = b'0' AND warehouse_id IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v292: 存在入货仓为空的有效组装单';
  END IF;
  IF EXISTS (
    SELECT 1 FROM erp_warehouse_move
    WHERE deleted = b'0' AND source_type = 30 AND source_id IS NOT NULL
    GROUP BY tenant_id, source_id HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v292: 已存在重复的组装来源移货单';
  END IF;

  CALL erp_add_column_v292('erp_assembly_order', 'warehouse_move_id',
       'bigint NULL COMMENT ''跨仓组装自动移货单编号'' AFTER `out_warehouse_id`');
  CALL erp_add_column_v292('erp_warehouse_move', 'void_user_id',
       'bigint NULL COMMENT ''作废人'' AFTER `confirm_time`');
  CALL erp_add_column_v292('erp_warehouse_move', 'void_time',
       'datetime NULL COMMENT ''作废时间'' AFTER `void_user_id`');
  CALL erp_add_column_v292('erp_warehouse_move', 'void_reason',
       'varchar(500) NULL COMMENT ''作废原因'' AFTER `void_time`');

  ALTER TABLE erp_assembly_order
    MODIFY COLUMN out_warehouse_id bigint NULL COMMENT '组成配件出货仓编号；为空时使用入货仓';

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'erp_assembly_order'
      AND index_name = 'idx_erp_assembly_order_warehouse_move'
  ) THEN
    ALTER TABLE erp_assembly_order
      ADD KEY `idx_erp_assembly_order_warehouse_move` (`warehouse_move_id`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'erp_warehouse_move'
      AND index_name = 'idx_erp_warehouse_move_source'
  ) THEN
    ALTER TABLE erp_warehouse_move
      ADD KEY `idx_erp_warehouse_move_source` (`source_type`, `source_id`);
  END IF;
END$$

DELIMITER ;

CALL `erp_migrate_assembly_auto_move_v292`();

DROP PROCEDURE IF EXISTS `erp_migrate_assembly_auto_move_v292`;
DROP PROCEDURE IF EXISTS `erp_add_column_v292`;

INSERT INTO system_dict_data
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 36, '仓库移货入库（作废）', '36', 'erp_stock_record_biz_type', 0, 'danger', '', '',
       '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_data
  WHERE dict_type = 'erp_stock_record_biz_type' AND value = '36' AND deleted = b'0'
);

INSERT INTO system_dict_data
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 37, '仓库移货出库（作废）', '37', 'erp_stock_record_biz_type', 0, 'success', '', '',
       '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM system_dict_data
  WHERE dict_type = 'erp_stock_record_biz_type' AND value = '37' AND deleted = b'0'
);

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'erp_assembly_order'
        AND column_name IN ('out_warehouse_id', 'warehouse_move_id'))
       OR (table_name = 'erp_warehouse_move'
           AND column_name IN ('void_user_id', 'void_time', 'void_reason')))
ORDER BY table_name, ordinal_position;

SELECT COUNT(*) AS historical_orders_linked_to_move
FROM erp_assembly_order
WHERE warehouse_move_id IS NOT NULL;

