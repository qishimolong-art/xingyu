-- 经营报表模块 11/12 前端骨架菜单增量脚本，2026-09-16。
-- 只新增“固定资产及折旧”“异常审计及绩效”两个经营报表骨架入口。
-- 不迁移旧菜单、不授予角色/套餐、不创建按钮权限、不启用任何核算开关。
-- 执行后需要管理员在角色权限中手动勾选新增页面，并刷新权限缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_stage1_11_12_20260916`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_stage1_11_12_20260916`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE asset_group_id BIGINT;
  DECLARE audit_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_ext_11_12_20260916'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_ext_11_12_20260916', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 11/12 extension: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 11/12 extension: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 11/12 extension: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 11/12 extension: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND component IN (
      'erp/report/fixed-asset/index', 'erp/report/audit-trace-performance/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 11/12 extension: component already exists, review before inserting';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND parent_id = report_id
      AND path IN ('fixed-asset', 'audit-trace-performance') AND type <> 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 11/12 extension: category path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '固定资产及折旧', '', 1, 11, report_id, 'fixed-asset', 'ep:folder', '',
    'ErpBusinessReports_fixedAsset', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'fixed-asset');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '异常审计及绩效', '', 1, 12, report_id, 'audit-trace-performance', 'ep:folder', '',
    'ErpBusinessReports_auditTracePerformance', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'audit-trace-performance');

  SELECT id INTO asset_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'fixed-asset';
  SELECT id INTO audit_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'audit-trace-performance';

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '固定资产及折旧报表', '', 2, 1, asset_group_id, '/erp/report/fixed-asset', 'ep:files',
    'erp/report/fixed-asset/index', 'ErpReportFixedAsset', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/fixed-asset/index');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '异常审计业务追踪及绩效', '', 2, 1, audit_group_id, '/erp/report/audit-trace-performance', 'ep:warning',
    'erp/report/audit-trace-performance/index', 'ErpReportAuditTracePerformance', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/audit-trace-performance/index');

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_ext_11_12_20260916');
  SET got_lock = 0;
END$$
DELIMITER ;

CALL `erp_add_business_report_stage1_11_12_20260916`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_stage1_11_12_20260916`;

-- 执行后核对：只应返回新增两个页面及其父级，不应出现角色/套餐授权变更。
SELECT id, parent_id, name, type, sort, path, component, component_name, permission, status, visible
FROM system_menu
WHERE deleted = b'0'
  AND (component IN ('erp/report/fixed-asset/index', 'erp/report/audit-trace-performance/index')
       OR path IN ('fixed-asset', 'audit-trace-performance'))
ORDER BY parent_id, sort, id;
