-- ERP 云打印队列兜底调度频率：5 秒调整为 30 秒
-- 说明：新任务、成功回调、人工恢复仍会即时触发调度；本 Job 仅负责异常情况下的兜底扫描。
-- 适用：已经执行过 erp_cloud_print_queue_v246.sql 的环境。

UPDATE `infra_job`
   SET `cron_expression` = '0/30 * * * * ?',
       `updater` = '1',
       `update_time` = NOW()
 WHERE `handler_name` = 'erpCloudPrintQueueDispatchJob'
   AND `deleted` = b'0'
   AND `cron_expression` = '0/5 * * * * ?';

SELECT `id`, `name`, `status`, `handler_name`, `cron_expression`, `deleted`
  FROM `infra_job`
 WHERE `handler_name` = 'erpCloudPrintQueueDispatchJob'
   AND `deleted` = b'0';
