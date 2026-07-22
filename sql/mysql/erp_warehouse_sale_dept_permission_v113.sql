-- v113: warehouse sale department permission distribution.
-- Purpose: allow a warehouse to be selected by specified departments in sales only.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

CREATE TABLE IF NOT EXISTS `erp_warehouse_sale_dept_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `warehouse_id` bigint NOT NULL COMMENT 'Warehouse ID',
  `dept_id` bigint NOT NULL COMMENT 'Sales department ID',
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_dept_tenant_deleted` (`warehouse_id`, `dept_id`, `tenant_id`, `deleted`),
  KEY `idx_dept_tenant` (`dept_id`, `tenant_id`),
  KEY `idx_warehouse_tenant` (`warehouse_id`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP warehouse sales department permission';
