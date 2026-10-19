-- v255: 将销售管理分组中的两个新明细报表固定到最前面。
-- 安全边界：仅更新目标分组下可见页面菜单的 sort、updater、update_time；
-- 不修改菜单层级、权限码、角色授权、租户套餐或任何业务数据。
-- 执行前设置：SET @sale_detail_report_menu_order_schema = '已复核的目标数据库名';

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_sale_detail_report_menu_order_v255;
DELIMITER $$
CREATE PROCEDURE migrate_sale_detail_report_menu_order_v255()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE sale_group_id BIGINT;
  DECLARE quote_page_id BIGINT;
  DECLARE out_page_id BIGINT;
  DECLARE target_count INT DEFAULT 0;
  DECLARE got_lock INT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_sale_report_menu_order_v255;
    IF got_lock=1 THEN
      DO RELEASE_LOCK('erp_sale_detail_report_menu_order_v255');
    END IF;
    RESIGNAL;
  END;

  IF @sale_detail_report_menu_order_schema IS NULL
      OR CAST(DATABASE() AS BINARY)<>CAST(@sale_detail_report_menu_order_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v255: target database was not reviewed';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='system_menu') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v255: system_menu does not exist';
  END IF;

  SELECT GET_LOCK('erp_sale_detail_report_menu_order_v255',10) INTO got_lock;
  IF got_lock<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v255: migration lock unavailable';
  END IF;

  SELECT id INTO erp_id
  FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  SELECT id INTO report_id
  FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  SELECT id INTO sale_group_id
  FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=report_id AND path='sale';
  IF erp_id IS NULL OR report_id IS NULL OR sale_group_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v255: stable sale report menu path not found';
  END IF;

  SELECT COUNT(*),MAX(CASE WHEN path='quote-detail' THEN id END),MAX(CASE WHEN path='out-detail' THEN id END)
  INTO target_count,quote_page_id,out_page_id
  FROM system_menu
  WHERE deleted=b'0' AND parent_id=sale_group_id AND type=2
    AND status=0 AND visible=b'1' AND path IN ('quote-detail','out-detail');
  IF target_count<>2 OR quote_page_id IS NULL OR out_page_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v255: expected sale detail report pages not found';
  END IF;

  START TRANSACTION;
  DROP TEMPORARY TABLE IF EXISTS tmp_sale_report_menu_order_v255;
  CREATE TEMPORARY TABLE tmp_sale_report_menu_order_v255(
    menu_id BIGINT PRIMARY KEY,
    new_sort INT NOT NULL
  ) ENGINE=MEMORY;

  INSERT INTO tmp_sale_report_menu_order_v255(menu_id,new_sort)
  SELECT id,ROW_NUMBER() OVER (
    ORDER BY CASE path WHEN 'quote-detail' THEN 0 WHEN 'out-detail' THEN 1 ELSE 2 END,
      sort,id
  )
  FROM system_menu
  WHERE deleted=b'0' AND parent_id=sale_group_id AND type=2
    AND status=0 AND visible=b'1';

  UPDATE system_menu menu
  JOIN tmp_sale_report_menu_order_v255 target ON target.menu_id=menu.id
  SET menu.sort=target.new_sort,menu.updater='1',menu.update_time=NOW()
  WHERE menu.deleted=b'0' AND menu.parent_id=sale_group_id
    AND menu.type=2 AND menu.status=0 AND menu.visible=b'1'
    AND menu.sort<>target.new_sort;

  IF NOT EXISTS (SELECT 1 FROM system_menu
      WHERE id=quote_page_id AND deleted=b'0' AND parent_id=sale_group_id AND sort=1)
      OR NOT EXISTS (SELECT 1 FROM system_menu
      WHERE id=out_page_id AND deleted=b'0' AND parent_id=sale_group_id AND sort=2) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v255: sale detail report order verification failed';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_sale_report_menu_order_v255;
  COMMIT;
  DO RELEASE_LOCK('erp_sale_detail_report_menu_order_v255');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_sale_detail_report_menu_order_v255();
DROP PROCEDURE migrate_sale_detail_report_menu_order_v255;

SELECT child.id,child.name,child.path,child.sort
FROM system_menu child
JOIN system_menu sale_group ON sale_group.id=child.parent_id
  AND sale_group.deleted=b'0' AND sale_group.type=1 AND sale_group.path='sale'
JOIN system_menu report_group ON report_group.id=sale_group.parent_id
  AND report_group.deleted=b'0' AND report_group.type=1 AND report_group.path='business-report'
JOIN system_menu erp ON erp.id=report_group.parent_id
  AND erp.deleted=b'0' AND erp.type=1 AND erp.parent_id=0 AND erp.path='/erp'
WHERE child.deleted=b'0' AND child.type=2 AND child.status=0 AND child.visible=b'1'
ORDER BY child.sort,child.id;
