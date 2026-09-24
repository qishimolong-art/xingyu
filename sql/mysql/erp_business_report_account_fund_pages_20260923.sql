-- 经营报表 M7 账户资金及总部归集独立页面入口，2026-09-23。
-- 将账户资金流水、账户余额对账、总部归集、代收代付、转账、收款、付款和历史资金口径拆为独立经营报表页面。
-- 只新增或修正经营报表菜单入口，不修改账户、转账、收款、付款、核销、费用或往来业务表，不改变业务流程。
-- 本脚本不写角色授权，不写租户套餐，不启用新核算开关。
-- 执行后需要在角色权限中勾选新增页面，并刷新菜单缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_account_fund_pages_20260923`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_account_fund_pages_20260923`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE fund_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_account_fund_pages_20260923'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_account_fund_pages_20260923', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report account fund pages: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report account fund pages: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report account fund pages: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report account fund pages: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'account-fund'
      AND (type <> 1 OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report account fund pages: account-fund group conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '账户资金及总部归集', '', 1, 7, report_id, 'account-fund', 'ep:folder', '',
    'ErpBusinessReports_accountFund', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'account-fund');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'account-fund') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report account fund pages: duplicate account-fund groups';
  END IF;
  SELECT id INTO fund_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'account-fund';

  UPDATE system_menu
  SET name = '账户资金及总部归集', sort = 7, icon = 'ep:folder', component = '',
      component_name = 'ErpBusinessReports_accountFund', status = 0,
      visible = b'1', keep_alive = b'1', always_show = b'1', update_time = NOW(), updater = '1'
  WHERE id = fund_group_id AND deleted = b'0' AND type = 1;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_account_fund_pages_20260923;
  CREATE TEMPORARY TABLE tmp_business_report_account_fund_pages_20260923 (
    page_path VARCHAR(64) PRIMARY KEY,
    page_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL,
    component_name VARCHAR(128) NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  INSERT INTO tmp_business_report_account_fund_pages_20260923 VALUES
    ('account-ledger', '账户资金流水表', 1, 'ErpBusinessReportAccountFundLedger'),
    ('account-balance', '账户余额及对账表', 2, 'ErpBusinessReportAccountFundBalance'),
    ('transfer-in-transit', '部门转总部资金归集及在途表', 3, 'ErpBusinessReportAccountFundTransferTransit'),
    ('hq-collection-payment', '总部代收代付及部门资金占用表', 4, 'ErpBusinessReportAccountFundHqCollectionPayment'),
    ('bank-transfer', '银行转账记录', 5, 'ErpBusinessReportAccountFundBankTransfer'),
    ('receipt-record', '收款记录', 6, 'ErpBusinessReportAccountFundReceipt'),
    ('payment-record', '付款记录', 7, 'ErpBusinessReportAccountFundPayment'),
    ('history', '历史资金口径', 8, 'ErpBusinessReportAccountFundHistory');

  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_business_report_account_fund_pages_20260923 p
      ON p.page_path = m.path
      WHERE m.deleted = b'0' AND m.parent_id = fund_group_id
        AND (m.type <> 2 OR IFNULL(m.component, '') <> 'erp/report/account-fund/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report account fund pages: existing page path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT p.page_name, '', 2, p.sort_order, fund_group_id, p.page_path, 'ep:data-analysis',
    'erp/report/account-fund/index', p.component_name, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM tmp_business_report_account_fund_pages_20260923 p
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0'
    AND m.parent_id = fund_group_id AND m.path = p.page_path);

  UPDATE system_menu m JOIN tmp_business_report_account_fund_pages_20260923 p ON p.page_path = m.path
  SET m.name = p.page_name,
      m.sort = p.sort_order,
      m.icon = 'ep:data-analysis',
      m.component = 'erp/report/account-fund/index',
      m.component_name = p.component_name,
      m.status = 0,
      m.visible = b'1',
      m.keep_alive = b'1',
      m.always_show = b'1',
      m.update_time = NOW(),
      m.updater = '1'
  WHERE m.deleted = b'0' AND m.parent_id = fund_group_id AND m.type = 2;

  UPDATE system_menu
  SET visible = b'0',
      sort = 90,
      update_time = NOW(),
      updater = '1'
  WHERE deleted = b'0'
    AND parent_id = fund_group_id
    AND type = 2
    AND path = '/erp/report/account-fund';

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_account_fund_pages_20260923');
  SET got_lock = 0;

  SELECT m.id, m.parent_id, m.name, m.type, m.sort, m.path, m.component, m.component_name, m.permission, m.status, m.visible
  FROM system_menu m
  WHERE m.deleted = b'0'
    AND (m.id = fund_group_id OR m.parent_id = fund_group_id)
  ORDER BY m.parent_id, m.sort, m.id;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_account_fund_pages_20260923;
END$$
DELIMITER ;

CALL `erp_add_business_report_account_fund_pages_20260923`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_account_fund_pages_20260923`;
