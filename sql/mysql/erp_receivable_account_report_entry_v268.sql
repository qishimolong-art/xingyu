-- 将“资金管理 → 应收账款”复制为“经营报表 → 资金报表 → 应收报表”只读入口（MySQL 8）
-- 原页面、菜单、查询/核销权限和角色授权保持不变；新入口通过独立包装组件复用原查询能力。
SET @receivable_account_report_entry_schema := DATABASE();

DROP PROCEDURE IF EXISTS add_receivable_account_report_entry_v268;
DELIMITER $$
CREATE PROCEDURE add_receivable_account_report_entry_v268()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
  DECLARE fund_report_id BIGINT;
  DECLARE receivable_group_id BIGINT;
  DECLARE source_page_id BIGINT;
  DECLARE source_query_id BIGINT;
  DECLARE report_page_id BIGINT;
  DECLARE target_sort INT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_receivable_account_report_package_v268;
    IF got_lock=1 THEN DO RELEASE_LOCK('erp_receivable_account_report_entry_v268'); END IF;
    RESIGNAL;
  END;

  IF @receivable_account_report_entry_schema IS NULL OR @receivable_account_report_entry_schema='' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: no target database selected';
  END IF;
  IF CAST(DATABASE() AS BINARY)<>CAST(@receivable_account_report_entry_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: target database changed';
  END IF;
  IF VERSION()<'8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: MySQL 8 is required';
  END IF;

  SELECT GET_LOCK('erp_receivable_account_report_entry_v268',10) INTO got_lock;
  IF got_lock IS NULL OR got_lock<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: migration lock unavailable';
  END IF;
  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: ERP root conflict';
  END IF;
  SELECT id INTO erp_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=0 AND path='/erp';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: business report root conflict';
  END IF;
  SELECT id INTO business_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=erp_id AND path='business-report';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: fund report group conflict';
  END IF;
  SELECT id INTO fund_report_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=business_report_id AND path='counterparty';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND type=1 AND parent_id=fund_report_id AND path='receivable-detail')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: receivable report group conflict';
  END IF;
  SELECT id INTO receivable_group_id FROM system_menu
  WHERE deleted=b'0' AND type=1 AND parent_id=fund_report_id AND path='receivable-detail';

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2
      AND component='erp/finance/receivable/account/index'
      AND component_name='ErpReceivableAccount')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: source receivable account page missing or duplicated';
  END IF;
  SELECT id INTO source_page_id FROM system_menu WHERE deleted=b'0' AND type=2
    AND component='erp/finance/receivable/account/index'
    AND component_name='ErpReceivableAccount';
  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted=b'0' AND permission='erp:receivable-account:query'
        AND type=3 AND parent_id=source_page_id)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: source receivable account query permission conflict';
  END IF;
  SELECT id INTO source_query_id FROM system_menu
  WHERE deleted=b'0' AND permission='erp:receivable-account:query'
    AND type=3 AND parent_id=source_page_id;

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0'
      AND (path='receivable-account' OR component='erp/report/receivable-account/index'
        OR component_name='ErpReceivableAccountReport')
      AND (type<>2 OR parent_id<>receivable_group_id OR path<>'receivable-account'
        OR component<>'erp/report/receivable-account/index'
        OR component_name<>'ErpReceivableAccountReport')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: receivable account report entry conflict';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND type=2
      AND parent_id=receivable_group_id AND path='receivable-account'
      AND component='erp/report/receivable-account/index'
      AND component_name='ErpReceivableAccountReport') THEN
    SELECT COALESCE(MAX(sort),0)+1 INTO target_sort
    FROM system_menu WHERE deleted=b'0' AND parent_id=receivable_group_id;
    INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,
      visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
    VALUES('应收账款','',2,target_sort,receivable_group_id,'receivable-account','ep:histogram',
      'erp/report/receivable-account/index','ErpReceivableAccountReport',0,b'1',b'1',b'0',
      '1',NOW(),'1',NOW(),b'0');
  END IF;
  SELECT id INTO report_page_id FROM system_menu WHERE deleted=b'0' AND type=2
    AND parent_id=receivable_group_id AND path='receivable-account'
    AND component='erp/report/receivable-account/index'
    AND component_name='ErpReceivableAccountReport';

  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,target.menu_id,'1',NOW(),'1',NOW(),b'0',source.tenant_id
  FROM system_role_menu source
  INNER JOIN system_role role_row ON role_row.id=source.role_id
    AND role_row.tenant_id=source.tenant_id AND role_row.deleted=b'0'
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
    UNION ALL SELECT receivable_group_id UNION ALL SELECT report_page_id) target
  WHERE source.deleted=b'0' AND source.menu_id=source_query_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu current
      WHERE current.deleted=b'0' AND current.role_id=source.role_id
        AND current.tenant_id=source.tenant_id AND current.menu_id=target.menu_id);

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_receivable_account_report_package_v268;
  CREATE TEMPORARY TABLE tmp_receivable_account_report_package_v268(
    package_id BIGINT PRIMARY KEY,
    missing_ids JSON NOT NULL
  );
  INSERT INTO tmp_receivable_account_report_package_v268(package_id,missing_ids)
  SELECT package_id,JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT package_row.id package_id,target.menu_id
    FROM system_tenant_package package_row
    JOIN (SELECT business_report_id menu_id UNION ALL SELECT fund_report_id
      UNION ALL SELECT receivable_group_id UNION ALL SELECT report_page_id) target
    WHERE package_row.deleted=b'0'
      AND JSON_CONTAINS(package_row.menu_ids,CAST(source_query_id AS CHAR),'$')
      AND NOT JSON_CONTAINS(package_row.menu_ids,CAST(target.menu_id AS CHAR),'$')
  ) missing GROUP BY package_id;

  SELECT character_maximum_length INTO package_limit FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='system_tenant_package' AND column_name='menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (SELECT 1 FROM system_tenant_package package_row
      JOIN tmp_receivable_account_report_package_v268 target ON target.package_id=package_row.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(package_row.menu_ids,target.missing_ids))>package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package package_row
  JOIN tmp_receivable_account_report_package_v268 target ON target.package_id=package_row.id
  SET package_row.menu_ids=JSON_MERGE_PRESERVE(package_row.menu_ids,target.missing_ids),
      package_row.update_time=NOW()
  WHERE package_row.deleted=b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_receivable_account_report_package_v268;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=2
      AND parent_id=receivable_group_id AND path='receivable-account'
      AND component='erp/report/receivable-account/index'
      AND component_name='ErpReceivableAccountReport')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: report page verification failed';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role_menu source
      INNER JOIN system_role role_row ON role_row.id=source.role_id
        AND role_row.tenant_id=source.tenant_id AND role_row.deleted=b'0'
      WHERE source.deleted=b'0' AND source.menu_id=source_query_id
        AND NOT EXISTS (SELECT 1 FROM system_role_menu report_link
          WHERE report_link.deleted=b'0' AND report_link.role_id=source.role_id
            AND report_link.tenant_id=source.tenant_id AND report_link.menu_id=report_page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: source query role missing report page';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role_menu report_link
      WHERE report_link.deleted=b'0' AND report_link.menu_id=report_page_id
        AND NOT EXISTS (SELECT 1 FROM system_role_menu source
          WHERE source.deleted=b'0' AND source.role_id=report_link.role_id
            AND source.tenant_id=report_link.tenant_id AND source.menu_id=source_query_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: report page granted beyond source query roles';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role_menu report_link
      WHERE report_link.deleted=b'0' AND report_link.menu_id=report_page_id
        AND (NOT EXISTS (SELECT 1 FROM system_role_menu parent_link
            WHERE parent_link.deleted=b'0' AND parent_link.role_id=report_link.role_id
              AND parent_link.tenant_id=report_link.tenant_id AND parent_link.menu_id=business_report_id)
          OR NOT EXISTS (SELECT 1 FROM system_role_menu parent_link
            WHERE parent_link.deleted=b'0' AND parent_link.role_id=report_link.role_id
              AND parent_link.tenant_id=report_link.tenant_id AND parent_link.menu_id=fund_report_id)
          OR NOT EXISTS (SELECT 1 FROM system_role_menu parent_link
            WHERE parent_link.deleted=b'0' AND parent_link.role_id=report_link.role_id
              AND parent_link.tenant_id=report_link.tenant_id AND parent_link.menu_id=receivable_group_id))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: report role parent menu chain incomplete';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package package_row
      WHERE package_row.deleted=b'0'
        AND JSON_CONTAINS(package_row.menu_ids,CAST(report_page_id AS CHAR),'$')
        AND NOT JSON_CONTAINS(package_row.menu_ids,CAST(source_query_id AS CHAR),'$')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: report page granted beyond source query packages';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package package_row
      WHERE package_row.deleted=b'0'
        AND JSON_CONTAINS(package_row.menu_ids,CAST(report_page_id AS CHAR),'$')
        AND (NOT JSON_CONTAINS(package_row.menu_ids,CAST(business_report_id AS CHAR),'$')
          OR NOT JSON_CONTAINS(package_row.menu_ids,CAST(fund_report_id AS CHAR),'$')
          OR NOT JSON_CONTAINS(package_row.menu_ids,CAST(receivable_group_id AS CHAR),'$'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v268: report package parent menu chain incomplete';
  END IF;

  COMMIT;
  DO RELEASE_LOCK('erp_receivable_account_report_entry_v268');
  SET got_lock=0;
END$$
DELIMITER ;

CALL add_receivable_account_report_entry_v268();
DROP PROCEDURE IF EXISTS add_receivable_account_report_entry_v268;

SELECT child.id,child.parent_id,parent.name parent_name,child.name,child.type,child.sort,
       child.path,child.component,child.component_name,child.permission
FROM system_menu child
LEFT JOIN system_menu parent ON parent.id=child.parent_id AND parent.deleted=b'0'
WHERE child.deleted=b'0' AND (child.component IN (
    'erp/finance/receivable/account/index','erp/report/receivable-account/index'
  ) OR child.permission IN ('erp:receivable-account:query','erp:receivable-account:writeoff'))
ORDER BY child.parent_id,child.type,child.sort,child.id;
