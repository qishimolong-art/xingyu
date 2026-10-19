-- 费用支付查询归入应付报表（MySQL 8）
-- 仅移动既有页面菜单并补齐父级可见权限；页面 ID、按钮权限、路由及业务数据保持不变。
SET @expense_report_relocation_schema := DATABASE();

DROP PROCEDURE IF EXISTS relocate_payable_expense_report_v265;
DELIMITER $$
CREATE PROCEDURE relocate_payable_expense_report_v265()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE payable_group_id BIGINT;
  DECLARE expense_page_id BIGINT;
  DECLARE expense_query_id BIGINT;
  DECLARE expense_export_id BIGINT;
  DECLARE original_parent_id BIGINT;
  DECLARE original_sort INT;
  DECLARE target_sort INT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_expense_relocation_package_v265;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_payable_expense_report_relocation_v265'); END IF;
    RESIGNAL;
  END;

  IF @expense_report_relocation_schema IS NULL OR @expense_report_relocation_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: no target database selected';
  END IF;
  IF CAST(DATABASE() AS BINARY)<>CAST(@expense_report_relocation_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: target database changed';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: MySQL 8 is required';
  END IF;

  SELECT GET_LOCK('erp_payable_expense_report_relocation_v265',10) INTO got_lock;
  IF got_lock IS NULL OR got_lock<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: migration lock unavailable';
  END IF;
  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: ERP root conflict';
  END IF;
  SELECT id INTO erp_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: business report root conflict';
  END IF;
  SELECT id INTO business_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: fund report group conflict';
  END IF;
  SELECT id INTO fund_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=fund_report_id AND path='payable-detail')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: payable report group conflict';
  END IF;
  SELECT id INTO payable_group_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=fund_report_id AND path='payable-detail';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=2 AND path='payable-expense-detail'
        AND component='erp/report/payable-expense-detail/index'
        AND component_name='ErpPayableExpenseDetailReport')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: expense report page missing or duplicated';
  END IF;
  SELECT id,parent_id,sort INTO expense_page_id,original_parent_id,original_sort
  FROM system_menu
  WHERE deleted=b'0' AND type=2 AND path='payable-expense-detail'
    AND component='erp/report/payable-expense-detail/index'
    AND component_name='ErpPayableExpenseDetailReport';

  IF original_parent_id NOT IN (fund_report_id,payable_group_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: expense report has an unexpected parent';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND permission='erp:payable-expense-detail-report:query')<>1
      OR (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND permission='erp:payable-expense-detail-report:export')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: expense report permission missing or duplicated';
  END IF;
  SELECT id INTO expense_query_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:payable-expense-detail-report:query';
  SELECT id INTO expense_export_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:payable-expense-detail-report:export';
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0'
      AND id IN (expense_query_id,expense_export_id)
      AND (type<>3 OR parent_id<>expense_page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: expense report permission parent conflict';
  END IF;

  IF original_parent_id=fund_report_id THEN
    SELECT COALESCE(MAX(sort),0)+1 INTO target_sort
    FROM system_menu WHERE deleted=b'0' AND parent_id=payable_group_id;

    UPDATE system_menu
    SET sort=sort-1,updater='1',update_time=NOW()
    WHERE deleted=b'0' AND parent_id=fund_report_id
      AND id<>expense_page_id AND sort>original_sort;

    UPDATE system_menu
    SET parent_id=payable_group_id,sort=target_sort,updater='1',update_time=NOW()
    WHERE id=expense_page_id AND deleted=b'0' AND parent_id=fund_report_id;
  END IF;

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source
  INNER JOIN system_role role_row ON role_row.id=source.role_id
    AND role_row.tenant_id=source.tenant_id AND role_row.deleted=b'0'
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT payable_group_id UNION ALL SELECT expense_page_id) target
  WHERE source.deleted=b'0'
    AND source.menu_id IN (expense_page_id,expense_query_id,expense_export_id)
    AND NOT EXISTS (SELECT 1 FROM system_role_menu current
      WHERE current.deleted=b'0' AND current.role_id=source.role_id
        AND current.tenant_id=source.tenant_id AND current.menu_id=target.menu_id);

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_expense_relocation_package_v265;
  CREATE TEMPORARY TABLE tmp_expense_relocation_package_v265(
    package_id BIGINT PRIMARY KEY,
    missing_ids JSON NOT NULL
  );
  INSERT INTO tmp_expense_relocation_package_v265(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT package_row.id package_id,target.menu_id
    FROM system_tenant_package package_row
    JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
      UNION ALL SELECT payable_group_id UNION ALL SELECT expense_page_id) target
    WHERE package_row.deleted=b'0'
      AND (JSON_CONTAINS(package_row.menu_ids,CAST(expense_page_id AS CHAR),'$')
        OR JSON_CONTAINS(package_row.menu_ids,CAST(expense_query_id AS CHAR),'$')
        OR JSON_CONTAINS(package_row.menu_ids,CAST(expense_export_id AS CHAR),'$'))
      AND NOT JSON_CONTAINS(package_row.menu_ids,CAST(target.menu_id AS CHAR),'$')
  ) missing GROUP BY package_id;

  SELECT character_maximum_length INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package package_row
      JOIN tmp_expense_relocation_package_v265 target ON target.package_id=package_row.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(package_row.menu_ids,target.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v265: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package package_row
  JOIN tmp_expense_relocation_package_v265 target ON target.package_id=package_row.id
  SET package_row.menu_ids=JSON_MERGE_PRESERVE(package_row.menu_ids,target.missing_ids),
      package_row.update_time=NOW()
  WHERE package_row.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_expense_relocation_package_v265;

  COMMIT;
  DO RELEASE_LOCK('erp_payable_expense_report_relocation_v265');
  SET got_lock=0;
END$$
DELIMITER ;

CALL relocate_payable_expense_report_v265();
DROP PROCEDURE IF EXISTS relocate_payable_expense_report_v265;

SELECT child.id,child.parent_id,parent.name parent_name,child.name,child.type,child.sort,
       child.path,child.component,child.component_name,child.permission
FROM system_menu child
LEFT JOIN system_menu parent ON parent.id=child.parent_id AND parent.deleted=b'0'
WHERE child.deleted=b'0' AND (child.id IN (
    SELECT id FROM system_menu WHERE deleted=b'0' AND type=1 AND path='payable-detail'
  ) OR child.path='payable-expense-detail'
  OR child.permission IN ('erp:payable-expense-detail-report:query','erp:payable-expense-detail-report:export'))
ORDER BY child.parent_id,child.type,child.sort,child.id;
