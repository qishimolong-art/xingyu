-- v270 其他入库明细报表（MySQL 8）。
-- 执行前必须备份 erp_stock_in、system_menu、system_role_menu、
-- system_tenant_package、system_field_definition、system_role_field_permission，并先运行预检脚本。
-- 历史已审核单据的 approve_time 由 update_time 近似回填。
-- SET @stock_in_detail_schema = 'reviewed_database';
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_stock_in_detail_schema_v270;
DELIMITER $$
CREATE PROCEDURE migrate_stock_in_detail_schema_v270()
BEGIN
  IF @stock_in_detail_schema IS NULL
      OR CAST(DATABASE() AS BINARY) <> CAST(@stock_in_detail_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: set reviewed target database';
  END IF;
  IF VERSION() < '8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: MySQL 8 is required';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_in')
     OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_in_item') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: stock in tables are missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_in' AND column_name = 'update_time')
     OR NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_in' AND column_name = 'status') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: stock in schema is incompatible';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_in' AND column_name = 'approve_time') THEN
    ALTER TABLE erp_stock_in
      ADD COLUMN approve_time DATETIME NULL COMMENT '审核时间' AFTER status;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_in'
      GROUP BY index_name
      HAVING index_columns = 'tenant_id,status,deleted,approve_time,id') equivalent_index) THEN
    ALTER TABLE erp_stock_in ADD INDEX idx_stock_in_detail_report
      (tenant_id, status, deleted, approve_time, id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM (
      SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
      FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'erp_stock_in_item'
      GROUP BY index_name
      HAVING index_columns = 'tenant_id,in_id,deleted,id') equivalent_index) THEN
    ALTER TABLE erp_stock_in_item ADD INDEX idx_stock_in_item_detail_report
      (tenant_id, in_id, deleted, id);
  END IF;

  UPDATE erp_stock_in
  SET approve_time = update_time
  WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;
  SELECT ROW_COUNT() approve_time_backfilled_rows;
END$$
DELIMITER ;
CALL migrate_stock_in_detail_schema_v270();
DROP PROCEDURE migrate_stock_in_detail_schema_v270;

DROP PROCEDURE IF EXISTS migrate_stock_in_detail_config_v270;
DELIMITER $$
CREATE PROCEDURE migrate_stock_in_detail_config_v270()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE business_report_id BIGINT;
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
    DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_role_v270;
    DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_package_menu_v270;
    DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_package_v270;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_stock_in_detail_v270'); END IF;
    RESIGNAL;
  END;

  IF @stock_in_detail_schema IS NULL
      OR CAST(DATABASE() AS BINARY) <> CAST(@stock_in_detail_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: target database changed';
  END IF;
  SELECT GET_LOCK('erp_stock_in_detail_v270', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: migration lock unavailable';
  END IF;
  START TRANSACTION;

  SELECT id INTO erp_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp';
  SELECT id INTO business_report_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = erp_id AND path = 'business-report';
  SELECT id INTO inventory_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = business_report_id AND path = 'inventory';
  SELECT id INTO source_query_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:stock-in:query';
  SELECT id INTO source_export_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:stock-in:export';
  IF erp_id IS NULL OR business_report_id IS NULL OR inventory_id IS NULL
      OR source_query_id IS NULL OR source_export_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: report path or source permissions missing';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND path = 'other-in-detail'
      AND (parent_id <> inventory_id OR type <> 2
           OR IFNULL(component, '') <> 'erp/stock/report/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: report page path conflict';
  END IF;
  IF EXISTS (SELECT permission FROM system_menu WHERE deleted = b'0'
      AND permission IN ('erp:stock-in-detail-report:query', 'erp:stock-in-detail-report:export')
      GROUP BY permission HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: duplicate report permissions';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids) <> 'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: invalid tenant package JSON';
  END IF;

  INSERT INTO system_field_definition
    (module, field_key, field_label, field_group, sort, creator, create_time,
     updater, update_time, deleted, tenant_id)
  SELECT DISTINCT source.module, 'approveTime', '审核时间', 'audit_info', 925,
         '1', NOW(), '1', NOW(), b'0', source.tenant_id
  FROM system_field_definition source
  WHERE source.deleted = b'0' AND source.module = 'erp_stock_in'
    AND NOT EXISTS (SELECT 1 FROM system_field_definition target
      WHERE target.deleted = b'0' AND target.module = source.module
        AND target.field_key = 'approveTime' AND target.tenant_id = source.tenant_id);

  INSERT INTO system_role_field_permission
    (role_id, field_id, hidden, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT permission_row.role_id, target.id, permission_row.hidden,
         '1', NOW(), '1', NOW(), b'0', permission_row.tenant_id
  FROM system_role_field_permission permission_row
  INNER JOIN system_field_definition source ON source.id = permission_row.field_id
    AND source.tenant_id = permission_row.tenant_id AND source.deleted = b'0'
  INNER JOIN system_field_definition target ON target.module = source.module
    AND target.tenant_id = source.tenant_id AND target.deleted = b'0'
    AND target.field_key = 'approveTime'
  WHERE permission_row.deleted = b'0' AND permission_row.hidden = b'1'
    AND source.module = 'erp_stock_in' AND source.field_key = 'updateTime'
    AND NOT EXISTS (SELECT 1 FROM system_role_field_permission existing
      WHERE existing.deleted = b'0' AND existing.role_id = permission_row.role_id
        AND existing.field_id = target.id AND existing.tenant_id = permission_row.tenant_id);

  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND type = 2 AND parent_id = inventory_id AND path = 'other-in-detail') THEN
    UPDATE system_menu SET sort = sort + 1, updater = '1', update_time = NOW()
    WHERE deleted = b'0' AND parent_id = inventory_id AND sort >= 5;
    INSERT INTO system_menu
      (name, permission, type, sort, parent_id, path, icon, component, component_name,
       status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
    VALUES ('其他入库明细报表', '', 2, 5, inventory_id, 'other-in-detail',
      'ep:document', 'erp/stock/report/index', 'ErpBusinessReportInventoryOtherInDetail',
      0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0');
  END IF;
  SELECT id INTO page_id FROM system_menu
  WHERE deleted = b'0' AND type = 2 AND parent_id = inventory_id AND path = 'other-in-detail';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND permission IN ('erp:stock-in-detail-report:query', 'erp:stock-in-detail-report:export')
      AND (type <> 3 OR parent_id <> page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: report permission parent conflict';
  END IF;
  INSERT INTO system_menu
    (name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT seed.name, seed.permission, 3, seed.sort_order, page_id, '', '', '', NULL,
         0, b'1', b'0', b'0', '1', NOW(), '1', NOW(), b'0'
  FROM (
    SELECT '其他入库明细报表查询' name, 'erp:stock-in-detail-report:query' permission, 1 sort_order
    UNION ALL
    SELECT '其他入库明细报表导出', 'erp:stock-in-detail-report:export', 2
  ) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu current
    WHERE current.deleted = b'0' AND current.permission = seed.permission);
  SELECT id INTO query_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:stock-in-detail-report:query';
  SELECT id INTO export_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:stock-in-detail-report:export';

  DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_role_v270;
  CREATE TEMPORARY TABLE tmp_stock_in_detail_role_v270 (
    role_id BIGINT NOT NULL, tenant_id BIGINT NOT NULL, menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, tenant_id, menu_id));
  INSERT IGNORE INTO tmp_stock_in_detail_role_v270
  SELECT source.role_id, source.tenant_id, target.menu_id
  FROM system_role_menu source
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT inventory_id
        UNION ALL SELECT page_id UNION ALL SELECT query_id) target
  WHERE source.deleted = b'0' AND source.menu_id = source_query_id;
  INSERT IGNORE INTO tmp_stock_in_detail_role_v270
  SELECT source.role_id, source.tenant_id, export_id
  FROM system_role_menu source
  WHERE source.deleted = b'0' AND source.menu_id = source_export_id;
  INSERT INTO system_role_menu
    (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT target.role_id, target.menu_id, '1', NOW(), '1', NOW(), b'0', target.tenant_id
  FROM tmp_stock_in_detail_role_v270 target
  WHERE NOT EXISTS (SELECT 1 FROM system_role_menu current
    WHERE current.deleted = b'0' AND current.role_id = target.role_id
      AND current.tenant_id = target.tenant_id AND current.menu_id = target.menu_id);
  DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_role_v270;

  DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_package_menu_v270;
  CREATE TEMPORARY TABLE tmp_stock_in_detail_package_menu_v270 (
    package_id BIGINT NOT NULL, menu_id BIGINT NOT NULL,
    PRIMARY KEY (package_id, menu_id));
  INSERT IGNORE INTO tmp_stock_in_detail_package_menu_v270
  SELECT package_row.id, target.menu_id
  FROM system_tenant_package package_row
  JOIN (SELECT business_report_id menu_id UNION ALL SELECT inventory_id
        UNION ALL SELECT page_id UNION ALL SELECT query_id) target
  WHERE package_row.deleted = b'0'
    AND JSON_CONTAINS(package_row.menu_ids, CAST(source_query_id AS CHAR), '$');
  INSERT IGNORE INTO tmp_stock_in_detail_package_menu_v270
  SELECT package_row.id, export_id
  FROM system_tenant_package package_row
  WHERE package_row.deleted = b'0'
    AND JSON_CONTAINS(package_row.menu_ids, CAST(source_export_id AS CHAR), '$');
  DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_package_v270;
  CREATE TEMPORARY TABLE tmp_stock_in_detail_package_v270 (
    package_id BIGINT PRIMARY KEY, missing_ids JSON NOT NULL);
  INSERT INTO tmp_stock_in_detail_package_v270
  SELECT pending.package_id, JSON_ARRAYAGG(pending.menu_id)
  FROM tmp_stock_in_detail_package_menu_v270 pending
  INNER JOIN system_tenant_package package_row ON package_row.id = pending.package_id
    AND package_row.deleted = b'0'
  WHERE NOT JSON_CONTAINS(package_row.menu_ids, CAST(pending.menu_id AS CHAR), '$')
  GROUP BY pending.package_id;
  SELECT character_maximum_length INTO package_limit
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'system_tenant_package' AND column_name = 'menu_ids';
  IF package_limit IS NOT NULL AND EXISTS (
    SELECT 1 FROM system_tenant_package package_row
    INNER JOIN tmp_stock_in_detail_package_v270 pending ON pending.package_id = package_row.id
    WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(package_row.menu_ids, pending.missing_ids)) > package_limit
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v270: package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package package_row
  INNER JOIN tmp_stock_in_detail_package_v270 pending ON pending.package_id = package_row.id
  SET package_row.menu_ids = JSON_MERGE_PRESERVE(package_row.menu_ids, pending.missing_ids),
      package_row.update_time = NOW()
  WHERE package_row.deleted = b'0';
  DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_package_v270;
  DROP TEMPORARY TABLE IF EXISTS tmp_stock_in_detail_package_menu_v270;

  COMMIT;
  DO RELEASE_LOCK('erp_stock_in_detail_v270');
  SET got_lock = 0;
END$$
DELIMITER ;
CALL migrate_stock_in_detail_config_v270();
DROP PROCEDURE migrate_stock_in_detail_config_v270;

SELECT COUNT(*) approved_rows_without_approve_time
FROM erp_stock_in WHERE deleted = b'0' AND status = 20 AND approve_time IS NULL;

SELECT table_name, index_name,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name IN ('erp_stock_in', 'erp_stock_in_item')
  AND index_name IN ('idx_stock_in_detail_report', 'idx_stock_in_item_detail_report')
GROUP BY table_name, index_name;

SELECT id, parent_id, name, type, sort, path, component, component_name, permission
FROM system_menu
WHERE deleted = b'0' AND (path = 'other-in-detail'
  OR permission IN ('erp:stock-in-detail-report:query', 'erp:stock-in-detail-report:export'))
ORDER BY parent_id, type, sort, id;

SELECT tenant_id, field_key, field_label, field_group, sort
FROM system_field_definition
WHERE deleted = b'0' AND module = 'erp_stock_in' AND field_key = 'approveTime'
ORDER BY tenant_id;
