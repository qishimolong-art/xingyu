-- Fix role permission configuration for forms that can be listed but not checked (v74).
--
-- Scope:
--   1. Ensure these form menus and button permissions exist:
--      - 价格体系
--      - 配件价格调整
--      - 其他应收单
--      - 预收款单
--   2. Append missing ids to tenant packages without replacing menu_ids.
--   3. Grant target menus/buttons to active super_admin roles and roles that already
--      own the corresponding parent business menu.
--
-- Safety:
--   - No DELETE and no persistent DROP/TRUNCATE.
--   - DROP is only used for temporary-table cleanup in the current session.
--   - No blanket grant to all roles.
--   - No broad overwrite of system_tenant_package.menu_ids.
--   - Button parent_id is repaired only for the target permissions.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_root_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = 'ERP 系统'
    AND `type` = 1
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @base_parent_id := (
  SELECT menu.`id`
  FROM `system_menu` menu
  WHERE menu.`name` = '基础数据'
    AND menu.`type` = 1
    AND menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR menu.`parent_id` = @erp_root_id)
  ORDER BY menu.`id` DESC
  LIMIT 1
);

SET @purchase_parent_id := (
  SELECT menu.`id`
  FROM `system_menu` menu
  WHERE menu.`name` = '采购管理'
    AND menu.`type` = 1
    AND menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR menu.`parent_id` = @erp_root_id)
  ORDER BY menu.`id` DESC
  LIMIT 1
);

SET @system_config_parent_id := (
  SELECT menu.`id`
  FROM `system_menu` menu
  WHERE menu.`name` = '系统配置'
    AND menu.`type` = 1
    AND menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR menu.`parent_id` = @erp_root_id)
  ORDER BY menu.`id` DESC
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '系统配置', '', 1, 90, @erp_root_id, 'system', 'ep:setting',
       '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_root_id IS NOT NULL
  AND @system_config_parent_id IS NULL;

SET @system_config_parent_id := (
  SELECT menu.`id`
  FROM `system_menu` menu
  WHERE menu.`name` = '系统配置'
    AND menu.`type` = 1
    AND menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR menu.`parent_id` = @erp_root_id)
  ORDER BY menu.`id` DESC
  LIMIT 1
);

SET @accounting_parent_id := (
  SELECT menu.`id`
  FROM `system_menu` menu
  WHERE menu.`name` = '财务核算'
    AND menu.`type` = 1
    AND menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR menu.`parent_id` = @erp_root_id)
  ORDER BY menu.`id` DESC
  LIMIT 1
);

-- 1. 价格体系.
INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '价格体系', '', 2, 60, @base_parent_id, 'price-system', 'fa:tags',
       'erp/product/pricesystem/index', 'ErpPriceSystem',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @base_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `component` = 'erp/product/pricesystem/index'
      AND `deleted` = b'0'
  );

SET @price_system_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/product/pricesystem/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '价格体系',
    `parent_id` = @base_parent_id,
    `path` = 'price-system',
    `component_name` = 'ErpPriceSystem',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @base_parent_id IS NOT NULL
  AND `id` = @price_system_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '价格体系'
    OR `parent_id` <> @base_parent_id
    OR `path` <> 'price-system'
    OR IFNULL(`component_name`, '') <> 'ErpPriceSystem'
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, source.`permission`, 3, source.`sort`, @price_system_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '价格体系查询' AS `name`, 'erp:price-system:query' AS `permission`, 1 AS `sort`
  UNION ALL SELECT '价格体系创建', 'erp:price-system:create', 2
  UNION ALL SELECT '价格体系更新', 'erp:price-system:update', 3
  UNION ALL SELECT '价格体系删除', 'erp:price-system:delete', 4
) source
WHERE @price_system_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing
    WHERE existing.`permission` = source.`permission`
      AND existing.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `parent_id` = @price_system_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @price_system_menu_id IS NOT NULL
  AND `permission` IN (
    'erp:price-system:query',
    'erp:price-system:create',
    'erp:price-system:update',
    'erp:price-system:delete'
  )
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @price_system_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

-- 2. 配件价格调整.
INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '配件价格调整', '', 2, 20, @system_config_parent_id, 'price-adjust', 'ep:price-tag',
       'erp/purchase/price-adjust/index', 'ErpPurchasePriceAdjust',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @system_config_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `component` = 'erp/purchase/price-adjust/index'
      AND `deleted` = b'0'
  );

SET @parts_price_adjust_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/purchase/price-adjust/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '配件价格调整',
    `parent_id` = @system_config_parent_id,
    `path` = 'price-adjust',
    `component_name` = 'ErpPurchasePriceAdjust',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @system_config_parent_id IS NOT NULL
  AND `id` = @parts_price_adjust_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '配件价格调整'
    OR `parent_id` <> @system_config_parent_id
    OR `path` <> 'price-adjust'
    OR IFNULL(`component_name`, '') <> 'ErpPurchasePriceAdjust'
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, source.`permission`, 3, source.`sort`, @parts_price_adjust_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '配件价格查询' AS `name`, 'erp:product:query' AS `permission`, 1 AS `sort`
  UNION ALL SELECT '配件价格保存', 'erp:product:update', 2
  UNION ALL SELECT '配件价格批量调整', 'erp:parts:adjust-price', 3
) source
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing
    WHERE existing.`permission` = source.`permission`
      AND existing.`parent_id` = @parts_price_adjust_menu_id
      AND existing.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND (
    (`parent_id` = @parts_price_adjust_menu_id AND `permission` IN ('erp:product:query', 'erp:product:update'))
    OR `permission` = 'erp:parts:adjust-price'
  )
  AND `deleted` = b'0'
  AND (
    `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

UPDATE `system_menu`
SET `parent_id` = @parts_price_adjust_menu_id,
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND `permission` = 'erp:parts:adjust-price'
  AND `deleted` = b'0'
  AND `parent_id` <> @parts_price_adjust_menu_id;

-- 3. 财务核算 / 其他应收单、预收款单.
INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, '', 2, source.`sort`, @accounting_parent_id, source.`path`, source.`icon`,
       source.`component`, source.`component_name`,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '其他应收单' AS `name`, 10 AS `sort`, 'other-receivable' AS `path`, 'ep:money' AS `icon`,
         'erp/accounting/other-receivable/index' AS `component`, 'ErpOtherReceivable' AS `component_name`
  UNION ALL
  SELECT '预收款单', 11, 'pre-receipt', 'ep:wallet',
         'erp/accounting/pre-receipt/index', 'ErpPreReceipt'
) source
WHERE @accounting_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing
    WHERE existing.`component` = source.`component`
      AND existing.`deleted` = b'0'
  );

SET @other_receivable_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/accounting/other-receivable/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @pre_receipt_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/accounting/pre-receipt/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '其他应收单',
    `parent_id` = @accounting_parent_id,
    `path` = 'other-receivable',
    `component_name` = 'ErpOtherReceivable',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @accounting_parent_id IS NOT NULL
  AND `id` = @other_receivable_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '其他应收单'
    OR `parent_id` <> @accounting_parent_id
    OR `path` <> 'other-receivable'
    OR IFNULL(`component_name`, '') <> 'ErpOtherReceivable'
    OR `status` <> 0
    OR `visible` <> b'1'
  );

UPDATE `system_menu`
SET `name` = '预收款单',
    `parent_id` = @accounting_parent_id,
    `path` = 'pre-receipt',
    `component_name` = 'ErpPreReceipt',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @accounting_parent_id IS NOT NULL
  AND `id` = @pre_receipt_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '预收款单'
    OR `parent_id` <> @accounting_parent_id
    OR `path` <> 'pre-receipt'
    OR IFNULL(`component_name`, '') <> 'ErpPreReceipt'
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, source.`permission`, 3, source.`sort`, @other_receivable_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '其他应收单查询' AS `name`, 'erp:other-receivable:query' AS `permission`, 1 AS `sort`
  UNION ALL SELECT '其他应收单创建', 'erp:other-receivable:create', 2
  UNION ALL SELECT '其他应收单修改', 'erp:other-receivable:update', 3
  UNION ALL SELECT '其他应收单删除', 'erp:other-receivable:delete', 4
  UNION ALL SELECT '其他应收单审核', 'erp:other-receivable:update-status', 5
) source
WHERE @other_receivable_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing
    WHERE existing.`permission` = source.`permission`
      AND existing.`deleted` = b'0'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT source.`name`, source.`permission`, 3, source.`sort`, @pre_receipt_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '预收款单查询' AS `name`, 'erp:pre-receipt:query' AS `permission`, 1 AS `sort`
  UNION ALL SELECT '预收款单创建', 'erp:pre-receipt:create', 2
  UNION ALL SELECT '预收款单修改', 'erp:pre-receipt:update', 3
  UNION ALL SELECT '预收款单删除', 'erp:pre-receipt:delete', 4
  UNION ALL SELECT '预收款单审核', 'erp:pre-receipt:update-status', 5
) source
WHERE @pre_receipt_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing
    WHERE existing.`permission` = source.`permission`
      AND existing.`deleted` = b'0'
  );

UPDATE `system_menu`
SET `parent_id` = @other_receivable_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @other_receivable_menu_id IS NOT NULL
  AND `permission` IN (
    'erp:other-receivable:query',
    'erp:other-receivable:create',
    'erp:other-receivable:update',
    'erp:other-receivable:delete',
    'erp:other-receivable:update-status'
  )
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @other_receivable_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

UPDATE `system_menu`
SET `parent_id` = @pre_receipt_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @pre_receipt_menu_id IS NOT NULL
  AND `permission` IN (
    'erp:pre-receipt:query',
    'erp:pre-receipt:create',
    'erp:pre-receipt:update',
    'erp:pre-receipt:delete',
    'erp:pre-receipt:update-status'
  )
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @pre_receipt_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

-- 4. Collect target ids.
DROP TEMPORARY TABLE IF EXISTS tmp_role_permission_form_fix_targets_v74;
CREATE TEMPORARY TABLE tmp_role_permission_form_fix_targets_v74 (
  form_key varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  group_key varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  menu_id bigint NOT NULL,
  PRIMARY KEY (menu_id)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO tmp_role_permission_form_fix_targets_v74 (form_key, group_key, menu_id)
SELECT 'erp_price_system', 'base', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (
    m.`id` = @price_system_menu_id
    OR (m.`parent_id` = @price_system_menu_id AND m.`permission` LIKE 'erp:price-system:%')
  );

INSERT IGNORE INTO tmp_role_permission_form_fix_targets_v74 (form_key, group_key, menu_id)
SELECT 'erp_parts_price_adjust', 'erp_system', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (
    m.`id` = @parts_price_adjust_menu_id
    OR (m.`parent_id` = @parts_price_adjust_menu_id AND m.`permission` IN ('erp:product:query', 'erp:product:update'))
    OR (m.`permission` = 'erp:parts:adjust-price')
  );

INSERT IGNORE INTO tmp_role_permission_form_fix_targets_v74 (form_key, group_key, menu_id)
SELECT 'erp_accounting_other_receivable', 'accounting', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (
    m.`id` = @other_receivable_menu_id
    OR (m.`parent_id` = @other_receivable_menu_id AND m.`permission` LIKE 'erp:other-receivable:%')
  );

INSERT IGNORE INTO tmp_role_permission_form_fix_targets_v74 (form_key, group_key, menu_id)
SELECT 'erp_accounting_pre_receipt', 'accounting', m.`id`
FROM `system_menu` m
WHERE m.`deleted` = b'0'
  AND (
    m.`id` = @pre_receipt_menu_id
    OR (m.`parent_id` = @pre_receipt_menu_id AND m.`permission` LIKE 'erp:pre-receipt:%')
  );

-- 5. Tenant package menu_ids append. Packages that already own the matching
-- parent business menu or one existing child are expanded.
DROP TEMPORARY TABLE IF EXISTS tmp_role_permission_form_fix_package_append_v74;
CREATE TEMPORARY TABLE tmp_role_permission_form_fix_package_append_v74 (
  package_id bigint NOT NULL PRIMARY KEY,
  missing_menu_ids varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_role_permission_form_fix_package_append_v74 (package_id, missing_menu_ids)
SELECT package_menu.package_id, JSON_ARRAYAGG(package_menu.menu_id)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.menu_id
  FROM `system_tenant_package` tenant_package
  JOIN tmp_role_permission_form_fix_targets_v74 target
    ON target.group_key = 'base'
  WHERE @base_parent_id IS NOT NULL
    AND tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@base_parent_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` base_child
        WHERE base_child.`parent_id` = @base_parent_id
          AND base_child.`deleted` = b'0'
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(base_child.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.menu_id AS CHAR), '$')
) package_menu
GROUP BY package_menu.package_id
ON DUPLICATE KEY UPDATE
  missing_menu_ids = JSON_MERGE_PRESERVE(missing_menu_ids, VALUES(missing_menu_ids));

INSERT INTO tmp_role_permission_form_fix_package_append_v74 (package_id, missing_menu_ids)
SELECT package_menu.package_id, JSON_ARRAYAGG(package_menu.menu_id)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.menu_id
  FROM `system_tenant_package` tenant_package
  JOIN tmp_role_permission_form_fix_targets_v74 target
    ON target.group_key = 'erp_system'
  WHERE @system_config_parent_id IS NOT NULL
    AND tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@system_config_parent_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` system_config_child
        WHERE system_config_child.`parent_id` = @system_config_parent_id
          AND system_config_child.`deleted` = b'0'
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(system_config_child.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.menu_id AS CHAR), '$')
) package_menu
GROUP BY package_menu.package_id
ON DUPLICATE KEY UPDATE
  missing_menu_ids = JSON_MERGE_PRESERVE(missing_menu_ids, VALUES(missing_menu_ids));

INSERT INTO tmp_role_permission_form_fix_package_append_v74 (package_id, missing_menu_ids)
SELECT package_menu.package_id, JSON_ARRAYAGG(package_menu.menu_id)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.menu_id
  FROM `system_tenant_package` tenant_package
  JOIN tmp_role_permission_form_fix_targets_v74 target
    ON target.group_key = 'accounting'
  WHERE @accounting_parent_id IS NOT NULL
    AND tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@accounting_parent_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` accounting_child
        WHERE accounting_child.`parent_id` = @accounting_parent_id
          AND accounting_child.`deleted` = b'0'
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(accounting_child.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.menu_id AS CHAR), '$')
) package_menu
GROUP BY package_menu.package_id
ON DUPLICATE KEY UPDATE
  missing_menu_ids = JSON_MERGE_PRESERVE(missing_menu_ids, VALUES(missing_menu_ids));

UPDATE `system_tenant_package` tenant_package
JOIN tmp_role_permission_form_fix_package_append_v74 package_append
  ON package_append.package_id = tenant_package.`id`
SET tenant_package.`menu_ids` = JSON_MERGE_PRESERVE(tenant_package.`menu_ids`, package_append.missing_menu_ids)
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND JSON_VALID(package_append.missing_menu_ids)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) + CHAR_LENGTH(package_append.missing_menu_ids) < 4000;

-- 6. Role grants. Grant to super admins and roles that already own the matching
-- parent business menu or one sibling menu.
INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT source_role.`role_id`, target.`menu_id`, '1', NOW(), '1', NOW(), b'0', source_role.`tenant_id`
FROM (
  SELECT role_menu.`role_id`, role_menu.`tenant_id`, 'base' AS group_key
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`id` = @base_parent_id
      OR owned_menu.`parent_id` = @base_parent_id
    )
  UNION
  SELECT role_menu.`role_id`, role_menu.`tenant_id`, 'erp_system' AS group_key
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`id` = @system_config_parent_id
      OR owned_menu.`parent_id` = @system_config_parent_id
    )
  UNION
  SELECT role_menu.`role_id`, role_menu.`tenant_id`, 'accounting' AS group_key
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`id` = @accounting_parent_id
      OR owned_menu.`parent_id` = @accounting_parent_id
    )
  UNION
  SELECT role.`id`, role.`tenant_id`, 'base' AS group_key
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
  UNION
  SELECT role.`id`, role.`tenant_id`, 'erp_system' AS group_key
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
  UNION
  SELECT role.`id`, role.`tenant_id`, 'accounting' AS group_key
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
) source_role
JOIN tmp_role_permission_form_fix_targets_v74 target
  ON target.group_key = source_role.group_key
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` existing
  WHERE existing.`role_id` = source_role.`role_id`
    AND existing.`menu_id` = target.`menu_id`
    AND existing.`tenant_id` = source_role.`tenant_id`
    AND existing.`deleted` = b'0'
);

-- 7. Verification result.
SELECT m.`id`, m.`name`, m.`permission`, m.`type`, m.`parent_id`, m.`component`, m.`status`, m.`visible`, m.`deleted`
FROM `system_menu` m
WHERE m.`id` IN (
    @price_system_menu_id,
    @parts_price_adjust_menu_id,
    @other_receivable_menu_id,
    @pre_receipt_menu_id
  )
   OR m.`permission` IN (
    'erp:price-system:query',
    'erp:price-system:create',
    'erp:price-system:update',
    'erp:price-system:delete',
    'erp:parts:adjust-price',
    'erp:other-receivable:query',
    'erp:other-receivable:create',
    'erp:other-receivable:update',
    'erp:other-receivable:delete',
    'erp:other-receivable:update-status',
    'erp:pre-receipt:query',
    'erp:pre-receipt:create',
    'erp:pre-receipt:update',
    'erp:pre-receipt:delete',
    'erp:pre-receipt:update-status'
  )
   OR (
    m.`parent_id` = @parts_price_adjust_menu_id
    AND m.`permission` IN ('erp:product:query', 'erp:product:update')
  )
ORDER BY m.`parent_id`, m.`type`, m.`sort`, m.`id`;

DROP TEMPORARY TABLE IF EXISTS tmp_role_permission_form_fix_targets_v74;
