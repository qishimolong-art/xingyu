-- 经营报表 M0 第二阶段总览入口菜单增量脚本，2026-09-22。
-- 只新增“经营报表总览”页面入口，指向前端 erp/report/overview/index。
-- 不迁移旧菜单、不授予角色/套餐、不创建按钮权限、不改业务表、不启用任何核算开关。
-- 可重复执行；若发现同路径或同组件已被其他菜单占用，会主动报错要求人工核对。
-- 执行后需要管理员在角色权限中手动勾选新增页面，并刷新权限缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_overview_20260922`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_overview_20260922`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE overview_count INT DEFAULT 0;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_overview_20260922'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_overview_20260922', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report overview: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report overview: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report overview: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report overview: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  SELECT COUNT(*) INTO overview_count FROM system_menu
  WHERE deleted = b'0' AND component = 'erp/report/overview/index';

  IF overview_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report overview: duplicate overview components';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND component = 'erp/report/overview/index'
      AND (type <> 2 OR parent_id <> report_id OR path <> '/erp/report/overview'
        OR IFNULL(component_name, '') <> 'ErpReportOverview' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report overview: existing overview component conflicts';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = '/erp/report/overview'
      AND (type <> 2 OR component <> 'erp/report/overview/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report overview: overview path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表总览', '', 2, 0, report_id, '/erp/report/overview', 'ep:data-analysis',
    'erp/report/overview/index', 'ErpReportOverview', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/overview/index');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND component = 'erp/report/overview/index') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report overview: overview insert failed';
  END IF;

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_overview_20260922');
  SET got_lock = 0;
END$$
DELIMITER ;

CALL `erp_add_business_report_overview_20260922`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_overview_20260922`;

-- 执行后核对：只应返回“经营报表”父级和“经营报表总览”页面，不应出现角色/套餐授权变更。
SELECT id, parent_id, name, type, sort, path, component, component_name, permission, status, visible
FROM system_menu
WHERE deleted = b'0'
  AND (component = 'erp/report/overview/index'
       OR (parent_id = 0 AND path = '/erp')
       OR (path = 'business-report' AND component_name = 'ErpBusinessReports'))
ORDER BY parent_id, sort, id;
