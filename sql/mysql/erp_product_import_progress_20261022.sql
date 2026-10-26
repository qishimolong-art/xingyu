-- 配件信息异步导入进度字段。
-- 仅新增字段和索引，不删除、不覆盖、不回填历史业务数据，可重复执行。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_product_import_progress_20261022`;
DELIMITER $$
CREATE PROCEDURE `erp_add_product_import_progress_20261022`()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_import_export_record'
      AND COLUMN_NAME = 'stage'
  ) THEN
    ALTER TABLE `erp_import_export_record`
      ADD COLUMN `stage` varchar(30) DEFAULT NULL COMMENT '执行阶段' AFTER `status`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_import_export_record'
      AND COLUMN_NAME = 'processed_count'
  ) THEN
    ALTER TABLE `erp_import_export_record`
      ADD COLUMN `processed_count` int NOT NULL DEFAULT 0 COMMENT '已处理条数' AFTER `total_count`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_import_export_record'
      AND COLUMN_NAME = 'heartbeat_time'
  ) THEN
    ALTER TABLE `erp_import_export_record`
      ADD COLUMN `heartbeat_time` datetime DEFAULT NULL COMMENT '最近进度更新时间' AFTER `end_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'erp_import_export_record'
      AND INDEX_NAME = 'idx_import_task_active'
  ) THEN
    ALTER TABLE `erp_import_export_record`
      ADD INDEX `idx_import_task_active`
        (`tenant_id`, `operation_type`, `module_key`, `status`, `heartbeat_time`);
  END IF;
END$$
DELIMITER ;

CALL `erp_add_product_import_progress_20261022`();
DROP PROCEDURE IF EXISTS `erp_add_product_import_progress_20261022`;

SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'erp_import_export_record'
  AND COLUMN_NAME IN ('stage', 'processed_count', 'heartbeat_time')
ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'erp_import_export_record'
  AND INDEX_NAME = 'idx_import_task_active'
ORDER BY SEQ_IN_INDEX;
