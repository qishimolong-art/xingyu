-- 经营报表 M4 调拨移仓及在途独立页面入口，2026-09-23。
-- 将“调拨移仓及在途”拆为独立经营报表页面。
-- 只新增或修正经营报表菜单入口，不修改调拨、移仓、库存、销售或财务业务表，不改变业务流程。
-- 本脚本不启用双成本核算，不执行库存游标，不写角色授权，不写租户套餐。
-- 执行后需要在角色权限中勾选新增页面，并刷新菜单缓存或重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_transfer_pages_20260923`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_transfer_pages_20260923`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE transfer_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_transfer_pages_20260923'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_transfer_pages_20260923', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report transfer pages: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report transfer pages: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report transfer pages: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report transfer pages: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'transfer'
      AND (type <> 1 OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report transfer pages: transfer group conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '调拨移仓及在途', '', 1, 4, report_id, 'transfer', 'ep:folder', '',
    'ErpBusinessReports_transfer', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'transfer');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'transfer') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report transfer pages: duplicate transfer groups';
  END IF;
  SELECT id INTO transfer_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'transfer';

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_transfer_pages_20260923;
  CREATE TEMPORARY TABLE tmp_business_report_transfer_pages_20260923 (
    page_path VARCHAR(64) PRIMARY KEY,
    page_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL,
    component_name VARCHAR(128) NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  INSERT INTO tmp_business_report_transfer_pages_20260923 VALUES
    ('ledger', '调拨出入库台账', 1, 'ErpBusinessReportTransferLedger'),
    ('in-transit', '调拨在途明细', 2, 'ErpBusinessReportTransferInTransit'),
    ('move', '移仓流水明细', 3, 'ErpBusinessReportTransferMove'),
    ('price-difference', '调拨差价分析', 4, 'ErpBusinessReportTransferPriceDifference'),
    ('chain', '调拨链路追踪', 5, 'ErpBusinessReportTransferChain'),
    ('legacy', '历史调拨口径', 6, 'ErpBusinessReportTransferLegacy');

  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_business_report_transfer_pages_20260923 p
      ON p.page_path = m.path
      WHERE m.deleted = b'0' AND m.parent_id = transfer_group_id
        AND (m.type <> 2 OR IFNULL(m.component, '') <> 'erp/report/transfer/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report transfer pages: existing transfer page path conflicts';
  END IF;

  UPDATE system_menu
  SET visible = b'0', keep_alive = b'0', update_time = NOW(), updater = '1'
  WHERE deleted = b'0'
    AND parent_id = transfer_group_id
    AND type = 2
    AND (component = 'erp/stock/transfer-ledger/index' OR path = '/erp/stock/transfer-ledger');

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT p.page_name, '', 2, p.sort_order, transfer_group_id, p.page_path, 'ep:data-analysis',
    'erp/report/transfer/index', p.component_name, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM tmp_business_report_transfer_pages_20260923 p
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0'
    AND m.parent_id = transfer_group_id AND m.path = p.page_path);

  UPDATE system_menu m JOIN tmp_business_report_transfer_pages_20260923 p ON p.page_path = m.path
  SET m.name = p.page_name,
      m.sort = p.sort_order,
      m.icon = 'ep:data-analysis',
      m.component = 'erp/report/transfer/index',
      m.component_name = p.component_name,
      m.status = 0,
      m.visible = b'1',
      m.keep_alive = b'1',
      m.always_show = b'1',
      m.update_time = NOW(),
      m.updater = '1'
  WHERE m.deleted = b'0' AND m.parent_id = transfer_group_id AND m.type = 2;

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_transfer_pages_20260923');
  SET got_lock = 0;

  SELECT m.id, m.parent_id, m.name, m.type, m.sort, m.path, m.component, m.component_name, m.permission, m.status, m.visible
  FROM system_menu m
  WHERE m.deleted = b'0'
    AND (m.id = transfer_group_id OR m.parent_id = transfer_group_id)
  ORDER BY m.parent_id, m.sort, m.id;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_transfer_pages_20260923;
END$$
DELIMITER ;

CALL `erp_add_business_report_transfer_pages_20260923`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_transfer_pages_20260923`;
