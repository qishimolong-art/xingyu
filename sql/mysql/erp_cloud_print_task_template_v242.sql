-- ERP cloud print task template traceability (v242).
-- Safe to rerun. Adds nullable template_id only; does not modify devices,
-- task history, menu, role, permission, or business data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_cloud_print_task_template_v242;
DELIMITER //
CREATE PROCEDURE add_erp_cloud_print_task_template_v242()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'erp_cloud_print_task'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'erp_cloud_print_task'
         AND COLUMN_NAME = 'template_id'
    ) THEN
      ALTER TABLE `erp_cloud_print_task`
        ADD COLUMN `template_id` bigint NULL DEFAULT NULL COMMENT '打印模板编号' AFTER `device_id`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE()
         AND TABLE_NAME = 'erp_cloud_print_task'
         AND INDEX_NAME = 'idx_template_id'
    ) THEN
      ALTER TABLE `erp_cloud_print_task`
        ADD KEY `idx_template_id` (`template_id`, `tenant_id`, `deleted`) USING BTREE;
    END IF;
  END IF;
END//
DELIMITER ;

CALL add_erp_cloud_print_task_template_v242();
DROP PROCEDURE IF EXISTS add_erp_cloud_print_task_template_v242;
