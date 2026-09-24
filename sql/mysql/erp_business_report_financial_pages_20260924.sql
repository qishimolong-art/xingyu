-- 经营报表 M10 发票凭证及财务报表独立页面入口，2026-09-24。
-- 将采购发票、采购入库开票状态、销售退货红冲来源、凭证、凭证归集、凭证字、会计科目、报表模板、三大报表和历史口径拆为独立经营报表页面。
-- 只新增或修正经营报表菜单入口，不修改采购、销售、凭证、科目、发票或财务业务表，不改变业务流程。
-- 本脚本不写角色授权，不写租户套餐，不启用双成本或财务公式开关。
-- 执行后需要在角色权限中勾选新增页面，并刷新菜单缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_financial_pages_20260924`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_financial_pages_20260924`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE financial_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_financial_pages_20260924'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_financial_pages_20260924', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report financial pages: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report financial pages: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report financial pages: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report financial pages: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path IN ('financial', 'financial-report')
      AND (type <> 1 OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report financial pages: financial group conflicts';
  END IF;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'financial') > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report financial pages: duplicate financial groups';
  END IF;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'financial') = 1 THEN
    SELECT id INTO financial_group_id FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'financial';
  ELSEIF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'financial-report') = 1 THEN
    SELECT id INTO financial_group_id FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'financial-report';
  ELSEIF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'financial-report') > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report financial pages: duplicate financial-report groups';
  ELSE
    INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
      component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
    VALUES ('发票凭证及财务报表', '', 1, 10, report_id, 'financial', 'ep:folder', '',
      'ErpBusinessReports_financial', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');
    SET financial_group_id = LAST_INSERT_ID();
  END IF;

  UPDATE system_menu
  SET name = '发票凭证及财务报表', sort = 10, path = 'financial', icon = 'ep:folder', component = '',
      component_name = 'ErpBusinessReports_financial', status = 0,
      visible = b'1', keep_alive = b'1', always_show = b'1', update_time = NOW(), updater = '1'
  WHERE id = financial_group_id AND deleted = b'0' AND type = 1;

  UPDATE system_menu
  SET visible = b'0', keep_alive = b'0', always_show = b'0', sort = 90,
      update_time = NOW(), updater = '1'
  WHERE deleted = b'0'
    AND parent_id = report_id
    AND path = 'financial-statement'
    AND id <> financial_group_id;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_financial_pages_20260924;
  CREATE TEMPORARY TABLE tmp_business_report_financial_pages_20260924 (
    page_path VARCHAR(64) PRIMARY KEY,
    page_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL,
    component_path VARCHAR(128) NOT NULL,
    component_name VARCHAR(128) NOT NULL,
    page_icon VARCHAR(64) NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  INSERT INTO tmp_business_report_financial_pages_20260924 VALUES
    ('purchase-invoice', '采购发票明细', 1, 'erp/report/financial-statement/index', 'ErpBusinessReportFinancialPurchaseInvoice', 'ep:tickets'),
    ('purchase-in-invoice-status', '采购入库开票状态', 2, 'erp/report/financial-statement/index', 'ErpBusinessReportFinancialPurchaseInInvoiceStatus', 'ep:data-analysis'),
    ('sale-return-red-offset', '销售退货红冲来源', 3, 'erp/report/financial-statement/index', 'ErpBusinessReportFinancialSaleReturnRedOffset', 'ep:document'),
    ('voucher', '凭证明细', 4, 'erp/report/financial-statement/index', 'ErpBusinessReportFinancialVoucher', 'ep:document'),
    ('voucher-attribution', '凭证归集来源', 5, 'erp/report/financial-statement/index', 'ErpBusinessReportFinancialVoucherAttribution', 'ep:connection'),
    ('voucher-word', '凭证字基础资料', 6, 'erp/report/financial-statement/index', 'ErpBusinessReportFinancialVoucherWord', 'ep:collection-tag'),
    ('accounting-subject', '会计科目及辅助核算', 7, 'erp/report/financial-statement/index', 'ErpBusinessReportFinancialSubject', 'ep:coin'),
    ('report-template', '财务报表项目模板', 8, 'erp/report/financial/report-template/index', 'ErpBusinessReportFinancialReportTemplate', 'ep:files'),
    ('balance-sheet', '资产负债表', 9, 'erp/report/financial/balance-sheet/index', 'ErpBusinessReportFinancialBalanceSheet', 'ep:histogram'),
    ('income-statement', '利润表', 10, 'erp/report/financial/income-statement/index', 'ErpBusinessReportFinancialIncomeStatement', 'ep:trend-charts'),
    ('cash-flow', '现金流量表', 11, 'erp/report/financial/cash-flow/index', 'ErpBusinessReportFinancialCashFlow', 'ep:money'),
    ('history', '历史财务口径', 12, 'erp/report/financial-statement/index', 'ErpBusinessReportFinancialHistory', 'ep:clock');

  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_business_report_financial_pages_20260924 p
      ON p.page_path = m.path
      WHERE m.deleted = b'0' AND m.parent_id = financial_group_id
        AND (m.type <> 2 OR IFNULL(m.component, '') NOT IN (p.component_path, 'erp/report/financial-statement/index', 'erp/report/financial/report-template/index', 'erp/report/financial/balance-sheet/index', 'erp/report/financial/income-statement/index', 'erp/report/financial/cash-flow/index'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report financial pages: existing financial page path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT p.page_name, '', 2, p.sort_order, financial_group_id, p.page_path, p.page_icon,
    p.component_path, p.component_name, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM tmp_business_report_financial_pages_20260924 p
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0'
    AND m.parent_id = financial_group_id AND m.path = p.page_path);

  UPDATE system_menu m JOIN tmp_business_report_financial_pages_20260924 p ON p.page_path = m.path
  SET m.name = p.page_name,
      m.sort = p.sort_order,
      m.icon = p.page_icon,
      m.component = p.component_path,
      m.component_name = p.component_name,
      m.status = 0,
      m.visible = b'1',
      m.keep_alive = b'1',
      m.always_show = b'1',
      m.update_time = NOW(),
      m.updater = '1'
  WHERE m.deleted = b'0' AND m.parent_id = financial_group_id AND m.type = 2;

  UPDATE system_menu
  SET visible = b'0', keep_alive = b'0', always_show = b'0', sort = 90,
      update_time = NOW(), updater = '1'
  WHERE deleted = b'0'
    AND parent_id = financial_group_id
    AND type = 2
    AND (path IN ('/erp/report/financial-statement', '/erp/business-report/financial-statement')
      OR path LIKE '/erp/report/financial/%');

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_financial_pages_20260924');
  SET got_lock = 0;

  SELECT m.id, m.parent_id, m.name, m.type, m.sort, m.path, m.component, m.component_name, m.permission, m.status, m.visible
  FROM system_menu m
  WHERE m.deleted = b'0'
    AND (m.id = financial_group_id OR m.parent_id = financial_group_id)
  ORDER BY m.parent_id, m.sort, m.id;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_financial_pages_20260924;
END$$
DELIMITER ;

CALL `erp_add_business_report_financial_pages_20260924`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_financial_pages_20260924`;

