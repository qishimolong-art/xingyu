-- ERP 资金管理：应收报表 / 应付报表菜单权限 v175
--
-- Scope:
--   1. Add two finance report menus:
--      - 应收报表: erp/finance/receivable/report/index
--      - 应付报表: erp/finance/payable/report/index
--   2. Add query/export button permissions.
--   3. Append missing menu ids to tenant packages without replacing menu_ids.
--   4. Grant target menus/buttons to active super_admin roles and roles that
--      already own the corresponding receivable/payable finance parent menu.
--
-- Safety:
--   - No DELETE and no persistent DROP/TRUNCATE.
--   - DROP is only used for temporary-table cleanup in the current session.
--   - No blanket grant to all roles.
--   - No broad overwrite of system_tenant_package.menu_ids.
--   - Updates only repair the target menus/buttons.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @receivable_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `id` = 3100
      OR `component_name` = 'ErpReceivable'
      OR (`path` = 'receivable' AND `parent_id` = 2645)
    )
  ORDER BY (`id` = 3100) DESC, `id` DESC
  LIMIT 1
);

SET @payable_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `id` = 31100
      OR `component_name` = 'ErpPayable'
      OR (`path` = 'payable' AND `parent_id` = 2645)
    )
  ORDER BY (`id` = 31100) DESC, `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '应收报表', '', 2, 4, @receivable_parent_id, 'report', 'ep:data-line',
       'erp/finance/receivable/report/index', 'ErpReceivableReport',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @receivable_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `component` = 'erp/finance/receivable/report/index'
      AND `deleted` = b'0'
  );

SET @receivable_report_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/finance/receivable/report/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '应收报表',
    `parent_id` = @receivable_parent_id,
    `sort` = 4,
    `path` = 'report',
    `icon` = 'ep:data-line',
    `component_name` = 'ErpReceivableReport',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @receivable_parent_id IS NOT NULL
  AND `id` = @receivable_report_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '应收报表'
    OR `parent_id` <> @receivable_parent_id
    OR `sort` <> 4
    OR `path` <> 'report'
    OR IFNULL(`icon`, '') <> 'ep:data-line'
    OR IFNULL(`component_name`, '') <> 'ErpReceivableReport'
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, source.`permission`, 3, source.`sort`, @receivable_report_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '应收报表查询' AS `name`, 'erp:receivable-report:query' AS `permission`, 1 AS `sort`
  UNION ALL SELECT '应收报表导出', 'erp:receivable-report:export', 2
) source
WHERE @receivable_report_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing
    WHERE existing.`permission` = source.`permission`
      AND existing.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `parent_id` = @receivable_report_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @receivable_report_menu_id IS NOT NULL
  AND `permission` IN ('erp:receivable-report:query', 'erp:receivable-report:export')
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @receivable_report_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '应付报表', '', 2, 4, @payable_parent_id, 'report', 'ep:data-line',
       'erp/finance/payable/report/index', 'ErpPayableReport',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @payable_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `component` = 'erp/finance/payable/report/index'
      AND `deleted` = b'0'
  );

SET @payable_report_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/finance/payable/report/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '应付报表',
    `parent_id` = @payable_parent_id,
    `sort` = 4,
    `path` = 'report',
    `icon` = 'ep:data-line',
    `component_name` = 'ErpPayableReport',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @payable_parent_id IS NOT NULL
  AND `id` = @payable_report_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '应付报表'
    OR `parent_id` <> @payable_parent_id
    OR `sort` <> 4
    OR `path` <> 'report'
    OR IFNULL(`icon`, '') <> 'ep:data-line'
    OR IFNULL(`component_name`, '') <> 'ErpPayableReport'
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, source.`permission`, 3, source.`sort`, @payable_report_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '应付报表查询' AS `name`, 'erp:payable-report:query' AS `permission`, 1 AS `sort`
  UNION ALL SELECT '应付报表导出', 'erp:payable-report:export', 2
) source
WHERE @payable_report_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing
    WHERE existing.`permission` = source.`permission`
      AND existing.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `parent_id` = @payable_report_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @payable_report_menu_id IS NOT NULL
  AND `permission` IN ('erp:payable-report:query', 'erp:payable-report:export')
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @payable_report_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_finance_other_reports_targets_v175;
CREATE TEMPORARY TABLE tmp_finance_other_reports_targets_v175 (
  group_key varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  menu_id bigint NOT NULL,
  PRIMARY KEY (menu_id)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO tmp_finance_other_reports_targets_v175 (group_key, menu_id)
SELECT 'receivable', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (
    m.`id` = @receivable_report_menu_id
    OR (m.`parent_id` = @receivable_report_menu_id AND m.`permission` IN (
      'erp:receivable-report:query',
      'erp:receivable-report:export'
    ))
  );

INSERT IGNORE INTO tmp_finance_other_reports_targets_v175 (group_key, menu_id)
SELECT 'payable', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (
    m.`id` = @payable_report_menu_id
    OR (m.`parent_id` = @payable_report_menu_id AND m.`permission` IN (
      'erp:payable-report:query',
      'erp:payable-report:export'
    ))
  );

DROP TEMPORARY TABLE IF EXISTS tmp_finance_other_reports_package_append_v175;
CREATE TEMPORARY TABLE tmp_finance_other_reports_package_append_v175 (
  package_id bigint NOT NULL PRIMARY KEY,
  missing_menu_ids varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_finance_other_reports_package_append_v175 (package_id, missing_menu_ids)
SELECT package_menu.package_id, JSON_ARRAYAGG(package_menu.menu_id)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.menu_id
  FROM `system_tenant_package` tenant_package
  JOIN tmp_finance_other_reports_targets_v175 target
    ON target.group_key = 'receivable'
  WHERE @receivable_parent_id IS NOT NULL
    AND tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@receivable_parent_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` owned_child
        WHERE owned_child.`deleted` = b'0'
          AND owned_child.`parent_id` = @receivable_parent_id
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(owned_child.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.menu_id AS CHAR), '$')
) package_menu
GROUP BY package_menu.package_id
ON DUPLICATE KEY UPDATE
  missing_menu_ids = JSON_MERGE_PRESERVE(missing_menu_ids, VALUES(missing_menu_ids));

INSERT INTO tmp_finance_other_reports_package_append_v175 (package_id, missing_menu_ids)
SELECT package_menu.package_id, JSON_ARRAYAGG(package_menu.menu_id)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.menu_id
  FROM `system_tenant_package` tenant_package
  JOIN tmp_finance_other_reports_targets_v175 target
    ON target.group_key = 'payable'
  WHERE @payable_parent_id IS NOT NULL
    AND tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@payable_parent_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` owned_child
        WHERE owned_child.`deleted` = b'0'
          AND owned_child.`parent_id` = @payable_parent_id
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(owned_child.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.menu_id AS CHAR), '$')
) package_menu
GROUP BY package_menu.package_id
ON DUPLICATE KEY UPDATE
  missing_menu_ids = JSON_MERGE_PRESERVE(missing_menu_ids, VALUES(missing_menu_ids));

UPDATE `system_tenant_package` tenant_package
JOIN tmp_finance_other_reports_package_append_v175 package_append
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
  SELECT role_menu.`role_id`, role_menu.`tenant_id`, 'receivable' AS group_key
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`id` = @receivable_parent_id
      OR owned_menu.`parent_id` = @receivable_parent_id
    )
  UNION
  SELECT role_menu.`role_id`, role_menu.`tenant_id`, 'payable' AS group_key
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`id` = @payable_parent_id
      OR owned_menu.`parent_id` = @payable_parent_id
    )
  UNION
  SELECT role.`id`, role.`tenant_id`, 'receivable' AS group_key
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
  UNION
  SELECT role.`id`, role.`tenant_id`, 'payable' AS group_key
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
) source_role
JOIN tmp_finance_other_reports_targets_v175 target
  ON target.group_key = source_role.group_key
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
WHERE m.`id` IN (@receivable_report_menu_id, @payable_report_menu_id)
   OR m.`permission` IN (
    'erp:receivable-report:query',
    'erp:receivable-report:export',
    'erp:payable-report:query',
    'erp:payable-report:export'
  )
ORDER BY m.`parent_id`, m.`type`, m.`sort`, m.`id`;

DROP TEMPORARY TABLE IF EXISTS tmp_finance_other_reports_targets_v175;
DROP TEMPORARY TABLE IF EXISTS tmp_finance_other_reports_package_append_v175;
