-- 应付账明细查询报表：查询索引、菜单及权限（MySQL 8）
-- 安全边界：不删除或覆盖业务、菜单、角色及套餐权限数据。
SET @payable_detail_report_schema := DATABASE();

DROP PROCEDURE IF EXISTS migrate_payable_detail_schema_v257;
DELIMITER $$
CREATE PROCEDURE migrate_payable_detail_schema_v257()
BEGIN
  IF @payable_detail_report_schema IS NULL OR @payable_detail_report_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: no target database selected';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: MySQL 8 is required for report window functions';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_purchase_in')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_purchase_return')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_purchase_price_adjust')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_payable_other')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_finance_payment') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: required payable source tables are missing';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_in' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,in_time,supplier_id,dept_id,id') x) THEN
    ALTER TABLE erp_purchase_in ADD INDEX idx_erp_purchase_in_payable_report
      (tenant_id,deleted,status,in_time,supplier_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_return' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,return_time,supplier_id,dept_id,id') x) THEN
    ALTER TABLE erp_purchase_return ADD INDEX idx_erp_purchase_return_payable_report
      (tenant_id,deleted,status,return_time,supplier_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_price_adjust' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,adjust_time,supplier_id,dept_id,id') x) THEN
    ALTER TABLE erp_purchase_price_adjust ADD INDEX idx_erp_purchase_adjust_payable_report
      (tenant_id,deleted,status,adjust_time,supplier_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_payable_other' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,biz_time,supplier_id,dept_id,id') x) THEN
    ALTER TABLE erp_payable_other ADD INDEX idx_erp_payable_other_detail_report
      (tenant_id,deleted,status,biz_time,supplier_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_finance_payment' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,payment_time,supplier_id,dept_id,id') x) THEN
    ALTER TABLE erp_finance_payment ADD INDEX idx_erp_payment_payable_report
      (tenant_id,deleted,status,payment_time,supplier_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_payable_other' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,source_type,source_id') x) THEN
    ALTER TABLE erp_payable_other ADD INDEX idx_erp_payable_other_report_source
      (tenant_id,deleted,status,source_type,source_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_voucher' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,source_biz_type,source_biz_id') x) THEN
    ALTER TABLE erp_voucher ADD INDEX idx_erp_voucher_report_source
      (tenant_id,deleted,source_biz_type,source_biz_id);
  END IF;
END$$
DELIMITER ;

CALL migrate_payable_detail_schema_v257();
DROP PROCEDURE migrate_payable_detail_schema_v257;

DROP PROCEDURE IF EXISTS migrate_payable_detail_menu_v257;
DELIMITER $$
CREATE PROCEDURE migrate_payable_detail_menu_v257()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE receivable_group_id BIGINT;
  DECLARE payable_group_id BIGINT;
  DECLARE report_page_id BIGINT;
  DECLARE query_menu_id BIGINT;
  DECLARE export_menu_id BIGINT;
  DECLARE source_query_id BIGINT;
  DECLARE target_sort INT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_payable_detail_report_v257'); END IF;
    RESIGNAL;
  END;

  IF @payable_detail_report_schema IS NULL OR CAST(DATABASE() AS BINARY)<>CAST(@payable_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: target database changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_payable_detail_report_v257',10) INTO got_lock;
  IF got_lock<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: migration lock unavailable'; END IF;
  START TRANSACTION;

  SELECT id INTO erp_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  SELECT id INTO business_report_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  SELECT id INTO fund_report_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty';
  SELECT id INTO receivable_group_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=fund_report_id AND path='receivable-detail';
  SELECT id INTO source_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:payable-account:query';
  IF erp_id IS NULL OR business_report_id IS NULL OR fund_report_id IS NULL
      OR receivable_group_id IS NULL OR source_query_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: stable report path, receivable group, or source permission not found';
  END IF;
  SELECT sort+1 INTO target_sort FROM system_menu WHERE id=receivable_group_id;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id AND path='payable-detail'
      AND (type<>1 OR name<>'应付报表')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: payable report group path conflict';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id AND path='payable-detail') THEN
    UPDATE system_menu SET sort=sort+1,update_time=NOW(),updater='1'
    WHERE deleted=b'0' AND parent_id=fund_report_id AND sort>=target_sort;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
      keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('应付报表','',1,target_sort,fund_report_id,'payable-detail','ep:coin','','',0,b'1',b'1',b'1',
      '1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO payable_group_id FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id AND path='payable-detail';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id AND path='account-detail'
      AND (type<>2 OR component<>'erp/report/payable-detail/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: payable detail page path conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '应付账明细查询','',2,1,payable_group_id,'account-detail','ep:list','erp/report/payable-detail/index',
    'ErpPayableDetailReport',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id AND path='account-detail');
  SELECT id INTO report_page_id FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id AND path='account-detail';

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:payable-detail-report:query','erp:payable-detail-report:export') GROUP BY permission HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: report permission conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,status,visible,keep_alive,always_show,
    creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '应付账明细查询' name,'erp:payable-detail-report:query' permission,1 sort_order
    UNION ALL SELECT '应付账明细导出','erp:payable-detail-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_menu_id FROM system_menu WHERE deleted=b'0' AND permission='erp:payable-detail-report:query';
  SELECT id INTO export_menu_id FROM system_menu WHERE deleted=b'0' AND permission='erp:payable-detail-report:export';

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
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_payable_detail_package_v257;
  CREATE TEMPORARY TABLE tmp_payable_detail_package_v257(package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_payable_detail_package_v257(package_id,missing_ids)
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
      JOIN tmp_payable_detail_package_v257 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v257: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_payable_detail_package_v257 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW() WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_payable_detail_package_v257;

  COMMIT;
  DO RELEASE_LOCK('erp_payable_detail_report_v257');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_payable_detail_menu_v257();
DROP PROCEDURE migrate_payable_detail_menu_v257;

SELECT id,parent_id,name,type,sort,path,component,permission FROM system_menu WHERE deleted=b'0'
AND (path='payable-detail' OR permission IN ('erp:payable-detail-report:query','erp:payable-detail-report:export')
  OR (path='account-detail' AND component='erp/report/payable-detail/index')) ORDER BY parent_id,type,sort,id;
