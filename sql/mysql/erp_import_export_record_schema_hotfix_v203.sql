-- ERP import/export record schema hotfix (v203).
--
-- Purpose:
--   Some databases already have erp_import_export_record from an older script,
--   but miss the columns later used by ErpImportExportRecordDO. This causes
--   Unknown column 'template_key' when opening the import/export record pages.
--
-- Safety:
--   - No DELETE.
--   - No data overwrite.
--   - Only adds nullable metadata columns, relaxes failure_reason nullability,
--     and enlarges raw_data from TEXT to MEDIUMTEXT when needed.
--   - Checks information_schema before each ALTER so it can be re-run safely.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @schema_name := DATABASE();

SET @sql := (
  SELECT IF(COUNT(*) = 1 AND SUM(CASE WHEN COLUMN_NAME = 'template_key' THEN 1 ELSE 0 END) = 0,
            'ALTER TABLE `erp_import_export_record` ADD COLUMN `template_key` varchar(255) DEFAULT NULL COMMENT ''导入模板类标识'' AFTER `module_name`',
            'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'erp_import_export_record'
    AND COLUMN_NAME IN ('id', 'template_key')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 1 AND SUM(CASE WHEN COLUMN_NAME = 'group_key' THEN 1 ELSE 0 END) = 0,
            'ALTER TABLE `erp_import_export_record_detail` ADD COLUMN `group_key` varchar(128) DEFAULT NULL COMMENT ''单据组标识'' AFTER `row_no`',
            'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'erp_import_export_record_detail'
    AND COLUMN_NAME IN ('id', 'group_key')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 1 AND SUM(CASE WHEN COLUMN_NAME = 'detail_type' THEN 1 ELSE 0 END) = 0,
            'ALTER TABLE `erp_import_export_record_detail` ADD COLUMN `detail_type` varchar(20) DEFAULT NULL COMMENT ''明细类型：FAILURE/CONTEXT'' AFTER `group_key`',
            'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'erp_import_export_record_detail'
    AND COLUMN_NAME IN ('id', 'detail_type')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) > 0,
            'ALTER TABLE `erp_import_export_record_detail` MODIFY COLUMN `failure_reason` varchar(1000) DEFAULT NULL COMMENT ''失败原因''',
            'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'erp_import_export_record_detail'
    AND COLUMN_NAME = 'failure_reason'
    AND IS_NULLABLE = 'NO'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) > 0,
            'ALTER TABLE `erp_import_export_record_detail` MODIFY COLUMN `raw_data` mediumtext DEFAULT NULL COMMENT ''原始行数据 JSON''',
            'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'erp_import_export_record_detail'
    AND COLUMN_NAME = 'raw_data'
    AND DATA_TYPE = 'text'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
            EXISTS (
              SELECT 1
              FROM information_schema.TABLES
              WHERE TABLE_SCHEMA = @schema_name
                AND TABLE_NAME = 'erp_import_export_record_detail'
            )
            AND NOT EXISTS (
              SELECT 1
              FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = @schema_name
                AND TABLE_NAME = 'erp_import_export_record_detail'
                AND INDEX_NAME = 'idx_record_group'
            ),
            'ALTER TABLE `erp_import_export_record_detail` ADD KEY `idx_record_group` (`tenant_id`, `record_id`, `group_key`)',
            'SELECT 1')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
