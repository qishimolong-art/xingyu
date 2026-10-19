-- 应付账龄分析：菜单、权限、角色和租户套餐迁移（MySQL 8）
-- 仅新增报表配置，不修改业务表结构或业务数据；复用 v257 五类应付来源索引。
SET @payable_aging_report_schema := DATABASE();

DROP PROCEDURE IF EXISTS migrate_payable_aging_report_v264;
DELIMITER $$
CREATE PROCEDURE migrate_payable_aging_report_v264()
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
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_payable_aging_report_v264'); END IF;
    RESIGNAL;
  END;

  IF @payable_aging_report_schema IS NULL OR @payable_aging_report_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: no target database selected';
  END IF;
  IF CAST(DATABASE() AS BINARY)<>CAST(@payable_aging_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: target database changed';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: MySQL 8 is required';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_supplier')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_purchase_in')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_purchase_return')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_purchase_price_adjust')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_payable_other')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_finance_payment') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: required payable source table missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_in' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,in_time,supplier_id,dept_id,id') x)
      OR NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_return' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,return_time,supplier_id,dept_id,id') x)
      OR NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_price_adjust' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,adjust_time,supplier_id,dept_id,id') x)
      OR NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_payable_other' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,biz_time,supplier_id,dept_id,id') x)
      OR NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_finance_payment' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,payment_time,supplier_id,dept_id,id') x) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: v257 payable report indexes are missing';
  END IF;

  SELECT GET_LOCK('erp_payable_aging_report_v264',10) INTO got_lock;
  IF got_lock IS NULL OR got_lock<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: migration lock unavailable';
  END IF;
  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: ERP root conflict';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: business report root conflict';
  END IF;
  SELECT id INTO business_report_id FROM system_menu WHERE deleted=b'0' AND type=1
    AND parent_id=erp_id AND path='business-report';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1
      AND parent_id=business_report_id AND path='counterparty')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: fund report group conflict';
  END IF;
  SELECT id INTO fund_report_id FROM system_menu WHERE deleted=b'0' AND type=1
    AND parent_id=business_report_id AND path='counterparty';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1
      AND parent_id=fund_report_id AND path='payable-detail')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: payable report group conflict';
  END IF;
  SELECT id INTO payable_group_id FROM system_menu WHERE deleted=b'0' AND type=1
    AND parent_id=fund_report_id AND path='payable-detail';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:payable-account:query')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: source payable query permission missing or duplicated';
  END IF;
  SELECT id INTO source_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:payable-account:query';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id
      AND path='aging-analysis' AND (type<>2 OR IFNULL(component,'')<>'erp/report/payable-aging/index'
      OR IFNULL(component_name,'')<>'ErpPayableAgingReport')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: aging-analysis menu conflict';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id
      AND path='aging-analysis') THEN
    UPDATE system_menu SET sort=sort+1,update_time=NOW(),updater='1'
    WHERE deleted=b'0' AND parent_id=payable_group_id AND sort>=3;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
      keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('应付账龄分析','',2,3,payable_group_id,'aging-analysis','ep:histogram',
      'erp/report/payable-aging/index','ErpPayableAgingReport',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO report_page_id FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id
    AND path='aging-analysis';

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:payable-aging-report:query','erp:payable-aging-report:export')
      GROUP BY permission HAVING COUNT(*)>1)
      OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:payable-aging-report:query','erp:payable-aging-report:export')
      AND (type<>3 OR parent_id<>report_page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: report permission conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',NULL,0,b'1',b'0',b'0',
    '1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '应付账龄分析查询' name,'erp:payable-aging-report:query' permission,1 sort_order
        UNION ALL SELECT '应付账龄分析导出','erp:payable-aging-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_menu_id FROM system_menu WHERE deleted=b'0' AND permission='erp:payable-aging-report:query';
  SELECT id INTO export_menu_id FROM system_menu WHERE deleted=b'0' AND permission='erp:payable-aging-report:export';

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source
  JOIN system_role r ON r.id=source.role_id AND r.tenant_id=source.tenant_id AND r.deleted=b'0'
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id UNION ALL SELECT payable_group_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id UNION ALL SELECT export_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.deleted=b'0'
      AND x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=target.menu_id);

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_payable_aging_package_v264;
  CREATE TEMPORARY TABLE tmp_payable_aging_package_v264(package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_payable_aging_package_v264(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id FROM system_tenant_package p
    JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id UNION ALL SELECT payable_group_id
      UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id UNION ALL SELECT export_menu_id) target
    WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
  ) missing GROUP BY package_id;
  SELECT character_maximum_length INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package p
      JOIN tmp_payable_aging_package_v264 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v264: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_payable_aging_package_v264 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW()
  WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_payable_aging_package_v264;

  COMMIT;
  DO RELEASE_LOCK('erp_payable_aging_report_v264');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_payable_aging_report_v264();
DROP PROCEDURE IF EXISTS migrate_payable_aging_report_v264;

SELECT id,parent_id,name,type,sort,path,component,component_name,permission
FROM system_menu WHERE deleted=b'0' AND ((path='aging-analysis'
    AND component='erp/report/payable-aging/index')
  OR permission IN ('erp:payable-aging-report:query','erp:payable-aging-report:export'))
ORDER BY parent_id,type,sort,id;
