-- Move recycle bin under ERP / system configuration, and parts price adjustment
-- under ERP / base data / parts basic info.
--
-- Scope:
--   1. Ensure the ERP system-configuration directory exists.
--   2. Move existing recycle-bin under it.
--   3. Move existing parts-price-adjustment menus under parts basic info.
--   4. Keep existing button permissions under their page menus.
--   5. Append the needed parents to roles and tenant packages that already own
--      either moved menu.
--
-- Safety:
--   - No DELETE.
--   - No broad overwrite of system_tenant_package.menu_ids.
--   - No blanket grant to all roles.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = 'ERP 系统'
    AND `type` = 1
    AND `parent_id` = 0
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @system_config_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '系统配置'
    AND `type` = 1
    AND `parent_id` = @erp_parent_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @base_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '基础数据'
    AND `type` = 1
    AND `parent_id` = @erp_parent_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @parts_basic_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '配件基本信息'
    AND `type` = 1
    AND `parent_id` = @base_parent_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '系统配置', '', 1, 90, @erp_parent_id, 'system', 'ep:setting',
  '', '',
  0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_parent_id IS NOT NULL
  AND @system_config_parent_id IS NULL;

SET @system_config_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '系统配置'
    AND `type` = 1
    AND `parent_id` = @erp_parent_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '基础数据', '', 1, 5, @erp_parent_id, 'base', 'ep:data-analysis',
  '', '',
  0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_parent_id IS NOT NULL
  AND @base_parent_id IS NULL;

SET @base_parent_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '基础数据'
    AND `type` = 1
    AND `parent_id` = @erp_parent_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '配件基本信息', '', 1, 30, @base_parent_id, 'parts-basic', 'fa-solid:tools',
  '', '',
  0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @base_parent_id IS NOT NULL
  AND @parts_basic_menu_id IS NULL;

SET @parts_basic_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `name` = '配件基本信息'
    AND `type` = 1
    AND `parent_id` = @base_parent_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @recycle_bin_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/base/recycle-bin/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
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
SET `parent_id` = @system_config_parent_id,
    `updater` = '1',
    `update_time` = NOW()
WHERE @system_config_parent_id IS NOT NULL
  AND `id` = @recycle_bin_menu_id
  AND `deleted` = b'0'
  AND `parent_id` <> @system_config_parent_id;

UPDATE `system_menu`
SET `parent_id` = @parts_basic_menu_id,
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_basic_menu_id IS NOT NULL
  AND `id` = @parts_price_adjust_menu_id
  AND `deleted` = b'0'
  AND `parent_id` <> @parts_basic_menu_id;

UPDATE `system_menu`
SET `sort` = 80,
    `updater` = '1',
    `update_time` = NOW()
WHERE @recycle_bin_menu_id IS NOT NULL
  AND `id` = @recycle_bin_menu_id
  AND `deleted` = b'0'
  AND `sort` <> 80;

UPDATE `system_menu`
SET `sort` = 40,
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND `id` = @parts_price_adjust_menu_id
  AND `deleted` = b'0'
  AND `sort` <> 40;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.`role_id`, @system_config_parent_id, '1', NOW(), '1', NOW(), b'0', owned_role.`tenant_id`
FROM `system_role_menu` owned_role
WHERE @system_config_parent_id IS NOT NULL
  AND owned_role.`deleted` = b'0'
  AND owned_role.`menu_id` = @recycle_bin_menu_id
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing
    WHERE existing.`role_id` = owned_role.`role_id`
      AND existing.`menu_id` = @system_config_parent_id
      AND existing.`tenant_id` = owned_role.`tenant_id`
      AND existing.`deleted` = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.`role_id`, target_menu.`id`, '1', NOW(), '1', NOW(), b'0', owned_role.`tenant_id`
FROM `system_role_menu` owned_role
JOIN `system_menu` target_menu
  ON target_menu.`id` IN (@base_parent_id, @parts_basic_menu_id)
 AND target_menu.`deleted` = b'0'
WHERE owned_role.`deleted` = b'0'
  AND owned_role.`menu_id` = @parts_price_adjust_menu_id
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing
    WHERE existing.`role_id` = owned_role.`role_id`
      AND existing.`menu_id` = target_menu.`id`
      AND existing.`tenant_id` = owned_role.`tenant_id`
      AND existing.`deleted` = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT role.`id`, @system_config_parent_id, '1', NOW(), '1', NOW(), b'0', role.`tenant_id`
FROM `system_role` role
WHERE @system_config_parent_id IS NOT NULL
  AND role.`code` = 'super_admin'
  AND role.`deleted` = b'0'
  AND role.`status` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing
    WHERE existing.`role_id` = role.`id`
      AND existing.`menu_id` = @system_config_parent_id
      AND existing.`tenant_id` = role.`tenant_id`
      AND existing.`deleted` = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT role.`id`, target_menu.`id`, '1', NOW(), '1', NOW(), b'0', role.`tenant_id`
FROM `system_role` role
JOIN `system_menu` target_menu
  ON target_menu.`id` IN (@base_parent_id, @parts_basic_menu_id)
 AND target_menu.`deleted` = b'0'
WHERE role.`code` = 'super_admin'
  AND role.`deleted` = b'0'
  AND role.`status` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing
    WHERE existing.`role_id` = role.`id`
      AND existing.`menu_id` = target_menu.`id`
      AND existing.`tenant_id` = role.`tenant_id`
      AND existing.`deleted` = b'0'
  );

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @system_config_parent_id)
WHERE @system_config_parent_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND (
    (
      @recycle_bin_menu_id IS NOT NULL
      AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@recycle_bin_menu_id AS CHAR), '$')
    )
  )
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@system_config_parent_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @base_parent_id)
WHERE @base_parent_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND @parts_price_adjust_menu_id IS NOT NULL
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_adjust_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@base_parent_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @parts_basic_menu_id)
WHERE @parts_basic_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND @parts_price_adjust_menu_id IS NOT NULL
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_adjust_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_basic_menu_id AS CHAR), '$');

SELECT child.`id`,
       child.`name`,
       parent.`name` AS `parent_name`,
       child.`permission`,
       child.`type`,
       child.`sort`,
       child.`component`,
       child.`status`,
       child.`visible`,
       child.`deleted`
FROM `system_menu` child
LEFT JOIN `system_menu` parent
  ON parent.`id` = child.`parent_id`
WHERE child.`id` IN (@recycle_bin_menu_id, @parts_price_adjust_menu_id)
   OR child.`parent_id` IN (@recycle_bin_menu_id, @parts_price_adjust_menu_id)
ORDER BY child.`parent_id`, child.`type`, child.`sort`, child.`id`;
