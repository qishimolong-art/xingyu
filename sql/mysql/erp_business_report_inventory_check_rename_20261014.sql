-- 经营报表“配件盈亏明细表”菜单更名，2026-10-14。
-- 只更新经营报表 > 库存及商品追溯 > check 菜单名称；
-- 不新增、删除或覆盖角色、按钮、字段、数据权限及租户套餐关系。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_rename_inventory_check_report_20261014`;
DELIMITER $$
CREATE PROCEDURE `erp_rename_inventory_check_report_20261014`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE inventory_group_id BIGINT;
  DECLARE check_menu_id BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN
      DO RELEASE_LOCK('erp_rename_inventory_check_report_20261014');
    END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_rename_inventory_check_report_20261014', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'rename inventory check report: migration lock unavailable';
  END IF;

  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'rename inventory check report: expected one ERP root';
  END IF;
  SELECT id INTO erp_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND type = 1 AND parent_id = erp_id
        AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'rename inventory check report: expected one business-report group';
  END IF;
  SELECT id INTO report_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = erp_id
    AND path = 'business-report';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND type = 1 AND parent_id = report_id
        AND path = 'inventory') <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'rename inventory check report: expected one inventory group';
  END IF;
  SELECT id INTO inventory_group_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = report_id
    AND path = 'inventory';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND type = 2 AND parent_id = inventory_group_id
        AND path = 'check'
        AND component = 'erp/stock/report/index'
        AND component_name = 'ErpBusinessReportInventoryCheck') <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'rename inventory check report: expected one compatible check page';
  END IF;
  SELECT id INTO check_menu_id FROM system_menu
  WHERE deleted = b'0' AND type = 2 AND parent_id = inventory_group_id
    AND path = 'check'
    AND component = 'erp/stock/report/index'
    AND component_name = 'ErpBusinessReportInventoryCheck';

  IF EXISTS (SELECT 1 FROM system_menu
      WHERE deleted = b'0' AND parent_id = inventory_group_id
        AND name = '配件盈亏明细表' AND id <> check_menu_id) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'rename inventory check report: target menu name already exists';
  END IF;

  UPDATE system_menu
  SET name = '配件盈亏明细表',
      updater = '1',
      update_time = NOW()
  WHERE id = check_menu_id
    AND deleted = b'0'
    AND name <> '配件盈亏明细表';

  IF (SELECT name FROM system_menu WHERE id = check_menu_id) <> '配件盈亏明细表' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'rename inventory check report: post-check failed';
  END IF;

  COMMIT;
  DO RELEASE_LOCK('erp_rename_inventory_check_report_20261014');
  SET got_lock = 0;

  SELECT id, parent_id, name, type, sort, path, component, component_name,
         permission, status, visible, deleted
  FROM system_menu
  WHERE id = check_menu_id;
END$$
DELIMITER ;

CALL `erp_rename_inventory_check_report_20261014`();
DROP PROCEDURE IF EXISTS `erp_rename_inventory_check_report_20261014`;
