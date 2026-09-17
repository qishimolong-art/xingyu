-- 无单销售退货成本权限 00G。仅追加到原销售退货页面，不自动授权任何角色或套餐。
-- 执行前备份 system_menu，停止并发权限编辑；执行后管理员按需授权并刷新权限缓存/重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP PROCEDURE IF EXISTS erp_sale_return_current_cost_permission_20260909;
DELIMITER $$
CREATE PROCEDURE erp_sale_return_current_cost_permission_20260909()
BEGIN
  DECLARE sale_return_menu_id BIGINT;
  DECLARE locked INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF locked = 1 THEN DO RELEASE_LOCK('erp_sale_return_current_cost_permission_20260909'); END IF;
    RESIGNAL;
  END;
  SELECT GET_LOCK('erp_sale_return_current_cost_permission_20260909', 10) INTO locked;
  IF locked IS NULL OR locked <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '00G: permission migration lock unavailable';
  END IF;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2 AND component='erp/sale/return/index') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '00G: expected one existing sale return page';
  END IF;
  SELECT id INTO sale_return_menu_id FROM system_menu WHERE deleted=b'0' AND type=2 AND component='erp/sale/return/index';
  IF EXISTS (
    SELECT permission FROM system_menu WHERE deleted=b'0'
    AND permission IN ('erp:sale-return-current-cost:query','erp:sale-return-current-cost:confirm')
    GROUP BY permission HAVING COUNT(*) > 1
  ) OR EXISTS (
    SELECT 1 FROM system_menu WHERE deleted=b'0'
    AND permission IN ('erp:sale-return-current-cost:query','erp:sale-return-current-cost:confirm')
    AND (type<>3 OR parent_id<>sale_return_menu_id)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '00G: existing cost confirmation permission conflicts';
  END IF;
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '核算成本查询','erp:sale-return-current-cost:query',3,80,sale_return_menu_id,'','','','',0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-return-current-cost:query');
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '确认无单退货两套成本','erp:sale-return-current-cost:confirm',3,81,sale_return_menu_id,'','','','',0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-return-current-cost:confirm');
  COMMIT;
  DO RELEASE_LOCK('erp_sale_return_current_cost_permission_20260909');
END$$
DELIMITER ;
CALL erp_sale_return_current_cost_permission_20260909();
DROP PROCEDURE erp_sale_return_current_cost_permission_20260909;
