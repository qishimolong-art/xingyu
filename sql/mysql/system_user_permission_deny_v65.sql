-- User-level button permission deny list.
-- Safe to execute repeatedly. This table only records permissions that are
-- denied for a specific user after role permissions have granted them.

CREATE TABLE IF NOT EXISTS `system_user_permission_deny` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `user_id` BIGINT NOT NULL COMMENT 'User id',
  `permission` VARCHAR(100) NOT NULL COMMENT 'Denied button permission code',
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_permission` (`user_id`, `permission`, `tenant_id`, `deleted`),
  KEY `idx_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System user permission deny';
