-- ERP 云打印回调可靠收件箱（v247）。
-- 可重复执行；仅扩展回调日志字段、索引并注册重试任务，不修改打印提交、队列、菜单或权限。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_cloud_print_callback_v247;
DELIMITER $$
CREATE PROCEDURE add_erp_cloud_print_callback_v247()
BEGIN
  DECLARE process_status_added BOOLEAN DEFAULT FALSE;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
         AND COLUMN_NAME = 'process_status'
    ) THEN
      ALTER TABLE `erp_cloud_print_callback_log`
        ADD COLUMN `process_status` tinyint NOT NULL DEFAULT 0
          COMMENT '处理状态：0待处理 1处理中 2成功 3忽略 4待重试'
          AFTER `matched`;
      SET process_status_added = TRUE;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
         AND COLUMN_NAME = 'retry_count'
    ) THEN
      ALTER TABLE `erp_cloud_print_callback_log`
        ADD COLUMN `retry_count` int NOT NULL DEFAULT 0 COMMENT '处理重试次数'
          AFTER `process_status`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
         AND COLUMN_NAME = 'next_retry_time'
    ) THEN
      ALTER TABLE `erp_cloud_print_callback_log`
        ADD COLUMN `next_retry_time` datetime NULL DEFAULT NULL COMMENT '下次重试时间'
          AFTER `retry_count`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
         AND COLUMN_NAME = 'process_started_time'
    ) THEN
      ALTER TABLE `erp_cloud_print_callback_log`
        ADD COLUMN `process_started_time` datetime NULL DEFAULT NULL COMMENT '本次开始处理时间'
          AFTER `next_retry_time`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
         AND COLUMN_NAME = 'processed_time'
    ) THEN
      ALTER TABLE `erp_cloud_print_callback_log`
        ADD COLUMN `processed_time` datetime NULL DEFAULT NULL COMMENT '处理完成时间'
          AFTER `process_started_time`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
         AND COLUMN_NAME = 'process_token'
    ) THEN
      ALTER TABLE `erp_cloud_print_callback_log`
        ADD COLUMN `process_token` varchar(64) NULL DEFAULT NULL COMMENT '处理租约令牌'
          AFTER `processed_time`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
         AND COLUMN_NAME = 'process_message'
    ) THEN
      ALTER TABLE `erp_cloud_print_callback_log`
        ADD COLUMN `process_message` varchar(1000) NULL DEFAULT NULL COMMENT '处理说明或错误摘要'
          AFTER `process_token`;
    END IF;

    IF process_status_added THEN
      UPDATE `erp_cloud_print_callback_log`
         SET `process_status` = 3,
             `retry_count` = 0,
             `process_message` = 'v247 上线前历史回调，仅归档不重放',
             `processed_time` = COALESCE(`update_time`, `create_time`, NOW())
       WHERE `process_status` = 0;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
         AND INDEX_NAME = 'idx_callback_retry'
    ) THEN
      ALTER TABLE `erp_cloud_print_callback_log`
        ADD INDEX `idx_callback_retry`
          (`process_status`, `next_retry_time`, `deleted`) USING BTREE;
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'infra_job'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_callback_log'
  ) THEN
    INSERT INTO `infra_job` (
      `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
      `retry_count`, `retry_interval`, `monitor_timeout`,
      `creator`, `create_time`, `updater`, `update_time`, `deleted`
    )
    SELECT
      'ERP 云打印回调重试 Job', 1, 'erpCloudPrintCallbackRetryJob', '', '0 0/1 * * * ?',
      0, 0, 0, '1', NOW(), '1', NOW(), b'0'
    WHERE NOT EXISTS (
      SELECT 1 FROM `infra_job`
       WHERE `handler_name` = 'erpCloudPrintCallbackRetryJob' AND `deleted` = b'0'
    );

    UPDATE `infra_job`
       SET `name` = 'ERP 云打印回调重试 Job', `status` = 1, `handler_param` = '',
           `cron_expression` = '0 0/1 * * * ?', `retry_count` = 0,
           `retry_interval` = 0, `monitor_timeout` = 0,
           `updater` = '1', `update_time` = NOW()
     WHERE `handler_name` = 'erpCloudPrintCallbackRetryJob' AND `deleted` = b'0';
  END IF;
END$$
DELIMITER ;

CALL add_erp_cloud_print_callback_v247();
DROP PROCEDURE IF EXISTS add_erp_cloud_print_callback_v247;
