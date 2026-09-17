-- ERP attachment scan upload foundation (v232)
-- Scope:
--   1. Create independent scan upload session table.
--   2. Create independent scan uploaded file table.
--
-- Safety:
--   - Uses CREATE TABLE IF NOT EXISTS.
--   - Does not add menu permissions.
--   - Does not update, delete, or copy existing business data.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_attachment_scan_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `ticket` varchar(64) NOT NULL COMMENT 'temporary upload ticket',
  `biz_type` varchar(64) DEFAULT NULL COMMENT 'reserved business type',
  `biz_id` bigint DEFAULT NULL COMMENT 'reserved business id',
  `biz_no` varchar(64) DEFAULT NULL COMMENT 'reserved business no',
  `title` varchar(100) DEFAULT NULL COMMENT 'upload title',
  `status` tinyint NOT NULL DEFAULT 10 COMMENT 'status: 10 waiting, 20 uploaded, 30 expired, 40 canceled',
  `uploaded_count` int NOT NULL DEFAULT 0 COMMENT 'uploaded file count',
  `max_file_count` int NOT NULL DEFAULT 9 COMMENT 'max file count',
  `expire_time` datetime NOT NULL COMMENT 'expire time',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'tenant id',
  `creator` varchar(64) DEFAULT NULL COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT NULL COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attachment_scan_ticket` (`ticket`, `deleted`),
  KEY `idx_attachment_scan_creator` (`tenant_id`, `creator`, `create_time`),
  KEY `idx_attachment_scan_status_expire` (`tenant_id`, `status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP scan upload session';

CREATE TABLE IF NOT EXISTS `erp_attachment_scan_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `session_id` bigint NOT NULL COMMENT 'scan upload session id',
  `file_name` varchar(255) NOT NULL COMMENT 'file name',
  `file_url` varchar(512) NOT NULL COMMENT 'file url',
  `file_type` varchar(128) DEFAULT NULL COMMENT 'file MIME type',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT 'file size bytes',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'tenant id',
  `creator` varchar(64) DEFAULT NULL COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT NULL COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  PRIMARY KEY (`id`),
  KEY `idx_attachment_scan_file_session` (`tenant_id`, `session_id`, `create_time`),
  KEY `idx_attachment_scan_file_url` (`tenant_id`, `file_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP scan uploaded file';

