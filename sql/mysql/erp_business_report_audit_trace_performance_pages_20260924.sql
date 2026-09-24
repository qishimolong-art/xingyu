-- 经营报表 M12 异常审计及绩效独立页面入口，2026-09-24。
-- 将原单反向追踪、业务链路、未闭环业务、异常待办、操作审计、岗位绩效和历史口径拆为独立经营报表页面。
-- 只新增或修正经营报表菜单入口，不修改销售、采购、库存、资金、日志、绩效或业务表，不改变业务流程。
-- 本脚本不写角色授权，不写租户套餐，不启用双成本、异常规则、绩效评分或核算开关。
-- 执行后需要在角色权限中勾选新增页面，并刷新菜单缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_audit_pages_20260924`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_audit_pages_20260924`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE audit_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_audit_pages_20260924'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_audit_pages_20260924', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report audit pages: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report audit pages: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report audit pages: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report audit pages: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'audit-trace-performance'
      AND (type <> 1 OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report audit pages: audit group conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '异常审计及绩效', '', 1, 12, report_id, 'audit-trace-performance', 'ep:folder', '',
    'ErpBusinessReports_auditTracePerformance', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'audit-trace-performance');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'audit-trace-performance') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report audit pages: duplicate audit groups';
  END IF;
  SELECT id INTO audit_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'audit-trace-performance';

  UPDATE system_menu
  SET name = '异常审计及绩效', sort = 12, icon = 'ep:folder', component = '',
      component_name = 'ErpBusinessReports_auditTracePerformance', status = 0,
      visible = b'1', keep_alive = b'1', always_show = b'1', update_time = NOW(), updater = '1'
  WHERE id = audit_group_id AND deleted = b'0' AND type = 1;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_audit_pages_20260924;
  CREATE TEMPORARY TABLE tmp_business_report_audit_pages_20260924 (
    page_path VARCHAR(64) PRIMARY KEY,
    page_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL,
    component_name VARCHAR(128) NOT NULL,
    page_icon VARCHAR(64) NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  INSERT INTO tmp_business_report_audit_pages_20260924 VALUES
    ('reverse-trace', '原单与反向业务追踪', 1, 'ErpBusinessReportAuditReverseTrace', 'ep:connection'),
    ('business-chain', '业务链路追踪', 2, 'ErpBusinessReportAuditBusinessChain', 'ep:share'),
    ('open-loop', '未闭环业务清单', 3, 'ErpBusinessReportAuditOpenLoop', 'ep:warning'),
    ('exception-todo', '异常规则与待办', 4, 'ErpBusinessReportAuditExceptionTodo', 'ep:bell'),
    ('operation-audit', '操作日志审计', 5, 'ErpBusinessReportAuditOperationAudit', 'ep:document-checked'),
    ('performance-score', '岗位绩效评分', 6, 'ErpBusinessReportAuditPerformanceScore', 'ep:medal'),
    ('history', '历史异常审计口径', 7, 'ErpBusinessReportAuditHistory', 'ep:clock');

  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_business_report_audit_pages_20260924 p
      ON p.page_path = m.path
      WHERE m.deleted = b'0' AND m.parent_id = audit_group_id
        AND (m.type <> 2 OR IFNULL(m.component, '') <> 'erp/report/audit-trace-performance/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report audit pages: existing audit page path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT p.page_name, '', 2, p.sort_order, audit_group_id, p.page_path, p.page_icon,
    'erp/report/audit-trace-performance/index', p.component_name, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM tmp_business_report_audit_pages_20260924 p
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0'
    AND m.parent_id = audit_group_id AND m.path = p.page_path);

  UPDATE system_menu m JOIN tmp_business_report_audit_pages_20260924 p ON p.page_path = m.path
  SET m.name = p.page_name,
      m.sort = p.sort_order,
      m.icon = p.page_icon,
      m.component = 'erp/report/audit-trace-performance/index',
      m.component_name = p.component_name,
      m.status = 0,
      m.visible = b'1',
      m.keep_alive = b'1',
      m.always_show = b'1',
      m.update_time = NOW(),
      m.updater = '1'
  WHERE m.deleted = b'0' AND m.parent_id = audit_group_id AND m.type = 2;

  UPDATE system_menu
  SET visible = b'0', keep_alive = b'0', always_show = b'0', sort = 90,
      update_time = NOW(), updater = '1'
  WHERE deleted = b'0'
    AND parent_id = audit_group_id
    AND type = 2
    AND path IN ('/erp/report/audit-trace-performance', '/erp/business-report/audit-trace-performance');

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_audit_pages_20260924');
  SET got_lock = 0;

  SELECT m.id, m.parent_id, m.name, m.type, m.sort, m.path, m.component, m.component_name, m.permission, m.status, m.visible
  FROM system_menu m
  WHERE m.deleted = b'0'
    AND (m.id = audit_group_id OR m.parent_id = audit_group_id)
  ORDER BY m.parent_id, m.sort, m.id;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_audit_pages_20260924;
END$$
DELIMITER ;

CALL `erp_add_business_report_audit_pages_20260924`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_audit_pages_20260924`;
