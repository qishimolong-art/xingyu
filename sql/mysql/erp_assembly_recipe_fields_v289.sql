-- v289：模块业务字段与套件组装人工费成本快照。
-- DDL 会由 MySQL 自动提交；业务回填使用事务，任何预检冲突都会在写入前停止。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_column_v289`;
DROP PROCEDURE IF EXISTS `erp_migrate_assembly_recipe_fields_v289`;

DELIMITER $$

CREATE PROCEDURE `erp_add_column_v289`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name
  ) THEN
    SET @v289_ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `',
                           p_column_name, '` ', p_definition);
    PREPARE v289_stmt FROM @v289_ddl;
    EXECUTE v289_stmt;
    DEALLOCATE PREPARE v289_stmt;
  END IF;
END$$

CREATE PROCEDURE `erp_migrate_assembly_recipe_fields_v289`()
BEGIN
  IF DATABASE() IS NULL OR DATABASE() = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v289: 未选择目标数据库';
  END IF;

  IF (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name IN ('erp_assembly_recipe', 'erp_assembly_order', 'erp_product')) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v289: 组装配方、组装单或配件表不存在';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM erp_assembly_recipe r
    LEFT JOIN erp_product p
           ON p.id = r.product_id
          AND p.tenant_id = r.tenant_id
    WHERE r.deleted = b'0'
      AND (p.id IS NULL OR TRIM(COALESCE(p.code, '')) = '' OR TRIM(COALESCE(p.name, '')) = '')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v289: 存在缺少组成件编码或名称的有效配方';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM erp_assembly_recipe r
    JOIN erp_product p
         ON p.id = r.product_id
        AND p.tenant_id = r.tenant_id
    WHERE r.deleted = b'0'
    GROUP BY r.tenant_id, p.code
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v289: 按组成件编码回填后模块编码会重复';
  END IF;

  CALL erp_add_column_v289('erp_assembly_recipe', 'purpose',
       'tinyint NOT NULL DEFAULT 1 COMMENT ''用途：1 装配'' AFTER `product_id`');
  CALL erp_add_column_v289('erp_assembly_recipe', 'module_code',
       'varchar(64) NULL COMMENT ''模块编码'' AFTER `purpose`');
  CALL erp_add_column_v289('erp_assembly_recipe', 'module_name',
       'varchar(255) NULL COMMENT ''模块名称'' AFTER `module_code`');
  CALL erp_add_column_v289('erp_assembly_recipe', 'labor_cost_per_set',
       'decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT ''每套人工费'' AFTER `module_name`');

  CALL erp_add_column_v289('erp_assembly_order', 'recipe_module_code',
       'varchar(64) NULL COMMENT ''模块编码快照'' AFTER `recipe_version`');
  CALL erp_add_column_v289('erp_assembly_order', 'recipe_module_name',
       'varchar(255) NULL COMMENT ''模块名称快照'' AFTER `recipe_module_code`');
  CALL erp_add_column_v289('erp_assembly_order', 'labor_cost_per_set',
       'decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT ''每套人工费快照'' AFTER `finished_batch_no`');
  CALL erp_add_column_v289('erp_assembly_order', 'material_cost',
       'decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT ''材料成本'' AFTER `labor_cost_per_set`');
  CALL erp_add_column_v289('erp_assembly_order', 'labor_cost',
       'decimal(24,6) NOT NULL DEFAULT 0.000000 COMMENT ''人工费合计'' AFTER `material_cost`');

  START TRANSACTION;

  UPDATE erp_assembly_recipe r
  JOIN erp_product p
       ON p.id = r.product_id
      AND p.tenant_id = r.tenant_id
  SET r.purpose = COALESCE(r.purpose, 1),
      r.module_code = CASE
        WHEN TRIM(COALESCE(r.module_code, '')) = '' THEN TRIM(p.code)
        ELSE TRIM(r.module_code)
      END,
      r.module_name = CASE
        WHEN TRIM(COALESCE(r.module_name, '')) = '' THEN TRIM(p.name)
        ELSE TRIM(r.module_name)
      END,
      r.labor_cost_per_set = COALESCE(r.labor_cost_per_set, 0.000000)
  WHERE r.deleted = b'0'
    AND (r.purpose IS NULL
      OR TRIM(COALESCE(r.module_code, '')) = ''
      OR TRIM(COALESCE(r.module_name, '')) = ''
      OR r.labor_cost_per_set IS NULL);

  UPDATE erp_assembly_order o
  JOIN erp_assembly_recipe r
       ON r.id = o.recipe_id
      AND r.tenant_id = o.tenant_id
  SET o.recipe_module_code = COALESCE(NULLIF(TRIM(o.recipe_module_code), ''), r.module_code),
      o.recipe_module_name = COALESCE(NULLIF(TRIM(o.recipe_module_name), ''), r.module_name),
      o.labor_cost_per_set = COALESCE(o.labor_cost_per_set, 0.000000),
      o.material_cost = CASE
        WHEN COALESCE(o.material_cost, 0.000000) = 0.000000
         AND COALESCE(o.labor_cost, 0.000000) = 0.000000
         AND COALESCE(o.labor_cost_per_set, 0.000000) = 0.000000
        THEN COALESCE(o.total_cost, 0.000000)
        ELSE o.material_cost
      END,
      o.labor_cost = COALESCE(o.labor_cost, 0.000000)
  WHERE o.recipe_module_code IS NULL
     OR TRIM(o.recipe_module_code) = ''
     OR o.recipe_module_name IS NULL
     OR TRIM(o.recipe_module_name) = ''
     OR o.labor_cost_per_set IS NULL
     OR o.material_cost IS NULL
     OR o.labor_cost IS NULL;

  IF EXISTS (
    SELECT 1
    FROM erp_assembly_recipe
    WHERE deleted = b'0'
      AND (purpose <> 1
        OR TRIM(COALESCE(module_code, '')) = ''
        OR TRIM(COALESCE(module_name, '')) = ''
        OR labor_cost_per_set < 0)
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v289: 模块字段回填后检失败';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM erp_assembly_recipe
    WHERE deleted = b'0'
    GROUP BY tenant_id, module_code
    HAVING COUNT(*) > 1
  ) THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v289: 模块编码回填后仍有重复';
  END IF;

  COMMIT;

  CALL erp_add_column_v289('erp_assembly_recipe', 'active_module_code',
       'varchar(64) GENERATED ALWAYS AS (IF(`deleted` = b''0'', `module_code`, NULL)) STORED COMMENT ''未删除模块编码唯一键'' AFTER `active_product_id`');

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_assembly_recipe'
      AND index_name = 'uk_erp_assembly_recipe_active_module_code'
  ) THEN
    ALTER TABLE erp_assembly_recipe
      ADD UNIQUE KEY `uk_erp_assembly_recipe_active_module_code` (`tenant_id`, `active_module_code`);
  END IF;
END$$

DELIMITER ;

CALL `erp_migrate_assembly_recipe_fields_v289`();

DROP PROCEDURE IF EXISTS `erp_migrate_assembly_recipe_fields_v289`;
DROP PROCEDURE IF EXISTS `erp_add_column_v289`;

SELECT COUNT(*) AS active_recipe_missing_fields
FROM erp_assembly_recipe
WHERE deleted = b'0'
  AND (purpose <> 1
    OR TRIM(COALESCE(module_code, '')) = ''
    OR TRIM(COALESCE(module_name, '')) = ''
    OR labor_cost_per_set < 0);

SELECT tenant_id, module_code, COUNT(*) AS duplicate_count
FROM erp_assembly_recipe
WHERE deleted = b'0'
GROUP BY tenant_id, module_code
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS active_order_missing_snapshots
FROM erp_assembly_order
WHERE deleted = b'0'
  AND (TRIM(COALESCE(recipe_module_code, '')) = ''
    OR TRIM(COALESCE(recipe_module_name, '')) = ''
    OR labor_cost_per_set < 0
    OR material_cost < 0
    OR labor_cost < 0);
