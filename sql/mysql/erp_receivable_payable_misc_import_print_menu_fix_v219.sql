-- 其他应收 / 其他应付：补充导入与打印模板按钮权限
-- 适用场景：
--   已执行 v216-v218 后，列表页需要显示“导入”“打印模板”按钮。
-- 安全性：
--   - 仅追加按钮菜单和角色菜单关系。
--   - 不删除业务数据，不覆盖已有角色权限。

ALTER TABLE `system_tenant_package`
  MODIFY COLUMN `menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联的菜单编号';

SET @receivable_misc_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `component_name` = 'ErpReceivableMisc'
  ORDER BY `id`
  LIMIT 1
);

SET @payable_misc_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `component_name` = 'ErpPayableMisc'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT child_name, permission, 3, sort_no, parent_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '其他应收导入' AS child_name, 'erp:receivable-misc:import' AS permission, 7 AS sort_no, @receivable_misc_menu_id AS parent_id
  UNION ALL SELECT '其他应收打印模板', 'erp:receivable-misc:print-template', 8, @receivable_misc_menu_id
  UNION ALL SELECT '其他应付导入', 'erp:payable-misc:import', 7, @payable_misc_menu_id
  UNION ALL SELECT '其他应付打印模板', 'erp:payable-misc:print-template', 8, @payable_misc_menu_id
) source
WHERE source.parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing_menu
    WHERE existing_menu.`deleted` = b'0'
      AND existing_menu.`permission` = source.permission
  );

UPDATE `system_menu`
SET `parent_id` = CASE
    WHEN `permission` LIKE 'erp:receivable-misc:%' THEN @receivable_misc_menu_id
    WHEN `permission` LIKE 'erp:payable-misc:%' THEN @payable_misc_menu_id
    ELSE `parent_id`
  END,
  `type` = 3,
  `status` = 0,
  `visible` = b'1',
  `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`permission` IN ('erp:receivable-misc:import', 'erp:receivable-misc:print-template')
    OR `permission` IN ('erp:payable-misc:import', 'erp:payable-misc:print-template'))
  AND ((`permission` LIKE 'erp:receivable-misc:%' AND @receivable_misc_menu_id IS NOT NULL)
    OR (`permission` LIKE 'erp:payable-misc:%' AND @payable_misc_menu_id IS NOT NULL));

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_import_print_targets_v219`;
CREATE TEMPORARY TABLE `tmp_erp_misc_import_print_targets_v219` (`menu_id` bigint NOT NULL PRIMARY KEY);

INSERT IGNORE INTO `tmp_erp_misc_import_print_targets_v219` (`menu_id`)
SELECT `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `permission` IN (
    'erp:receivable-misc:import',
    'erp:receivable-misc:print-template',
    'erp:payable-misc:import',
    'erp:payable-misc:print-template'
  );

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_import_print_package_append_v219`;
CREATE TEMPORARY TABLE `tmp_erp_misc_import_print_package_append_v219` (
  `package_id` bigint NOT NULL PRIMARY KEY,
  `missing_menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO `tmp_erp_misc_import_print_package_append_v219` (`package_id`, `missing_menu_ids`)
SELECT package_menu.`package_id`, JSON_ARRAYAGG(package_menu.`menu_id`)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.`menu_id`
  FROM `system_tenant_package` tenant_package
  JOIN `tmp_erp_misc_import_print_targets_v219` target ON TRUE
  JOIN `system_menu` target_menu ON target_menu.`id` = target.`menu_id` AND target_menu.`deleted` = b'0'
  WHERE tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@receivable_misc_menu_id AS CHAR), '$')
      OR JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@payable_misc_menu_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` owned_button
        WHERE owned_button.`deleted` = b'0'
          AND owned_button.`parent_id` IN (@receivable_misc_menu_id, @payable_misc_menu_id)
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(owned_button.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.`menu_id` AS CHAR), '$')
) package_menu
GROUP BY package_menu.`package_id`;

UPDATE `system_tenant_package` tenant_package
JOIN `tmp_erp_misc_import_print_package_append_v219` package_append
  ON package_append.`package_id` = tenant_package.`id`
SET tenant_package.`menu_ids` = JSON_MERGE_PRESERVE(tenant_package.`menu_ids`, package_append.`missing_menu_ids`),
    tenant_package.`update_time` = NOW()
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND JSON_VALID(package_append.`missing_menu_ids`);

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_import_print_package_append_v219`;

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT role_info.`id`, target.`menu_id`, '1', NOW(), '1', NOW(), b'0', role_info.`tenant_id`
FROM `system_role` role_info
CROSS JOIN `tmp_erp_misc_import_print_targets_v219` target
WHERE role_info.`deleted` = b'0'
  AND role_info.`code` = 'super_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`deleted` = b'0'
      AND existing_role_menu.`role_id` = role_info.`id`
      AND existing_role_menu.`menu_id` = target.`menu_id`
      AND existing_role_menu.`tenant_id` = role_info.`tenant_id`
  );

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT role_menu.`role_id`, target.`menu_id`, '1', NOW(), '1', NOW(), b'0', role_menu.`tenant_id`
FROM `tmp_erp_misc_import_print_targets_v219` target
JOIN `system_menu` target_menu ON target_menu.`id` = target.`menu_id` AND target_menu.`deleted` = b'0'
JOIN `system_role_menu` role_menu ON role_menu.`deleted` = b'0'
  AND (
    role_menu.`menu_id` = target_menu.`parent_id`
    OR role_menu.`menu_id` IN (
      SELECT sibling_button.`id`
      FROM `system_menu` sibling_button
      WHERE sibling_button.`deleted` = b'0'
        AND sibling_button.`parent_id` = target_menu.`parent_id`
        AND sibling_button.`permission` IN (
          'erp:receivable-misc:query',
          'erp:receivable-misc:create',
          'erp:receivable-misc:update',
          'erp:receivable-misc:delete',
          'erp:receivable-misc:update-status',
          'erp:receivable-misc:export',
          'erp:payable-misc:query',
          'erp:payable-misc:create',
          'erp:payable-misc:update',
          'erp:payable-misc:delete',
          'erp:payable-misc:update-status',
          'erp:payable-misc:export'
        )
    )
  )
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` existing_role_menu
  WHERE existing_role_menu.`deleted` = b'0'
    AND existing_role_menu.`role_id` = role_menu.`role_id`
    AND existing_role_menu.`menu_id` = target.`menu_id`
    AND existing_role_menu.`tenant_id` = role_menu.`tenant_id`
);

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_import_print_targets_v219`;
