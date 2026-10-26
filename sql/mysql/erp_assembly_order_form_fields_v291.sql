-- v291：套件组装新增/详情字段与入、出货仓拆分。
-- 历史 warehouse_id 保持为成品入货仓；out_warehouse_id 回填为同一仓库，确保历史库存语义不变。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_column_v291`;
DROP PROCEDURE IF EXISTS `erp_migrate_assembly_order_form_fields_v291`;

DELIMITER $$

CREATE PROCEDURE `erp_add_column_v291`(
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
    SET @v291_ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `',
                           p_column_name, '` ', p_definition);
    PREPARE v291_stmt FROM @v291_ddl;
    EXECUTE v291_stmt;
    DEALLOCATE PREPARE v291_stmt;
  END IF;
END$$

CREATE PROCEDURE `erp_migrate_assembly_order_form_fields_v291`()
BEGIN
  IF DATABASE() IS NULL OR DATABASE() = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v291: 未选择目标数据库';
  END IF;

  IF (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name IN ('erp_assembly_order', 'erp_warehouse', 'erp_customer')) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v291: 组装单、仓库或客户表不存在';
  END IF;

  IF EXISTS (
    SELECT 1 FROM erp_assembly_order
    WHERE deleted = b'0' AND warehouse_id IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v291: 存在入货仓为空的有效组装单';
  END IF;

  CALL erp_add_column_v291('erp_assembly_order', 'customer_id',
       'bigint NULL COMMENT ''对象客户编号'' AFTER `product_id`');
  CALL erp_add_column_v291('erp_assembly_order', 'handler_id',
       'bigint NULL COMMENT ''经办人编号'' AFTER `customer_id`');
  CALL erp_add_column_v291('erp_assembly_order', 'out_warehouse_id',
       'bigint NULL COMMENT ''组成配件出货仓编号'' AFTER `warehouse_id`');
  CALL erp_add_column_v291('erp_assembly_order', 'shelf',
       'varchar(64) NULL COMMENT ''成品入库仓位'' AFTER `dept_id`');
  CALL erp_add_column_v291('erp_assembly_order', 'other_cost',
       'decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT ''其他组装成本'' AFTER `labor_cost_per_set`');

  START TRANSACTION;

  UPDATE erp_assembly_order
  SET out_warehouse_id = warehouse_id
  WHERE out_warehouse_id IS NULL;

  UPDATE erp_assembly_order
  SET other_cost = 0.000000
  WHERE other_cost IS NULL;

  -- 旧前端曾把未初始化日期保存为 Unix 起点；使用该单据的创建时间修复明确的历史哨兵值。
  UPDATE erp_assembly_order
  SET assembly_time = create_time
  WHERE (assembly_time IS NULL OR assembly_time < '2000-01-01 00:00:00')
    AND create_time IS NOT NULL;

  IF EXISTS (
    SELECT 1 FROM erp_assembly_order
    WHERE deleted = b'0'
      AND (out_warehouse_id IS NULL OR other_cost IS NULL OR other_cost < 0)
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v291: 历史组装单字段回填后检查失败';
  END IF;

  COMMIT;

  ALTER TABLE erp_assembly_order
    MODIFY COLUMN out_warehouse_id bigint NOT NULL COMMENT '组成配件出货仓编号';

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_assembly_order'
      AND index_name = 'idx_erp_assembly_order_customer'
  ) THEN
    ALTER TABLE erp_assembly_order ADD KEY `idx_erp_assembly_order_customer` (`customer_id`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_assembly_order'
      AND index_name = 'idx_erp_assembly_order_handler'
  ) THEN
    ALTER TABLE erp_assembly_order ADD KEY `idx_erp_assembly_order_handler` (`handler_id`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_assembly_order'
      AND index_name = 'idx_erp_assembly_order_out_warehouse'
  ) THEN
    ALTER TABLE erp_assembly_order ADD KEY `idx_erp_assembly_order_out_warehouse` (`out_warehouse_id`);
  END IF;
END$$

DELIMITER ;

CALL `erp_migrate_assembly_order_form_fields_v291`();

DROP PROCEDURE IF EXISTS `erp_migrate_assembly_order_form_fields_v291`;
DROP PROCEDURE IF EXISTS `erp_add_column_v291`;

SELECT COUNT(*) AS active_order_missing_fields
FROM erp_assembly_order
WHERE deleted = b'0'
  AND (out_warehouse_id IS NULL OR other_cost IS NULL OR other_cost < 0);

SELECT COUNT(*) AS legacy_invalid_assembly_time
FROM erp_assembly_order
WHERE (assembly_time IS NULL OR assembly_time < '2000-01-01 00:00:00')
  AND create_time IS NOT NULL;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'erp_assembly_order'
  AND column_name IN ('customer_id', 'handler_id', 'out_warehouse_id', 'shelf', 'other_cost')
ORDER BY ordinal_position;

