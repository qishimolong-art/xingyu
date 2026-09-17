-- 经营报表模块 07/08/10 第一阶段汇总入口菜单增量脚本，2026-09-16。
-- 只新增“账户资金及总部归集”“费用分摊及经营收支”“发票凭证及财务报表”三个经营报表汇总入口。
-- 不迁移旧菜单、不授予角色/套餐、不创建按钮权限、不启用任何核算开关。
-- 执行后需要管理员在角色权限中手动勾选新增页面，并刷新权限缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_stage1_07_08_10_20260916`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_stage1_07_08_10_20260916`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE fund_group_id BIGINT;
  DECLARE expense_group_id BIGINT;
  DECLARE finance_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_ext_07_08_10_20260916'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_ext_07_08_10_20260916', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 07/08/10 extension: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 07/08/10 extension: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 07/08/10 extension: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 07/08/10 extension: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND component IN (
      'erp/report/account-fund/index',
      'erp/report/expense-income/index',
      'erp/report/financial-statement/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 07/08/10 extension: component already exists, review before inserting';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND parent_id = report_id
      AND path IN ('account-fund', 'expense-income', 'financial-statement') AND type <> 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'report 07/08/10 extension: category path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '账户资金及总部归集', '', 1, 7, report_id, 'account-fund', 'ep:folder', '',
    'ErpBusinessReports_accountFund', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'account-fund');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '费用分摊及经营收支', '', 1, 8, report_id, 'expense-income', 'ep:folder', '',
    'ErpBusinessReports_expenseIncome', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'expense-income');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '发票凭证及财务报表', '', 1, 10, report_id, 'financial-statement', 'ep:folder', '',
    'ErpBusinessReports_financialStatement', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'financial-statement');

  SELECT id INTO fund_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'account-fund';
  SELECT id INTO expense_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'expense-income';
  SELECT id INTO finance_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'financial-statement';

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '账户资金及总部归集报表', '', 2, 1, fund_group_id, '/erp/report/account-fund', 'ep:money',
    'erp/report/account-fund/index', 'ErpReportAccountFund', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/account-fund/index');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '费用分摊及经营收支报表', '', 2, 1, expense_group_id, '/erp/report/expense-income', 'ep:coin',
    'erp/report/expense-income/index', 'ErpReportExpenseIncome', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/expense-income/index');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '发票凭证及财务报表', '', 2, 1, finance_group_id, '/erp/report/financial-statement', 'ep:tickets',
    'erp/report/financial-statement/index', 'ErpReportFinancialStatement', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND component = 'erp/report/financial-statement/index');

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_ext_07_08_10_20260916');
  SET got_lock = 0;
END$$
DELIMITER ;

CALL `erp_add_business_report_stage1_07_08_10_20260916`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_stage1_07_08_10_20260916`;

-- 执行后核对：只应返回新增三个页面及其父级，不应出现角色/套餐授权变更。
SELECT id, parent_id, name, type, sort, path, component, component_name, permission, status, visible
FROM system_menu
WHERE deleted = b'0'
  AND (component IN (
        'erp/report/account-fund/index',
        'erp/report/expense-income/index',
        'erp/report/financial-statement/index')
       OR path IN ('account-fund', 'expense-income', 'financial-statement'))
ORDER BY parent_id, sort, id;
