-- 其他收入查询：审核时间、报表索引、直属菜单及权限（MySQL 8）
-- 安全边界：只回填已审核且审核时间为空的数据；不删除或覆盖既有业务、菜单、角色及套餐权限。
SET @other_income_report_schema := DATABASE();

SELECT COUNT(*) AS other_income_rows_before,
       SUM(CASE WHEN status=20 AND deleted=b'0' THEN 1 ELSE 0 END) AS approved_active_rows_before
FROM erp_receivable_other_income;

DROP PROCEDURE IF EXISTS migrate_other_income_report_schema_v261;
DELIMITER $$
CREATE PROCEDURE migrate_other_income_report_schema_v261()
BEGIN
  DECLARE backfill_rows BIGINT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF @other_income_report_schema IS NULL OR @other_income_report_schema=''
      OR CAST(DATABASE() AS BINARY)<>CAST(@other_income_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: no stable target database selected';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: MySQL 8 is required';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income_item') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: other income tables are missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income' AND column_name='update_time')
      OR NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income' AND column_name='status')
      OR NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income' AND column_name='tenant_id')
      OR NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income' AND column_name='deleted') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: other income schema is incompatible';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income' AND column_name='approve_time') THEN
    ALTER TABLE erp_receivable_other_income
      ADD COLUMN approve_time DATETIME NULL COMMENT '审核时间' AFTER status;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,deleted,status,approve_time,id') x) THEN
    ALTER TABLE erp_receivable_other_income ADD INDEX idx_other_income_report
      (tenant_id,deleted,status,approve_time,id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_receivable_other_income_item' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='income_id,tenant_id,deleted,id') x) THEN
    ALTER TABLE erp_receivable_other_income_item ADD INDEX idx_other_income_item_report
      (income_id,tenant_id,deleted,id);
  END IF;

  START TRANSACTION;
  UPDATE erp_receivable_other_income
  SET approve_time=update_time
  WHERE deleted=b'0' AND status=20 AND approve_time IS NULL;
  SET backfill_rows=ROW_COUNT();

  INSERT INTO system_field_definition(module,field_key,field_label,field_group,sort,creator,create_time,
    updater,update_time,deleted,tenant_id)
  SELECT source.module,'approveTime','审核时间','audit_info',925,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_field_definition source
  WHERE source.deleted=b'0' AND source.module='erp_finance_receivable_other_income'
    AND source.field_key='updateTime'
    AND NOT EXISTS (SELECT 1 FROM system_field_definition target
      WHERE target.deleted=b'0' AND target.module=source.module AND target.field_key='approveTime'
        AND target.tenant_id=source.tenant_id);
  INSERT INTO system_field_definition(module,field_key,field_label,field_group,sort,creator,create_time,
    updater,update_time,deleted,tenant_id)
  SELECT source.module,'col_approveTime','列表-审核时间','list_col',925,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_field_definition source
  WHERE source.deleted=b'0' AND source.module='erp_finance_receivable_other_income'
    AND source.field_key='col_updateTime'
    AND NOT EXISTS (SELECT 1 FROM system_field_definition target
      WHERE target.deleted=b'0' AND target.module=source.module AND target.field_key='col_approveTime'
        AND target.tenant_id=source.tenant_id);

  INSERT INTO system_role_field_permission(role_id,field_id,hidden,creator,create_time,updater,update_time,
    deleted,tenant_id)
  SELECT source_permission.role_id,target_field.id,source_permission.hidden,'1',NOW(),'1',NOW(),b'0',
    source_permission.tenant_id
  FROM system_role_field_permission source_permission
  JOIN system_field_definition source_field ON source_field.id=source_permission.field_id
    AND source_field.tenant_id=source_permission.tenant_id AND source_field.deleted=b'0'
  JOIN system_field_definition target_field ON target_field.module=source_field.module
    AND target_field.tenant_id=source_field.tenant_id AND target_field.deleted=b'0'
    AND target_field.field_key=CASE source_field.field_key
      WHEN 'updateTime' THEN 'approveTime' WHEN 'col_updateTime' THEN 'col_approveTime' END
  WHERE source_permission.deleted=b'0' AND source_permission.hidden=b'1'
    AND source_field.module='erp_finance_receivable_other_income'
    AND source_field.field_key IN ('updateTime','col_updateTime')
    AND NOT EXISTS (SELECT 1 FROM system_role_field_permission existing
      WHERE existing.deleted=b'0' AND existing.role_id=source_permission.role_id
        AND existing.field_id=target_field.id AND existing.tenant_id=source_permission.tenant_id);
  COMMIT;
  SELECT backfill_rows AS approve_time_backfilled_rows;
END$$
DELIMITER ;

CALL migrate_other_income_report_schema_v261();
DROP PROCEDURE migrate_other_income_report_schema_v261;

DROP PROCEDURE IF EXISTS migrate_other_income_report_menu_v261;
DELIMITER $$
CREATE PROCEDURE migrate_other_income_report_menu_v261()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE expense_report_id BIGINT;
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
    DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_role_v261;
    DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_package_v261;
    DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_package_menu_v261;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_other_income_report_v261'); END IF;
    RESIGNAL;
  END;

  IF @other_income_report_schema IS NULL
      OR CAST(DATABASE() AS BINARY)<>CAST(@other_income_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: target database changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_other_income_report_v261',10) INTO got_lock;
  IF got_lock<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: migration lock unavailable'; END IF;
  START TRANSACTION;

  SELECT id INTO erp_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';
  SELECT id INTO business_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';
  SELECT id INTO fund_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty';
  SELECT id INTO expense_report_id FROM system_menu
  WHERE deleted=b'0' AND type=2 AND parent_id=fund_report_id AND path='payable-expense-detail';
  SELECT id INTO source_query_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:receivable-other-income:query';
  SELECT id INTO source_export_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:receivable-other-income:export';
  IF erp_id IS NULL OR business_report_id IS NULL OR fund_report_id IS NULL OR expense_report_id IS NULL
      OR source_query_id IS NULL OR source_export_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: stable fund report path or source permissions not found';
  END IF;
  SELECT sort+1 INTO target_sort FROM system_menu WHERE id=expense_report_id;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id
      AND path='other-income-report' AND (type<>2 OR component<>'erp/report/other-income/index'
      OR component_name<>'ErpOtherIncomeReport')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: other income report page path conflict';
  END IF;
  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:other-income-report:query','erp:other-income-report:export')
      GROUP BY permission HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: other income report permission conflict';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id
      AND path='other-income-report') THEN
    UPDATE system_menu SET sort=sort+1,update_time=NOW(),updater='1'
    WHERE deleted=b'0' AND parent_id=fund_report_id AND sort>=target_sort;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,
      visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('其他收入查询','',2,target_sort,fund_report_id,'other-income-report','ep:list',
      'erp/report/other-income/index','ErpOtherIncomeReport',0,b'1',b'1',b'0',
      '1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO report_page_id FROM system_menu
  WHERE deleted=b'0' AND parent_id=fund_report_id AND path='other-income-report';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0'
      AND permission='erp:other-income-report:query' AND parent_id<>report_page_id)
      OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0'
      AND permission='erp:other-income-report:export' AND parent_id<>report_page_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: other income report permission parent conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',0,b'1',b'0',b'0',
    '1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '其他收入查询' name,'erp:other-income-report:query' permission,1 sort_order
        UNION ALL SELECT '其他收入导出','erp:other-income-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m
    WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_menu_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:other-income-report:query';
  SELECT id INTO export_menu_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:other-income-report:export';

  DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_role_v261;
  CREATE TEMPORARY TABLE tmp_other_income_report_role_v261(
    role_id BIGINT NOT NULL,tenant_id BIGINT NOT NULL,menu_id BIGINT NOT NULL,
    PRIMARY KEY(role_id,tenant_id,menu_id));
  INSERT IGNORE INTO tmp_other_income_report_role_v261(role_id,tenant_id,menu_id)
  SELECT source.role_id,source.tenant_id,target.menu_id FROM system_role_menu source
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id;
  INSERT IGNORE INTO tmp_other_income_report_role_v261(role_id,tenant_id,menu_id)
  SELECT source.role_id,source.tenant_id,target.menu_id FROM system_role_menu source
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT export_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_export_id;
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT target.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',target.tenant_id
  FROM tmp_other_income_report_role_v261 target
  WHERE NOT EXISTS (SELECT 1 FROM system_role_menu current
    WHERE current.deleted=b'0' AND current.role_id=target.role_id
      AND current.tenant_id=target.tenant_id AND current.menu_id=target.menu_id);
  DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_role_v261;

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_package_menu_v261;
  CREATE TEMPORARY TABLE tmp_other_income_report_package_menu_v261(
    package_id BIGINT NOT NULL,menu_id BIGINT NOT NULL,PRIMARY KEY(package_id,menu_id));
  INSERT IGNORE INTO tmp_other_income_report_package_menu_v261(package_id,menu_id)
  SELECT p.id,target.menu_id FROM system_tenant_package p
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id) target
  WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$');
  INSERT IGNORE INTO tmp_other_income_report_package_menu_v261(package_id,menu_id)
  SELECT p.id,target.menu_id FROM system_tenant_package p
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT export_menu_id) target
  WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_export_id AS CHAR),'$');
  DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_package_v261;
  CREATE TEMPORARY TABLE tmp_other_income_report_package_v261(
    package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_other_income_report_package_v261(package_id,missing_ids)
  SELECT target.package_id,JSON_ARRAYAGG(target.menu_id)
  FROM tmp_other_income_report_package_menu_v261 target
  INNER JOIN system_tenant_package p ON p.id=target.package_id AND p.deleted=b'0'
  WHERE NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
  GROUP BY target.package_id;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package p
      JOIN tmp_other_income_report_package_v261 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v261: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_other_income_report_package_v261 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW()
  WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_package_v261;
  DROP TEMPORARY TABLE IF EXISTS tmp_other_income_report_package_menu_v261;

  COMMIT;
  DO RELEASE_LOCK('erp_other_income_report_v261');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_other_income_report_menu_v261();
DROP PROCEDURE migrate_other_income_report_menu_v261;

SELECT COUNT(*) AS approved_rows_without_approve_time_after
FROM erp_receivable_other_income WHERE deleted=b'0' AND status=20 AND approve_time IS NULL;
SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name IN ('erp_receivable_other_income','erp_receivable_other_income_item')
  AND index_name IN ('idx_other_income_report','idx_other_income_item_report')
GROUP BY table_name,index_name;
SELECT id,parent_id,name,type,sort,path,component,component_name,permission
FROM system_menu WHERE deleted=b'0' AND (path='other-income-report'
  OR permission IN ('erp:other-income-report:query','erp:other-income-report:export'))
ORDER BY parent_id,type,sort,id;
