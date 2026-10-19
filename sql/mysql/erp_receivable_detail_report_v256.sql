-- 应收账明细查询报表：审批时间、查询索引、菜单及权限（MySQL 8）
-- 安全边界：不删除业务/权限数据；历史审批时间只按可靠业务证据回填。
SET @receivable_detail_report_schema := DATABASE();

DROP PROCEDURE IF EXISTS migrate_receivable_detail_schema_v256;
DELIMITER $$
CREATE PROCEDURE migrate_receivable_detail_schema_v256()
BEGIN
  IF @receivable_detail_report_schema IS NULL OR @receivable_detail_report_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: no target database selected';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: MySQL 8 is required for report window functions';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_finance_receipt')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_receivable_other') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: required receivable tables are missing';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_finance_receipt' AND column_name='approve_time') THEN
    ALTER TABLE erp_finance_receipt ADD COLUMN approve_time DATETIME NULL COMMENT '审核时间' AFTER receipt_time;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_receivable_other' AND column_name='approve_time') THEN
    ALTER TABLE erp_receivable_other ADD COLUMN approve_time DATETIME NULL COMMENT '审核时间' AFTER biz_time;
  END IF;

  -- 销售出库：审批库存流水是可靠证据。
  UPDATE erp_sale_out so
  INNER JOIN (SELECT tenant_id,biz_id,MIN(create_time) approve_time FROM erp_stock_record
    WHERE deleted=b'0' AND biz_type=50 GROUP BY tenant_id,biz_id) evidence
    ON evidence.tenant_id=so.tenant_id AND evidence.biz_id=so.id
  SET so.approve_time=evidence.approve_time
  WHERE so.deleted=b'0' AND so.status=20 AND so.approve_time IS NULL;

  -- 收款单：仅已生效核销明细的生效时间可作为历史审批证据。
  UPDATE erp_finance_receipt fr
  INNER JOIN (SELECT tenant_id,receipt_id,MIN(write_off_time) approve_time FROM erp_finance_receipt_item
    WHERE deleted=b'0' AND write_off_status=1 AND write_off_time IS NOT NULL GROUP BY tenant_id,receipt_id) evidence
    ON evidence.tenant_id=fr.tenant_id AND evidence.receipt_id=fr.id
  SET fr.approve_time=evidence.approve_time
  WHERE fr.deleted=b'0' AND fr.status=20 AND fr.approve_time IS NULL;

  -- 系统生成的“收款单优惠”调账可精确追溯其来源收款审批时间；其余历史调账保持 NULL。
  UPDATE erp_receivable_other ro
  INNER JOIN erp_finance_receipt fr ON fr.tenant_id=ro.tenant_id AND fr.id=ro.source_id
    AND fr.deleted=b'0' AND fr.status=20 AND fr.approve_time IS NOT NULL
  SET ro.approve_time=fr.approve_time
  WHERE ro.deleted=b'0' AND ro.status=20 AND ro.approve_time IS NULL AND ro.source_type='收款单优惠';

  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_return' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,return_time,customer_id,dept_id,id') x) THEN
    ALTER TABLE erp_sale_return ADD INDEX idx_erp_sale_return_receivable_report
      (tenant_id,deleted,status,return_time,customer_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_price_adjust' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,adjust_date,customer_id,dept_id,id') x) THEN
    ALTER TABLE erp_sale_price_adjust ADD INDEX idx_erp_sale_adjust_receivable_report
      (tenant_id,deleted,status,adjust_date,customer_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_finance_receipt' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,receipt_time,customer_id,dept_id,id') x) THEN
    ALTER TABLE erp_finance_receipt ADD INDEX idx_erp_receipt_receivable_report
      (tenant_id,deleted,status,receipt_time,customer_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,biz_time,customer_id,dept_id,id') x) THEN
    ALTER TABLE erp_receivable_other ADD INDEX idx_erp_receivable_other_detail_report
      (tenant_id,deleted,status,biz_time,customer_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_writeoff' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,write_off_time,customer_id,dept_id,id') x) THEN
    ALTER TABLE erp_receivable_writeoff ADD INDEX idx_erp_receivable_writeoff_detail_report
      (tenant_id,deleted,write_off_time,customer_id,dept_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_finance_receipt_item' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,write_off_status,biz_type,biz_id,receipt_id') x) THEN
    ALTER TABLE erp_finance_receipt_item ADD INDEX idx_erp_receipt_item_detail_report
      (tenant_id,deleted,write_off_status,biz_type,biz_id,receipt_id);
  END IF;
END$$
DELIMITER ;

CALL migrate_receivable_detail_schema_v256();
DROP PROCEDURE migrate_receivable_detail_schema_v256;

DROP PROCEDURE IF EXISTS migrate_receivable_detail_menu_v256;
DELIMITER $$
CREATE PROCEDURE migrate_receivable_detail_menu_v256()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE receivable_group_id BIGINT;
  DECLARE report_page_id BIGINT;
  DECLARE query_menu_id BIGINT;
  DECLARE export_menu_id BIGINT;
  DECLARE source_query_id BIGINT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_receivable_detail_report_v256'); END IF;
    RESIGNAL;
  END;

  IF @receivable_detail_report_schema IS NULL OR CAST(DATABASE() AS BINARY)<>CAST(@receivable_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: target database changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_receivable_detail_report_v256',10) INTO got_lock;
  IF got_lock<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: migration lock unavailable'; END IF;
  START TRANSACTION;

  SELECT id INTO erp_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  SELECT id INTO business_report_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  SELECT id INTO fund_report_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty';
  SELECT id INTO source_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:receivable-account:query';
  IF erp_id IS NULL OR business_report_id IS NULL OR fund_report_id IS NULL OR source_query_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: stable report path or source permission not found';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id AND path='receivable-detail'
      AND (type<>1 OR name<>'应收报表')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: receivable report group path conflict';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id AND path='receivable-detail') THEN
    UPDATE system_menu SET sort=sort+1,update_time=NOW(),updater='1'
    WHERE deleted=b'0' AND parent_id=fund_report_id AND sort>=1;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
      keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('应收报表','',1,1,fund_report_id,'receivable-detail','ep:wallet','','',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO receivable_group_id FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id AND path='receivable-detail';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=receivable_group_id AND path='account-detail'
      AND (type<>2 OR component<>'erp/report/receivable-detail/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: receivable detail page path conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '应收账明细查询','',2,1,receivable_group_id,'account-detail','ep:list','erp/report/receivable-detail/index',
    'ErpReceivableDetailReport',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=receivable_group_id AND path='account-detail');
  SELECT id INTO report_page_id FROM system_menu WHERE deleted=b'0' AND parent_id=receivable_group_id AND path='account-detail';

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:receivable-detail-report:query','erp:receivable-detail-report:export') GROUP BY permission HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: report permission conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,status,visible,keep_alive,always_show,
    creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '应收账明细查询' name,'erp:receivable-detail-report:query' permission,1 sort_order
    UNION ALL SELECT '应收账明细导出','erp:receivable-detail-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_menu_id FROM system_menu WHERE deleted=b'0' AND permission='erp:receivable-detail-report:query';
  SELECT id INTO export_menu_id FROM system_menu WHERE deleted=b'0' AND permission='erp:receivable-detail-report:export';

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT receivable_group_id UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id
    UNION ALL SELECT export_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.deleted=b'0' AND x.role_id=source.role_id
      AND x.tenant_id=source.tenant_id AND x.menu_id=target.menu_id);

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_receivable_detail_package_v256;
  CREATE TEMPORARY TABLE tmp_receivable_detail_package_v256(package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_receivable_detail_package_v256(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id FROM system_tenant_package p
    JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id UNION ALL SELECT receivable_group_id
      UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id UNION ALL SELECT export_menu_id) target
    WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
  ) missing GROUP BY package_id;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package p
      JOIN tmp_receivable_detail_package_v256 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v256: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_receivable_detail_package_v256 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW() WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_receivable_detail_package_v256;

  COMMIT;
  DO RELEASE_LOCK('erp_receivable_detail_report_v256');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_receivable_detail_menu_v256();
DROP PROCEDURE migrate_receivable_detail_menu_v256;

SELECT table_name,column_name,column_type,is_nullable FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name IN ('erp_finance_receipt','erp_receivable_other') AND column_name='approve_time';
SELECT id,parent_id,name,type,sort,path,component,permission FROM system_menu WHERE deleted=b'0'
AND (path IN ('receivable-detail','account-detail') OR permission IN
  ('erp:receivable-detail-report:query','erp:receivable-detail-report:export')) ORDER BY parent_id,type,sort,id;
