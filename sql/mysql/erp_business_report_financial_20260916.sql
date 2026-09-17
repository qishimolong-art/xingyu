-- 经营报表模块 10 财务报表骨架菜单增量脚本，2026-09-16。
-- 只新增“发票凭证及财务报表”经营报表分组及报表项目模板、资产负债表、利润表、现金流量表页面入口。
-- 不迁移旧财务菜单、不授予角色/套餐、不创建按钮权限、不启用任何核算开关。
-- 执行后需要管理员在角色权限中手动勾选新增页面，并刷新权限缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_financial_20260916`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_financial_20260916`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE financial_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_financial_20260916'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_financial_20260916', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report financial extension: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report financial extension: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report financial extension: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report financial extension: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND component IN (
      'erp/report/financial/report-template/index',
      'erp/report/financial/balance-sheet/index',
      'erp/report/financial/income-statement/index',
      'erp/report/financial/cash-flow/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report financial extension: component already exists, review before inserting';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND parent_id = report_id
      AND path = 'financial-report' AND type <> 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report financial extension: category path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '发票凭证及财务报表', '', 1, 10, report_id, 'financial-report', 'ep:folder', '',
    'ErpBusinessReports_financialReport', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'financial-report');

  SELECT id INTO financial_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'financial-report';

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '报表项目模板', '', 2, 1, financial_group_id, '/erp/report/financial/report-template', 'ep:document',
    'erp/report/financial/report-template/index', 'ErpBusinessReportFinancialReportTemplate', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/financial/report-template/index');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '资产负债表', '', 2, 2, financial_group_id, '/erp/report/financial/balance-sheet', 'ep:document',
    'erp/report/financial/balance-sheet/index', 'ErpBusinessReportFinancialBalanceSheet', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/financial/balance-sheet/index');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '利润表', '', 2, 3, financial_group_id, '/erp/report/financial/income-statement', 'ep:document',
    'erp/report/financial/income-statement/index', 'ErpBusinessReportFinancialIncomeStatement', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/financial/income-statement/index');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '现金流量表', '', 2, 4, financial_group_id, '/erp/report/financial/cash-flow', 'ep:document',
    'erp/report/financial/cash-flow/index', 'ErpBusinessReportFinancialCashFlow', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/financial/cash-flow/index');

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_financial_20260916');
  SET got_lock = 0;
END$$
DELIMITER ;

CALL `erp_add_business_report_financial_20260916`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_financial_20260916`;

-- 执行后核对：只应返回新增四个页面及其父级，不应出现角色/套餐授权变更。
SELECT id, parent_id, name, type, sort, path, component, component_name, permission, status, visible
FROM system_menu
WHERE deleted = b'0'
  AND (component IN (
       'erp/report/financial/report-template/index',
       'erp/report/financial/balance-sheet/index',
       'erp/report/financial/income-statement/index',
       'erp/report/financial/cash-flow/index')
       OR path = 'financial-report')
ORDER BY parent_id, sort, id;
