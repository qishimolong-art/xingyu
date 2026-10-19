-- 费用支付查询：审批时间、查询索引、直属菜单及权限（MySQL 8）
-- 安全边界：只回填已审批且审批时间为空的数据；不删除或覆盖既有业务、菜单、角色及套餐权限。
SET @expense_detail_report_schema := DATABASE();

SELECT COUNT(*) AS expense_rows_before,
       SUM(CASE WHEN status=20 AND deleted=b'0' THEN 1 ELSE 0 END) AS approved_active_rows_before
FROM erp_payable_expense;

DROP PROCEDURE IF EXISTS migrate_expense_detail_schema_v260;
DELIMITER $$
CREATE PROCEDURE migrate_expense_detail_schema_v260()
BEGIN
  DECLARE backfill_rows BIGINT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF @expense_detail_report_schema IS NULL OR @expense_detail_report_schema=''
      OR CAST(DATABASE() AS BINARY)<>CAST(@expense_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: no stable target database selected';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: MySQL 8 is required';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_payable_expense')
      OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_payable_expense_item') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: payable expense tables are missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_payable_expense' AND column_name='update_time')
      OR NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_payable_expense' AND column_name='status')
      OR NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_payable_expense' AND column_name='tenant_id')
      OR NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_payable_expense' AND column_name='deleted') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: payable expense schema is incompatible';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_payable_expense' AND column_name='approve_time') THEN
    ALTER TABLE erp_payable_expense
      ADD COLUMN approve_time DATETIME NULL COMMENT '审批时间' AFTER status;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (SELECT index_name FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_payable_expense' GROUP BY index_name
      HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)=
        'tenant_id,deleted,status,approve_time,no,id') x) THEN
    ALTER TABLE erp_payable_expense ADD INDEX idx_payable_expense_detail_report
      (tenant_id,deleted,status,approve_time,no,id);
  END IF;

  START TRANSACTION;
  UPDATE erp_payable_expense
  SET approve_time=update_time
  WHERE deleted=b'0' AND status=20 AND approve_time IS NULL;
  SET backfill_rows=ROW_COUNT();
  COMMIT;
  SELECT backfill_rows AS approve_time_backfilled_rows;
END$$
DELIMITER ;

CALL migrate_expense_detail_schema_v260();
DROP PROCEDURE migrate_expense_detail_schema_v260;

DROP PROCEDURE IF EXISTS migrate_expense_detail_menu_v260;
DELIMITER $$
CREATE PROCEDURE migrate_expense_detail_menu_v260()
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
    DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_role_v260;
    DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_package_v260;
    DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_package_menu_v260;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_payable_expense_detail_report_v260'); END IF;
    RESIGNAL;
  END;

  IF @expense_detail_report_schema IS NULL
      OR CAST(DATABASE() AS BINARY)<>CAST(@expense_detail_report_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: target database changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_payable_expense_detail_report_v260',10) INTO got_lock;
  IF got_lock<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: migration lock unavailable'; END IF;
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
  WHERE deleted=b'0' AND permission='erp:payable-expense:query';
  SELECT id INTO source_export_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:payable-expense:export';
  IF erp_id IS NULL OR business_report_id IS NULL OR fund_report_id IS NULL
      OR payable_group_id IS NULL OR source_query_id IS NULL OR source_export_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: stable fund report path or source permissions not found';
  END IF;
  SELECT sort+1 INTO target_sort FROM system_menu WHERE id=payable_group_id;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id
      AND path='payable-expense-detail' AND (type<>2
      OR component<>'erp/report/payable-expense-detail/index'
      OR component_name<>'ErpPayableExpenseDetailReport')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: expense detail page path conflict';
  END IF;
  IF EXISTS (SELECT permission FROM system_menu WHERE deleted=b'0' AND permission IN
      ('erp:payable-expense-detail-report:query','erp:payable-expense-detail-report:export')
      GROUP BY permission HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: expense detail permission conflict';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND parent_id=fund_report_id
      AND path='payable-expense-detail') THEN
    UPDATE system_menu SET sort=sort+1,update_time=NOW(),updater='1'
    WHERE deleted=b'0' AND parent_id=fund_report_id AND sort>=target_sort;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,
      visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('费用支付查询','',2,target_sort,fund_report_id,'payable-expense-detail','ep:list',
      'erp/report/payable-expense-detail/index','ErpPayableExpenseDetailReport',0,b'1',b'1',b'0',
      '1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO report_page_id FROM system_menu
  WHERE deleted=b'0' AND parent_id=fund_report_id AND path='payable-expense-detail';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0'
      AND permission='erp:payable-expense-detail-report:query' AND parent_id<>report_page_id)
      OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0'
      AND permission='erp:payable-expense-detail-report:export' AND parent_id<>report_page_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: expense detail permission parent conflict';
  END IF;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,status,visible,
    keep_alive,always_show,creator,create_time,updater,update_time,deleted)
  SELECT seed.name,seed.permission,3,seed.sort_order,report_page_id,'','','',0,b'1',b'0',b'0',
    '1',NOW(),'1',NOW(),b'0'
  FROM (SELECT '费用支付查询' name,'erp:payable-expense-detail-report:query' permission,1 sort_order
        UNION ALL SELECT '费用支付导出','erp:payable-expense-detail-report:export',2) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m
    WHERE m.deleted=b'0' AND m.permission=seed.permission);
  SELECT id INTO query_menu_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:payable-expense-detail-report:query';
  SELECT id INTO export_menu_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:payable-expense-detail-report:export';

  DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_role_v260;
  CREATE TEMPORARY TABLE tmp_expense_detail_role_v260(
    role_id BIGINT NOT NULL,tenant_id BIGINT NOT NULL,menu_id BIGINT NOT NULL,
    PRIMARY KEY(role_id,tenant_id,menu_id));
  INSERT IGNORE INTO tmp_expense_detail_role_v260(role_id,tenant_id,menu_id)
  SELECT source.role_id,source.tenant_id,target.menu_id FROM system_role_menu source
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id;
  INSERT IGNORE INTO tmp_expense_detail_role_v260(role_id,tenant_id,menu_id)
  SELECT source.role_id,source.tenant_id,target.menu_id FROM system_role_menu source
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT export_menu_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_export_id;
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT target.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',target.tenant_id
  FROM tmp_expense_detail_role_v260 target
  WHERE NOT EXISTS (SELECT 1 FROM system_role_menu current
    WHERE current.deleted=b'0' AND current.role_id=target.role_id
      AND current.tenant_id=target.tenant_id AND current.menu_id=target.menu_id);
  DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_role_v260;

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_package_menu_v260;
  CREATE TEMPORARY TABLE tmp_expense_detail_package_menu_v260(
    package_id BIGINT NOT NULL,menu_id BIGINT NOT NULL,PRIMARY KEY(package_id,menu_id));
  INSERT IGNORE INTO tmp_expense_detail_package_menu_v260(package_id,menu_id)
  SELECT p.id,target.menu_id FROM system_tenant_package p
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT query_menu_id) target
  WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_query_id AS CHAR),'$');
  INSERT IGNORE INTO tmp_expense_detail_package_menu_v260(package_id,menu_id)
  SELECT p.id,target.menu_id FROM system_tenant_package p
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT report_page_id UNION ALL SELECT export_menu_id) target
  WHERE p.deleted=b'0' AND JSON_CONTAINS(p.menu_ids,CAST(source_export_id AS CHAR),'$');
  DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_package_v260;
  CREATE TEMPORARY TABLE tmp_expense_detail_package_v260(
    package_id BIGINT PRIMARY KEY,missing_ids JSON NOT NULL);
  INSERT INTO tmp_expense_detail_package_v260(package_id,missing_ids)
  SELECT target.package_id,JSON_ARRAYAGG(target.menu_id)
  FROM tmp_expense_detail_package_menu_v260 target
  INNER JOIN system_tenant_package p ON p.id=target.package_id AND p.deleted=b'0'
  WHERE NOT JSON_CONTAINS(p.menu_ids,CAST(target.menu_id AS CHAR),'$')
  GROUP BY target.package_id;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package p
      JOIN tmp_expense_detail_package_v260 t ON t.package_id=p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v260: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_expense_detail_package_v260 t ON t.package_id=p.id
  SET p.menu_ids=JSON_MERGE_PRESERVE(p.menu_ids,t.missing_ids),p.update_time=NOW()
  WHERE p.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_package_v260;
  DROP TEMPORARY TABLE IF EXISTS tmp_expense_detail_package_menu_v260;

  COMMIT;
  DO RELEASE_LOCK('erp_payable_expense_detail_report_v260');
  SET got_lock=0;
END$$
DELIMITER ;

CALL migrate_expense_detail_menu_v260();
DROP PROCEDURE migrate_expense_detail_menu_v260;

SELECT COUNT(*) AS approved_rows_without_approve_time_after
FROM erp_payable_expense WHERE deleted=b'0' AND status=20 AND approve_time IS NULL;
SELECT index_name,GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='erp_payable_expense'
  AND index_name='idx_payable_expense_detail_report'
GROUP BY index_name;
SELECT id,parent_id,name,type,sort,path,component,component_name,permission
FROM system_menu WHERE deleted=b'0' AND (path='payable-expense-detail'
  OR permission IN ('erp:payable-expense-detail-report:query','erp:payable-expense-detail-report:export'))
ORDER BY parent_id,type,sort,id;
