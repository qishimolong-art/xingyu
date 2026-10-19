-- 付款明细查询：菜单、按钮及继承权限（MySQL 8）
-- 安全边界：不修改业务表结构，不删除或覆盖现有菜单、角色及租户套餐权限。
SET @payment_detail_report_schema := DATABASE();

DROP PROCEDURE IF EXISTS migrate_payment_detail_report_v259;
DELIMITER $$
CREATE PROCEDURE migrate_payment_detail_report_v259()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE payable_group_id BIGINT;
  DECLARE report_page_id BIGINT;
  DECLARE query_menu_id BIGINT;
  DECLARE export_menu_id BIGINT;
  DECLARE source_query_id BIGINT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_payment_detail_package_v259;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_payment_detail_report_v259'); END IF;
    RESIGNAL;
  END;

  IF @payment_detail_report_schema IS NULL OR @payment_detail_report_schema=''
      OR CAST(DATABASE() AS BINARY)<>CAST(@payment_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: no stable target database selected';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: MySQL 8 is required for window functions';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_finance_payment')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_finance_payment_item')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_voucher') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: required payment report tables are missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='erp_finance_payment' AND index_name='idx_erp_payment_payable_report')
      OR NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='erp_finance_payment_item' AND index_name='idx_payment_writeoff_status')
      OR NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='erp_voucher' AND index_name='idx_erp_voucher_report_source') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: existing report indexes are incomplete; review before migration';
  END IF;

  SELECT GET_LOCK('erp_payment_detail_report_v259',10) INTO got_lock;
  IF got_lock<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: migration lock unavailable'; END IF;
  START TRANSACTION;

  SELECT id INTO erp_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  SELECT id INTO business_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  SELECT id INTO fund_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty';
  SELECT id INTO payable_group_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=fund_report_id AND path='payable-detail';
  SELECT id INTO source_query_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:finance-payment:query';
  IF erp_id IS NULL OR business_report_id IS NULL OR fund_report_id IS NULL
      OR payable_group_id IS NULL OR source_query_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: stable payable report path or source permission not found';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id
      AND path='payment-detail' AND (type<>2 OR component<>'erp/report/payment-detail/index'
      OR component_name<>'ErpPaymentDetailReport')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: payment detail page path conflict';
  END IF;
  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:payment-detail-report:query','erp:payment-detail-report:export')
      GROUP BY permission HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: payment report permission conflict';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id
      AND path='payment-detail') THEN
    UPDATE system_menu SET sort=sort+1,update_time=NOW(),updater='1'
    WHERE deleted=b'0' AND parent_id=payable_group_id AND sort>=2;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
      keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('付款明细查询','',2,2,payable_group_id,'payment-detail','ep:list','erp/report/payment-detail/index',
      'ErpPaymentDetailReport',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO report_page_id FROM system_menu
  WHERE deleted=b'0' AND parent_id=payable_group_id AND path='payment-detail';

  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,status,visible,keep_alive,always_show,
    creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',0,b'1',b'0',b'0',
    '1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '付款明细查询' name,'erp:payment-detail-report:query' permission,1 sort_order
    UNION ALL SELECT '付款明细导出','erp:payment-detail-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_menu_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:payment-detail-report:query';
  SELECT id INTO export_menu_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:payment-detail-report:export';

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT payable_group_id UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id
    UNION ALL SELECT export_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.deleted=b'0' AND x.role_id=source.role_id
      AND x.tenant_id=source.tenant_id AND x.menu_id=target.menu_id);

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_payment_detail_package_v259;
  CREATE TEMPORARY TABLE tmp_payment_detail_package_v259(package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_payment_detail_package_v259(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id FROM system_tenant_package p
    JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id UNION ALL SELECT payable_group_id
      UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id UNION ALL SELECT export_menu_id) target
    WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
  ) missing GROUP BY package_id;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package p
      JOIN tmp_payment_detail_package_v259 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v259: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_payment_detail_package_v259 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW()
  WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_payment_detail_package_v259;

  COMMIT;
  DO RELEASE_LOCK('erp_payment_detail_report_v259');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_payment_detail_report_v259();
DROP PROCEDURE migrate_payment_detail_report_v259;

SELECT id,parent_id,name,type,sort,path,component,component_name,permission
FROM system_menu WHERE deleted=b'0' AND (path='payment-detail'
  OR permission IN ('erp:payment-detail-report:query','erp:payment-detail-report:export'))
ORDER BY parent_id,type,sort,id;
