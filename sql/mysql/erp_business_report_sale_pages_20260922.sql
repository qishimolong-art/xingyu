-- 经营报表 M1 销售及客户经营独立页面入口，2026-09-22。
-- 将“销售及客户经营”下原页面内的多个页签拆为独立菜单页面。
-- 只新增经营报表菜单入口，不修改销售业务表、业务流程、角色授权、租户套餐或核算开关。
-- 执行后需要在角色权限中勾选新增页面，并保留对应旧业务查询/导出按钮权限。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_business_report_sale_pages_20260922`;
DELIMITER $$
CREATE PROCEDURE `erp_add_business_report_sale_pages_20260922`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE sale_group_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_report_sale_pages_20260922'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_report_sale_pages_20260922', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report sale pages: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report sale pages: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report sale pages: business-report path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report sale pages: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'sale'
      AND (type <> 1 OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report sale pages: sale group conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '销售及客户经营', '', 1, 1, report_id, 'sale', 'ep:folder', '',
    'ErpBusinessReports_sale', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = report_id AND path = 'sale');

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = report_id AND path = 'sale') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report sale pages: duplicate sale groups';
  END IF;
  SELECT id INTO sale_group_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'sale';

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_sale_pages_20260922;
  CREATE TEMPORARY TABLE tmp_business_report_sale_pages_20260922 (
    page_path VARCHAR(64) PRIMARY KEY,
    page_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL,
    component_name VARCHAR(128) NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  INSERT INTO tmp_business_report_sale_pages_20260922 VALUES
    ('detail', '销售综合明细', 1, 'ErpBusinessReportSaleDetail'),
    ('customer-summary', '客户销售汇总', 2, 'ErpBusinessReportSaleCustomerSummary'),
    ('product-summary', '商品销售汇总', 3, 'ErpBusinessReportSaleProductSummary'),
    ('staff-summary', '业务员/部门汇总', 4, 'ErpBusinessReportSaleStaffSummary'),
    ('gross-warning', '价格毛利异常', 5, 'ErpBusinessReportSaleGrossWarning'),
    ('order', '销售订单履约', 6, 'ErpBusinessReportSaleOrderFulfillment'),
    ('pick', '拣货情况', 7, 'ErpBusinessReportSalePick'),
    ('delivery', '送货情况', 8, 'ErpBusinessReportSaleDelivery'),
    ('return', '退货原因分析', 9, 'ErpBusinessReportSaleReturnReason'),
    ('adjust', '销售调价业务', 10, 'ErpBusinessReportSaleAdjust'),
    ('receivable', '客户经营分析', 11, 'ErpBusinessReportSaleReceivable'),
    ('legacy', '历史口径', 12, 'ErpBusinessReportSaleLegacy');

  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_business_report_sale_pages_20260922 p
      ON p.page_path = m.path
      WHERE m.deleted = b'0' AND m.parent_id = sale_group_id
        AND (m.type <> 2 OR IFNULL(m.component, '') <> 'erp/sale/report/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'business report sale pages: existing sale page path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT p.page_name, '', 2, p.sort_order, sale_group_id, p.page_path, 'ep:data-analysis',
    'erp/sale/report/index', p.component_name, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM tmp_business_report_sale_pages_20260922 p
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0'
    AND m.parent_id = sale_group_id AND m.path = p.page_path);

  UPDATE system_menu m JOIN tmp_business_report_sale_pages_20260922 p ON p.page_path = m.path
  SET m.name = p.page_name,
      m.sort = p.sort_order,
      m.icon = 'ep:data-analysis',
      m.component = 'erp/sale/report/index',
      m.component_name = p.component_name,
      m.status = 0,
      m.visible = b'1',
      m.keep_alive = b'1',
      m.always_show = b'1',
      m.update_time = NOW(),
      m.updater = '1'
  WHERE m.deleted = b'0' AND m.parent_id = sale_group_id AND m.type = 2;

  COMMIT;
  DO RELEASE_LOCK('erp_business_report_sale_pages_20260922');
  SET got_lock = 0;

  SELECT m.id, m.parent_id, m.name, m.type, m.sort, m.path, m.component, m.component_name, m.permission, m.status, m.visible
  FROM system_menu m
  WHERE m.deleted = b'0'
    AND (m.id = sale_group_id OR m.parent_id = sale_group_id)
  ORDER BY m.parent_id, m.sort, m.id;

  DROP TEMPORARY TABLE IF EXISTS tmp_business_report_sale_pages_20260922;
END$$
DELIMITER ;

CALL `erp_add_business_report_sale_pages_20260922`();
DROP PROCEDURE IF EXISTS `erp_add_business_report_sale_pages_20260922`;
