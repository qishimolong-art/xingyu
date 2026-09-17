-- 采购来源核算确认权限 v222。仅追加到原采购入库页面，不自动授权任何角色或套餐。
-- 执行前备份 system_menu，停止并发权限编辑；执行后管理员按需授权并刷新权限缓存/重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP PROCEDURE IF EXISTS erp_purchase_cost_permission_v222;
DELIMITER $$
CREATE PROCEDURE erp_purchase_cost_permission_v222()
BEGIN
  DECLARE purchase_menu_id BIGINT;
  DECLARE locked INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF locked = 1 THEN DO RELEASE_LOCK('erp_purchase_cost_permission_v222'); END IF;
    RESIGNAL;
  END;
  SELECT GET_LOCK('erp_purchase_cost_permission_v222', 10) INTO locked;
  IF locked IS NULL OR locked <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v222: permission migration lock unavailable';
  END IF;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2 AND component='erp/purchase/in/index') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v222: expected one existing purchase inbound page';
  END IF;
  SELECT id INTO purchase_menu_id FROM system_menu WHERE deleted=b'0' AND type=2 AND component='erp/purchase/in/index';
  IF EXISTS (
    SELECT permission FROM system_menu WHERE deleted=b'0'
    AND permission IN ('erp:purchase-cost-confirmation:query','erp:purchase-cost-confirmation:confirm')
    GROUP BY permission HAVING COUNT(*) > 1
  ) OR EXISTS (
    SELECT 1 FROM system_menu WHERE deleted=b'0'
    AND permission IN ('erp:purchase-cost-confirmation:query','erp:purchase-cost-confirmation:confirm')
    AND (type<>3 OR parent_id<>purchase_menu_id)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v222: existing cost confirmation permission conflicts';
  END IF;
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '核算成本查询','erp:purchase-cost-confirmation:query',3,80,purchase_menu_id,'','','','',0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-cost-confirmation:query');
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '确认采购未税成本','erp:purchase-cost-confirmation:confirm',3,81,purchase_menu_id,'','','','',0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-cost-confirmation:confirm');
  COMMIT;
  DO RELEASE_LOCK('erp_purchase_cost_permission_v222');
END$$
DELIMITER ;
CALL erp_purchase_cost_permission_v222();
DROP PROCEDURE erp_purchase_cost_permission_v222;
