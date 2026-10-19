-- 账户流水报表：索引、菜单、按钮及继承权限（MySQL 8）
-- 安全边界：仅补充报表索引和权限种子；不删除业务数据，不覆盖既有角色或租户套餐权限。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @account_ledger_schema := DATABASE();

DROP PROCEDURE IF EXISTS precheck_account_ledger_report_v262;
DELIMITER $$
CREATE PROCEDURE precheck_account_ledger_report_v262()
BEGIN
  DECLARE required_columns INT DEFAULT 0;

  IF @account_ledger_schema IS NULL OR @account_ledger_schema = ''
      OR CAST(DATABASE() AS BINARY) <> CAST(@account_ledger_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: no stable target database selected';
  END IF;
  IF VERSION() < '8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: MySQL 8 is required for window functions';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'system_menu')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'system_role_menu')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'system_tenant_package') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: required permission tables are missing';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids) <> 'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: invalid tenant package menu JSON';
  END IF;

  SELECT COUNT(*) INTO required_columns
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND (
    (table_name = 'erp_finance_receipt' AND column_name IN
      ('id','tenant_id','deleted','status','account_id','receipt_time')) OR
    (table_name = 'erp_finance_payment' AND column_name IN
      ('id','tenant_id','deleted','status','account_id','payment_time')) OR
    (table_name = 'erp_receivable_other_income' AND column_name IN
      ('id','tenant_id','deleted','status','account_id','biz_time')) OR
    (table_name = 'erp_payable_expense' AND column_name IN
      ('id','tenant_id','deleted','status','account_id','biz_time')) OR
    (table_name = 'erp_finance_transfer' AND column_name IN
      ('id','tenant_id','deleted','status','out_account_id','in_account_id','transfer_time')));
  IF required_columns <> 31 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: required account ledger source columns are missing';
  END IF;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1
      OR (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND path = 'business-report' AND parent_id = (SELECT id FROM system_menu
        WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp')) <> 1
      OR (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND path = 'account-fund' AND parent_id = (SELECT id FROM system_menu
        WHERE deleted = b'0' AND type = 1 AND path = 'business-report'
          AND parent_id = (SELECT id FROM system_menu
            WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp'))) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: stable account fund report path not found';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND permission = 'erp:account:query') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: source account query permission is missing or duplicated';
  END IF;
  IF EXISTS (SELECT permission FROM system_menu WHERE deleted = b'0' AND permission IN
      ('erp:account-ledger-report:query','erp:account-ledger-report:export')
      GROUP BY permission HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: account ledger report permission is duplicated';
  END IF;
END$$
DELIMITER ;

CALL precheck_account_ledger_report_v262();
DROP PROCEDURE precheck_account_ledger_report_v262;

-- MySQL DDL 会自动提交，因此在完整预检通过后单独幂等补充索引。
DROP PROCEDURE IF EXISTS add_account_ledger_report_indexes_v262;
DELIMITER $$
CREATE PROCEDURE add_account_ledger_report_indexes_v262()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_list
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt'
      GROUP BY index_name) i
      WHERE i.columns_list = 'tenant_id,deleted,status,account_id,receipt_time,id') THEN
    CREATE INDEX idx_account_ledger_receipt
      ON erp_finance_receipt(tenant_id,deleted,status,account_id,receipt_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_list
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment'
      GROUP BY index_name) i
      WHERE i.columns_list = 'tenant_id,deleted,status,account_id,payment_time,id') THEN
    CREATE INDEX idx_account_ledger_payment
      ON erp_finance_payment(tenant_id,deleted,status,account_id,payment_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_list
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income'
      GROUP BY index_name) i
      WHERE i.columns_list = 'tenant_id,deleted,status,account_id,biz_time,id') THEN
    CREATE INDEX idx_account_ledger_other_income
      ON erp_receivable_other_income(tenant_id,deleted,status,account_id,biz_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_list
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_payable_expense'
      GROUP BY index_name) i
      WHERE i.columns_list = 'tenant_id,deleted,status,account_id,biz_time,id') THEN
    CREATE INDEX idx_account_ledger_expense
      ON erp_payable_expense(tenant_id,deleted,status,account_id,biz_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_list
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_finance_transfer'
      GROUP BY index_name) i
      WHERE i.columns_list = 'tenant_id,deleted,status,out_account_id,transfer_time,id') THEN
    CREATE INDEX idx_account_ledger_transfer_out
      ON erp_finance_transfer(tenant_id,deleted,status,out_account_id,transfer_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_list
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_finance_transfer'
      GROUP BY index_name) i
      WHERE i.columns_list = 'tenant_id,deleted,status,in_account_id,transfer_time,id') THEN
    CREATE INDEX idx_account_ledger_transfer_in
      ON erp_finance_transfer(tenant_id,deleted,status,in_account_id,transfer_time,id);
  END IF;
END$$
DELIMITER ;

CALL add_account_ledger_report_indexes_v262();
DROP PROCEDURE add_account_ledger_report_indexes_v262;

DROP PROCEDURE IF EXISTS migrate_account_ledger_report_v262;
DELIMITER $$
CREATE PROCEDURE migrate_account_ledger_report_v262()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_group_id BIGINT;
  DECLARE legacy_page_id BIGINT;
  DECLARE report_page_id BIGINT;
  DECLARE query_menu_id BIGINT;
  DECLARE export_menu_id BIGINT;
  DECLARE source_query_id BIGINT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_account_ledger_package_v262;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_account_ledger_report_v262'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_account_ledger_report_v262',10) INTO got_lock;
  IF got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: migration lock unavailable';
  END IF;
  START TRANSACTION;

  SELECT id INTO erp_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp';
  SELECT id INTO business_report_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = erp_id AND path = 'business-report';
  SELECT id INTO fund_group_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = business_report_id AND path = 'account-fund';
  SELECT id INTO legacy_page_id FROM system_menu
  WHERE deleted = b'0' AND type = 2 AND parent_id = fund_group_id AND path = 'account-ledger';
  SELECT id INTO source_query_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:account:query';
  IF erp_id IS NULL OR business_report_id IS NULL OR fund_group_id IS NULL
      OR legacy_page_id IS NULL OR source_query_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: stable menu path or source permission not found';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND parent_id = fund_group_id
      AND path = 'account-ledger-report' AND (type <> 2
        OR component <> 'erp/report/account-ledger/index'
        OR component_name <> 'ErpAccountLedgerReport')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: account ledger page path conflict';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND parent_id = fund_group_id
      AND path = 'account-ledger-report') THEN
    UPDATE system_menu SET sort = sort + 1,update_time = NOW(),updater = '1'
    WHERE deleted = b'0' AND parent_id = fund_group_id AND sort >= 2 AND sort < 90;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
      keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('账户流水报表','',2,2,fund_group_id,'account-ledger-report','ep:list',
      'erp/report/account-ledger/index','ErpAccountLedgerReport',0,b'1',b'1',b'1',
      '1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO report_page_id FROM system_menu
  WHERE deleted = b'0' AND parent_id = fund_group_id AND path = 'account-ledger-report';

  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,status,visible,keep_alive,always_show,
    creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',0,b'1',b'0',b'0',
    '1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '账户流水查询' name,'erp:account-ledger-report:query' permission,1 sort_order
    UNION ALL SELECT '账户流水导出','erp:account-ledger-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0' AND m.permission = seed.permission);
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND permission IN ('erp:account-ledger-report:query','erp:account-ledger-report:export')
      AND (type <> 3 OR parent_id <> report_page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: account ledger button permission parent conflict';
  END IF;
  SELECT id INTO query_menu_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:account-ledger-report:query';
  SELECT id INTO export_menu_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:account-ledger-report:export';
  IF query_menu_id IS NULL OR export_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: account ledger button permission creation failed';
  END IF;

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN (
    SELECT business_report_id menu_id UNION ALL SELECT fund_group_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id UNION ALL SELECT export_menu_id) target
  WHERE source.deleted = b'0' AND source.menu_id = source_query_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.deleted = b'0'
      AND x.role_id = source.role_id AND x.tenant_id = source.tenant_id AND x.menu_id = target.menu_id);

  DROP TEMPORARY TABLE IF EXISTS tmp_account_ledger_package_v262;
  CREATE TEMPORARY TABLE tmp_account_ledger_package_v262(
    package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_account_ledger_package_v262(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id FROM system_tenant_package p
    JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_group_id
      UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id UNION ALL SELECT export_menu_id) target
    WHERE p.deleted = b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')) missing
  GROUP BY package_id;
  SELECT character_maximum_length INTO package_limit FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'system_tenant_package' AND column_name = 'menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package p
      JOIN tmp_account_ledger_package_v262 t ON t.package_id = p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids)) > package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v262: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_account_ledger_package_v262 t ON t.package_id = p.id
  SET p.menu_ids = JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time = NOW()
  WHERE p.deleted = b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_account_ledger_package_v262;

  COMMIT;
  DO RELEASE_LOCK('erp_account_ledger_report_v262');
  SET got_lock = 0;
END$$
DELIMITER ;

CALL migrate_account_ledger_report_v262();
DROP PROCEDURE migrate_account_ledger_report_v262;

SELECT id,parent_id,name,type,sort,path,component,component_name,permission
FROM system_menu WHERE deleted = b'0' AND (path IN ('account-ledger','account-ledger-report')
  OR permission IN ('erp:account-ledger-report:query','erp:account-ledger-report:export'))
ORDER BY parent_id,type,sort,id;

SELECT table_name,index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_list
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND index_name LIKE 'idx_account_ledger_%'
GROUP BY table_name,index_name ORDER BY table_name,index_name;

SELECT COUNT(DISTINCT CONCAT(role_id,':',tenant_id)) inherited_roles
FROM system_role_menu WHERE deleted = b'0'
  AND menu_id = (SELECT id FROM system_menu WHERE deleted = b'0'
    AND permission = 'erp:account-ledger-report:query');
SELECT COUNT(*) inherited_packages FROM system_tenant_package p
WHERE p.deleted = b'0' AND JSON_CONTAINS(p.menu_ids,CAST((SELECT id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:account-ledger-report:query') AS CHAR),'$');
