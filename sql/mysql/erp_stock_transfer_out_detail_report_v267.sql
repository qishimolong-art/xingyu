-- v267 调拨出库查询报表（MySQL 8）。
-- DDL 会隐式提交；正式库执行前必须备份 system_menu、system_role_menu、system_tenant_package
-- 及 erp_stock_move 表结构，并先执行同名 precheck 脚本。
-- SET @stock_transfer_out_detail_schema = 'reviewed_database';
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_stock_transfer_out_detail_index_v267;
DELIMITER $$
CREATE PROCEDURE migrate_stock_transfer_out_detail_index_v267()
BEGIN
  IF @stock_transfer_out_detail_schema IS NULL
     OR BINARY DATABASE() <> BINARY @stock_transfer_out_detail_schema THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: set reviewed target database';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_move' AND table_type = 'BASE TABLE')
     OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_move_item' AND table_type = 'BASE TABLE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: stock transfer tables missing';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM (
      SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_move'
      GROUP BY index_name
      HAVING index_columns LIKE 'tenant_id,transfer_direction,status,deleted,move_time%'
    ) equivalent_index
  ) THEN
    ALTER TABLE erp_stock_move
      ADD INDEX idx_stock_transfer_out_detail_report
        (tenant_id, transfer_direction, status, deleted, move_time, id);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM (
      SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_move_item'
      GROUP BY index_name
      HAVING index_columns LIKE 'tenant_id,move_id%'
    ) parent_index
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: item tenant/move parent index missing';
  END IF;
END$$
DELIMITER ;
CALL migrate_stock_transfer_out_detail_index_v267();
DROP PROCEDURE migrate_stock_transfer_out_detail_index_v267;

DROP PROCEDURE IF EXISTS migrate_stock_transfer_out_detail_menu_v267;
DELIMITER $$
CREATE PROCEDURE migrate_stock_transfer_out_detail_menu_v267()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE inventory_id BIGINT;
  DECLARE page_id BIGINT;
  DECLARE query_id BIGINT;
  DECLARE export_id BIGINT;
  DECLARE source_query_id BIGINT;
  DECLARE source_export_id BIGINT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_stock_transfer_out_detail_v267'); END IF;
    RESIGNAL;
  END;

  IF @stock_transfer_out_detail_schema IS NULL
     OR BINARY DATABASE() <> BINARY @stock_transfer_out_detail_schema THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: target changed before menu migration';
  END IF;
  SELECT GET_LOCK('erp_stock_transfer_out_detail_v267', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: migration lock unavailable';
  END IF;
  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: ERP root conflict';
  END IF;
  SELECT id INTO erp_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND type = 1 AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: business report root conflict';
  END IF;
  SELECT id INTO report_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = erp_id AND path = 'business-report';

  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND type = 1 AND parent_id = report_id AND path = 'inventory') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: inventory report group conflict';
  END IF;
  SELECT id INTO inventory_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = report_id AND path = 'inventory';

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND permission = 'erp:stock-transfer-out:query') <> 1
     OR (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND permission = 'erp:stock-transfer-out:export') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: source permissions missing or duplicated';
  END IF;
  SELECT id INTO source_query_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:stock-transfer-out:query';
  SELECT id INTO source_export_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:stock-transfer-out:export';

  IF EXISTS (SELECT permission FROM system_menu
      WHERE deleted = b'0' AND permission IN
        ('erp:stock-transfer-out-detail-report:query', 'erp:stock-transfer-out-detail-report:export')
      GROUP BY permission HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: duplicate report permissions';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND path = 'transfer-out-detail' AND parent_id <> inventory_id) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: report page path conflict';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND path = 'transfer-out-detail' AND parent_id = inventory_id
      AND (type <> 2 OR IFNULL(component, '') <> 'erp/stock/report/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: report page component conflict';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND type = 2 AND parent_id = inventory_id AND path = 'transfer-out-detail') THEN
    UPDATE system_menu
    SET sort = sort + 1, updater = '1', update_time = NOW()
    WHERE deleted = b'0' AND parent_id = inventory_id AND sort >= 3;
    INSERT INTO system_menu
      (name, permission, type, sort, parent_id, path, icon, component, component_name,
       status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
    VALUES
      ('调拨出库查询报表', '', 2, 3, inventory_id, 'transfer-out-detail',
       'ep:document', 'erp/stock/report/index', 'ErpBusinessReportInventoryTransferOutDetail', 0, b'1', b'1', b'0',
       '1', NOW(), '1', NOW(), b'0');
  END IF;
  SELECT id INTO page_id FROM system_menu
  WHERE deleted = b'0' AND type = 2 AND parent_id = inventory_id AND path = 'transfer-out-detail';
  UPDATE system_menu
  SET name = '调拨出库查询报表', sort = 3, component = 'erp/stock/report/index',
      component_name = 'ErpBusinessReportInventoryTransferOutDetail', updater = '1', update_time = NOW()
  WHERE id = page_id
    AND (name <> '调拨出库查询报表' OR sort <> 3
         OR IFNULL(component, '') <> 'erp/stock/report/index'
         OR IFNULL(component_name, '') <> 'ErpBusinessReportInventoryTransferOutDetail');

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND permission IN ('erp:stock-transfer-out-detail-report:query',
                         'erp:stock-transfer-out-detail-report:export')
      AND (type <> 3 OR parent_id <> page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: report permission parent conflict';
  END IF;
  INSERT INTO system_menu
    (name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT seed.name, seed.permission, 3, seed.sort_order, page_id, '', '', '', NULL,
         0, b'1', b'0', b'0', '1', NOW(), '1', NOW(), b'0'
  FROM (
    SELECT '调拨出库报表查询' name, 'erp:stock-transfer-out-detail-report:query' permission, 1 sort_order
    UNION ALL
    SELECT '调拨出库报表导出', 'erp:stock-transfer-out-detail-report:export', 2
  ) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m
    WHERE m.deleted = b'0' AND m.permission = seed.permission);

  SELECT id INTO query_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:stock-transfer-out-detail-report:query';
  SELECT id INTO export_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:stock-transfer-out-detail-report:export';

  INSERT INTO system_role_menu
    (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT DISTINCT source.role_id, target.menu_id, '1', NOW(), '1', NOW(), b'0', source.tenant_id
  FROM system_role_menu source
  INNER JOIN system_role role_row ON role_row.id = source.role_id
    AND role_row.tenant_id = source.tenant_id AND role_row.deleted = b'0'
  JOIN (SELECT report_id menu_id UNION ALL SELECT inventory_id UNION ALL SELECT page_id UNION ALL SELECT query_id) target
  WHERE source.deleted = b'0' AND source.menu_id = source_query_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
      WHERE existing.deleted = b'0' AND existing.role_id = source.role_id
        AND existing.tenant_id = source.tenant_id AND existing.menu_id = target.menu_id);

  INSERT INTO system_role_menu
    (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT DISTINCT source.role_id, export_id, '1', NOW(), '1', NOW(), b'0', source.tenant_id
  FROM system_role_menu source
  WHERE source.deleted = b'0' AND source.menu_id = source_export_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
      WHERE existing.deleted = b'0' AND existing.role_id = source.role_id
        AND existing.tenant_id = source.tenant_id AND existing.menu_id = export_id);

  IF EXISTS (SELECT 1 FROM system_tenant_package
      WHERE deleted = b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids) <> 'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: invalid package JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_stock_transfer_out_package_v267;
  CREATE TEMPORARY TABLE tmp_stock_transfer_out_package_v267
    (package_id BIGINT PRIMARY KEY, missing_ids JSON NOT NULL);
  INSERT INTO tmp_stock_transfer_out_package_v267
  SELECT package_id, JSON_ARRAYAGG(menu_id)
  FROM (
    SELECT DISTINCT package_row.id package_id, target.menu_id
    FROM system_tenant_package package_row
    JOIN (SELECT report_id menu_id UNION ALL SELECT inventory_id UNION ALL SELECT page_id UNION ALL SELECT query_id) target
    WHERE package_row.deleted = b'0'
      AND JSON_CONTAINS(package_row.menu_ids, CAST(source_query_id AS CHAR), '$')
      AND NOT JSON_CONTAINS(package_row.menu_ids, CAST(target.menu_id AS CHAR), '$')
    UNION ALL
    SELECT package_row.id, export_id
    FROM system_tenant_package package_row
    WHERE package_row.deleted = b'0'
      AND JSON_CONTAINS(package_row.menu_ids, CAST(source_export_id AS CHAR), '$')
      AND NOT JSON_CONTAINS(package_row.menu_ids, CAST(export_id AS CHAR), '$')
  ) pending
  GROUP BY package_id;
  SELECT character_maximum_length INTO package_limit
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'system_tenant_package' AND column_name = 'menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (
    SELECT 1 FROM system_tenant_package package_row
    INNER JOIN tmp_stock_transfer_out_package_v267 pending ON pending.package_id = package_row.id
    WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(package_row.menu_ids, pending.missing_ids)) > package_limit
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v267: package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package package_row
  INNER JOIN tmp_stock_transfer_out_package_v267 pending ON pending.package_id = package_row.id
  SET package_row.menu_ids = JSON_MERGE_PRESERVE(package_row.menu_ids, pending.missing_ids),
      package_row.update_time = NOW()
  WHERE package_row.deleted = b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_stock_transfer_out_package_v267;

  COMMIT;
  DO RELEASE_LOCK('erp_stock_transfer_out_detail_v267');
  SET got_lock = 0;
END$$
DELIMITER ;
CALL migrate_stock_transfer_out_detail_menu_v267();
DROP PROCEDURE migrate_stock_transfer_out_detail_menu_v267;

SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'erp_stock_move'
GROUP BY index_name
HAVING index_columns LIKE 'tenant_id,transfer_direction,status,deleted,move_time%';

SELECT id, parent_id, name, type, sort, path, component, component_name, permission
FROM system_menu
WHERE deleted = b'0'
  AND (path = 'transfer-out-detail'
       OR permission IN ('erp:stock-transfer-out-detail-report:query',
                         'erp:stock-transfer-out-detail-report:export'))
ORDER BY parent_id, type, sort, id;
