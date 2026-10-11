-- v251: 配件订货明细表。
-- 1. 采购订单明细增加首次下订库存快照；历史数据保持 NULL，不做伪造回填。
-- 2. 增加报表日期分页、订单明细关联索引；存在同列顺序索引时跳过。
-- 3. 在“经营报表 -> 采购管理”下增加独立页面和查询/导出权限。
-- 4. 角色、套餐仅基于既有采购订单查询/导出权限幂等追加，不删除、不覆盖任何原权限。
--
-- DDL 会隐式提交。执行前请停写、备份相关表，并把变量设置成已复核的目标数据库名：
-- SET @purchase_order_detail_report_schema = 'your_database';

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_purchase_order_detail_report_schema_v251;
DELIMITER $$
CREATE PROCEDURE migrate_purchase_order_detail_report_schema_v251()
BEGIN
  DECLARE column_count INT DEFAULT 0;
  DECLARE order_index_count INT DEFAULT 0;
  DECLARE item_index_count INT DEFAULT 0;

  IF @purchase_order_detail_report_schema IS NULL
      OR BINARY DATABASE() <> BINARY @purchase_order_detail_report_schema THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'v251: set purchase_order_detail_report_schema to the reviewed target database';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'erp_purchase_order'
        AND table_type = 'BASE TABLE')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'erp_purchase_order_items'
        AND table_type = 'BASE TABLE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: purchase order tables do not exist';
  END IF;

  SELECT COUNT(*) INTO column_count
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'erp_purchase_order_items'
    AND column_name = 'stock_count_at_order';

  IF column_count = 0 THEN
    ALTER TABLE erp_purchase_order_items
      ADD COLUMN stock_count_at_order DECIMAL(24,6) NULL
      COMMENT '首次下订时产品仓库实际库存快照';
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'erp_purchase_order_items'
        AND column_name = 'stock_count_at_order' AND data_type = 'decimal'
        AND numeric_precision = 24 AND numeric_scale = 6 AND is_nullable = 'YES') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'v251: unexpected stock_count_at_order definition; inspect before continuing';
  END IF;

  SELECT COUNT(*) INTO order_index_count
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'erp_purchase_order'
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') =
      'tenant_id,deleted,status,order_date,id'
  ) matching_order_indexes;
  IF order_index_count = 0 THEN
    ALTER TABLE erp_purchase_order
      ADD INDEX idx_erp_purchase_order_detail_report
        (tenant_id, deleted, status, order_date, id);
  END IF;

  SELECT COUNT(*) INTO item_index_count
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'erp_purchase_order_items'
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') =
      'tenant_id,deleted,order_id,id'
  ) matching_item_indexes;
  IF item_index_count = 0 THEN
    ALTER TABLE erp_purchase_order_items
      ADD INDEX idx_erp_purchase_order_items_detail_report
        (tenant_id, deleted, order_id, id);
  END IF;
END$$
DELIMITER ;

CALL migrate_purchase_order_detail_report_schema_v251();
DROP PROCEDURE migrate_purchase_order_detail_report_schema_v251;

DROP PROCEDURE IF EXISTS migrate_purchase_order_detail_report_menu_v251;
DELIMITER $$
CREATE PROCEDURE migrate_purchase_order_detail_report_menu_v251()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE purchase_group_id BIGINT;
  DECLARE page_id BIGINT;
  DECLARE report_query_id BIGINT;
  DECLARE report_export_id BIGINT;
  DECLARE purchase_query_id BIGINT;
  DECLARE purchase_export_id BIGINT;
  DECLARE package_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN
      DO RELEASE_LOCK('erp_purchase_order_detail_report_v251');
    END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_purchase_order_detail_report_v251', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: migration lock unavailable';
  END IF;
  START TRANSACTION;

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = 0 AND path = '/erp';

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: expected one business-report root';
  END IF;
  SELECT id INTO report_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = erp_id AND path = 'business-report';

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = report_id AND path = 'purchase') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: expected one purchase report group';
  END IF;
  SELECT id INTO purchase_group_id FROM system_menu
  WHERE deleted = b'0' AND type = 1 AND parent_id = report_id AND path = 'purchase';

  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = purchase_group_id AND path = 'order-detail'
      AND (type <> 2 OR IFNULL(component, '') <> 'erp/purchase/report/index'
        OR IFNULL(component_name, '') <> 'ErpBusinessReportPurchaseOrderDetail')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: order-detail menu path conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time,
    updater, update_time, deleted)
  SELECT '配件订货明细表', '', 2, 6, purchase_group_id, 'order-detail', 'ep:list',
    'erp/purchase/report/index', 'ErpBusinessReportPurchaseOrderDetail', 0, b'1', b'1', b'1',
    '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = purchase_group_id AND path = 'order-detail');

  SELECT id INTO page_id FROM system_menu
  WHERE deleted = b'0' AND parent_id = purchase_group_id AND path = 'order-detail';

  IF EXISTS (SELECT permission FROM system_menu WHERE deleted = b'0'
      AND permission IN ('erp:purchase-order-detail-report:query',
                         'erp:purchase-order-detail-report:export')
      GROUP BY permission HAVING COUNT(*) > 1)
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND permission IN ('erp:purchase-order-detail-report:query',
                         'erp:purchase-order-detail-report:export')
      AND (type <> 3 OR parent_id <> page_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: report permission conflicts';
  END IF;

  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time,
    updater, update_time, deleted)
  SELECT seed.name, seed.permission, 3, seed.sort_order, page_id, '', '', '', NULL,
    0, b'1', b'0', b'0', '1', NOW(), '1', NOW(), b'0'
  FROM (
    SELECT '配件订货明细查询' AS name,
      'erp:purchase-order-detail-report:query' AS permission, 1 AS sort_order
    UNION ALL
    SELECT '配件订货明细导出', 'erp:purchase-order-detail-report:export', 2
  ) seed
  WHERE NOT EXISTS (SELECT 1 FROM system_menu m
    WHERE m.deleted = b'0' AND m.permission = seed.permission);

  SELECT id INTO report_query_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:purchase-order-detail-report:query';
  SELECT id INTO report_export_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:purchase-order-detail-report:export';

  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND permission = 'erp:purchase-order:query') <> 1
    OR (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND permission = 'erp:purchase-order:export') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: existing purchase order permissions are not unique';
  END IF;
  SELECT id INTO purchase_query_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:purchase-order:query';
  SELECT id INTO purchase_export_id FROM system_menu
  WHERE deleted = b'0' AND permission = 'erp:purchase-order:export';

  -- 已有采购订单查询权限的角色继承报表目录、页面和查询权限。
  INSERT INTO system_role_menu
    (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT DISTINCT source.role_id, target.menu_id, '1', NOW(), '1', NOW(), b'0', source.tenant_id
  FROM system_role_menu source
  JOIN system_role role_data ON role_data.id = source.role_id
    AND role_data.tenant_id = source.tenant_id AND role_data.deleted = b'0'
  JOIN (
    SELECT report_id AS menu_id UNION ALL SELECT purchase_group_id
    UNION ALL SELECT page_id UNION ALL SELECT report_query_id
  ) target
  WHERE source.deleted = b'0' AND source.menu_id = purchase_query_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
      WHERE existing.role_id = source.role_id AND existing.tenant_id = source.tenant_id
        AND existing.menu_id = target.menu_id AND existing.deleted = b'0');

  -- 只有同时拥有采购订单查询、导出权限的角色才继承报表导出权限。
  INSERT INTO system_role_menu
    (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT DISTINCT source.role_id, report_export_id, '1', NOW(), '1', NOW(), b'0', source.tenant_id
  FROM system_role_menu source
  JOIN system_role_menu query_grant ON query_grant.role_id = source.role_id
    AND query_grant.tenant_id = source.tenant_id AND query_grant.menu_id = purchase_query_id
    AND query_grant.deleted = b'0'
  JOIN system_role role_data ON role_data.id = source.role_id
    AND role_data.tenant_id = source.tenant_id AND role_data.deleted = b'0'
  WHERE source.deleted = b'0' AND source.menu_id = purchase_export_id
    AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
      WHERE existing.role_id = source.role_id AND existing.tenant_id = source.tenant_id
        AND existing.menu_id = report_export_id AND existing.deleted = b'0');

  -- 套餐沿用相同继承原则，只向 JSON 数组追加缺失 ID。
  IF EXISTS (SELECT 1 FROM system_tenant_package
      WHERE deleted = b'0' AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids) <> 'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: invalid tenant package menu JSON';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS tmp_purchase_order_detail_package_v251;
  CREATE TEMPORARY TABLE tmp_purchase_order_detail_package_v251 (
    package_id BIGINT PRIMARY KEY,
    missing_ids JSON NOT NULL
  );
  INSERT INTO tmp_purchase_order_detail_package_v251 (package_id, missing_ids)
  SELECT package_id, JSON_ARRAYAGG(menu_id)
  FROM (
    SELECT DISTINCT package_data.id AS package_id, target.menu_id
    FROM system_tenant_package package_data
    JOIN (
      SELECT report_id AS menu_id UNION ALL SELECT purchase_group_id
      UNION ALL SELECT page_id UNION ALL SELECT report_query_id
    ) target
    WHERE package_data.deleted = b'0'
      AND JSON_CONTAINS(package_data.menu_ids, CAST(purchase_query_id AS CHAR), '$')
      AND NOT JSON_CONTAINS(package_data.menu_ids, CAST(target.menu_id AS CHAR), '$')
    UNION ALL
    SELECT package_data.id, report_export_id
    FROM system_tenant_package package_data
    WHERE package_data.deleted = b'0'
      AND JSON_CONTAINS(package_data.menu_ids, CAST(purchase_query_id AS CHAR), '$')
      AND JSON_CONTAINS(package_data.menu_ids, CAST(purchase_export_id AS CHAR), '$')
      AND NOT JSON_CONTAINS(package_data.menu_ids, CAST(report_export_id AS CHAR), '$')
  ) missing
  GROUP BY package_id;

  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'system_tenant_package'
    AND column_name = 'menu_ids';
  IF EXISTS (SELECT 1 FROM system_tenant_package package_data
      JOIN tmp_purchase_order_detail_package_v251 pending ON pending.package_id = package_data.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(package_data.menu_ids, pending.missing_ids)) > package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v251: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package package_data
  JOIN tmp_purchase_order_detail_package_v251 pending ON pending.package_id = package_data.id
  SET package_data.menu_ids = JSON_MERGE_PRESERVE(package_data.menu_ids, pending.missing_ids),
      package_data.update_time = NOW()
  WHERE package_data.deleted = b'0';

  UPDATE system_menu
  SET name = '采购管理', updater = '1', update_time = NOW()
  WHERE id = purchase_group_id AND deleted = b'0' AND type = 1 AND path = 'purchase'
    AND name <> '采购管理';

  DROP TEMPORARY TABLE IF EXISTS tmp_purchase_order_detail_package_v251;
  COMMIT;
  DO RELEASE_LOCK('erp_purchase_order_detail_report_v251');
  SET got_lock = 0;

  SELECT id, parent_id, name, type, sort, path, component, component_name, permission,
    status, visible
  FROM system_menu
  WHERE deleted = b'0' AND (id = purchase_group_id OR id = page_id OR parent_id = page_id)
  ORDER BY parent_id, sort, id;
END$$
DELIMITER ;

CALL migrate_purchase_order_detail_report_menu_v251();
DROP PROCEDURE migrate_purchase_order_detail_report_menu_v251;

-- 执行后核对：历史快照必须仍为 NULL；新增报表权限不得替换旧权限。
SELECT column_name, column_type, is_nullable, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'erp_purchase_order_items'
  AND column_name = 'stock_count_at_order';

SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_purchase_order', 'erp_purchase_order_items')
  AND index_name IN ('idx_erp_purchase_order_detail_report',
                     'idx_erp_purchase_order_items_detail_report')
GROUP BY table_name, index_name;

SELECT COUNT(*) AS historical_snapshot_non_null_count
FROM erp_purchase_order_items
WHERE deleted = b'0' AND stock_count_at_order IS NOT NULL;
