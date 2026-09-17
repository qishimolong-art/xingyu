-- 经营报表统一导航 v220；与 web-antd router/business-reports.ts 同批部署。
-- 仅迁移已存在的报表页面，保留 ID、组件、按钮、字段、状态和可见性。
-- 原完整 URL 保存在叶子 path；前端恢复该地址，收藏和 query 下钻继续有效。
-- 角色/套餐只补原已授权报表所需的空权限祖先，不新增查询/导出权限。
-- 未实现报表不创建页面；期初确认按钮不自动分配角色/套餐。
-- 使用支持 DELIMITER 的 MySQL 客户端，要求 MySQL 5.7.22+。
-- 执行前停止菜单/角色/套餐编辑并备份三张权限表；执行后刷新权限缓存、重新登录。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_migrate_business_reports_v220`;
DELIMITER $$
CREATE PROCEDURE `erp_migrate_business_reports_v220`()
BEGIN
  DECLARE erp_id BIGINT;
  DECLARE report_id BIGINT;
  DECLARE settings_id BIGINT;
  DECLARE depth_count INT DEFAULT 0;
  DECLARE added_count INT DEFAULT 0;
  DECLARE package_limit BIGINT;
  DECLARE path_limit BIGINT;
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN DO RELEASE_LOCK('erp_business_reports_v220'); END IF;
    RESIGNAL;
  END;

  SELECT GET_LOCK('erp_business_reports_v220', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: migration lock unavailable';
  END IF;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND type = 1
      AND parent_id = 0 AND path = '/erp') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: expected one ERP root at /erp';
  END IF;
  SELECT id INTO erp_id FROM system_menu WHERE deleted = b'0' AND type = 1
    AND parent_id = 0 AND path = '/erp';

  DROP TEMPORARY TABLE IF EXISTS tmp_report_groups_v220;
  CREATE TEMPORARY TABLE tmp_report_groups_v220 (
    group_key VARCHAR(32) PRIMARY KEY, group_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL, menu_id BIGINT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  INSERT INTO tmp_report_groups_v220 VALUES
    ('sale', '销售及客户经营', 1, NULL),
    ('purchase', '采购及供应商经营', 2, NULL),
    ('inventory', '库存及商品追溯', 3, NULL),
    ('transfer', '调拨移仓及在途', 4, NULL),
    ('other-settlement', '其他应收及其他应付', 6, NULL),
    ('profit', '部门利润及驾驶舱', 9, NULL);
  DROP TEMPORARY TABLE IF EXISTS tmp_report_components_v220;
  CREATE TEMPORARY TABLE tmp_report_components_v220 (
    component VARCHAR(255) PRIMARY KEY, group_key VARCHAR(32) NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  INSERT INTO tmp_report_components_v220 VALUES
    ('erp/sale/report/index', 'sale'),
    ('erp/purchase/report/index', 'purchase'),
    ('erp/stock/report/index', 'inventory'),
    ('erp/stock/record/index', 'inventory'),
    ('erp/stock/transfer-ledger/index', 'transfer'),
    ('erp/finance/receivable/report/index', 'other-settlement'),
    ('erp/finance/payable/report/index', 'other-settlement'),
    ('erp/report/system/index', 'profit');

  -- 同一组件多个菜单时不擅自挑选/合并，整批回滚供核对。
  IF EXISTS (SELECT m.component FROM system_menu m
      JOIN tmp_report_components_v220 c ON c.component = m.component
      WHERE m.deleted = b'0' AND m.type = 2 GROUP BY m.component HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: duplicate report components need review';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report'
      AND (type <> 1 OR IFNULL(component_name, '') <> 'ErpBusinessReports' OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: business-report path conflicts';
  END IF;
  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '经营报表', '', 1, 98, erp_id, 'business-report', 'ep:data-analysis', '',
    'ErpBusinessReports', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu
    WHERE deleted = b'0' AND parent_id = erp_id AND path = 'business-report');
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND parent_id = erp_id AND path = 'business-report') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: duplicate business-report roots';
  END IF;
  SELECT id INTO report_id FROM system_menu WHERE deleted = b'0'
    AND parent_id = erp_id AND path = 'business-report';
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND id <> report_id
      AND (component_name = 'ErpBusinessReports' OR (IFNULL(component_name, '') = '' AND name = 'ErpBusinessReports'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: reserved report route name conflicts';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_report_pages_v220;
  CREATE TEMPORARY TABLE tmp_report_pages_v220 (
    menu_id BIGINT PRIMARY KEY, group_key VARCHAR(32) NOT NULL,
    ancestor_id BIGINT NOT NULL, original_path TEXT NOT NULL
  ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  INSERT INTO tmp_report_pages_v220
  SELECT m.id, c.group_key,
    CASE WHEN p.parent_id = report_id AND p.type = 1 AND m.path LIKE '/%' THEN 0 ELSE m.parent_id END,
    m.path
  FROM system_menu m JOIN tmp_report_components_v220 c ON c.component = m.component
  LEFT JOIN system_menu p ON p.id = m.parent_id AND p.deleted = b'0'
  WHERE m.deleted = b'0' AND m.type = 2;

  -- 按现有转换器的实际父路径拼接规则采集旧 URL，不能按组件猜路径。
  WHILE EXISTS (SELECT 1 FROM tmp_report_pages_v220 WHERE ancestor_id <> 0) DO
    SET depth_count = depth_count + 1;
    IF depth_count > 16 OR EXISTS (SELECT 1 FROM tmp_report_pages_v220 t
        LEFT JOIN system_menu p ON p.id = t.ancestor_id AND p.deleted = b'0'
        WHERE t.ancestor_id <> 0 AND p.id IS NULL) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: report parent missing or cyclic';
    END IF;
    UPDATE tmp_report_pages_v220 t JOIN system_menu p ON p.id = t.ancestor_id AND p.deleted = b'0'
    SET t.original_path = CONCAT(p.path, '/', t.original_path), t.ancestor_id = p.parent_id
    WHERE t.ancestor_id <> 0;
  END WHILE;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO path_limit FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'system_menu' AND column_name = 'path';
  IF EXISTS (SELECT 1 FROM tmp_report_pages_v220
      WHERE original_path NOT LIKE '/erp/%' OR original_path LIKE '%//%'
         OR original_path LIKE '%?%' OR original_path LIKE '%#%'
         OR CHAR_LENGTH(original_path) > path_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: unsupported original path; review before migration';
  END IF;

  IF EXISTS (SELECT 1 FROM system_menu m JOIN tmp_report_groups_v220 g ON g.group_key = m.path
      WHERE m.deleted = b'0' AND m.parent_id = report_id
      AND (m.type <> 1 OR IFNULL(m.permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: category conflicts';
  END IF;
  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT g.group_name, '', 1, g.sort_order, report_id, g.group_key, 'ep:folder', '',
    CONCAT('ErpBusinessReports_', g.group_key), 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM tmp_report_groups_v220 g
  WHERE EXISTS (SELECT 1 FROM tmp_report_pages_v220 p WHERE p.group_key = g.group_key)
    AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted = b'0'
      AND m.parent_id = report_id AND m.path = g.group_key);
  IF EXISTS (SELECT m.path FROM system_menu m JOIN tmp_report_groups_v220 g ON g.group_key = m.path
      WHERE m.deleted = b'0' AND m.parent_id = report_id GROUP BY m.path HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: duplicate categories';
  END IF;
  UPDATE tmp_report_groups_v220 g JOIN system_menu m ON m.path = g.group_key
    AND m.parent_id = report_id AND m.deleted = b'0' SET g.menu_id = m.id;

  -- 采集页面及其按钮/字段后代，分层复制避免 MySQL 临时表 reopen 限制。
  DROP TEMPORARY TABLE IF EXISTS tmp_report_owned_v220;
  CREATE TEMPORARY TABLE tmp_report_owned_v220 (menu_id BIGINT PRIMARY KEY, group_key VARCHAR(32) NOT NULL)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  INSERT INTO tmp_report_owned_v220 SELECT menu_id, group_key FROM tmp_report_pages_v220;
  DROP TEMPORARY TABLE IF EXISTS tmp_report_level_v220;
  CREATE TEMPORARY TABLE tmp_report_level_v220 LIKE tmp_report_owned_v220;
  SET depth_count = 0;
  REPEAT
    DELETE FROM tmp_report_level_v220;
    INSERT INTO tmp_report_level_v220 SELECT * FROM tmp_report_owned_v220;
    INSERT IGNORE INTO tmp_report_owned_v220
      SELECT child.id, parent.group_key FROM system_menu child
      JOIN tmp_report_level_v220 parent ON parent.menu_id = child.parent_id
      WHERE child.deleted = b'0';
    SET added_count = ROW_COUNT();
    SET depth_count = depth_count + 1;
  UNTIL added_count = 0 OR depth_count >= 16 END REPEAT;
  IF added_count <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: report descendants exceed supported depth';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_report_ancestors_v220;
  CREATE TEMPORARY TABLE tmp_report_ancestors_v220 (group_key VARCHAR(32), menu_id BIGINT,
    PRIMARY KEY (group_key, menu_id)) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  INSERT INTO tmp_report_ancestors_v220 SELECT group_key, menu_id FROM tmp_report_groups_v220 WHERE menu_id IS NOT NULL;
  INSERT INTO tmp_report_ancestors_v220 SELECT group_key, report_id FROM tmp_report_groups_v220 WHERE menu_id IS NOT NULL;
  INSERT INTO tmp_report_ancestors_v220 SELECT group_key, erp_id FROM tmp_report_groups_v220 WHERE menu_id IS NOT NULL;

  DROP TEMPORARY TABLE IF EXISTS tmp_report_package_v220;
  CREATE TEMPORARY TABLE tmp_report_package_v220 (package_id BIGINT PRIMARY KEY, missing_ids TEXT NOT NULL)
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  -- 无效 JSON 不静默跳过，避免迁移菜单后套餐丢失祖先；回滚后核对原始数据。
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND NOT JSON_VALID(menu_ids)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: invalid tenant package menu JSON';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND JSON_TYPE(menu_ids) <> 'ARRAY') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: tenant package menu_ids must be an array';
  END IF;
  INSERT INTO tmp_report_package_v220
  SELECT package_id, JSON_ARRAYAGG(menu_id) FROM (
    SELECT DISTINCT p.id AS package_id, a.menu_id
    FROM system_tenant_package p JOIN tmp_report_owned_v220 owned
      ON JSON_CONTAINS(p.menu_ids, CAST(owned.menu_id AS CHAR), '$')
    JOIN tmp_report_ancestors_v220 a ON a.group_key = owned.group_key
    WHERE p.deleted = b'0' AND NOT JSON_CONTAINS(p.menu_ids, CAST(a.menu_id AS CHAR), '$')
  ) missing GROUP BY package_id;
  SELECT CHARACTER_MAXIMUM_LENGTH INTO package_limit FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'system_tenant_package' AND column_name = 'menu_ids';
  IF EXISTS (SELECT 1 FROM system_tenant_package p JOIN tmp_report_package_v220 x ON x.package_id = p.id
      WHERE CHAR_LENGTH(JSON_MERGE_PRESERVE(p.menu_ids, x.missing_ids)) > package_limit) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: tenant package menu_ids capacity exceeded';
  END IF;
  UPDATE system_tenant_package p JOIN tmp_report_package_v220 x ON x.package_id = p.id
    SET p.menu_ids = JSON_MERGE_PRESERVE(p.menu_ids, x.missing_ids), p.update_time = NOW()
    WHERE p.deleted = b'0' AND JSON_VALID(p.menu_ids);

  INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
  SELECT DISTINCT rm.role_id, a.menu_id, '1', NOW(), '1', NOW(), b'0', rm.tenant_id
  FROM system_role_menu rm JOIN tmp_report_owned_v220 owned ON owned.menu_id = rm.menu_id
  JOIN tmp_report_ancestors_v220 a ON a.group_key = owned.group_key
  JOIN system_role r ON r.id = rm.role_id AND r.tenant_id = rm.tenant_id AND r.deleted = b'0'
  WHERE rm.deleted = b'0' AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
    WHERE existing.role_id = rm.role_id AND existing.tenant_id = rm.tenant_id
      AND existing.menu_id = a.menu_id AND existing.deleted = b'0');

  UPDATE system_menu m JOIN tmp_report_pages_v220 p ON p.menu_id = m.id
  JOIN tmp_report_groups_v220 g ON g.group_key = p.group_key
  SET m.parent_id = g.menu_id, m.path = p.original_path, m.update_time = NOW()
  WHERE m.deleted = b'0' AND m.type = 2 AND g.menu_id IS NOT NULL
    AND (m.parent_id <> g.menu_id OR m.path <> p.original_path);

  -- 后续按需授权的管理按钮，无虚构页面；不进入上述角色/套餐补齐集合。
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND parent_id = report_id
      AND path = 'foundation' AND (type <> 1 OR IFNULL(permission, '') <> '')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: foundation directory conflicts';
  END IF;
  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '核算基础设置', '', 1, 99, report_id, 'foundation', 'ep:setting', '',
    'ErpBusinessReportFoundation', 0, b'0', b'0', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
    AND parent_id = report_id AND path = 'foundation');
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0' AND parent_id = report_id AND path = 'foundation') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: duplicate foundation directories';
  END IF;
  SELECT id INTO settings_id FROM system_menu WHERE deleted = b'0' AND parent_id = report_id AND path = 'foundation';
  IF (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
      AND permission = 'erp:report-stock-opening:confirm') > 1
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
      AND permission = 'erp:report-stock-opening:confirm' AND (type <> 3 OR parent_id <> settings_id)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v220: stock opening permission conflicts';
  END IF;
  INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
    component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
  SELECT '库存核算期初确认', 'erp:report-stock-opening:confirm', 3, 1, settings_id, '', '', '',
    NULL, 0, b'1', b'0', b'0', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0'
    AND permission = 'erp:report-stock-opening:confirm');

  COMMIT;
  SELECT m.id, m.name, m.parent_id, m.path, m.component, m.permission, m.status, m.visible
    FROM system_menu m JOIN tmp_report_pages_v220 p ON p.menu_id = m.id ORDER BY m.parent_id, m.sort;
  DROP TEMPORARY TABLE IF EXISTS tmp_report_package_v220;
  DROP TEMPORARY TABLE IF EXISTS tmp_report_ancestors_v220;
  DROP TEMPORARY TABLE IF EXISTS tmp_report_level_v220;
  DROP TEMPORARY TABLE IF EXISTS tmp_report_owned_v220;
  DROP TEMPORARY TABLE IF EXISTS tmp_report_pages_v220;
  DROP TEMPORARY TABLE IF EXISTS tmp_report_components_v220;
  DROP TEMPORARY TABLE IF EXISTS tmp_report_groups_v220;
  DO RELEASE_LOCK('erp_business_reports_v220');
END$$
DELIMITER ;
CALL `erp_migrate_business_reports_v220`();
DROP PROCEDURE IF EXISTS `erp_migrate_business_reports_v220`;
