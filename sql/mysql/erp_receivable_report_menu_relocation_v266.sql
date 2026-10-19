-- 收款明细查询、其他收入查询归入应收报表（MySQL 8）
-- 仅移动既有页面菜单并补齐父级可见权限；页面/按钮 ID、路由、权限及业务数据保持不变。
SET @receivable_report_relocation_schema := DATABASE();

DROP PROCEDURE IF EXISTS relocate_receivable_reports_v266;
DELIMITER $$
CREATE PROCEDURE relocate_receivable_reports_v266()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE receivable_group_id BIGINT;
  DECLARE receipt_page_id BIGINT;
  DECLARE receipt_query_id BIGINT;
  DECLARE receipt_export_id BIGINT;
  DECLARE other_income_page_id BIGINT;
  DECLARE other_income_query_id BIGINT;
  DECLARE other_income_export_id BIGINT;
  DECLARE receipt_parent_id BIGINT;
  DECLARE other_income_parent_id BIGINT;
  DECLARE target_sort INT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_receivable_report_package_v266;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_receivable_report_menu_relocation_v266'); END IF;
    RESIGNAL;
  END;

  IF @receivable_report_relocation_schema IS NULL OR @receivable_report_relocation_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: no target database selected';
  END IF;
  IF CAST(DATABASE() AS BINARY)<>CAST(@receivable_report_relocation_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: target database changed';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: MySQL 8 is required';
  END IF;

  SELECT GET_LOCK('erp_receivable_report_menu_relocation_v266',10) INTO got_lock;
  IF got_lock IS NULL OR got_lock<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: migration lock unavailable';
  END IF;
  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: ERP root conflict';
  END IF;
  SELECT id INTO erp_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: business report root conflict';
  END IF;
  SELECT id INTO business_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: fund report group conflict';
  END IF;
  SELECT id INTO fund_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=fund_report_id AND path='receivable-detail')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: receivable report group conflict';
  END IF;
  SELECT id INTO receivable_group_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=fund_report_id AND path='receivable-detail';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=2 AND path='receipt-detail'
        AND component='erp/report/receipt-detail/index'
        AND component_name='ErpReceiptDetailReport')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: receipt detail report page missing or duplicated';
  END IF;
  SELECT id,parent_id INTO receipt_page_id,receipt_parent_id
  FROM system_menu
  WHERE deleted=b'0' AND type=2 AND path='receipt-detail'
    AND component='erp/report/receipt-detail/index'
    AND component_name='ErpReceiptDetailReport';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=2 AND path='other-income-report'
        AND component='erp/report/other-income/index'
        AND component_name='ErpOtherIncomeReport')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: other income report page missing or duplicated';
  END IF;
  SELECT id,parent_id INTO other_income_page_id,other_income_parent_id
  FROM system_menu
  WHERE deleted=b'0' AND type=2 AND path='other-income-report'
    AND component='erp/report/other-income/index'
    AND component_name='ErpOtherIncomeReport';

  IF receipt_parent_id NOT IN (fund_report_id,receivable_group_id)
      OR other_income_parent_id NOT IN (fund_report_id,receivable_group_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: report page has an unexpected parent';
  END IF;

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND permission='erp:receipt-detail-report:query')<>1
      OR (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND permission='erp:receipt-detail-report:export')<>1
      OR (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND permission='erp:other-income-report:query')<>1
      OR (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND permission='erp:other-income-report:export')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: report permission missing or duplicated';
  END IF;
  SELECT id INTO receipt_query_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:receipt-detail-report:query';
  SELECT id INTO receipt_export_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:receipt-detail-report:export';
  SELECT id INTO other_income_query_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:other-income-report:query';
  SELECT id INTO other_income_export_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:other-income-report:export';
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0'
      AND ((id IN (receipt_query_id,receipt_export_id)
          AND (type<>3 OR parent_id<>receipt_page_id))
        OR (id IN (other_income_query_id,other_income_export_id)
          AND (type<>3 OR parent_id<>other_income_page_id)))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: report permission parent conflict';
  END IF;

  -- 保持两张报表在原资金报表中的相对顺序：其他收入在前、收款明细在后。
  IF other_income_parent_id=fund_report_id THEN
    SELECT COALESCE(MAX(sort),0)+1 INTO target_sort
    FROM system_menu WHERE deleted=b'0' AND parent_id=receivable_group_id;
    UPDATE system_menu
    SET parent_id=receivable_group_id,sort=target_sort,updater='1',update_time=NOW()
    WHERE id=other_income_page_id AND deleted=b'0' AND parent_id=fund_report_id;
  END IF;
  IF receipt_parent_id=fund_report_id THEN
    SELECT COALESCE(MAX(sort),0)+1 INTO target_sort
    FROM system_menu WHERE deleted=b'0' AND parent_id=receivable_group_id;
    UPDATE system_menu
    SET parent_id=receivable_group_id,sort=target_sort,updater='1',update_time=NOW()
    WHERE id=receipt_page_id AND deleted=b'0' AND parent_id=fund_report_id;
  END IF;

  -- 页面与按钮已有权限保持不变，仅为其既有角色补齐新父级菜单链。
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source
  INNER JOIN system_role role_row ON role_row.id=source.role_id
    AND role_row.tenant_id=source.tenant_id AND role_row.deleted=b'0'
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT receivable_group_id) target
  WHERE source.deleted=b'0'
    AND source.menu_id IN (receipt_page_id,receipt_query_id,receipt_export_id,
      other_income_page_id,other_income_query_id,other_income_export_id)
    AND NOT EXISTS (SELECT 1 FROM system_role_menu current
      WHERE current.deleted=b'0' AND current.role_id=source.role_id
        AND current.tenant_id=source.tenant_id AND current.menu_id=target.menu_id);

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_receivable_report_package_v266;
  CREATE TEMPORARY TABLE tmp_receivable_report_package_v266(
    package_id BIGINT PRIMARY KEY,
    missing_ids JSON NOT NULL
  );
  INSERT INTO tmp_receivable_report_package_v266(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT package_row.id package_id,target.menu_id
    FROM system_tenant_package package_row
    JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
      UNION ALL SELECT receivable_group_id) target
    WHERE package_row.deleted=b'0'
      AND (JSON_CONTAINS(package_row.menu_ids,CAST(receipt_page_id AS CHAR),'$')
        OR JSON_CONTAINS(package_row.menu_ids,CAST(receipt_query_id AS CHAR),'$')
        OR JSON_CONTAINS(package_row.menu_ids,CAST(receipt_export_id AS CHAR),'$')
        OR JSON_CONTAINS(package_row.menu_ids,CAST(other_income_page_id AS CHAR),'$')
        OR JSON_CONTAINS(package_row.menu_ids,CAST(other_income_query_id AS CHAR),'$')
        OR JSON_CONTAINS(package_row.menu_ids,CAST(other_income_export_id AS CHAR),'$'))
      AND NOT JSON_CONTAINS(package_row.menu_ids,CAST(target.menu_id AS CHAR),'$')
  ) missing GROUP BY package_id;

  SELECT character_maximum_length INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package package_row
      JOIN tmp_receivable_report_package_v266 target ON target.package_id=package_row.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(package_row.menu_ids,target.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package package_row
  JOIN tmp_receivable_report_package_v266 target ON target.package_id=package_row.id
  SET package_row.menu_ids=JSON_MERGE_PRESERVE(package_row.menu_ids,target.missing_ids),
      package_row.update_time=NOW()
  WHERE package_row.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_receivable_report_package_v266;

  IF EXISTS (SELECT 1 FROM system_menu
      WHERE deleted=b'0' AND id IN (receipt_page_id,other_income_page_id)
        AND parent_id<>receivable_group_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: report relocation verification failed';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role_menu source
      INNER JOIN system_role role_row ON role_row.id=source.role_id
        AND role_row.tenant_id=source.tenant_id AND role_row.deleted=b'0'
      WHERE source.deleted=b'0'
        AND source.menu_id IN (receipt_page_id,receipt_query_id,receipt_export_id,
          other_income_page_id,other_income_query_id,other_income_export_id)
        AND NOT EXISTS (SELECT 1 FROM system_role_menu parent_link
          WHERE parent_link.deleted=b'0' AND parent_link.role_id=source.role_id
            AND parent_link.tenant_id=source.tenant_id AND parent_link.menu_id=receivable_group_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: role parent menu chain incomplete';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package package_row
      WHERE package_row.deleted=b'0'
        AND (JSON_CONTAINS(package_row.menu_ids,CAST(receipt_page_id AS CHAR),'$')
          OR JSON_CONTAINS(package_row.menu_ids,CAST(receipt_query_id AS CHAR),'$')
          OR JSON_CONTAINS(package_row.menu_ids,CAST(receipt_export_id AS CHAR),'$')
          OR JSON_CONTAINS(package_row.menu_ids,CAST(other_income_page_id AS CHAR),'$')
          OR JSON_CONTAINS(package_row.menu_ids,CAST(other_income_query_id AS CHAR),'$')
          OR JSON_CONTAINS(package_row.menu_ids,CAST(other_income_export_id AS CHAR),'$'))
        AND NOT JSON_CONTAINS(package_row.menu_ids,CAST(receivable_group_id AS CHAR),'$')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v266: tenant package parent menu chain incomplete';
  END IF;

  COMMIT;
  DO RELEASE_LOCK('erp_receivable_report_menu_relocation_v266');
  SET got_lock=0;
END$$
DELIMITER ;

CALL relocate_receivable_reports_v266();
DROP PROCEDURE IF EXISTS relocate_receivable_reports_v266;

SELECT child.id,child.parent_id,parent.name parent_name,child.name,child.type,child.sort,
       child.path,child.component,child.component_name,child.permission
FROM system_menu child
LEFT JOIN system_menu parent ON parent.id=child.parent_id AND parent.deleted=b'0'
WHERE child.deleted=b'0' AND (child.id IN (
    SELECT id FROM system_menu WHERE deleted=b'0' AND type=1 AND path='receivable-detail'
  ) OR child.path IN ('receipt-detail','other-income-report')
  OR child.permission IN ('erp:receipt-detail-report:query','erp:receipt-detail-report:export',
    'erp:other-income-report:query','erp:other-income-report:export'))
ORDER BY child.parent_id,child.type,child.sort,child.id;
