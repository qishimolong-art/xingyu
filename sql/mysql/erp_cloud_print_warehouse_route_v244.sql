-- ERP cloud print warehouse routing (v244).
-- Safe to rerun. Adds warehouse-device binding and warehouse trace fields for cloud print tasks.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_cloud_print_warehouse_route_v244;

DELIMITER $$
CREATE PROCEDURE add_erp_cloud_print_warehouse_route_v244()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'erp_warehouse'
       AND COLUMN_NAME = 'cloud_print_device_id'
  ) THEN
    ALTER TABLE `erp_warehouse`
      ADD COLUMN `cloud_print_device_id` bigint NULL DEFAULT NULL COMMENT '销售单云打印设备编号' AFTER `region_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'erp_warehouse'
       AND INDEX_NAME = 'idx_cloud_print_device_id'
  ) THEN
    ALTER TABLE `erp_warehouse`
      ADD INDEX `idx_cloud_print_device_id` (`cloud_print_device_id`, `tenant_id`, `deleted`) USING BTREE;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'erp_cloud_print_task'
       AND COLUMN_NAME = 'warehouse_id'
  ) THEN
    ALTER TABLE `erp_cloud_print_task`
      ADD COLUMN `warehouse_id` bigint NULL DEFAULT NULL COMMENT '仓库编号' AFTER `biz_no`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'erp_cloud_print_task'
       AND COLUMN_NAME = 'warehouse_name'
  ) THEN
    ALTER TABLE `erp_cloud_print_task`
      ADD COLUMN `warehouse_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '仓库名称' AFTER `warehouse_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'erp_cloud_print_task'
       AND INDEX_NAME = 'idx_biz_warehouse_status'
  ) THEN
    ALTER TABLE `erp_cloud_print_task`
      ADD INDEX `idx_biz_warehouse_status` (`biz_type`, `biz_id`, `warehouse_id`, `status`, `tenant_id`, `deleted`) USING BTREE;
  END IF;
END$$
DELIMITER ;

CALL add_erp_cloud_print_warehouse_route_v244();
DROP PROCEDURE IF EXISTS add_erp_cloud_print_warehouse_route_v244;
