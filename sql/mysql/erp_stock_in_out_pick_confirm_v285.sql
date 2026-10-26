-- v285: 其它入库/其它出库下单确认、仅拣货、拣货完成自动审批。
-- MySQL 5.7 / 8.0 compatible；可重复执行，不回填历史状态、确认信息、库存流水或拣货任务。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @current_schema := DATABASE();

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_stock_in' AND COLUMN_NAME='confirm_user_id'),
  'SELECT 1', 'ALTER TABLE erp_stock_in ADD COLUMN confirm_user_id BIGINT DEFAULT NULL COMMENT ''下单确认人'' AFTER approve_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_stock_in' AND COLUMN_NAME='confirm_time'),
  'SELECT 1', 'ALTER TABLE erp_stock_in ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT ''下单确认时间'' AFTER confirm_user_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_stock_in' AND COLUMN_NAME='approve_user_id'),
  'SELECT 1', 'ALTER TABLE erp_stock_in ADD COLUMN approve_user_id BIGINT DEFAULT NULL COMMENT ''审批人'' AFTER confirm_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_stock_out' AND COLUMN_NAME='confirm_user_id'),
  'SELECT 1', 'ALTER TABLE erp_stock_out ADD COLUMN confirm_user_id BIGINT DEFAULT NULL COMMENT ''下单确认人'' AFTER approve_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_stock_out' AND COLUMN_NAME='confirm_time'),
  'SELECT 1', 'ALTER TABLE erp_stock_out ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT ''下单确认时间'' AFTER confirm_user_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_stock_out' AND COLUMN_NAME='approve_user_id'),
  'SELECT 1', 'ALTER TABLE erp_stock_out ADD COLUMN approve_user_id BIGINT DEFAULT NULL COMMENT ''审批人'' AFTER confirm_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='stock_in_item_id'),
  'SELECT 1', CONCAT('ALTER TABLE erp_sale_pick_delivery_item ADD COLUMN stock_in_item_id BIGINT DEFAULT NULL COMMENT ''其它入库明细编号'' AFTER ',
    IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='warehouse_move_item_id'),
      'warehouse_move_item_id', IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='sale_return_item_id'), 'sale_return_item_id', 'transfer_out_item_id'))));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='stock_out_item_id'),
  'SELECT 1', 'ALTER TABLE erp_sale_pick_delivery_item ADD COLUMN stock_out_item_id BIGINT DEFAULT NULL COMMENT ''其它出库明细编号'' AFTER stock_in_item_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND INDEX_NAME='idx_stock_in_item'),
  'SELECT 1', 'ALTER TABLE erp_sale_pick_delivery_item ADD INDEX idx_stock_in_item (tenant_id,stock_in_item_id,deleted)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND INDEX_NAME='idx_stock_out_item'),
  'SELECT 1', 'ALTER TABLE erp_sale_pick_delivery_item ADD INDEX idx_stock_out_item (tenant_id,stock_out_item_id,deleted)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @stock_in_parent_id := (SELECT parent_id FROM system_menu WHERE permission='erp:stock-in:update-status' AND deleted=b'0' ORDER BY id DESC LIMIT 1);
SET @stock_out_parent_id := (SELECT parent_id FROM system_menu WHERE permission='erp:stock-out:update-status' AND deleted=b'0' ORDER BY id DESC LIMIT 1);

INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
SELECT '其它入库下单确认','erp:stock-in:confirm',3,7,@stock_in_parent_id,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE @stock_in_parent_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='erp:stock-in:confirm' AND deleted=b'0');
INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
SELECT '其它入库撤销确认','erp:stock-in:cancel-confirm',3,8,@stock_in_parent_id,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE @stock_in_parent_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='erp:stock-in:cancel-confirm' AND deleted=b'0');
INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
SELECT '其它出库下单确认','erp:stock-out:confirm',3,7,@stock_out_parent_id,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE @stock_out_parent_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='erp:stock-out:confirm' AND deleted=b'0');
INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
SELECT '其它出库撤销确认','erp:stock-out:cancel-confirm',3,8,@stock_out_parent_id,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE @stock_out_parent_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='erp:stock-out:cancel-confirm' AND deleted=b'0');

INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
SELECT DISTINCT old.role_id,target.id,'1',NOW(),'1',NOW(),b'0',old.tenant_id
FROM system_role_menu old
INNER JOIN system_menu legacy ON legacy.id=old.menu_id AND legacy.deleted=b'0'
INNER JOIN system_menu target ON target.deleted=b'0' AND target.permission IN ('erp:stock-in:confirm','erp:stock-in:cancel-confirm')
WHERE old.deleted=b'0' AND legacy.permission='erp:stock-in:update-status'
  AND NOT EXISTS(SELECT 1 FROM system_role_menu current WHERE current.role_id=old.role_id AND current.menu_id=target.id AND current.tenant_id=old.tenant_id AND current.deleted=b'0');

INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
SELECT DISTINCT old.role_id,target.id,'1',NOW(),'1',NOW(),b'0',old.tenant_id
FROM system_role_menu old
INNER JOIN system_menu legacy ON legacy.id=old.menu_id AND legacy.deleted=b'0'
INNER JOIN system_menu target ON target.deleted=b'0' AND target.permission IN ('erp:stock-out:confirm','erp:stock-out:cancel-confirm')
WHERE old.deleted=b'0' AND legacy.permission='erp:stock-out:update-status'
  AND NOT EXISTS(SELECT 1 FROM system_role_menu current WHERE current.role_id=old.role_id AND current.menu_id=target.id AND current.tenant_id=old.tenant_id AND current.deleted=b'0');

SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=@current_schema AND ((TABLE_NAME IN ('erp_stock_in','erp_stock_out') AND COLUMN_NAME IN ('confirm_user_id','confirm_time','approve_user_id'))
  OR (TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME IN ('stock_in_item_id','stock_out_item_id')))
ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT id,name,permission FROM system_menu
WHERE permission IN ('erp:stock-in:confirm','erp:stock-in:cancel-confirm','erp:stock-out:confirm','erp:stock-out:cancel-confirm') AND deleted=b'0';
