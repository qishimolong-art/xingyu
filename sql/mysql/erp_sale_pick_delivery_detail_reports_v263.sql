-- v263 销售拣货、送货逐明细报表（MySQL 8）。DDL 会隐式提交，正式库执行前必须备份并完成只读预检。
-- SET @sale_pick_delivery_detail_schema = 'reviewed_database';
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_sale_pick_delivery_detail_schema_v263;
DELIMITER $$
CREATE PROCEDURE migrate_sale_pick_delivery_detail_schema_v263()
BEGIN
  IF @sale_pick_delivery_detail_schema IS NULL OR BINARY DATABASE()<>BINARY @sale_pick_delivery_detail_schema THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: set reviewed target database';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE()
      AND table_name='erp_sale_pick_delivery_pick_task' AND table_type='BASE TABLE')
     OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE()
      AND table_name='erp_sale_pick_delivery_item' AND table_type='BASE TABLE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: fulfillment tables missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_pick_delivery_pick_task' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,create_time,id') x) THEN
    ALTER TABLE erp_sale_pick_delivery_pick_task ADD INDEX idx_sale_pick_task_detail_report
      (tenant_id,deleted,create_time,id);
  END IF;
END$$
DELIMITER ;
CALL migrate_sale_pick_delivery_detail_schema_v263();
DROP PROCEDURE migrate_sale_pick_delivery_detail_schema_v263;

DROP PROCEDURE IF EXISTS migrate_sale_pick_delivery_detail_menu_v263;
DELIMITER $$
CREATE PROCEDURE migrate_sale_pick_delivery_detail_menu_v263()
BEGIN
  DECLARE erp_id BIGINT; DECLARE report_id BIGINT; DECLARE sale_id BIGINT;
  DECLARE pick_page_id BIGINT; DECLARE delivery_page_id BIGINT;
  DECLARE pick_query_id BIGINT; DECLARE pick_export_id BIGINT;
  DECLARE delivery_query_id BIGINT; DECLARE delivery_export_id BIGINT;
  DECLARE old_pick_query_id BIGINT; DECLARE old_pick_export_id BIGINT;
  DECLARE old_delivery_query_id BIGINT; DECLARE old_delivery_export_id BIGINT;
  DECLARE package_limit BIGINT; DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN
    ROLLBACK; IF got_lock=1 THEN DO RELEASE_LOCK('erp_sale_pick_delivery_detail_v263'); END IF; RESIGNAL;
  END;
  IF @sale_pick_delivery_detail_schema IS NULL OR BINARY DATABASE()<>BINARY @sale_pick_delivery_detail_schema THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: target changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_sale_pick_delivery_detail_v263',10) INTO got_lock;
  IF got_lock IS NULL OR got_lock<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: migration lock unavailable'; END IF;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: ERP root conflict'; END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: business report root conflict'; END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=report_id AND path='sale')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: sale report group conflict'; END IF;
  SELECT id INTO sale_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=report_id AND path='sale';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2 AND parent_id=sale_id AND path='pick')<>1
     OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2 AND parent_id=sale_id AND path='delivery')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: pick/delivery report page missing or duplicated'; END IF;
  SELECT id INTO pick_page_id FROM system_menu WHERE deleted=b'0' AND type=2 AND parent_id=sale_id AND path='pick';
  SELECT id INTO delivery_page_id FROM system_menu WHERE deleted=b'0' AND type=2 AND parent_id=sale_id AND path='delivery';
  IF EXISTS (SELECT 1 FROM system_menu WHERE id IN (pick_page_id,delivery_page_id)
      AND IFNULL(component,'')<>'erp/sale/report/index') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: report page component conflict'; END IF;
  UPDATE system_menu SET name='拣货明细查询',updater='1',update_time=NOW() WHERE id=pick_page_id AND name<>'拣货明细查询';
  UPDATE system_menu SET name='送货明细查询',updater='1',update_time=NOW() WHERE id=delivery_page_id AND name<>'送货明细查询';

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:sale-pick-detail-report:query','erp:sale-pick-detail-report:export',
       'erp:sale-delivery-detail-report:query','erp:sale-delivery-detail-report:export')
      GROUP BY permission HAVING COUNT(*)>1)
     OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND
      ((permission IN ('erp:sale-pick-detail-report:query','erp:sale-pick-detail-report:export') AND (type<>3 OR parent_id<>pick_page_id))
       OR (permission IN ('erp:sale-delivery-detail-report:query','erp:sale-delivery-detail-report:export') AND (type<>3 OR parent_id<>delivery_page_id)))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: report permission conflict'; END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,seed.parent_id,'','','',NULL,0,b'1',b'0',b'0','1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '拣货明细查询' name,'erp:sale-pick-detail-report:query' permission,1 sort_order,pick_page_id parent_id
        UNION ALL SELECT '拣货明细导出','erp:sale-pick-detail-report:export',2,pick_page_id
        UNION ALL SELECT '送货明细查询','erp:sale-delivery-detail-report:query',1,delivery_page_id
        UNION ALL SELECT '送货明细导出','erp:sale-delivery-detail-report:export',2,delivery_page_id) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO pick_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-pick-detail-report:query';
  SELECT id INTO pick_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-pick-detail-report:export';
  SELECT id INTO delivery_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-delivery-detail-report:query';
  SELECT id INTO delivery_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-delivery-detail-report:export';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-pick:query')<>1
     OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-pick:export')<>1
     OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-delivery:query')<>1
     OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-delivery:export')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: source permissions missing or duplicated'; END IF;
  SELECT id INTO old_pick_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-pick:query';
  SELECT id INTO old_pick_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-pick:export';
  SELECT id INTO old_delivery_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-delivery:query';
  SELECT id INTO old_delivery_export_id FROM system_menu WHERE deleted=b'0' AND permission='erp:sale-delivery:export';

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN system_role r ON r.id=source.role_id AND r.tenant_id=source.tenant_id AND r.deleted=b'0'
  JOIN (SELECT report_id menu_id UNION ALL SELECT sale_id UNION ALL SELECT pick_page_id UNION ALL SELECT pick_query_id) target
  WHERE source.deleted=b'0' AND source.menu_id=old_pick_query_id AND NOT EXISTS
    (SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=target.menu_id AND x.deleted=b'0');
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,pick_export_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source WHERE source.deleted=b'0' AND source.menu_id=old_pick_export_id
    AND EXISTS(SELECT 1 FROM system_role_menu q WHERE q.role_id=source.role_id AND q.tenant_id=source.tenant_id AND q.menu_id=old_pick_query_id AND q.deleted=b'0')
    AND NOT EXISTS(SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=pick_export_id AND x.deleted=b'0');
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN system_role r ON r.id=source.role_id AND r.tenant_id=source.tenant_id AND r.deleted=b'0'
  JOIN (SELECT report_id menu_id UNION ALL SELECT sale_id UNION ALL SELECT delivery_page_id UNION ALL SELECT delivery_query_id) target
  WHERE source.deleted=b'0' AND source.menu_id=old_delivery_query_id AND NOT EXISTS
    (SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=target.menu_id AND x.deleted=b'0');
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,delivery_export_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source WHERE source.deleted=b'0' AND source.menu_id=old_delivery_export_id
    AND EXISTS(SELECT 1 FROM system_role_menu q WHERE q.role_id=source.role_id AND q.tenant_id=source.tenant_id AND q.menu_id=old_delivery_query_id AND q.deleted=b'0')
    AND NOT EXISTS(SELECT 1 FROM system_role_menu x WHERE x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=delivery_export_id AND x.deleted=b'0');

  IF EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: invalid package JSON'; END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_sale_pick_delivery_package_v263;
  CREATE TEMPORARY TABLE tmp_sale_pick_delivery_package_v263(package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_sale_pick_delivery_package_v263
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id FROM system_tenant_package p
    JOIN (SELECT report_id menu_id UNION ALL SELECT sale_id UNION ALL SELECT pick_page_id UNION ALL SELECT pick_query_id) target
    WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(old_pick_query_id AS CHAR),'$') AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
    UNION ALL SELECT p.id,pick_export_id FROM system_tenant_package p WHERE p.deleted=b'0'
      AND JSON_CONTAINS(p.menu_ids,CAST(old_pick_query_id AS CHAR),'$') AND JSON_CONTAINS(p.menu_ids,CAST(old_pick_export_id AS CHAR),'$') AND NOT JSON_CONTAINS(p.menu_ids,CAST(pick_export_id AS CHAR),'$')
    UNION ALL SELECT DISTINCT p.id,target.menu_id FROM system_tenant_package p
    JOIN (SELECT report_id menu_id UNION ALL SELECT sale_id UNION ALL SELECT delivery_page_id UNION ALL SELECT delivery_query_id) target
    WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(old_delivery_query_id AS CHAR),'$') AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
    UNION ALL SELECT p.id,delivery_export_id FROM system_tenant_package p WHERE p.deleted=b'0'
      AND JSON_CONTAINS(p.menu_ids,CAST(old_delivery_query_id AS CHAR),'$') AND JSON_CONTAINS(p.menu_ids,CAST(old_delivery_export_id AS CHAR),'$') AND NOT JSON_CONTAINS(p.menu_ids,CAST(delivery_export_id AS CHAR),'$')
  ) pending GROUP BY package_id;
  SELECT character_maximum_length INTO package_limit FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF EXISTS(SELECT 1 FROM system_tenant_package p JOIN tmp_sale_pick_delivery_package_v263 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v263: package capacity exceeded'; END IF;
  UPDATE system_tenant_package p JOIN tmp_sale_pick_delivery_package_v263 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW() WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_sale_pick_delivery_package_v263;
  COMMIT; DO RELEASE_LOCK('erp_sale_pick_delivery_detail_v263'); SET got_lock=0;
END$$
DELIMITER ;
CALL migrate_sale_pick_delivery_detail_menu_v263();
DROP PROCEDURE migrate_sale_pick_delivery_detail_menu_v263;

SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='erp_sale_pick_delivery_pick_task'
GROUP BY index_name HAVING index_columns='tenant_id,deleted,create_time,id';
SELECT id,parent_id,name,type,sort,path,permission FROM system_menu WHERE deleted=b'0'
  AND (path IN ('pick','delivery') OR permission IN ('erp:sale-pick-detail-report:query','erp:sale-pick-detail-report:export',
    'erp:sale-delivery-detail-report:query','erp:sale-delivery-detail-report:export')) ORDER BY parent_id,type,sort,id;
