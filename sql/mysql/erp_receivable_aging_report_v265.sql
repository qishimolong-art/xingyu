-- 应收账龄分析：迁移旧占位菜单，增加查询/导出权限（MySQL 8）
-- 安全边界：不修改业务字段或业务数据；复用既有索引，仅补齐销售出库等价组合索引。
SET @receivable_aging_report_schema := DATABASE();

DROP PROCEDURE IF EXISTS migrate_receivable_aging_schema_v265;
DELIMITER $$
CREATE PROCEDURE migrate_receivable_aging_schema_v265()
BEGIN
  IF @receivable_aging_report_schema IS NULL OR @receivable_aging_report_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: no target database selected';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='erp_sale_out' AND column_name='out_time') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: erp_sale_out.out_time is missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_sale_out' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,out_time,customer_id,dept_id,id') x) THEN
    ALTER TABLE erp_sale_out ADD INDEX idx_erp_sale_out_receivable_aging
      (tenant_id,deleted,status,out_time,customer_id,dept_id,id);
  END IF;
END$$
DELIMITER ;

CALL migrate_receivable_aging_schema_v265();
DROP PROCEDURE IF EXISTS migrate_receivable_aging_schema_v265;

DROP PROCEDURE IF EXISTS migrate_receivable_aging_report_v265;
DELIMITER $$
CREATE PROCEDURE migrate_receivable_aging_report_v265()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE receivable_group_id BIGINT;
  DECLARE old_page_id BIGINT DEFAULT NULL;
  DECLARE target_page_id BIGINT DEFAULT NULL;
  DECLARE report_page_id BIGINT;
  DECLARE query_menu_id BIGINT;
  DECLARE export_menu_id BIGINT;
  DECLARE source_query_id BIGINT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_receivable_aging_report_v265'); END IF;
    RESIGNAL;
  END;

  IF @receivable_aging_report_schema IS NULL OR @receivable_aging_report_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: no target database selected';
  END IF;
  IF BINARY DATABASE()<>BINARY @receivable_aging_report_schema THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: target database changed';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: MySQL 8 is required';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_customer')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_customer_dept_credit')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_sale_out')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_sale_return')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_sale_price_adjust')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_finance_receipt')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_receivable_other') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: required receivable source table missing';
  END IF;

  SELECT GET_LOCK('erp_receivable_aging_report_v265',10) INTO got_lock;
  IF got_lock IS NULL OR got_lock<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: migration lock unavailable';
  END IF;
  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: ERP root conflict';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: business report root conflict';
  END IF;
  SELECT id INTO business_report_id FROM system_menu WHERE deleted=b'0' AND type=1
    AND parent_id=erp_id AND path='business-report';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1
      AND parent_id=business_report_id AND path='counterparty')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: fund report group conflict';
  END IF;
  SELECT id INTO fund_report_id FROM system_menu WHERE deleted=b'0' AND type=1
    AND parent_id=business_report_id AND path='counterparty';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1
      AND parent_id=fund_report_id AND path='receivable-detail')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: receivable report group conflict';
  END IF;
  SELECT id INTO receivable_group_id FROM system_menu WHERE deleted=b'0' AND type=1
    AND parent_id=fund_report_id AND path='receivable-detail';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND permission='erp:receivable-account:query')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: source receivable query permission missing or duplicated';
  END IF;
  SELECT id INTO source_query_id FROM system_menu WHERE deleted=b'0' AND permission='erp:receivable-account:query';

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id
      AND path='aging-receivable')>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: old receivable aging placeholder duplicated';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND parent_id=receivable_group_id
      AND path='aging-analysis')>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: target receivable aging menu duplicated';
  END IF;
  SELECT MAX(id) INTO old_page_id FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id
    AND path='aging-receivable';
  SELECT MAX(id) INTO target_page_id FROM system_menu WHERE deleted=b'0' AND parent_id=receivable_group_id
    AND path='aging-analysis';

  IF target_page_id IS NULL AND old_page_id IS NOT NULL THEN
    UPDATE system_menu SET name='应收账龄分析',permission='',type=2,sort=2,parent_id=receivable_group_id,
      path='aging-analysis',icon='ep:histogram',component='erp/report/receivable-aging/index',
      component_name='ErpReceivableAgingReport',status=0,visible=b'1',keep_alive=b'1',always_show=b'1',
      updater='1',update_time=NOW() WHERE id=old_page_id AND deleted=b'0';
    SET target_page_id=old_page_id;
  ELSEIF target_page_id IS NULL THEN
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
      keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('应收账龄分析','',2,2,receivable_group_id,'aging-analysis','ep:histogram',
      'erp/report/receivable-aging/index','ErpReceivableAgingReport',0,b'1',b'1',b'1','1',NOW(),'1',NOW(),b'0');
    SET target_page_id=LAST_INSERT_ID();
  ELSE
    UPDATE system_menu SET name='应收账龄分析',permission='',type=2,sort=2,
      icon='ep:histogram',component='erp/report/receivable-aging/index',
      component_name='ErpReceivableAgingReport',status=0,visible=b'1',keep_alive=b'1',always_show=b'1',
      updater='1',update_time=NOW() WHERE id=target_page_id AND deleted=b'0';
  END IF;
  SET report_page_id=target_page_id;

  -- 极端情况下新旧入口并存：先保留旧角色/套餐关联，再仅逻辑删除旧占位入口。
  IF old_page_id IS NOT NULL AND old_page_id<>report_page_id THEN
    INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT source.role_id,report_page_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
    FROM system_role_menu source
    WHERE source.deleted=b'0' AND source.menu_id=old_page_id
      AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.deleted=b'0' AND x.role_id=source.role_id
        AND x.tenant_id=source.tenant_id AND x.menu_id=report_page_id);
    UPDATE system_menu SET deleted=b'1',updater='1',update_time=NOW()
    WHERE id=old_page_id AND deleted=b'0';
  END IF;

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:receivable-aging-report:query','erp:receivable-aging-report:export')
      GROUP BY permission HAVING COUNT(*)>1)
      OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:receivable-aging-report:query','erp:receivable-aging-report:export')
      AND (type<>3 OR parent_id<>report_page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: report permission conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',NULL,0,b'1',b'0',b'0',
    '1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '应收账龄分析查询' name,'erp:receivable-aging-report:query' permission,1 sort_order
        UNION ALL SELECT '应收账龄分析导出','erp:receivable-aging-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_menu_id FROM system_menu WHERE deleted=b'0' AND permission='erp:receivable-aging-report:query';
  SELECT id INTO export_menu_id FROM system_menu WHERE deleted=b'0' AND permission='erp:receivable-aging-report:export';

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source
  JOIN system_role r ON r.id=source.role_id AND r.tenant_id=source.tenant_id AND r.deleted=b'0'
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id UNION ALL SELECT receivable_group_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id UNION ALL SELECT export_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu x WHERE x.deleted=b'0'
      AND x.role_id=source.role_id AND x.tenant_id=source.tenant_id AND x.menu_id=target.menu_id);

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_receivable_aging_package_v265;
  CREATE TEMPORARY TABLE tmp_receivable_aging_package_v265(package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_receivable_aging_package_v265(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id package_id,target.menu_id FROM system_tenant_package p
    JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id UNION ALL SELECT receivable_group_id
      UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id UNION ALL SELECT export_menu_id) target
    WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$')
      AND NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
  ) missing GROUP BY package_id;
  SELECT character_maximum_length INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package p
      JOIN tmp_receivable_aging_package_v265 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_receivable_aging_package_v265 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW()
  WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_receivable_aging_package_v265;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND name='应收账龄分析')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: active receivable aging menu is not unique';
  END IF;
  COMMIT;
  DO RELEASE_LOCK('erp_receivable_aging_report_v265');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_receivable_aging_report_v265();
DROP PROCEDURE IF EXISTS migrate_receivable_aging_report_v265;

SELECT id,parent_id,name,type,sort,path,component,component_name,permission
FROM system_menu WHERE deleted=b'0' AND (name='应收账龄分析'
  OR permission IN ('erp:receivable-aging-report:query','erp:receivable-aging-report:export'))
ORDER BY parent_id,type,sort,id;
