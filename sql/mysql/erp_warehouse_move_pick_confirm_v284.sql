-- v284: 仓库移货下单确认、仅拣货、拣货完成自动审核。
-- MySQL 5.7 / 8.0 compatible；可重复执行，不回填历史业务单据或拣货任务。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @current_schema := DATABASE();

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_warehouse_move' AND COLUMN_NAME='confirm_user_id'),
  'SELECT 1', 'ALTER TABLE erp_warehouse_move ADD COLUMN confirm_user_id BIGINT DEFAULT NULL COMMENT ''下单确认人'' AFTER approve_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_warehouse_move' AND COLUMN_NAME='confirm_time'),
  'SELECT 1', 'ALTER TABLE erp_warehouse_move ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT ''下单确认时间'' AFTER confirm_user_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='warehouse_move_item_id'),
  'SELECT 1', CONCAT('ALTER TABLE erp_sale_pick_delivery_item ADD COLUMN warehouse_move_item_id BIGINT DEFAULT NULL COMMENT ''仓库移货明细编号'' AFTER ',
    IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='sale_return_item_id'),
      'sale_return_item_id', 'transfer_out_item_id')));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item'
    AND INDEX_NAME='idx_warehouse_move_item'),
  'SELECT 1', 'ALTER TABLE erp_sale_pick_delivery_item ADD INDEX idx_warehouse_move_item (tenant_id,warehouse_move_item_id,deleted)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @warehouse_move_menu_id := (SELECT parent_id FROM system_menu
  WHERE permission='erp:warehouse-move:update-status' AND deleted=b'0' ORDER BY id DESC LIMIT 1);

INSERT INTO system_menu
(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,
 creator,create_time,updater,update_time,deleted)
SELECT '仓库移货下单确认','erp:warehouse-move:confirm',3,7,@warehouse_move_menu_id,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE @warehouse_move_menu_id IS NOT NULL AND NOT EXISTS(
  SELECT 1 FROM system_menu WHERE permission='erp:warehouse-move:confirm' AND deleted=b'0');

INSERT INTO system_menu
(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,
 creator,create_time,updater,update_time,deleted)
SELECT '仓库移货撤销下单确认','erp:warehouse-move:cancel-confirm',3,8,@warehouse_move_menu_id,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE @warehouse_move_menu_id IS NOT NULL AND NOT EXISTS(
  SELECT 1 FROM system_menu WHERE permission='erp:warehouse-move:cancel-confirm' AND deleted=b'0');

SET @old_menu_id := (SELECT id FROM system_menu
  WHERE permission='erp:warehouse-move:update-status' AND deleted=b'0' ORDER BY id DESC LIMIT 1);
SET @confirm_menu_id := (SELECT id FROM system_menu
  WHERE permission='erp:warehouse-move:confirm' AND deleted=b'0' ORDER BY id DESC LIMIT 1);
SET @cancel_menu_id := (SELECT id FROM system_menu
  WHERE permission='erp:warehouse-move:cancel-confirm' AND deleted=b'0' ORDER BY id DESC LIMIT 1);

INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
SELECT DISTINCT old.role_id,@confirm_menu_id,'1',NOW(),'1',NOW(),b'0',old.tenant_id
FROM system_role_menu old
WHERE old.menu_id=@old_menu_id AND old.deleted=b'0' AND @confirm_menu_id IS NOT NULL
  AND NOT EXISTS(SELECT 1 FROM system_role_menu target WHERE target.role_id=old.role_id
    AND target.menu_id=@confirm_menu_id AND target.tenant_id=old.tenant_id AND target.deleted=b'0');

INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
SELECT DISTINCT old.role_id,@cancel_menu_id,'1',NOW(),'1',NOW(),b'0',old.tenant_id
FROM system_role_menu old
WHERE old.menu_id=@old_menu_id AND old.deleted=b'0' AND @cancel_menu_id IS NOT NULL
  AND NOT EXISTS(SELECT 1 FROM system_role_menu target WHERE target.role_id=old.role_id
    AND target.menu_id=@cancel_menu_id AND target.tenant_id=old.tenant_id AND target.deleted=b'0');

SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=@current_schema AND ((TABLE_NAME='erp_warehouse_move'
  AND COLUMN_NAME IN ('confirm_user_id','confirm_time')) OR
  (TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='warehouse_move_item_id'))
ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT id,name,permission FROM system_menu
WHERE permission IN ('erp:warehouse-move:confirm','erp:warehouse-move:cancel-confirm') AND deleted=b'0';
