-- 经营报表 M9 部门利润及驾驶舱独立页面入口，2026-09-24。
-- 将部门利润、公司利润、内部抵销、未实现利润、已实现利润、驾驶舱、指标下钻和历史口径拆为独立经营报表页面。
-- 只新增或修正经营报表菜单入口，不修改销售、采购、库存、调拨、费用、资金、往来或业务表，不改变业务流程。
-- 本脚本不写角色授权，不写租户套餐，不启用双成本或利润核算开关。
-- 执行后需要在角色权限中勾选新增页面，并刷新菜单缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_profit_pages_20260924`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_profit_pages_20260924`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE profit_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_profit_pages_20260924'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_profit_pages_20260924', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report profit pages: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report profit pages: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report profit pages: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report profit pages: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'profit'
      AND (type <> 1 OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report profit pages: profit group conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '部门利润及驾驶舱', '', 1, 9, report_id, 'profit', 'ep:folder', '',
    'ErpBusinessReports_profit', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'profit');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'profit') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report profit pages: duplicate profit groups';
  END IF;
  SELECT id INTO profit_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'profit';

  UPDATE system_menu
  SET name = '部门利润及驾驶舱', sort = 9, icon = 'ep:folder', component = '',
      component_name = 'ErpBusinessReports_profit', status = 0,
      visible = b'1', keep_alive = b'1', always_show = b'1', update_time = NOW(), updater = '1'
  WHERE id = profit_group_id AND deleted = b'0' AND type = 1;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_profit_pages_20260924;
  CREATE TEMPORARY TABLE tmp_business_report_profit_pages_20260924 (
    page_path VARCHAR(64) PRIMARY KEY,
    page_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL,
    component_name VARCHAR(128) NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  INSERT INTO tmp_business_report_profit_pages_20260924 VALUES
    ('department-profit', '部门利润汇总', 1, 'ErpBusinessReportProfitDepartment'),
    ('company-profit', '公司经营利润汇总', 2, 'ErpBusinessReportProfitCompany'),
    ('internal-offset', '内部收入成本抵销', 3, 'ErpBusinessReportProfitInternalOffset'),
    ('unrealized-profit', '未实现内部利润明细', 4, 'ErpBusinessReportProfitUnrealized'),
    ('realized-profit', '已实现内部利润调整', 5, 'ErpBusinessReportProfitRealized'),
    ('cockpit', '经营驾驶舱', 6, 'ErpBusinessReportProfitCockpit'),
    ('metric-drilldown', '指标下钻明细', 7, 'ErpBusinessReportProfitMetricDrilldown'),
    ('history', '历史利润口径', 8, 'ErpBusinessReportProfitHistory');

  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_business_report_profit_pages_20260924 p
      ON p.page_path = m.path
      WHERE m.deleted = b'0' AND m.parent_id = profit_group_id
        AND (m.type <> 2 OR IFNULL(m.component, '') <> 'erp/report/profit-dashboard/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report profit pages: existing profit page path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT p.page_name, '', 2, p.sort_order, profit_group_id, p.page_path, 'ep:data-analysis',
    'erp/report/profit-dashboard/index', p.component_name, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM tmp_business_report_profit_pages_20260924 p
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0'
    AND m.parent_id = profit_group_id AND m.path = p.page_path);

  UPDATE system_menu m JOIN tmp_business_report_profit_pages_20260924 p ON p.page_path = m.path
  SET m.name = p.page_name,
      m.sort = p.sort_order,
      m.icon = 'ep:data-analysis',
      m.component = 'erp/report/profit-dashboard/index',
      m.component_name = p.component_name,
      m.status = 0,
      m.visible = b'1',
      m.keep_alive = b'1',
      m.always_show = b'1',
      m.update_time = NOW(),
      m.updater = '1'
  WHERE m.deleted = b'0' AND m.parent_id = profit_group_id AND m.type = 2;

  UPDATE system_menu
  SET visible = b'0',
      sort = 90,
      update_time = NOW(),
      updater = '1'
  WHERE deleted = b'0'
    AND parent_id = profit_group_id
    AND type = 2
    AND path IN ('/erp/system-report', '/erp/report/system');

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_profit_pages_20260924');
  SET got_lock = 0;

  SELECT m.id, m.parent_id, m.name, m.type, m.sort, m.path, m.component, m.component_name, m.permission, m.status, m.visible
  FROM system_menu m
  WHERE m.deleted = b'0'
    AND (m.id = profit_group_id OR m.parent_id = profit_group_id)
  ORDER BY m.parent_id, m.sort, m.id;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_profit_pages_20260924;
END$$
DELIMITER ;

CALL `erp_add_business_report_profit_pages_20260924`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_profit_pages_20260924`;
