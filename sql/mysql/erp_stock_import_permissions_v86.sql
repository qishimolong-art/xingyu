-- ERP stock document import permissions (v86)
-- Scope: add import button permissions for stock-in, stock-out, stock-move and stock-check.
-- Safety:
--   - No broad DELETE.
--   - No overwrite update for existing menus, roles or tenant packages.
--   - Existing query/create/update/delete/export/update-status permissions are kept.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @stock_in_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:stock-in:query' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_in_parent_menu_id := (
  SELECT `parent_id` FROM `system_menu`
  WHERE `id` = @stock_in_menu_id AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_out_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:stock-out:query' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_out_parent_menu_id := (
  SELECT `parent_id` FROM `system_menu`
  WHERE `id` = @stock_out_menu_id AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_move_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:stock-move:query' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_move_parent_menu_id := (
  SELECT `parent_id` FROM `system_menu`
  WHERE `id` = @stock_move_menu_id AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_check_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:stock-check:query' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_check_parent_menu_id := (
  SELECT `parent_id` FROM `system_menu`
  WHERE `id` = @stock_check_menu_id AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`,
  `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '其它入库单导入', 'erp:stock-in:import', 3, 7, @stock_in_parent_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_in_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-in:import' AND `deleted` = b'0'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`,
  `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '其它出库单导入', 'erp:stock-out:import', 3, 7, @stock_out_parent_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_out_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-out:import' AND `deleted` = b'0'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`,
  `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '库存调拨单导入', 'erp:stock-move:import', 3, 7, @stock_move_parent_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_move_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-move:import' AND `deleted` = b'0'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`,
  `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '库存盘点单导入', 'erp:stock-check:import', 3, 7, @stock_check_parent_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_check_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-check:import' AND `deleted` = b'0'
  );

SET @stock_in_import_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:stock-in:import' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_out_import_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:stock-out:import' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_move_import_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:stock-move:import' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);
SET @stock_check_import_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission` = 'erp:stock-check:import' AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT role_menu.`role_id`, target_menu.`import_menu_id`, '1', NOW(), '1', NOW(), b'0', role_menu.`tenant_id`
FROM `system_role_menu` role_menu
JOIN `system_menu` owned_menu
  ON owned_menu.`id` = role_menu.`menu_id`
 AND owned_menu.`deleted` = b'0'
JOIN (
  SELECT @stock_in_import_menu_id AS import_menu_id, 'erp:stock-in:query' AS owned_permission
  UNION ALL SELECT @stock_in_import_menu_id, 'erp:stock-in:create'
  UNION ALL SELECT @stock_out_import_menu_id, 'erp:stock-out:query'
  UNION ALL SELECT @stock_out_import_menu_id, 'erp:stock-out:create'
  UNION ALL SELECT @stock_move_import_menu_id, 'erp:stock-move:query'
  UNION ALL SELECT @stock_move_import_menu_id, 'erp:stock-move:create'
  UNION ALL SELECT @stock_check_import_menu_id, 'erp:stock-check:query'
  UNION ALL SELECT @stock_check_import_menu_id, 'erp:stock-check:create'
) target_menu
  ON target_menu.`owned_permission` = owned_menu.`permission`
WHERE role_menu.`deleted` = b'0'
  AND target_menu.`import_menu_id` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = role_menu.`role_id`
      AND existing_role_menu.`menu_id` = target_menu.`import_menu_id`
      AND existing_role_menu.`tenant_id` = role_menu.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );

UPDATE `system_tenant_package` tenant_package
JOIN (
  SELECT @stock_in_import_menu_id AS import_menu_id, @stock_in_menu_id AS owned_menu_id
  UNION ALL SELECT @stock_out_import_menu_id, @stock_out_menu_id
  UNION ALL SELECT @stock_move_import_menu_id, @stock_move_menu_id
  UNION ALL SELECT @stock_check_import_menu_id, @stock_check_menu_id
) target_menu
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', target_menu.`import_menu_id`)
WHERE target_menu.`import_menu_id` IS NOT NULL
  AND target_menu.`owned_menu_id` IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target_menu.`owned_menu_id` AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target_menu.`import_menu_id` AS CHAR), '$');

SELECT `id`, `name`, `permission`, `parent_id`, `deleted`
FROM `system_menu`
WHERE `permission` IN (
  'erp:stock-in:query', 'erp:stock-in:create', 'erp:stock-in:delete', 'erp:stock-in:export', 'erp:stock-in:update-status', 'erp:stock-in:import',
  'erp:stock-out:query', 'erp:stock-out:create', 'erp:stock-out:delete', 'erp:stock-out:export', 'erp:stock-out:update-status', 'erp:stock-out:import',
  'erp:stock-move:query', 'erp:stock-move:create', 'erp:stock-move:delete', 'erp:stock-move:export', 'erp:stock-move:update-status', 'erp:stock-move:import',
  'erp:stock-check:query', 'erp:stock-check:create', 'erp:stock-check:delete', 'erp:stock-check:export', 'erp:stock-check:update-status', 'erp:stock-check:import'
)
ORDER BY `permission`, `type`, `sort`, `id`;
