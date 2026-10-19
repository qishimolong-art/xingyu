-- 收款明细查询报表：查询索引、菜单及权限（MySQL 8）
-- 安全边界：不删除或覆盖业务、菜单、角色及套餐权限数据。
SET @receipt_detail_report_schema := DATABASE();

DROP PROCEDURE IF EXISTS migrate_receipt_detail_schema_v258;
DELIMITER $$
CREATE PROCEDURE migrate_receipt_detail_schema_v258()
BEGIN
  IF @receipt_detail_report_schema IS NULL OR @receipt_detail_report_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: no target database selected';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: MySQL 8 is required for report window functions';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_finance_receipt')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_finance_receipt_item') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: receipt tables are missing';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_finance_receipt' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)=
        'tenant_id,deleted,status,receipt_time,customer_id,dept_id,id') x) THEN
    ALTER TABLE erp_finance_receipt ADD INDEX idx_erp_receipt_detail_report
      (tenant_id,deleted,status,receipt_time,customer_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_finance_receipt_item' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) IN
        ('tenant_id,deleted,write_off_status,receipt_id,biz_type,biz_id,id',
         'tenant_id,deleted,write_off_status,biz_type,biz_id,receipt_id')) x) THEN
    ALTER TABLE erp_finance_receipt_item ADD INDEX idx_erp_receipt_item_detail_report
      (tenant_id,deleted,write_off_status,receipt_id,biz_type,biz_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_voucher' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)=
        'tenant_id,deleted,source_biz_type,source_biz_id') x) THEN
    ALTER TABLE erp_voucher ADD INDEX idx_erp_voucher_report_source
      (tenant_id,deleted,source_biz_type,source_biz_id);
  END IF;
END$$
DELIMITER ;

CALL migrate_receipt_detail_schema_v258();
DROP PROCEDURE migrate_receipt_detail_schema_v258;

DROP PROCEDURE IF EXISTS migrate_receipt_detail_menu_v258;
DELIMITER $$
CREATE PROCEDURE migrate_receipt_detail_menu_v258()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE payable_group_id BIGINT;
  DECLARE report_page_id BIGINT;
  DECLARE query_menu_id BIGINT;
  DECLARE export_menu_id BIGINT;
  DECLARE source_query_id BIGINT;
  DECLARE source_export_id BIGINT;
  DECLARE target_sort INT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_receipt_detail_report_v258'); END IF;
    RESIGNAL;
  END;

  IF @receipt_detail_report_schema IS NULL
      OR CAST(DATABASE() AS BINARY)<>CAST(@receipt_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: target database changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_receipt_detail_report_v258',10) INTO got_lock;
  IF got_lock<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: migration lock unavailable';
  END IF;
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
  WHERE deleted=b'0' AND permission='erp:finance-receipt:query';
  SELECT id INTO source_export_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:finance-receipt:export';
  IF erp_id IS NULL OR business_report_id IS NULL OR fund_report_id IS NULL
      OR payable_group_id IS NULL OR source_query_id IS NULL OR source_export_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: stable report path or receipt permissions not found';
  END IF;
  SELECT sort+1 INTO target_sort FROM system_menu WHERE id=payable_group_id;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id
      AND path='receipt-detail' AND (type<>2 OR component<>'erp/report/receipt-detail/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: receipt detail page path conflict';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id
      AND path='receipt-detail') THEN
    UPDATE system_menu SET sort=sort+1,update_time=NOW(),updater='1'
    WHERE deleted=b'0' AND parent_id=fund_report_id AND sort>=target_sort;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,
      visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('收款明细查询','',2,target_sort,fund_report_id,'receipt-detail','ep:list',
      'erp/report/receipt-detail/index','ErpReceiptDetailReport',0,b'1',b'1',b'0',
      '1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO report_page_id FROM system_menu
  WHERE deleted=b'0' AND parent_id=fund_report_id AND path='receipt-detail';

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:receipt-detail-report:query','erp:receipt-detail-report:export')
      GROUP BY permission HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: report permission conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',0,b'1',b'0',b'0',
    '1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '收款明细查询' name,'erp:receipt-detail-report:query' permission,1 sort_order
    UNION ALL SELECT '收款明细导出','erp:receipt-detail-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m
    WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_menu_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:receipt-detail-report:query';
  SELECT id INTO export_menu_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:receipt-detail-report:export';

  DROP TEMPORARY TABLE IF EXISTS tmp_receipt_detail_role_v258;
  CREATE TEMPORARY TABLE tmp_receipt_detail_role_v258(
    role_id BIGINT NOT NULL,tenant_id BIGINT NOT NULL,menu_id BIGINT NOT NULL,
    PRIMARY KEY(role_id,tenant_id,menu_id));
  INSERT IGNORE INTO tmp_receipt_detail_role_v258(role_id,tenant_id,menu_id)
  SELECT source.role_id,source.tenant_id,target.menu_id FROM system_role_menu source
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id;
  INSERT IGNORE INTO tmp_receipt_detail_role_v258(role_id,tenant_id,menu_id)
  SELECT source.role_id,source.tenant_id,target.menu_id FROM system_role_menu source
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT export_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_export_id;
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT target.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',target.tenant_id
  FROM tmp_receipt_detail_role_v258 target
  WHERE NOT EXISTS (SELECT 1 FROM system_role_menu current
    WHERE current.deleted=b'0' AND current.role_id=target.role_id
      AND current.tenant_id=target.tenant_id AND current.menu_id=target.menu_id);
  DROP TEMPORARY TABLE IF EXISTS tmp_receipt_detail_role_v258;

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_receipt_detail_package_menu_v258;
  CREATE TEMPORARY TABLE tmp_receipt_detail_package_menu_v258(
    package_id BIGINT NOT NULL,menu_id BIGINT NOT NULL,PRIMARY KEY(package_id,menu_id));
  INSERT IGNORE INTO tmp_receipt_detail_package_menu_v258(package_id,menu_id)
  SELECT p.id,target.menu_id FROM system_tenant_package p
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id) target
  WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$');
  INSERT IGNORE INTO tmp_receipt_detail_package_menu_v258(package_id,menu_id)
  SELECT p.id,target.menu_id FROM system_tenant_package p
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT export_menu_id) target
  WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_export_id AS CHAR),'$');
  DROP TEMPORARY TABLE IF EXISTS tmp_receipt_detail_package_v258;
  CREATE TEMPORARY TABLE tmp_receipt_detail_package_v258(
    package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_receipt_detail_package_v258(package_id,missing_ids)
  SELECT target.package_id,JSON_ARRAYAGG(target.menu_id)
  FROM tmp_receipt_detail_package_menu_v258 target
  INNER JOIN system_tenant_package p ON p.id=target.package_id AND p.deleted=b'0'
  WHERE NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
  GROUP BY target.package_id;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package p
      JOIN tmp_receipt_detail_package_v258 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v258: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_receipt_detail_package_v258 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW()
  WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_receipt_detail_package_v258;
  DROP TEMPORARY TABLE IF EXISTS tmp_receipt_detail_package_menu_v258;

  COMMIT;
  DO RELEASE_LOCK('erp_receipt_detail_report_v258');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_receipt_detail_menu_v258();
DROP PROCEDURE migrate_receipt_detail_menu_v258;

SELECT id,parent_id,name,type,sort,path,component,permission FROM system_menu WHERE deleted=b'0'
AND (path='receipt-detail' OR permission IN
  ('erp:receipt-detail-report:query','erp:receipt-detail-report:export'))
ORDER BY parent_id,type,sort,id;
