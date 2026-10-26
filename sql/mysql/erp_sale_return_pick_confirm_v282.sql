-- v282: 销售退货下单确认、仅拣货、拣货完成自动审批。
-- MySQL 5.7 / 8.0 compatible；可重复执行，不回填历史业务单据或拣货任务。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @current_schema := DATABASE();

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_return' AND COLUMN_NAME='confirm_user_id'),
  'SELECT 1', 'ALTER TABLE erp_sale_return ADD COLUMN confirm_user_id BIGINT DEFAULT NULL COMMENT ''下单确认人'' AFTER approve_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_return' AND COLUMN_NAME='confirm_time'),
  'SELECT 1', 'ALTER TABLE erp_sale_return ADD COLUMN confirm_time DATETIME DEFAULT NULL COMMENT ''下单确认时间'' AFTER confirm_user_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_return' AND COLUMN_NAME='frozen_cost_basis_signature'),
  'SELECT 1', 'ALTER TABLE erp_sale_return ADD COLUMN frozen_cost_basis_signature CHAR(64) CHARACTER SET ascii DEFAULT NULL COMMENT ''下单确认冻结成本基准'' AFTER confirm_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='sale_return_item_id'),
  'SELECT 1', 'ALTER TABLE erp_sale_pick_delivery_item ADD COLUMN sale_return_item_id BIGINT DEFAULT NULL COMMENT ''销售退货明细编号'' AFTER transfer_out_item_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@current_schema AND TABLE_NAME='erp_sale_return_current_cost_confirmation' AND COLUMN_NAME='stock_basis'),
  'SELECT 1', 'ALTER TABLE erp_sale_return_current_cost_confirmation ADD COLUMN stock_basis LONGTEXT NULL COMMENT ''下单确认冻结库存成本依据'' AFTER stock_signature');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=@current_schema
    AND TABLE_NAME='erp_sale_pick_delivery_item' AND INDEX_NAME='idx_sale_return_item'),
  'SELECT 1', 'ALTER TABLE erp_sale_pick_delivery_item ADD INDEX idx_sale_return_item (tenant_id,sale_return_item_id,deleted)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sale_return_menu_id := (SELECT parent_id FROM system_menu
  WHERE permission='erp:sale-return:update-status' AND deleted=b'0' ORDER BY id DESC LIMIT 1);

INSERT INTO system_menu
(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,
 creator,create_time,updater,update_time,deleted)
SELECT '销售退货下单确认','erp:sale-return:confirm',3,7,@sale_return_menu_id,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE @sale_return_menu_id IS NOT NULL AND NOT EXISTS(
  SELECT 1 FROM system_menu WHERE permission='erp:sale-return:confirm' AND deleted=b'0');

INSERT INTO system_menu
(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,
 creator,create_time,updater,update_time,deleted)
SELECT '销售退货撤销下单确认','erp:sale-return:cancel-confirm',3,8,@sale_return_menu_id,'','','',NULL,0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
FROM DUAL WHERE @sale_return_menu_id IS NOT NULL AND NOT EXISTS(
  SELECT 1 FROM system_menu WHERE permission='erp:sale-return:cancel-confirm' AND deleted=b'0');

SET @old_menu_id := (SELECT id FROM system_menu WHERE permission='erp:sale-return:update-status' AND deleted=b'0' ORDER BY id DESC LIMIT 1);
SET @confirm_menu_id := (SELECT id FROM system_menu WHERE permission='erp:sale-return:confirm' AND deleted=b'0' ORDER BY id DESC LIMIT 1);
SET @cancel_menu_id := (SELECT id FROM system_menu WHERE permission='erp:sale-return:cancel-confirm' AND deleted=b'0' ORDER BY id DESC LIMIT 1);

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
WHERE TABLE_SCHEMA=@current_schema AND ((TABLE_NAME='erp_sale_return' AND COLUMN_NAME IN ('confirm_user_id','confirm_time','frozen_cost_basis_signature'))
 OR (TABLE_NAME='erp_sale_pick_delivery_item' AND COLUMN_NAME='sale_return_item_id')
 OR (TABLE_NAME='erp_sale_return_current_cost_confirmation' AND COLUMN_NAME='stock_basis'))
ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT id,name,permission FROM system_menu WHERE permission IN ('erp:sale-return:confirm','erp:sale-return:cancel-confirm') AND deleted=b'0';

