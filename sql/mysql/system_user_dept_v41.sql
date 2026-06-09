-- System user multi-department relation.
-- MySQL 5.7 / 8.0 compatible.
-- Execute after backing up the database.

CREATE TABLE IF NOT EXISTS `system_user_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL DEFAULT 0 COMMENT 'user id',
  `dept_id` bigint NOT NULL DEFAULT 0 COMMENT 'department id',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_dept_tenant_deleted` (`user_id`, `dept_id`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'user department relation';

INSERT IGNORE INTO `system_user_dept` (
  `user_id`, `dept_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT
  `id`, `dept_id`, COALESCE(`creator`, ''), `create_time`, COALESCE(`updater`, ''), `update_time`, b'0', `tenant_id`
FROM `system_users`
WHERE `dept_id` IS NOT NULL
  AND `deleted` = b'0';
