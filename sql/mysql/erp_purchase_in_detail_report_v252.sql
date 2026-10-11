-- v252: 配件入库明细查询报表（版本1）。
-- 1. 采购入库、采购退货主单增加可靠审批时间；仅用对应库存流水最早时间回填历史审批单。
-- 2. 增加报表查询所需组合索引；仅在不存在同列顺序索引时新增。
-- 3. 在“经营报表 -> 采购管理”下增加独立页面和查询/导出权限。
-- 4. 角色、套餐只有同时拥有入库、退货、调价三类来源权限时才继承报表权限。
--
-- 本脚本无 DELETE/TRUNCATE/DROP TABLE 和业务金额更新；DDL 会隐式提交。
-- 执行前必须把变量设置为已复核的目标数据库名：
-- SET @purchase_in_detail_report_schema = 'your_database';

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_purchase_in_detail_report_schema_v252;
DELIMITER $$
CREATE PROCEDURE migrate_purchase_in_detail_report_schema_v252()
BEGIN
  DECLARE column_count INT DEFAULT 0;

  IF @purchase_in_detail_report_schema IS NULL
      OR CAST(DATABASE() AS BINARY) <> CAST(@purchase_in_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'v252: set purchase_in_detail_report_schema to the reviewed target database';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_in' AND table_type='BASE TABLE')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_return' AND table_type='BASE TABLE')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_price_adjust' AND table_type='BASE TABLE')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_stock_record' AND table_type='BASE TABLE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: required purchase or stock tables do not exist';
  END IF;

  SELECT COUNT(*) INTO column_count FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_purchase_in' AND column_name='approve_time';
  IF column_count=0 THEN
    ALTER TABLE erp_purchase_in ADD COLUMN approve_time DATETIME NULL COMMENT '审批时间' AFTER in_time;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_in' AND column_name='approve_time'
        AND data_type='datetime' AND is_nullable='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: unexpected erp_purchase_in.approve_time definition';
  END IF;

  SELECT COUNT(*) INTO column_count FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_purchase_return' AND column_name='approve_time';
  IF column_count=0 THEN
    ALTER TABLE erp_purchase_return ADD COLUMN approve_time DATETIME NULL COMMENT '审批时间' AFTER return_time;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_return' AND column_name='approve_time'
        AND data_type='datetime' AND is_nullable='YES') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: unexpected erp_purchase_return.approve_time definition';
  END IF;

  -- 只回填能由对应库存流水证明的历史审批时间；无证据数据保持 NULL。
  UPDATE erp_purchase_in pi
  INNER JOIN (
    SELECT tenant_id,biz_id,MIN(create_time) AS approve_time
    FROM erp_stock_record
    WHERE deleted=b'0' AND biz_type=70
    GROUP BY tenant_id,biz_id
  ) sr ON sr.tenant_id=pi.tenant_id AND sr.biz_id=pi.id
  SET pi.approve_time=sr.approve_time
  WHERE pi.deleted=b'0' AND pi.status=20 AND pi.approve_time IS NULL;

  UPDATE erp_purchase_return pr
  INNER JOIN (
    SELECT tenant_id,biz_id,MIN(create_time) AS approve_time
    FROM erp_stock_record
    WHERE deleted=b'0' AND biz_type=80
    GROUP BY tenant_id,biz_id
  ) sr ON sr.tenant_id=pr.tenant_id AND sr.biz_id=pr.id
  SET pr.approve_time=sr.approve_time
  WHERE pr.deleted=b'0' AND pr.status=20 AND pr.approve_time IS NULL;

  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_in' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)=
        'tenant_id,deleted,status,approve_time,id') x) THEN
    ALTER TABLE erp_purchase_in ADD INDEX idx_erp_purchase_in_detail_report
      (tenant_id,deleted,status,approve_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_return' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)=
        'tenant_id,deleted,status,approve_time,id') x) THEN
    ALTER TABLE erp_purchase_return ADD INDEX idx_erp_purchase_return_detail_report
      (tenant_id,deleted,status,approve_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_price_adjust' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)=
        'tenant_id,deleted,status,approve_time,id') x) THEN
    ALTER TABLE erp_purchase_price_adjust ADD INDEX idx_erp_purchase_adjust_detail_report
      (tenant_id,deleted,status,approve_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_in_items' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,in_id,id') x) THEN
    ALTER TABLE erp_purchase_in_items ADD INDEX idx_erp_purchase_in_items_report
      (tenant_id,deleted,in_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_return_items' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,return_id,id') x) THEN
    ALTER TABLE erp_purchase_return_items ADD INDEX idx_erp_purchase_return_items_report
      (tenant_id,deleted,return_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_price_adjust_item' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,adjust_id,id') x) THEN
    ALTER TABLE erp_purchase_price_adjust_item ADD INDEX idx_erp_purchase_adjust_items_report
      (tenant_id,deleted,adjust_id,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)=
        'tenant_id,deleted,source_in_item_id,invoice_id') x) THEN
    ALTER TABLE erp_purchase_invoice_item ADD INDEX idx_erp_purchase_invoice_item_report
      (tenant_id,deleted,source_in_item_id,invoice_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_stock_record' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)=
        'tenant_id,deleted,biz_type,biz_id,create_time') x) THEN
    ALTER TABLE erp_stock_record ADD INDEX idx_erp_stock_record_purchase_approve
      (tenant_id,deleted,biz_type,biz_id,create_time);
  END IF;
END$$
DELIMITER ;

CALL migrate_purchase_in_detail_report_schema_v252();
DROP PROCEDURE migrate_purchase_in_detail_report_schema_v252;

DROP PROCEDURE IF EXISTS migrate_purchase_in_detail_report_menu_v252;
DELIMITER $$
CREATE PROCEDURE migrate_purchase_in_detail_report_menu_v252()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE purchase_group_id BIGINT;
  DECLARE page_id BIGINT;
  DECLARE report_query_id BIGINT;
  DECLARE report_export_id BIGINT;
  DECLARE in_query_id BIGINT;
  DECLARE return_query_id BIGINT;
  DECLARE adjust_query_id BIGINT;
  DECLARE in_export_id BIGINT;
  DECLARE return_export_id BIGINT;
  DECLARE adjust_export_id BIGINT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_purchase_in_detail_report_v252'); END IF;
    RESIGNAL;
  END;

  IF @purchase_in_detail_report_schema IS NULL
      OR CAST(DATABASE() AS BINARY) <> CAST(@purchase_in_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: target database changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_purchase_in_detail_report_v252',10) INTO got_lock;
  IF got_lock IS NULL OR got_lock<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: migration lock unavailable';
  END IF;
  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: expected one ERP root';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: expected one business-report root';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=report_id AND path='purchase')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: expected one purchase report group';
  END IF;
  SELECT id INTO purchase_group_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=report_id AND path='purchase';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=purchase_group_id AND path='in-detail'
      AND (type<>2 OR IFNULL(component,'')<>'erp/purchase/report/index'
        OR IFNULL(component_name,'')<>'ErpBusinessReportPurchaseInDetail')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: in-detail menu path conflicts';
  END IF;
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,
    status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT '配件入库明细查询报表','',2,7,purchase_group_id,'in-detail','ep:list',
    'erp/purchase/report/index','ErpBusinessReportPurchaseInDetail',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted=b'0' AND parent_id=purchase_group_id AND path='in-detail');
  SELECT id INTO page_id FROM system_menu
  WHERE deleted=b'0' AND parent_id=purchase_group_id AND path='in-detail';

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0'
      AND permission IN ('erp:purchase-in-detail-report:query','erp:purchase-in-detail-report:export')
      GROUP BY permission HAVING COUNT(*)>1)
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0'
      AND permission IN ('erp:purchase-in-detail-report:query','erp:purchase-in-detail-report:export')
      AND (type<>3 OR parent_id<>page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: report permission conflicts';
  END IF;
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,
    status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,page_id,'','','',NULL,0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM (
    SELECT '配件入库明细查询' name,'erp:purchase-in-detail-report:query' permission,1 sort_order
    UNION ALL SELECT '配件入库明细导出','erp:purchase-in-detail-report:export',2
  ) seed WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO report_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-in-detail-report:query';
  SELECT id INTO report_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-in-detail-report:export';

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-in:query')<>1
    OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-return:query')<>1
    OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-price-adjust:query')<>1
    OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-in:export')<>1
    OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-return:export')<>1
    OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-price-adjust:export')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: source permissions missing or duplicated';
  END IF;
  SELECT id INTO in_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-in:query';
  SELECT id INTO return_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-return:query';
  SELECT id INTO adjust_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-price-adjust:query';
  SELECT id INTO in_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-in:export';
  SELECT id INTO return_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-return:export';
  SELECT id INTO adjust_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:purchase-price-adjust:export';

  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source
  JOIN system_role role_data ON role_data.id=source.role_id AND role_data.tenant_id=source.tenant_id AND role_data.deleted=b'0'
  JOIN (SELECT report_id menu_id UNION ALL SELECT purchase_group_id UNION ALL SELECT page_id UNION ALL SELECT report_query_id) target
  WHERE source.deleted=b'0' AND source.menu_id=in_query_id
    AND EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=return_query_id AND x.deleted=b'0')
    AND EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=adjust_query_id AND x.deleted=b'0')
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=target.menu_id AND x.deleted=b'0');

  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,report_export_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source
  JOIN system_role role_data ON role_data.id=source.role_id AND role_data.tenant_id=source.tenant_id AND role_data.deleted=b'0'
  WHERE source.deleted=b'0' AND source.menu_id=in_export_id
    AND EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=return_export_id AND x.deleted=b'0')
    AND EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=adjust_export_id AND x.deleted=b'0')
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=report_export_id AND x.deleted=b'0');

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_purchase_in_detail_package_v252;
  CREATE TEMPORARY TABLE tmp_purchase_in_detail_package_v252(package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_purchase_in_detail_package_v252(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id
    FROM system_tenant_package p
    JOIN (SELECT report_id menu_id UNION ALL SELECT purchase_group_id UNION ALL SELECT page_id UNION ALL SELECT report_query_id) target
    WHERE p.deleted=b'0'
      AND JSON_CONTAINS(p.menu_ids,CAST(in_query_id AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(return_query_id AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(adjust_query_id AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
    UNION ALL
    SELECT p.id,report_export_id FROM system_tenant_package p
    WHERE p.deleted=b'0'
      AND JSON_CONTAINS(p.menu_ids,CAST(in_export_id AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(return_export_id AS CHAR),'$')
      AND JSON_CONTAINS(p.menu_ids,CAST(adjust_export_id AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(report_export_id AS CHAR),'$')
  ) missing GROUP BY package_id;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF EXISTS (SELECT 1 FROM system_tenant_package p JOIN tmp_purchase_in_detail_package_v252 pending ON pending.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,pending.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v252: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_purchase_in_detail_package_v252 pending ON pending.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,pending.missing_ids),p.update_time=NOW()
  WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_purchase_in_detail_package_v252;

  COMMIT;
  DO RELEASE_LOCK('erp_purchase_in_detail_report_v252');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_purchase_in_detail_report_menu_v252();
DROP PROCEDURE migrate_purchase_in_detail_report_menu_v252;

-- 执行后核对：业务主子表行数不会被本脚本删除或新增。
SELECT table_name,column_name,column_type,is_nullable,column_comment
FROM information_schema.columns
WHERE table_schema=DATABASE() AND column_name='approve_time'
  AND table_name IN ('erp_purchase_in','erp_purchase_return') ORDER BY table_name;

SELECT 'purchase_in' document_type,
  SUM(status=20) approved_count,SUM(status=20 AND approve_time IS NOT NULL) backfilled_count
FROM erp_purchase_in WHERE deleted=b'0'
UNION ALL
SELECT 'purchase_return',SUM(status=20),SUM(status=20 AND approve_time IS NOT NULL)
FROM erp_purchase_return WHERE deleted=b'0';

SELECT id,parent_id,name,type,sort,path,component,component_name,permission,status,visible
FROM system_menu WHERE deleted=b'0'
  AND (path='in-detail' OR permission IN ('erp:purchase-in-detail-report:query','erp:purchase-in-detail-report:export'))
ORDER BY type,sort,id;
