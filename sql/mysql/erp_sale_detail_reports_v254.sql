-- v254: 配件报价订单明细表、配件出库明细查询报表。
-- 安全边界：无 DELETE/TRUNCATE/DROP TABLE；历史报价库存快照保持 NULL；审批时间仅由可靠库存流水回填。
-- 执行前设置：SET @sale_detail_report_schema = '已复核的目标数据库名';

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_sale_detail_report_schema_v254;
DELIMITER $$
CREATE PROCEDURE migrate_sale_detail_report_schema_v254()
BEGIN
  IF @sale_detail_report_schema IS NULL
      OR CAST(DATABASE() AS BINARY) <> CAST(@sale_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: target database was not reviewed';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_sale_quote_items')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_sale_out')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_sale_return')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_sale_price_adjust')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_stock_record') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: required sale or stock tables do not exist';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_sale_quote_items' AND column_name='stock_count_at_order') THEN
    ALTER TABLE erp_sale_quote_items ADD COLUMN stock_count_at_order DECIMAL(24,6) NULL
      COMMENT '首次下订时产品仓库实际库存快照' AFTER converted_count;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_sale_quote_items' AND column_name='stock_count_at_order'
      AND data_type='decimal' AND numeric_precision=24 AND numeric_scale=6 AND is_nullable='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: unexpected sale quote snapshot definition';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_sale_return' AND column_name='approve_time') THEN
    ALTER TABLE erp_sale_return ADD COLUMN approve_time DATETIME NULL COMMENT '审核时间' AFTER return_time;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_sale_return' AND column_name='approve_time' AND data_type='datetime' AND is_nullable='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: unexpected sale return approve_time definition';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_sale_price_adjust' AND column_name='approve_time') THEN
    ALTER TABLE erp_sale_price_adjust ADD COLUMN approve_time DATETIME NULL COMMENT '审核时间' AFTER adjust_date;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_sale_price_adjust' AND column_name='approve_time' AND data_type='datetime' AND is_nullable='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: unexpected sale adjust approve_time definition';
  END IF;

  UPDATE erp_sale_return sr
  INNER JOIN (SELECT tenant_id,biz_id,MIN(create_time) approve_time FROM erp_stock_record
    WHERE deleted=b'0' AND biz_type=60 GROUP BY tenant_id,biz_id) evidence
    ON evidence.tenant_id=sr.tenant_id AND evidence.biz_id=sr.id
  SET sr.approve_time=evidence.approve_time
  WHERE sr.deleted=b'0' AND sr.status=20 AND sr.approve_time IS NULL;

  UPDATE erp_sale_price_adjust sa
  INNER JOIN (SELECT tenant_id,biz_id,MIN(create_time) approve_time FROM erp_stock_record
    WHERE deleted=b'0' AND biz_type=92 GROUP BY tenant_id,biz_id) evidence
    ON evidence.tenant_id=sa.tenant_id AND evidence.biz_id=sa.id
  SET sa.approve_time=evidence.approve_time
  WHERE sa.deleted=b'0' AND sa.status=20 AND sa.approve_time IS NULL;

  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_quote' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,quote_time,id') x) THEN
    ALTER TABLE erp_sale_quote ADD INDEX idx_erp_sale_quote_detail_report
      (tenant_id,deleted,status,quote_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_quote_items' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,quote_id,id') x) THEN
    ALTER TABLE erp_sale_quote_items ADD INDEX idx_erp_sale_quote_items_report
      (tenant_id,deleted,quote_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_out' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,approve_time,id') x) THEN
    ALTER TABLE erp_sale_out ADD INDEX idx_erp_sale_out_detail_report
      (tenant_id,deleted,status,approve_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_return' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,approve_time,id') x) THEN
    ALTER TABLE erp_sale_return ADD INDEX idx_erp_sale_return_detail_report
      (tenant_id,deleted,status,approve_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_price_adjust' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,approve_time,id') x) THEN
    ALTER TABLE erp_sale_price_adjust ADD INDEX idx_erp_sale_adjust_detail_report
      (tenant_id,deleted,status,approve_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_convert_record' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,source_type,source_item_id,target_id') x) THEN
    ALTER TABLE erp_sale_convert_record ADD INDEX idx_erp_sale_convert_quote_report
      (tenant_id,deleted,source_type,source_item_id,target_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_stock_record' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,biz_type,biz_id,create_time') x) THEN
    ALTER TABLE erp_stock_record ADD INDEX idx_erp_stock_record_sale_approve
      (tenant_id,deleted,biz_type,biz_id,create_time);
  END IF;
END$$
DELIMITER ;

CALL migrate_sale_detail_report_schema_v254();
DROP PROCEDURE migrate_sale_detail_report_schema_v254;

DROP PROCEDURE IF EXISTS migrate_sale_detail_report_menu_v254;
DELIMITER $$
CREATE PROCEDURE migrate_sale_detail_report_menu_v254()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE sale_group_id BIGINT;
  DECLARE quote_page_id BIGINT;
  DECLARE out_page_id BIGINT;
  DECLARE quote_query_id BIGINT;
  DECLARE quote_export_id BIGINT;
  DECLARE out_query_id BIGINT;
  DECLARE out_export_id BIGINT;
  DECLARE source_quote_query BIGINT;
  DECLARE source_quote_export BIGINT;
  DECLARE source_out_query BIGINT;
  DECLARE source_out_export BIGINT;
  DECLARE source_return_query BIGINT;
  DECLARE source_return_export BIGINT;
  DECLARE source_adjust_query BIGINT;
  DECLARE source_adjust_export BIGINT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_sale_detail_reports_v254'); END IF;
    RESIGNAL;
  END;

  IF @sale_detail_report_schema IS NULL OR CAST(DATABASE() AS BINARY)<>CAST(@sale_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: target database changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_sale_detail_reports_v254',10) INTO got_lock;
  IF got_lock<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: migration lock unavailable'; END IF;
  START TRANSACTION;

  SELECT id INTO erp_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  SELECT id INTO report_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  SELECT id INTO sale_group_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=report_id AND path='sale';
  IF erp_id IS NULL OR report_id IS NULL OR sale_group_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: stable sale report menu path not found';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=sale_group_id
      AND path IN ('quote-detail','out-detail') AND (type<>2 OR component<>'erp/sale/report/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: sale report menu path conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '配件报价订单明细表','',2,6,sale_group_id,'quote-detail','ep:list','erp/sale/report/index',
    'ErpBusinessReportSaleQuoteDetail',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=sale_group_id AND path='quote-detail');
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '配件出库明细查询报表','',2,7,sale_group_id,'out-detail','ep:list','erp/sale/report/index',
    'ErpBusinessReportSaleOutDetail',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=sale_group_id AND path='out-detail');
  SELECT id INTO quote_page_id FROM system_menu WHERE deleted=b'0' AND parent_id=sale_group_id AND path='quote-detail';
  SELECT id INTO out_page_id FROM system_menu WHERE deleted=b'0' AND parent_id=sale_group_id AND path='out-detail';

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:sale-quote-detail-report:query','erp:sale-quote-detail-report:export',
       'erp:sale-out-detail-report:query','erp:sale-out-detail-report:export') GROUP BY permission HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: report permission conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,status,visible,keep_alive,always_show,
    creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,seed.parent_id,'','','',0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '配件报价订单明细查询' name,'erp:sale-quote-detail-report:query' permission,1 sort_order,quote_page_id parent_id
    UNION ALL SELECT '配件报价订单明细导出','erp:sale-quote-detail-report:export',2,quote_page_id
    UNION ALL SELECT '配件出库明细查询','erp:sale-out-detail-report:query',1,out_page_id
    UNION ALL SELECT '配件出库明细导出','erp:sale-out-detail-report:export',2,out_page_id) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);

  SELECT id INTO quote_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-quote-detail-report:query';
  SELECT id INTO quote_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-quote-detail-report:export';
  SELECT id INTO out_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-out-detail-report:query';
  SELECT id INTO out_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-out-detail-report:export';
  SELECT id INTO source_quote_query FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-quote:query';
  SELECT id INTO source_quote_export FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-quote:export';
  SELECT id INTO source_out_query FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-out:query';
  SELECT id INTO source_out_export FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-out:export';
  SELECT id INTO source_return_query FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-return:query';
  SELECT id INTO source_return_export FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-return:export';
  SELECT id INTO source_adjust_query FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-price-adjust:query';
  -- 销售调价现有导出接口使用 query 权限，没有独立 export 权限码。
  SET source_adjust_export=source_adjust_query;
  IF source_quote_query IS NULL OR source_quote_export IS NULL
      OR source_out_query IS NULL OR source_out_export IS NULL
      OR source_return_query IS NULL OR source_return_export IS NULL
      OR source_adjust_query IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: required sale source permission not found';
  END IF;

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT grant_row.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',grant_row.tenant_id
  FROM system_role_menu grant_row JOIN (SELECT report_id menu_id UNION ALL SELECT sale_group_id
    UNION ALL SELECT quote_page_id UNION ALL SELECT quote_query_id) target
  WHERE grant_row.deleted=b'0' AND grant_row.menu_id=source_quote_query
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=grant_row.role_id
      AND x.tenant_id=grant_row.tenant_id AND x.menu_id=target.menu_id AND x.deleted=b'0');
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT grant_row.role_id,quote_export_id,'1',NOW(),'1',NOW(),b'0',grant_row.tenant_id
  FROM system_role_menu grant_row WHERE grant_row.deleted=b'0' AND grant_row.menu_id=source_quote_export
    AND EXISTS (SELECT 1 FROM system_role_menu q WHERE q.role_id=grant_row.role_id AND q.tenant_id=grant_row.tenant_id
      AND q.menu_id=source_quote_query AND q.deleted=b'0')
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=grant_row.role_id
      AND x.tenant_id=grant_row.tenant_id AND x.menu_id=quote_export_id AND x.deleted=b'0');

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT grant_row.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',grant_row.tenant_id
  FROM system_role_menu grant_row JOIN (SELECT report_id menu_id UNION ALL SELECT sale_group_id
    UNION ALL SELECT out_page_id UNION ALL SELECT out_query_id) target
  WHERE grant_row.deleted=b'0' AND grant_row.menu_id=source_out_query
    AND EXISTS (SELECT 1 FROM system_role_menu q WHERE q.role_id=grant_row.role_id AND q.tenant_id=grant_row.tenant_id
      AND q.menu_id=source_return_query AND q.deleted=b'0')
    AND EXISTS (SELECT 1 FROM system_role_menu q WHERE q.role_id=grant_row.role_id AND q.tenant_id=grant_row.tenant_id
      AND q.menu_id=source_adjust_query AND q.deleted=b'0')
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=grant_row.role_id
      AND x.tenant_id=grant_row.tenant_id AND x.menu_id=target.menu_id AND x.deleted=b'0');
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT grant_row.role_id,out_export_id,'1',NOW(),'1',NOW(),b'0',grant_row.tenant_id
  FROM system_role_menu grant_row WHERE grant_row.deleted=b'0' AND grant_row.menu_id=source_out_export
    AND EXISTS (SELECT 1 FROM system_role_menu q WHERE q.role_id=grant_row.role_id AND q.tenant_id=grant_row.tenant_id
      AND q.menu_id=source_return_export AND q.deleted=b'0')
    AND EXISTS (SELECT 1 FROM system_role_menu q WHERE q.role_id=grant_row.role_id AND q.tenant_id=grant_row.tenant_id
      AND q.menu_id=source_adjust_export AND q.deleted=b'0')
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=grant_row.role_id
      AND x.tenant_id=grant_row.tenant_id AND x.menu_id=out_export_id AND x.deleted=b'0');

  -- 套餐仅合并当前套餐缺失的 ID，避免重复追加既有经营报表分组和页面权限。
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_sale_detail_package_v254;
  CREATE TEMPORARY TABLE tmp_sale_detail_package_v254(
    package_id BIGINT PRIMARY KEY,
    missing_ids JSON NOT NULL
  );
  INSERT INTO tmp_sale_detail_package_v254(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id
    FROM system_tenant_package p
    JOIN (SELECT report_id menu_id UNION ALL SELECT sale_group_id UNION ALL SELECT quote_page_id
      UNION ALL SELECT quote_query_id) target
    WHERE p.deleted=b'0'
      AND JSON_CONTAINS(p.menu_ids,CAST(source_quote_query AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
    UNION
    SELECT p.id,quote_export_id FROM system_tenant_package p
    WHERE p.deleted=b'0'
      AND JSON_CONTAINS(p.menu_ids,CAST(source_quote_query AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(source_quote_export AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(quote_export_id AS CHAR),'$')
    UNION
    SELECT DISTINCT p.id,target.menu_id
    FROM system_tenant_package p
    JOIN (SELECT report_id menu_id UNION ALL SELECT sale_group_id UNION ALL SELECT out_page_id
      UNION ALL SELECT out_query_id) target
    WHERE p.deleted=b'0'
      AND JSON_CONTAINS(p.menu_ids,CAST(source_out_query AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(source_return_query AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(source_adjust_query AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
    UNION
    SELECT p.id,out_export_id FROM system_tenant_package p
    WHERE p.deleted=b'0'
      AND JSON_CONTAINS(p.menu_ids,CAST(source_out_query AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(source_return_query AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(source_adjust_query AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(source_out_export AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(source_return_export AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(source_adjust_export AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(out_export_id AS CHAR),'$')
  ) missing GROUP BY package_id;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (
      SELECT 1 FROM system_tenant_package p
      JOIN tmp_sale_detail_package_v254 pending ON pending.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,pending.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v254: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p
  JOIN tmp_sale_detail_package_v254 pending ON pending.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,pending.missing_ids),p.update_time=NOW()
  WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_sale_detail_package_v254;

  COMMIT;
  DO RELEASE_LOCK('erp_sale_detail_reports_v254');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_sale_detail_report_menu_v254();
DROP PROCEDURE migrate_sale_detail_report_menu_v254;

SELECT table_name,column_name,column_type,is_nullable FROM information_schema.columns
WHERE table_schema=DATABASE() AND ((table_name='erp_sale_quote_items' AND column_name='stock_count_at_order')
  OR (table_name IN ('erp_sale_return','erp_sale_price_adjust') AND column_name='approve_time'));
SELECT id,parent_id,name,type,path,component,permission FROM system_menu WHERE deleted=b'0'
  AND (path IN ('quote-detail','out-detail') OR permission IN ('erp:sale-quote-detail-report:query',
  'erp:sale-quote-detail-report:export','erp:sale-out-detail-report:query','erp:sale-out-detail-report:export'))
ORDER BY parent_id,type,sort,id;
