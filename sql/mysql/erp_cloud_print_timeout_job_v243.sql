-- ERP 云打印超时任务初始化
-- 目的：定期将长时间未收到回调的云打印任务标记为超时，避免重复点击时命中旧的运行中任务。

INSERT INTO `infra_job` (
    `name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
    `retry_count`, `retry_interval`, `monitor_timeout`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
    'ERP 云打印超时 Job', 1, 'erpCloudPrintTaskTimeoutJob', '', '0 0/1 * * * ?',
    0, 0, 0,
    '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `infra_job`
    WHERE `handler_name` = 'erpCloudPrintTaskTimeoutJob'
      AND `deleted` = b'0'
);

UPDATE `infra_job`
SET `name` = 'ERP 云打印超时 Job',
    `status` = 1,
    `handler_param` = '',
    `cron_expression` = '0 0/1 * * * ?',
    `retry_count` = 0,
    `retry_interval` = 0,
    `monitor_timeout` = 0,
    `updater` = '1',
    `update_time` = NOW()
WHERE `handler_name` = 'erpCloudPrintTaskTimeoutJob'
  AND `deleted` = b'0';
