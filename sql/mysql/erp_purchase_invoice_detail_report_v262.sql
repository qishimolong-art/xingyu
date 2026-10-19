-- v262 采购票据明细查询（MySQL 8）。DDL 会隐式提交，正式库执行前必须完成备份和只读预检。
-- SET @purchase_invoice_detail_report_schema = 'reviewed_database';
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_purchase_invoice_detail_schema_v262;
DELIMITER $$
CREATE PROCEDURE migrate_purchase_invoice_detail_schema_v262()
BEGIN
  IF @purchase_invoice_detail_report_schema IS NULL OR BINARY DATABASE()<>BINARY @purchase_invoice_detail_report_schema THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: set reviewed target database';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice' AND table_type='BASE TABLE')
     OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item' AND table_type='BASE TABLE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: purchase invoice tables missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice' AND column_name='approve_time') THEN
    ALTER TABLE erp_purchase_invoice ADD COLUMN approve_time DATETIME NULL COMMENT '审批通过时间' AFTER invoice_date;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice' AND column_name='approve_time' AND data_type='datetime' AND is_nullable='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: unexpected approve_time definition';
  END IF;
  -- 显式自赋值 update_time，避免其 ON UPDATE CURRENT_TIMESTAMP 在历史回填时改写原更新时间。
  UPDATE erp_purchase_invoice SET approve_time=update_time,update_time=update_time
  WHERE deleted=b'0' AND status=20 AND approve_time IS NULL AND update_time IS NOT NULL;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,approve_time,no,id') x) THEN
    ALTER TABLE erp_purchase_invoice ADD INDEX idx_purchase_invoice_detail_report
      (tenant_id,deleted,status,approve_time,no,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,invoice_id,id') x) THEN
    ALTER TABLE erp_purchase_invoice_item ADD INDEX idx_purchase_invoice_item_detail_report
      (tenant_id,deleted,invoice_id,id);
  END IF;
END$$
DELIMITER ;
CALL migrate_purchase_invoice_detail_schema_v262();
DROP PROCEDURE migrate_purchase_invoice_detail_schema_v262;

DROP PROCEDURE IF EXISTS migrate_purchase_invoice_detail_menu_v262;
DELIMITER $$
CREATE PROCEDURE migrate_purchase_invoice_detail_menu_v262()
BEGIN
  DECLARE erp_id BIGINT; DECLARE report_id BIGINT; DECLARE purchase_id BIGINT; DECLARE page_id BIGINT;
  DECLARE query_id BIGINT; DECLARE export_id BIGINT; DECLARE source_query_id BIGINT; DECLARE source_export_id BIGINT;
  DECLARE package_limit BIGINT; DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; IF got_lock=1 THEN DO RELEASE_LOCK('erp_purchase_invoice_detail_v262'); END IF; RESIGNAL; END;
  IF @purchase_invoice_detail_report_schema IS NULL OR BINARY DATABASE()<>BINARY @purchase_invoice_detail_report_schema THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: target changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_purchase_invoice_detail_v262',10) INTO got_lock;
  IF got_lock IS NULL OR got_lock<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: migration lock unavailable'; END IF;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp')<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: ERP root conflict'; END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report')<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: business report root conflict'; END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=report_id AND path='purchase')<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: purchase report group conflict'; END IF;
  SELECT id INTO purchase_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=report_id AND path='purchase';
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=purchase_id AND path='invoice-detail'
      AND (type<>2 OR IFNULL(component,'')<>'erp/purchase/report/index' OR IFNULL(component_name,'')<>'ErpBusinessReportPurchaseInvoiceDetail')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: invoice-detail menu conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '采购票据明细查询','',2,2,purchase_id,'invoice-detail','ep:list','erp/purchase/report/index','ErpBusinessReportPurchaseInvoiceDetail',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=purchase_id AND path='invoice-detail');
  SELECT id INTO page_id FROM system_menu WHERE deleted=b'0' AND parent_id=purchase_id AND path='invoice-detail';
  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN ('erp:purchase-invoice-detail-report:query','erp:purchase-invoice-detail-report:export') GROUP BY permission HAVING COUNT(*)>1)
     OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND permission IN ('erp:purchase-invoice-detail-report:query','erp:purchase-invoice-detail-report:export') AND (type<>3 OR parent_id<>page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: report permission conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,page_id,'','','',NULL,0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '采购票据明细查询' name,'erp:purchase-invoice-detail-report:query' permission,1 sort_order
        UNION ALL SELECT '采购票据明细导出','erp:purchase-invoice-detail-report:export',2) seed
  WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-invoice-detail-report:query';
  SELECT id INTO export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-invoice-detail-report:export';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-invoice:query')<>1
     OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-invoice:export')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: source permissions missing or duplicated';
  END IF;
  SELECT id INTO source_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-invoice:query';
  SELECT id INTO source_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-invoice:export';
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN system_role r ON r.id=source.role_id AND r.tenant_id=source.tenant_id AND r.deleted=b'0'
  JOIN (SELECT report_id menu_id UNION ALL SELECT purchase_id UNION ALL SELECT page_id UNION ALL SELECT query_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id AND NOT EXISTS(SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=target.menu_id AND x.deleted=b'0');
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,export_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN system_role r ON r.id=source.role_id AND r.tenant_id=source.tenant_id AND r.deleted=b'0'
  WHERE source.deleted=b'0' AND source.menu_id=source_export_id
    AND EXISTS(SELECT 1 FROM system_role_menu q WHERE q.role_id=source.role_id AND q.tenant_id=source.tenant_id AND q.menu_id=source_query_id AND q.deleted=b'0')
    AND NOT EXISTS(SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=export_id AND x.deleted=b'0');
  IF EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: invalid package JSON'; END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_invoice_detail_package_v262;
  CREATE TEMPORARY TABLE tmp_invoice_detail_package_v262(package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_invoice_detail_package_v262
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id FROM system_tenant_package p JOIN (SELECT report_id menu_id UNION ALL SELECT purchase_id UNION ALL SELECT page_id UNION ALL SELECT query_id) target
    WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$') AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
    UNION ALL SELECT p.id,export_id FROM system_tenant_package p WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$') AND JSON_CONTAINS(p.menu_ids,CAST(source_export_id AS CHAR),'$') AND NOT JSON_CONTAINS(p.menu_ids,CAST(export_id AS CHAR),'$')) pending GROUP BY package_id;
  SELECT character_maximum_length INTO package_limit FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF EXISTS(SELECT 1 FROM system_tenant_package p JOIN tmp_invoice_detail_package_v262 t ON t.package_id=p.id WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v262: package capacity exceeded'; END IF;
  UPDATE system_tenant_package p JOIN tmp_invoice_detail_package_v262 t ON t.package_id=p.id SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW() WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_invoice_detail_package_v262;
  COMMIT; DO RELEASE_LOCK('erp_purchase_invoice_detail_v262'); SET got_lock=0;
END$$
DELIMITER ;
CALL migrate_purchase_invoice_detail_menu_v262();
DROP PROCEDURE migrate_purchase_invoice_detail_menu_v262;

SELECT column_name,column_type,is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice' AND column_name='approve_time';
SELECT SUM(status=20) approved_count,SUM(status=20 AND approve_time IS NOT NULL) approved_with_time FROM erp_purchase_invoice WHERE deleted=b'0';
SELECT id,parent_id,name,type,sort,path,permission FROM system_menu WHERE deleted=b'0' AND (path='invoice-detail' OR permission IN ('erp:purchase-invoice-detail-report:query','erp:purchase-invoice-detail-report:export')) ORDER BY type,sort,id;
