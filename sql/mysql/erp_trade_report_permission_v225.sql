-- 经营报表1/4新版查询权限：仅在现有对应报表页追加四个按钮；不改菜单、角色、套餐既有数据。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP PROCEDURE IF EXISTS erp_trade_report_permission_v225;
DELIMITER $$
CREATE PROCEDURE erp_trade_report_permission_v225()
BEGIN
 DECLARE sale_menu BIGINT;
 DECLARE purchase_menu BIGINT;
 DECLARE locked INT DEFAULT 0;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION
 BEGIN
  ROLLBACK;
  IF locked=1 THEN DO RELEASE_LOCK('erp_trade_report_permission_v225'); END IF;
  RESIGNAL;
 END;
 SELECT GET_LOCK('erp_trade_report_permission_v225',10) INTO locked;
 IF locked IS NULL OR locked<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v225: migration lock unavailable'; END IF;
 START TRANSACTION;
 IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2 AND component='erp/sale/report/index')<>1
 OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2 AND component='erp/purchase/report/index')<>1
 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v225: expected unique existing sale and purchase report pages'; END IF;
 SELECT id INTO sale_menu FROM system_menu WHERE deleted=b'0' AND type=2 AND component='erp/sale/report/index';
 SELECT id INTO purchase_menu FROM system_menu WHERE deleted=b'0' AND type=2 AND component='erp/purchase/report/index';
 IF EXISTS(SELECT permission FROM system_menu WHERE deleted=b'0'
  AND permission IN('erp:sale-report-v2:query','erp:sale-report-v2:export','erp:purchase-report-v2:query','erp:purchase-report-v2:export') GROUP BY permission HAVING COUNT(*)>1)
 OR EXISTS(SELECT 1 FROM system_menu WHERE deleted=b'0' AND
  ((permission IN('erp:sale-report-v2:query','erp:sale-report-v2:export') AND (type<>3 OR parent_id<>sale_menu))
  OR (permission IN('erp:purchase-report-v2:query','erp:purchase-report-v2:export') AND (type<>3 OR parent_id<>purchase_menu))))
 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v225: existing report permission conflicts'; END IF;
 INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
 SELECT seed.name,seed.permission,3,seed.sort,seed.parent_id,'','','','',0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
 FROM(
  SELECT '新版销售商品报表查询' name,'erp:sale-report-v2:query' permission,80 sort,sale_menu parent_id
  UNION ALL SELECT '新版销售商品报表导出','erp:sale-report-v2:export',81,sale_menu
  UNION ALL SELECT '新版采购商品报表查询','erp:purchase-report-v2:query',80,purchase_menu
  UNION ALL SELECT '新版采购商品报表导出','erp:purchase-report-v2:export',81,purchase_menu
 ) seed WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
 COMMIT;
 DO RELEASE_LOCK('erp_trade_report_permission_v225');
END$$
DELIMITER ;
CALL erp_trade_report_permission_v225();
DROP PROCEDURE erp_trade_report_permission_v225;
