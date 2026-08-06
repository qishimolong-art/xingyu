-- Move parts price adjustment under ERP / base data / parts basic info (v134).
--
-- Scope:
--   1. Ensure ERP / 基础数据 / 配件基本信息 directories exist.
--   2. Move the existing 配件价格调整 page under 配件基本信息.
--   3. Keep existing page and button permissions unchanged.
--   4. Append only the needed parent directories to roles and tenant packages
--      that already own the moved page or its buttons.
--
-- Safety:
--   - No DELETE.
--   - No broad overwrite of system_tenant_package.menu_ids.
--   - No blanket grant to all roles except existing active super_admin roles.

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

SET @parts_price_adjust_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/purchase/price-adjust/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @parts_basic_menu_id,
    `sort` = 40,
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_basic_menu_id IS NOT NULL
  AND @parts_price_adjust_menu_id IS NOT NULL
  AND `id` = @parts_price_adjust_menu_id
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @parts_basic_menu_id
    OR `sort` <> 40
  );

UPDATE `system_menu`
SET `parent_id` = @parts_price_adjust_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND `permission` IN ('erp:parts:adjust-price')
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @parts_price_adjust_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source_role.`role_id`, target_menu.`id`, '1', NOW(), '1', NOW(), b'0', source_role.`tenant_id`
FROM (
  SELECT role_menu.`role_id`, role_menu.`tenant_id`
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`id` = @parts_price_adjust_menu_id
      OR owned_menu.`parent_id` = @parts_price_adjust_menu_id
      OR owned_menu.`permission` = 'erp:parts:adjust-price'
    )
  UNION
  SELECT role.`id`, role.`tenant_id`
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
) source_role
JOIN `system_menu` target_menu
  ON target_menu.`id` IN (@base_parent_id, @parts_basic_menu_id)
 AND target_menu.`deleted` = b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` target
  WHERE target.`role_id` = source_role.`role_id`
    AND target.`menu_id` = target_menu.`id`
    AND target.`tenant_id` = source_role.`tenant_id`
    AND target.`deleted` = b'0'
);

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @base_parent_id)
WHERE @base_parent_id IS NOT NULL
  AND @parts_price_adjust_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND (
    JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_adjust_menu_id AS CHAR), '$')
    OR EXISTS (
      SELECT 1
      FROM `system_menu` price_child
      WHERE price_child.`parent_id` = @parts_price_adjust_menu_id
        AND price_child.`deleted` = b'0'
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(price_child.`id` AS CHAR), '$')
    )
  )
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@base_parent_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @parts_basic_menu_id)
WHERE @parts_basic_menu_id IS NOT NULL
  AND @parts_price_adjust_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND (
    JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_adjust_menu_id AS CHAR), '$')
    OR EXISTS (
      SELECT 1
      FROM `system_menu` price_child
      WHERE price_child.`parent_id` = @parts_price_adjust_menu_id
        AND price_child.`deleted` = b'0'
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(price_child.`id` AS CHAR), '$')
    )
  )
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
WHERE child.`id` IN (@base_parent_id, @parts_basic_menu_id, @parts_price_adjust_menu_id)
   OR child.`parent_id` = @parts_price_adjust_menu_id
   OR child.`permission` = 'erp:parts:adjust-price'
ORDER BY child.`parent_id`, child.`type`, child.`sort`, child.`id`;
