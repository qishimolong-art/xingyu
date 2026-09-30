-- 定向补处 2026-09-29 被误判为非 JSON 对象的两条打印成功回调。
--
-- 执行前提：
-- 1. 已部署兼容单层双重 JSON 的后端代码，回调重试 Job 正常运行。
-- 2. 厂商设备查询显示 taskcnt = 0，确认云端没有待打印任务。
-- 3. 脚本只重放回调日志 7、8，不重放过期的设备状态回调 9，也不直接修改打印任务和打印次数。
--
-- 重复执行时，若两条日志已进入待处理、处理中、成功或待重试状态，脚本安全结束；
-- 任何身份、任务、队列或部分状态不符合预期时均会回滚并报错。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS replay_erp_cloud_print_double_json_callback_20260929;
DELIMITER $$

CREATE PROCEDURE replay_erp_cloud_print_double_json_callback_20260929()
replay_main: BEGIN
  DECLARE identity_count INT DEFAULT 0;
  DECLARE candidate_count INT DEFAULT 0;
  DECLARE already_requeued_count INT DEFAULT 0;
  DECLARE task_count INT DEFAULT 0;
  DECLARE active_task_count INT DEFAULT 0;
  DECLARE paused_device_count INT DEFAULT 0;
  DECLARE affected_count INT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT COUNT(*)
    INTO identity_count
    FROM `erp_cloud_print_callback_log`
   WHERE `id` IN (7, 8)
     AND `deleted` = b'0'
     AND JSON_VALID(`raw_body`) = 1
     AND JSON_TYPE(`raw_body`) = 'STRING'
     AND JSON_UNQUOTE(JSON_EXTRACT(JSON_UNQUOTE(`raw_body`), '$.method')) = 'printRlt'
     AND ((`id` = 7 AND JSON_UNQUOTE(JSON_EXTRACT(JSON_UNQUOTE(`raw_body`), '$.reqid'))
                         = 'SOXSCK20260929000001-552725')
       OR (`id` = 8 AND JSON_UNQUOTE(JSON_EXTRACT(JSON_UNQUOTE(`raw_body`), '$.reqid'))
                         = 'SOXSCK20260929000001-186523'))
   FOR UPDATE;

  IF identity_count <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '回调日志 7/8 身份或原始正文与预期不符，已回滚';
  END IF;

  SELECT COUNT(*)
    INTO candidate_count
    FROM `erp_cloud_print_callback_log`
   WHERE `id` IN (7, 8)
     AND `deleted` = b'0'
     AND `process_status` = 3
     AND `process_message` = '请求正文必须是 JSON 对象';

  IF candidate_count = 0 THEN
    SELECT COUNT(*)
      INTO already_requeued_count
      FROM `erp_cloud_print_callback_log`
     WHERE `id` IN (7, 8)
       AND `deleted` = b'0'
       AND `process_status` IN (0, 1, 2, 4);
    IF already_requeued_count <> 2 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '回调日志 7/8 处于非预期的部分处理状态，已回滚';
    END IF;
    COMMIT;
    LEAVE replay_main;
  END IF;

  IF candidate_count <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '回调日志 7/8 只有部分符合重放条件，已回滚';
  END IF;

  SELECT COUNT(*)
    INTO task_count
    FROM `erp_cloud_print_task`
   WHERE `id` IN (40, 41)
     AND `device_id` = 1
     AND `status` = 6
     AND `deleted` = b'0'
     AND ((`id` = 40 AND `reqid` = 'SOXSCK20260929000001-552725')
       OR (`id` = 41 AND `reqid` = 'SOXSCK20260929000001-186523'))
   FOR UPDATE;

  IF task_count <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '打印任务 40/41 不再是预期的超时状态，已回滚';
  END IF;

  SELECT COUNT(*)
    INTO active_task_count
    FROM `erp_cloud_print_task`
   WHERE `device_id` = 1
     AND `status` IN (0, 1, 5)
     AND `deleted` = b'0';

  IF active_task_count <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '默认打印机存在待打印或在途任务，禁止解除暂停，已回滚';
  END IF;

  SELECT COUNT(*)
    INTO paused_device_count
    FROM `erp_cloud_print_device`
   WHERE `id` = 1
     AND `queue_paused` = b'1'
     AND `queue_pause_task_id` = 41
     AND `deleted` = b'0'
   FOR UPDATE;

  IF paused_device_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '默认打印机暂停状态或关联任务已变化，已回滚';
  END IF;

  UPDATE `erp_cloud_print_callback_log`
     SET `method` = NULL,
         `devid` = NULL,
         `reqid` = NULL,
         `code` = NULL,
         `matched` = b'0',
         `process_status` = 0,
         `retry_count` = 0,
         `next_retry_time` = NULL,
         `process_started_time` = NULL,
         `processed_time` = NULL,
         `process_token` = NULL,
         `process_message` = NULL,
         `updater` = '1',
         `update_time` = NOW()
   WHERE `id` IN (7, 8)
     AND `deleted` = b'0'
     AND `process_status` = 3
     AND `process_message` = '请求正文必须是 JSON 对象';

  SET affected_count = ROW_COUNT();
  IF affected_count <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '回调日志重置数量不是 2，已回滚';
  END IF;

  COMMIT;
END$$

DELIMITER ;
CALL replay_erp_cloud_print_double_json_callback_20260929();
DROP PROCEDURE IF EXISTS replay_erp_cloud_print_double_json_callback_20260929;

SELECT `id`, `method`, `devid`, `reqid`, `code`, `matched`, `process_status`,
       `retry_count`, `next_retry_time`, `processed_time`, `process_message`
  FROM `erp_cloud_print_callback_log`
 WHERE `id` IN (7, 8)
 ORDER BY `id`;

