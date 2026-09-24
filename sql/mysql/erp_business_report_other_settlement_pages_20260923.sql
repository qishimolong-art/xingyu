-- 经营报表 M6 其他应收及其他应付独立页面入口，2026-09-23。
-- 将其他应收、其他应付、其他往来余额、账龄、核销和历史口径拆为独立经营报表页面。
-- 只新增或修正经营报表菜单入口，不修改其他应收、其他应付、资金、核销、销售应收或采购应付业务表，不改变业务流程。
-- 本脚本不写角色授权，不写租户套餐，不启用新核算开关。
-- 执行后需要在角色权限中勾选新增页面，并刷新菜单缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_other_settlement_pages_20260923`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_other_settlement_pages_20260923`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE other_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_other_settlement_pages_20260923'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_other_settlement_pages_20260923', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report other settlement pages: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report other settlement pages: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report other settlement pages: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report other settlement pages: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'other-settlement'
      AND (type <> 1 OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report other settlement pages: other-settlement group conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '其他应收及其他应付', '', 1, 6, report_id, 'other-settlement', 'ep:folder', '',
    'ErpBusinessReports_otherSettlement', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'other-settlement');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'other-settlement') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report other settlement pages: duplicate other-settlement groups';
  END IF;
  SELECT id INTO other_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'other-settlement';

  UPDATE system_menu
  SET name = '其他应收及其他应付', sort = 6, icon = 'ep:folder', component = '',
      component_name = 'ErpBusinessReports_otherSettlement', status = 0,
      visible = b'1', keep_alive = b'1', always_show = b'1', update_time = NOW(), updater = '1'
  WHERE id = other_group_id AND deleted = b'0' AND type = 1;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_other_settlement_pages_20260923;
  CREATE TEMPORARY TABLE tmp_business_report_other_settlement_pages_20260923 (
    page_path VARCHAR(64) PRIMARY KEY,
    page_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL,
    component_name VARCHAR(128) NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  INSERT INTO tmp_business_report_other_settlement_pages_20260923 VALUES
    ('other-receivable', '其他应收报表', 1, 'ErpBusinessReportOtherSettlementReceivable'),
    ('other-payable', '其他应付报表', 2, 'ErpBusinessReportOtherSettlementPayable'),
    ('balance-summary', '其他往来余额汇总', 3, 'ErpBusinessReportOtherSettlementBalanceSummary'),
    ('aging-analysis', '其他往来账龄分析', 4, 'ErpBusinessReportOtherSettlementAgingAnalysis'),
    ('writeoff-detail', '其他往来核销明细', 5, 'ErpBusinessReportOtherSettlementWriteoffDetail'),
    ('history', '历史其他往来口径', 6, 'ErpBusinessReportOtherSettlementHistory');

  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_business_report_other_settlement_pages_20260923 p
      ON p.page_path = m.path
      WHERE m.deleted = b'0' AND m.parent_id = other_group_id
        AND (m.type <> 2 OR IFNULL(m.component, '') <> 'erp/report/other-settlement/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report other settlement pages: existing page path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT p.page_name, '', 2, p.sort_order, other_group_id, p.page_path, 'ep:data-analysis',
    'erp/report/other-settlement/index', p.component_name, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM tmp_business_report_other_settlement_pages_20260923 p
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0'
    AND m.parent_id = other_group_id AND m.path = p.page_path);

  UPDATE system_menu m JOIN tmp_business_report_other_settlement_pages_20260923 p ON p.page_path = m.path
  SET m.name = p.page_name,
      m.sort = p.sort_order,
      m.icon = 'ep:data-analysis',
      m.component = 'erp/report/other-settlement/index',
      m.component_name = p.component_name,
      m.status = 0,
      m.visible = b'1',
      m.keep_alive = b'1',
      m.always_show = b'1',
      m.update_time = NOW(),
      m.updater = '1'
  WHERE m.deleted = b'0' AND m.parent_id = other_group_id AND m.type = 2;

  UPDATE system_menu
  SET visible = b'0',
      sort = CASE path
        WHEN '/erp/finance/receivable/report' THEN 90
        WHEN '/erp/finance/payable/report' THEN 91
        ELSE sort
      END,
      update_time = NOW(),
      updater = '1'
  WHERE deleted = b'0'
    AND parent_id = other_group_id
    AND type = 2
    AND path IN ('/erp/finance/receivable/report', '/erp/finance/payable/report');

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_other_settlement_pages_20260923');
  SET got_lock = 0;

  SELECT m.id, m.parent_id, m.name, m.type, m.sort, m.path, m.component, m.component_name, m.permission, m.status, m.visible
  FROM system_menu m
  WHERE m.deleted = b'0'
    AND (m.id = other_group_id OR m.parent_id = other_group_id)
  ORDER BY m.parent_id, m.sort, m.id;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_other_settlement_pages_20260923;
END$$
DELIMITER ;

CALL `erp_add_business_report_other_settlement_pages_20260923`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_other_settlement_pages_20260923`;
