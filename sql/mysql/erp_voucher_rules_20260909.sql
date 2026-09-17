-- 小企业凭证规则：增量迁移；不更改科目、余额、历史凭证、角色授权。
-- 先迁移，再部署前后端；由管理员分配新增权限，财务保存配置后启用模板。
CREATE TABLE IF NOT EXISTS erp_voucher_rule_state (
  id BIGINT NOT NULL AUTO_INCREMENT,
  state_key VARCHAR(100) NOT NULL COMMENT 'CONFIG / CONTEXT:type:id / AUDIT:uuid',
  version BIGINT NOT NULL DEFAULT 1,
  payload LONGTEXT NOT NULL COMMENT '配置、补充信息或不可变操作快照 JSON',
  creator VARCHAR(64) DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0', tenant_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY(id), UNIQUE KEY uk_tenant_state(tenant_id,state_key,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭证规则与记账依据';

SET @voucher_rule_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_voucher_item' AND column_name='auxiliaries'),
  'SELECT 1', 'ALTER TABLE erp_voucher_item ADD COLUMN auxiliaries JSON NULL COMMENT ''多维辅助核算，旧单维字段保留''');
PREPARE voucher_rule_stmt FROM @voucher_rule_ddl;
EXECUTE voucher_rule_stmt;
DEALLOCATE PREPARE voucher_rule_stmt;
SET @voucher_rule_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_voucher' AND column_name='generation_snapshot'),
  'SELECT 1', 'ALTER TABLE erp_voucher ADD COLUMN generation_snapshot LONGTEXT NULL COMMENT ''生成时来源、规则版本、金额依据快照''');
PREPARE voucher_rule_stmt FROM @voucher_rule_ddl;
EXECUTE voucher_rule_stmt;
DEALLOCATE PREPARE voucher_rule_stmt;

SET @voucher_rule_parent = (SELECT parent_id FROM system_menu WHERE component='erp/accounting/voucher-attribution/index' AND deleted=b'0' ORDER BY id LIMIT 1);
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
SELECT '凭证生成规则','erp:voucher-rule:query',2,5,@voucher_rule_parent,'voucher-rule','lucide:settings','erp/accounting/voucher-rule/index','ErpVoucherRule',0,b'1',b'1',b'1','1','1',b'0'
WHERE @voucher_rule_parent IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='erp:voucher-rule:query' AND deleted=b'0');
SET @voucher_rule_menu=(SELECT id FROM system_menu WHERE permission='erp:voucher-rule:query' AND deleted=b'0' ORDER BY id LIMIT 1);
INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
SELECT '维护凭证规则','erp:voucher-rule:update',3,1,@voucher_rule_menu,'','','',NULL,0,b'1',b'1',b'1','1','1',b'0'
WHERE @voucher_rule_menu IS NOT NULL AND NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='erp:voucher-rule:update' AND deleted=b'0');
