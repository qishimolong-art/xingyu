-- ERP 基础档案回收站 v75.
-- 范围：
-- 1. 给供应商、客户、配件、仓库增加停用审计字段。
-- 2. 在 ERP 系统 -> 基础数据 下新增【回收站】菜单和按钮权限。
-- 3. 给已有基础档案权限角色、超管角色、租户套餐补齐菜单范围。
--
-- 约束：
-- - 不删除 system_menu / system_role_menu / system_tenant_package 数据。
-- - system_role_menu 没有唯一键，授权写入全部使用 NOT EXISTS 防重复。
-- - 字段新增使用 information_schema + prepared statement，支持重复执行。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 停用审计字段：供应商。
SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `erp_supplier` ADD COLUMN `disabled_by` bigint NULL COMMENT ''停用人用户编号''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'erp_supplier'
    AND column_name = 'disabled_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `erp_supplier` ADD COLUMN `disabled_time` datetime NULL COMMENT ''停用时间''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'erp_supplier'
    AND column_name = 'disabled_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 停用审计字段：客户。
SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `erp_customer` ADD COLUMN `disabled_by` bigint NULL COMMENT ''停用人用户编号''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'erp_customer'
    AND column_name = 'disabled_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `erp_customer` ADD COLUMN `disabled_time` datetime NULL COMMENT ''停用时间''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'erp_customer'
    AND column_name = 'disabled_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 停用审计字段：配件。
SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `erp_product` ADD COLUMN `disabled_by` bigint NULL COMMENT ''停用人用户编号''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'erp_product'
    AND column_name = 'disabled_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `erp_product` ADD COLUMN `disabled_time` datetime NULL COMMENT ''停用时间''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'erp_product'
    AND column_name = 'disabled_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 停用审计字段：仓库。
SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `erp_warehouse` ADD COLUMN `disabled_by` bigint NULL COMMENT ''停用人用户编号''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'erp_warehouse'
    AND column_name = 'disabled_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `erp_warehouse` ADD COLUMN `disabled_time` datetime NULL COMMENT ''停用时间''',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'erp_warehouse'
    AND column_name = 'disabled_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @erp_parent_id := (
  SELECT id
  FROM `system_menu`
  WHERE name = 'ERP 系统'
    AND type = 1
    AND parent_id = 0
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @base_parent_id := (
  SELECT id
  FROM `system_menu`
  WHERE name = '基础数据'
    AND type = 1
    AND parent_id = @erp_parent_id
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @system_config_parent_id := (
  SELECT id
  FROM `system_menu`
  WHERE name = '系统配置'
    AND type = 1
    AND parent_id = @erp_parent_id
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

-- ERP 专用基础数据目录。避免误挂到其它系统下同名【基础数据】目录。
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
  SELECT id
  FROM `system_menu`
  WHERE name = '基础数据'
    AND type = 1
    AND parent_id = @erp_parent_id
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

-- ERP 专用系统配置目录。回收站归入该分类。
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
  SELECT id
  FROM `system_menu`
  WHERE name = '系统配置'
    AND type = 1
    AND parent_id = @erp_parent_id
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, @base_parent_id, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE @base_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` target
    WHERE target.role_id = 1
      AND target.menu_id = @base_parent_id
      AND target.tenant_id = 1
      AND target.deleted = b'0'
  );

-- 基础档案回收站菜单。
INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '回收站', 'erp:recycle-bin:query', 2, 90, @system_config_parent_id, 'recycle-bin', 'ep:delete',
  'erp/base/recycle-bin/index', 'ErpRecycleBin',
  0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @system_config_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE component = 'erp/base/recycle-bin/index'
      AND deleted = b'0'
  );

SET @recycle_bin_menu_id := (
  SELECT id
  FROM `system_menu`
  WHERE component = 'erp/base/recycle-bin/index'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

UPDATE `system_menu`
SET parent_id = @system_config_parent_id,
    path = 'recycle-bin',
    updater = '1',
    update_time = NOW()
WHERE @system_config_parent_id IS NOT NULL
  AND id = @recycle_bin_menu_id
  AND deleted = b'0'
  AND (
    parent_id <> @system_config_parent_id
    OR path <> 'recycle-bin'
  );

-- 按钮权限。
INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '回收站查询', 'erp:recycle-bin:query', 3, 1, @recycle_bin_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @recycle_bin_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE parent_id = @recycle_bin_menu_id
      AND permission = 'erp:recycle-bin:query'
      AND deleted = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '回收站还原', 'erp:recycle-bin:restore', 3, 2, @recycle_bin_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @recycle_bin_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE parent_id = @recycle_bin_menu_id
      AND permission = 'erp:recycle-bin:restore'
      AND deleted = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '回收站清除', 'erp:recycle-bin:clear', 3, 3, @recycle_bin_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @recycle_bin_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE parent_id = @recycle_bin_menu_id
      AND permission = 'erp:recycle-bin:clear'
      AND deleted = b'0'
  );

SET @recycle_bin_query_menu_id := (
  SELECT id
  FROM `system_menu`
  WHERE parent_id = @recycle_bin_menu_id
    AND permission = 'erp:recycle-bin:query'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @recycle_bin_restore_menu_id := (
  SELECT id
  FROM `system_menu`
  WHERE parent_id = @recycle_bin_menu_id
    AND permission = 'erp:recycle-bin:restore'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @recycle_bin_clear_menu_id := (
  SELECT id
  FROM `system_menu`
  WHERE parent_id = @recycle_bin_menu_id
    AND permission = 'erp:recycle-bin:clear'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

-- 授权回收站目录/菜单给已有基础档案权限角色，以及全部超管角色。
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source_role.role_id, target_menu.id, '1', NOW(), '1', NOW(), b'0', source_role.tenant_id
FROM (
  SELECT DISTINCT role_menu.role_id, role_menu.tenant_id
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.id = role_menu.menu_id
   AND owned_menu.deleted = b'0'
  WHERE role_menu.deleted = b'0'
    AND (
      owned_menu.permission IN (
        'erp:base-data:query',
        'erp:supplier:query',
        'erp:customer:query',
        'erp:product:query',
        'erp:warehouse:query'
      )
      OR owned_menu.component IN (
        'erp/base/options/index',
        'erp/purchase/supplier/index',
        'erp/sale/customer/index',
        'erp/product/product/index',
        'erp/stock/warehouse/index'
      )
    )
  UNION
  SELECT sys_role.id, sys_role.tenant_id
  FROM `system_role` sys_role
  WHERE sys_role.code = 'super_admin'
    AND sys_role.deleted = b'0'
) source_role
JOIN `system_menu` target_menu
  ON target_menu.id IN (@system_config_parent_id, @recycle_bin_menu_id)
 AND target_menu.deleted = b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` target
  WHERE target.role_id = source_role.role_id
    AND target.menu_id = target_menu.id
    AND target.tenant_id = source_role.tenant_id
    AND target.deleted = b'0'
);

-- 授权回收站按钮给已有基础档案查询角色，以及全部超管角色。
-- 清除是物理删除能力，仅自动授予超管角色，避免普通查询角色拿到高风险权限。
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source_role.role_id, target_menu.id, '1', NOW(), '1', NOW(), b'0', source_role.tenant_id
FROM (
  SELECT DISTINCT role_menu.role_id, role_menu.tenant_id, button_permission.permission
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.id = role_menu.menu_id
   AND owned_menu.deleted = b'0'
  CROSS JOIN (
    SELECT 'erp:recycle-bin:query' AS permission
    UNION ALL SELECT 'erp:recycle-bin:restore'
  ) button_permission
  WHERE role_menu.deleted = b'0'
    AND owned_menu.permission IN (
      'erp:base-data:query',
      'erp:supplier:query',
      'erp:customer:query',
      'erp:product:query',
      'erp:warehouse:query'
    )
  UNION
  SELECT sys_role.id, sys_role.tenant_id, button_permission.permission
  FROM `system_role` sys_role
  CROSS JOIN (
    SELECT 'erp:recycle-bin:query' AS permission
    UNION ALL SELECT 'erp:recycle-bin:restore'
    UNION ALL SELECT 'erp:recycle-bin:clear'
  ) button_permission
  WHERE sys_role.code = 'super_admin'
    AND sys_role.deleted = b'0'
) source_role
JOIN `system_menu` target_menu
  ON target_menu.parent_id = @recycle_bin_menu_id
 AND target_menu.permission = source_role.permission
 AND target_menu.deleted = b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` target
  WHERE target.role_id = source_role.role_id
    AND target.menu_id = target_menu.id
    AND target.tenant_id = source_role.tenant_id
    AND target.deleted = b'0'
);

-- 租户套餐范围：已有 ERP / 基础数据 / 基础档案入口的套餐，补入回收站菜单。
UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @system_config_parent_id)
WHERE @system_config_parent_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND EXISTS (
    SELECT 1
    FROM `system_menu` owned_menu
    WHERE (
      owned_menu.id = @erp_parent_id
      OR owned_menu.component IN (
        'erp/base/options/index',
        'erp/purchase/supplier/index',
        'erp/sale/customer/index',
        'erp/product/product/index',
        'erp/stock/warehouse/index'
      )
    )
      AND owned_menu.deleted = b'0'
      AND JSON_CONTAINS(tenant_package.menu_ids, CAST(owned_menu.id AS CHAR), '$')
  )
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@system_config_parent_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @recycle_bin_menu_id)
WHERE @recycle_bin_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND (
    JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_parent_id AS CHAR), '$')
    OR JSON_CONTAINS(tenant_package.menu_ids, CAST(@system_config_parent_id AS CHAR), '$')
    OR JSON_CONTAINS(tenant_package.menu_ids, CAST(@erp_parent_id AS CHAR), '$')
  )
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@recycle_bin_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @recycle_bin_query_menu_id)
WHERE @recycle_bin_query_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND JSON_CONTAINS(tenant_package.menu_ids, CAST(@recycle_bin_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@recycle_bin_query_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @recycle_bin_restore_menu_id)
WHERE @recycle_bin_restore_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND JSON_CONTAINS(tenant_package.menu_ids, CAST(@recycle_bin_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@recycle_bin_restore_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @recycle_bin_clear_menu_id)
WHERE @recycle_bin_clear_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND JSON_CONTAINS(tenant_package.menu_ids, CAST(@recycle_bin_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@recycle_bin_clear_menu_id AS CHAR), '$');
