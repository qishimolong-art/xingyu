-- ERP 采购/销售/库存报表菜单权限 v202
--
-- Safety:
--   - No DELETE/TRUNCATE and no overwrite-style package permission reset.
--   - Only creates or repairs target menus/buttons.
--   - Tenant packages and role permissions are appended only when the owner already has
--     the corresponding business parent menu, or the role is active super_admin.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @purchase_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (`id` = 2602 OR (`name` = '采购管理' AND `path` = 'purchase'))
  ORDER BY (`id` = 2602) DESC, `id` DESC
  LIMIT 1
);

SET @sale_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (`id` = 2617 OR (`name` = '销售管理' AND `path` = 'sale'))
  ORDER BY (`id` = 2617) DESC, `id` DESC
  LIMIT 1
);

SET @stock_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (`id` = 2583 OR (`name` = '库存管理' AND `path` = 'stock'))
  ORDER BY (`id` = 2583) DESC, `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '采购报表', '', 2, 99, @purchase_parent_id, 'report', 'ep:data-line',
       'erp/purchase/report/index', 'ErpPurchaseReport',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @purchase_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `component` = 'erp/purchase/report/index' AND `deleted` = b'0'
  );

SET @purchase_report_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/purchase/report/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

UPDATE `system_menu`
SET `name` = '采购报表',
    `parent_id` = @purchase_parent_id,
    `sort` = 99,
    `path` = 'report',
    `icon` = 'ep:data-line',
    `component_name` = 'ErpPurchaseReport',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @purchase_parent_id IS NOT NULL
  AND `id` = @purchase_report_menu_id
  AND `deleted` = b'0';

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, source.`permission`, 3, source.`sort`, @purchase_report_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '采购报表查询' AS `name`, 'erp:purchase-report:query' AS `permission`, 1 AS `sort`
  UNION ALL SELECT '采购报表导出', 'erp:purchase-report:export', 2
) source
WHERE @purchase_report_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu` existing
    WHERE existing.`permission` = source.`permission` AND existing.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `parent_id` = @purchase_report_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @purchase_report_menu_id IS NOT NULL
  AND `permission` IN ('erp:purchase-report:query', 'erp:purchase-report:export')
  AND `deleted` = b'0';

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '销售报表', '', 2, 99, @sale_parent_id, 'report', 'ep:data-line',
       'erp/sale/report/index', 'ErpSaleReport',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @sale_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `component` = 'erp/sale/report/index' AND `deleted` = b'0'
  );

SET @sale_report_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/sale/report/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

UPDATE `system_menu`
SET `name` = '销售报表',
    `parent_id` = @sale_parent_id,
    `sort` = 99,
    `path` = 'report',
    `icon` = 'ep:data-line',
    `component_name` = 'ErpSaleReport',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @sale_parent_id IS NOT NULL
  AND `id` = @sale_report_menu_id
  AND `deleted` = b'0';

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, source.`permission`, 3, source.`sort`, @sale_report_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '销售报表查询' AS `name`, 'erp:sale-report:query' AS `permission`, 1 AS `sort`
  UNION ALL SELECT '销售报表导出', 'erp:sale-report:export', 2
) source
WHERE @sale_report_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu` existing
    WHERE existing.`permission` = source.`permission` AND existing.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `parent_id` = @sale_report_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @sale_report_menu_id IS NOT NULL
  AND `permission` IN ('erp:sale-report:query', 'erp:sale-report:export')
  AND `deleted` = b'0';

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '库存报表', '', 2, 99, @stock_parent_id, 'report', 'ep:data-line',
       'erp/stock/report/index', 'ErpStockReport',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `component` = 'erp/stock/report/index' AND `deleted` = b'0'
  );

SET @stock_report_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/stock/report/index' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

UPDATE `system_menu`
SET `name` = '库存报表',
    `parent_id` = @stock_parent_id,
    `sort` = 99,
    `path` = 'report',
    `icon` = 'ep:data-line',
    `component_name` = 'ErpStockReport',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @stock_parent_id IS NOT NULL
  AND `id` = @stock_report_menu_id
  AND `deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_reports_targets_v202;
CREATE TEMPORARY TABLE tmp_erp_reports_targets_v202 (
  group_key varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  menu_id bigint NOT NULL,
  PRIMARY KEY (menu_id)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO tmp_erp_reports_targets_v202 (group_key, menu_id)
SELECT 'purchase', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (m.`id` = @purchase_report_menu_id
    OR (m.`parent_id` = @purchase_report_menu_id
      AND m.`permission` IN ('erp:purchase-report:query', 'erp:purchase-report:export')));

INSERT IGNORE INTO tmp_erp_reports_targets_v202 (group_key, menu_id)
SELECT 'sale', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (m.`id` = @sale_report_menu_id
    OR (m.`parent_id` = @sale_report_menu_id
      AND m.`permission` IN ('erp:sale-report:query', 'erp:sale-report:export')));

INSERT IGNORE INTO tmp_erp_reports_targets_v202 (group_key, menu_id)
SELECT 'stock', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND m.`id` = @stock_report_menu_id;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_reports_package_append_v202;
CREATE TEMPORARY TABLE tmp_erp_reports_package_append_v202 (
  package_id bigint NOT NULL PRIMARY KEY,
  missing_menu_ids varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_reports_package_append_v202 (package_id, missing_menu_ids)
SELECT package_menu.package_id, JSON_ARRAYAGG(package_menu.menu_id)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.menu_id
  FROM `system_tenant_package` tenant_package
  JOIN tmp_erp_reports_targets_v202 target
    ON target.group_key IN ('purchase', 'sale', 'stock')
  WHERE tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
    AND (
      (target.group_key = 'purchase' AND @purchase_parent_id IS NOT NULL
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@purchase_parent_id AS CHAR), '$'))
      OR (target.group_key = 'sale' AND @sale_parent_id IS NOT NULL
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@sale_parent_id AS CHAR), '$'))
      OR (target.group_key = 'stock' AND @stock_parent_id IS NOT NULL
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_parent_id AS CHAR), '$'))
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.menu_id AS CHAR), '$')
) package_menu
GROUP BY package_menu.package_id;

UPDATE `system_tenant_package` tenant_package
JOIN tmp_erp_reports_package_append_v202 package_append
  ON package_append.package_id = tenant_package.`id`
SET tenant_package.`menu_ids` = JSON_MERGE_PRESERVE(tenant_package.`menu_ids`, package_append.missing_menu_ids)
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND JSON_VALID(package_append.missing_menu_ids)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) + CHAR_LENGTH(package_append.missing_menu_ids) < 4000;

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT source_role.`role_id`, target.`menu_id`, '1', NOW(), '1', NOW(), b'0', source_role.`tenant_id`
FROM (
  SELECT role_menu.`role_id`, role_menu.`tenant_id`, 'purchase' AS group_key
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu ON owned_menu.`id` = role_menu.`menu_id` AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (owned_menu.`id` = @purchase_parent_id OR owned_menu.`parent_id` = @purchase_parent_id)
  UNION
  SELECT role_menu.`role_id`, role_menu.`tenant_id`, 'sale' AS group_key
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu ON owned_menu.`id` = role_menu.`menu_id` AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (owned_menu.`id` = @sale_parent_id OR owned_menu.`parent_id` = @sale_parent_id)
  UNION
  SELECT role_menu.`role_id`, role_menu.`tenant_id`, 'stock' AS group_key
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu ON owned_menu.`id` = role_menu.`menu_id` AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (owned_menu.`id` = @stock_parent_id OR owned_menu.`parent_id` = @stock_parent_id)
  UNION
  SELECT role.`id`, role.`tenant_id`, 'purchase' AS group_key
  FROM `system_role` role
  WHERE role.`code` = 'super_admin' AND role.`deleted` = b'0' AND role.`status` = 0
  UNION
  SELECT role.`id`, role.`tenant_id`, 'sale' AS group_key
  FROM `system_role` role
  WHERE role.`code` = 'super_admin' AND role.`deleted` = b'0' AND role.`status` = 0
  UNION
  SELECT role.`id`, role.`tenant_id`, 'stock' AS group_key
  FROM `system_role` role
  WHERE role.`code` = 'super_admin' AND role.`deleted` = b'0' AND role.`status` = 0
) source_role
JOIN tmp_erp_reports_targets_v202 target ON target.group_key = source_role.group_key
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` existing
  WHERE existing.`role_id` = source_role.`role_id`
    AND existing.`menu_id` = target.`menu_id`
    AND existing.`tenant_id` = source_role.`tenant_id`
    AND existing.`deleted` = b'0'
);

SELECT m.`id`, m.`name`, m.`permission`, m.`type`, m.`parent_id`, m.`component`, m.`status`, m.`visible`, m.`deleted`
FROM `system_menu` m
WHERE m.`id` IN (@purchase_report_menu_id, @sale_report_menu_id, @stock_report_menu_id)
   OR m.`permission` IN (
    'erp:purchase-report:query',
    'erp:purchase-report:export',
    'erp:sale-report:query',
    'erp:sale-report:export'
  )
ORDER BY m.`parent_id`, m.`type`, m.`sort`, m.`id`;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_reports_targets_v202;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_reports_package_append_v202;
