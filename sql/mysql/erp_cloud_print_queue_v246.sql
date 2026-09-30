-- ERP 云打印本地持久化队列与故障闭环（v246）。
-- 可重复执行；仅新增设备队列字段/任务索引、注册调度任务，并将部署前已超时的在途任务标记为超时。
-- 不删除、不补打任何历史任务，也不修改菜单、角色、权限或业务单据。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_cloud_print_queue_v246;
DELIMITER $$
CREATE PROCEDURE add_erp_cloud_print_queue_v246()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_device'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_device'
         AND COLUMN_NAME = 'last_status_message'
    ) THEN
      ALTER TABLE `erp_cloud_print_device`
        ADD COLUMN `last_status_message` varchar(512) NULL DEFAULT NULL COMMENT '最近设备故障信息'
        AFTER `last_status_code`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_device'
         AND COLUMN_NAME = 'queue_paused'
    ) THEN
      ALTER TABLE `erp_cloud_print_device`
        ADD COLUMN `queue_paused` bit(1) NOT NULL DEFAULT b'0' COMMENT '打印队列是否暂停'
        AFTER `last_status_time`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_device'
         AND COLUMN_NAME = 'queue_pause_reason'
    ) THEN
      ALTER TABLE `erp_cloud_print_device`
        ADD COLUMN `queue_pause_reason` varchar(512) NULL DEFAULT NULL COMMENT '打印队列暂停原因'
        AFTER `queue_paused`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_device'
         AND COLUMN_NAME = 'queue_pause_task_id'
    ) THEN
      ALTER TABLE `erp_cloud_print_device`
        ADD COLUMN `queue_pause_task_id` bigint NULL DEFAULT NULL COMMENT '导致队列暂停的任务编号'
        AFTER `queue_pause_reason`;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.STATISTICS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_device'
         AND INDEX_NAME = 'idx_queue_paused'
    ) THEN
      ALTER TABLE `erp_cloud_print_device`
        ADD INDEX `idx_queue_paused` (`tenant_id`, `queue_paused`, `status`, `deleted`) USING BTREE;
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_task'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_cloud_print_task'
       AND INDEX_NAME = 'idx_device_queue'
  ) THEN
    ALTER TABLE `erp_cloud_print_task`
      ADD INDEX `idx_device_queue` (`tenant_id`, `device_id`, `status`, `id`, `deleted`) USING BTREE;
  END IF;
END$$
DELIMITER ;

CALL add_erp_cloud_print_queue_v246();
DROP PROCEDURE IF EXISTS add_erp_cloud_print_queue_v246;

INSERT INTO `infra_job` (
  `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
  `retry_count`, `retry_interval`, `monitor_timeout`,
  `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
  'ERP 云打印队列调度 Job', 1, 'erpCloudPrintQueueDispatchJob', '', '0/30 * * * * ?',
  0, 0, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
   WHERE `handler_name` = 'erpCloudPrintQueueDispatchJob' AND `deleted` = b'0'
);

UPDATE `infra_job`
   SET `name` = 'ERP 云打印队列调度 Job', `status` = 1, `handler_param` = '',
       `cron_expression` = '0/30 * * * * ?', `retry_count` = 0,
       `retry_interval` = 0, `monitor_timeout` = 0,
       `updater` = '1', `update_time` = NOW()
 WHERE `handler_name` = 'erpCloudPrintQueueDispatchJob' AND `deleted` = b'0';

INSERT INTO `infra_job` (
  `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
  `retry_count`, `retry_interval`, `monitor_timeout`,
  `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
  'ERP 云打印超时 Job', 1, 'erpCloudPrintTaskTimeoutJob', '', '0 0/1 * * * ?',
  0, 0, 0, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job`
   WHERE `handler_name` = 'erpCloudPrintTaskTimeoutJob' AND `deleted` = b'0'
);

UPDATE `infra_job`
   SET `name` = 'ERP 云打印超时 Job', `status` = 1, `handler_param` = '',
       `cron_expression` = '0 0/1 * * * ?', `retry_count` = 0,
       `retry_interval` = 0, `monitor_timeout` = 0,
       `updater` = '1', `update_time` = NOW()
 WHERE `handler_name` = 'erpCloudPrintTaskTimeoutJob' AND `deleted` = b'0';

START TRANSACTION;
DROP TEMPORARY TABLE IF EXISTS `tmp_erp_cloud_print_stale_v246`;
CREATE TEMPORARY TABLE `tmp_erp_cloud_print_stale_v246` AS
SELECT `id`, `tenant_id`, `device_id`, `biz_no`
  FROM `erp_cloud_print_task`
 WHERE `deleted` = b'0'
   AND `status` IN (1, 5)
   AND `submit_time` IS NOT NULL
   AND `submit_time` < DATE_SUB(NOW(), INTERVAL 10 MINUTE);

UPDATE `erp_cloud_print_task` task
JOIN `tmp_erp_cloud_print_stale_v246` stale ON stale.`id` = task.`id`
   SET `status` = 6,
       `error_msg` = '部署前任务超过10分钟未收到打印结果回调；未自动补打',
       `updater` = '1',
       `update_time` = NOW();

UPDATE `erp_cloud_print_device` d
JOIN (
  SELECT ranked.`tenant_id`, ranked.`device_id`, ranked.`id` AS `task_id`
    FROM (
      SELECT task.`id`, task.`tenant_id`, task.`device_id`,
             ROW_NUMBER() OVER (
               PARTITION BY task.`tenant_id`, task.`device_id`
               ORDER BY CASE WHEN task.`biz_no` IN ('XSCK20260929000001', 'XSCK20260929000002')
                             THEN 0 ELSE 1 END,
                        task.`id` DESC
             ) AS row_num
        FROM `tmp_erp_cloud_print_stale_v246` task
       WHERE task.`device_id` IS NOT NULL
    ) ranked
   WHERE ranked.row_num = 1
) stale ON stale.`tenant_id` = d.`tenant_id` AND stale.`device_id` = d.`id`
   SET d.`queue_paused` = b'1',
       d.`queue_pause_reason` = '存在部署前超时任务，未自动补打，请确认后重试或跳过',
       d.`queue_pause_task_id` = stale.`task_id`,
       d.`updater` = '1',
       d.`update_time` = NOW()
 WHERE d.`deleted` = b'0';
DROP TEMPORARY TABLE IF EXISTS `tmp_erp_cloud_print_stale_v246`;
COMMIT;
