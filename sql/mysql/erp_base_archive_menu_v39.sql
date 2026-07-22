-- ERP 基础选项合并与档案菜单归拢 v39.
-- 执行后刷新菜单缓存，并重新登录前端。
--
-- 范围：
-- 1. 在【基础数据】下新增统一【基础选项】菜单。
-- 2. 将供应商、客户、价格体系菜单移动到【基础数据】下。
-- 3. 在【基础数据】下新增【配件基本信息】，并将配件信息、配件分类、配件单位移动到该分组下。
-- 4. 可选隐藏旧基础选项独立菜单入口。
--
-- 约束：
-- - 不迁移 erp_supplier / erp_customer / erp_product 等档案数据。
-- - 不修改档案页面 component / permission。
-- - 不删除 system_menu 或 system_role_menu 数据。
-- - system_role_menu 没有唯一键，所有授权写入都用 NOT EXISTS 防重复。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_parent_id := (
  SELECT id
  FROM system_menu
  WHERE name = 'ERP 系统'
    AND type = 1
    AND parent_id = 0
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @base_parent_id := (
  SELECT id
  FROM system_menu
  WHERE name = '基础数据'
    AND type = 1
    AND parent_id = @erp_parent_id
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

-- ERP 专用基础数据目录。避免误挂到 MES 系统下同名【基础数据】目录。
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
  FROM system_menu
  WHERE name = '基础数据'
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

-- 统一基础选项入口，继续复用 erp:base-data:* 按钮权限。
INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '基础选项', '', 2, 1, @base_parent_id, 'base-options', 'ep:operation',
  'erp/base/options/index', 'ErpBaseOptions',
  0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @base_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE component = 'erp/base/options/index'
  );

UPDATE `system_menu`
SET `name` = '基础选项',
    `permission` = '',
    `type` = 2,
    `sort` = 1,
    `parent_id` = @base_parent_id,
    `path` = 'base-options',
    `icon` = 'ep:operation',
    `component` = 'erp/base/options/index',
    `component_name` = 'ErpBaseOptions',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `component` = 'erp/base/options/index'
  AND @base_parent_id IS NOT NULL;

SET @base_options_menu_id := (
  SELECT id
  FROM system_menu
  WHERE component = 'erp/base/options/index'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

-- 新统一入口下补齐基础数据按钮权限节点，权限码沿用 erp:base-data:*。
INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '基础选项查询', 'erp:base-data:query', 3, 1, @base_options_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @base_options_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @base_options_menu_id
      AND permission = 'erp:base-data:query'
      AND deleted = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '基础选项创建', 'erp:base-data:create', 3, 2, @base_options_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @base_options_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @base_options_menu_id
      AND permission = 'erp:base-data:create'
      AND deleted = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '基础选项更新', 'erp:base-data:update', 3, 3, @base_options_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @base_options_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @base_options_menu_id
      AND permission = 'erp:base-data:update'
      AND deleted = b'0'
  );

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '基础选项删除', 'erp:base-data:delete', 3, 4, @base_options_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @base_options_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE parent_id = @base_options_menu_id
      AND permission = 'erp:base-data:delete'
      AND deleted = b'0'
  );

-- 授权统一入口目录/菜单给已有基础选项权限的角色，以及全部超管角色。
-- 只新增缺失关系，不覆盖其它角色授权。
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
        'erp:base-data:create',
        'erp:base-data:update',
        'erp:base-data:delete'
      )
      OR owned_menu.component IN (
        'erp/base/region/index',
        'erp/base/category/index',
        'erp/base/supplier-type/index',
        'erp/base/logistics-company/index',
        'erp/base/purchase-type/index',
        'erp/base/delivery-method/index',
        'erp/base/settle-method/index',
        'erp/base/invoice-type/index',
        'erp/base/receive-address/index',
        'erp/base/order-company/index',
        'erp/base/storage-center/index',
        'erp/base/storage-warehouse/index',
        'erp/base/options/index'
      )
    )
  UNION
  SELECT sys_role.id, sys_role.tenant_id
  FROM `system_role` sys_role
  WHERE sys_role.code = 'super_admin'
    AND sys_role.deleted = b'0'
) source_role
JOIN `system_menu` target_menu
  ON target_menu.id IN (@base_parent_id, @base_options_menu_id)
 AND target_menu.deleted = b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` target
  WHERE target.role_id = source_role.role_id
    AND target.menu_id = target_menu.id
    AND target.tenant_id = source_role.tenant_id
    AND target.deleted = b'0'
);

-- 授权统一入口按钮给已有相同 erp:base-data:* 权限的角色，以及全部超管角色。
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source_role.role_id, target_menu.id, '1', NOW(), '1', NOW(), b'0', source_role.tenant_id
FROM (
  SELECT DISTINCT role_menu.role_id, role_menu.tenant_id, owned_menu.permission
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.id = role_menu.menu_id
   AND owned_menu.deleted = b'0'
  WHERE role_menu.deleted = b'0'
    AND owned_menu.permission IN (
      'erp:base-data:query',
      'erp:base-data:create',
      'erp:base-data:update',
      'erp:base-data:delete'
    )
  UNION
  SELECT sys_role.id, sys_role.tenant_id, base_permission.permission
  FROM `system_role` sys_role
  CROSS JOIN (
    SELECT 'erp:base-data:query' AS permission
    UNION ALL SELECT 'erp:base-data:create'
    UNION ALL SELECT 'erp:base-data:update'
    UNION ALL SELECT 'erp:base-data:delete'
  ) base_permission
  WHERE sys_role.code = 'super_admin'
    AND sys_role.deleted = b'0'
) source_role
JOIN `system_menu` target_menu
  ON target_menu.parent_id = @base_options_menu_id
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

SET @base_options_query_menu_id := (
  SELECT id FROM `system_menu`
  WHERE parent_id = @base_options_menu_id
    AND permission = 'erp:base-data:query'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @base_options_create_menu_id := (
  SELECT id FROM `system_menu`
  WHERE parent_id = @base_options_menu_id
    AND permission = 'erp:base-data:create'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @base_options_update_menu_id := (
  SELECT id FROM `system_menu`
  WHERE parent_id = @base_options_menu_id
    AND permission = 'erp:base-data:update'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @base_options_delete_menu_id := (
  SELECT id FROM `system_menu`
  WHERE parent_id = @base_options_menu_id
    AND permission = 'erp:base-data:delete'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @parts_basic_menu_id := (
  SELECT id
  FROM system_menu
  WHERE name = '配件基本信息'
    AND type = 1
    AND parent_id = @base_parent_id
    AND deleted = b'0'
  ORDER BY id DESC
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
  SELECT id
  FROM system_menu
  WHERE name = '配件基本信息'
    AND type = 1
    AND parent_id = @base_parent_id
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `permission` = '',
    `type` = 1,
    `sort` = 30,
    `parent_id` = @base_parent_id,
    `path` = 'parts-basic',
    `icon` = 'fa-solid:tools',
    `component` = '',
    `component_name` = '',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = @parts_basic_menu_id
  AND @base_parent_id IS NOT NULL;

-- 配件基本信息目录及其上级目录授权给已有配件菜单权限的角色，以及全部超管角色。
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
      owned_menu.component IN (
        'erp/product/product/index',
        'erp/product/category/index',
        'erp/product/unit/index'
      )
      OR owned_menu.permission IN (
        'erp:product:query',
        'erp:product:create',
        'erp:product:update',
        'erp:product:delete',
        'erp:product:export',
        'erp:product:import',
        'erp:product-category:query',
        'erp:product-category:create',
        'erp:product-category:update',
        'erp:product-category:delete',
        'erp:product-category:export',
        'erp:product-category:import',
        'erp:product-unit:query',
        'erp:product-unit:create',
        'erp:product-unit:update',
        'erp:product-unit:delete',
        'erp:product-unit:export',
        'erp:product-unit:import'
      )
    )
  UNION
  SELECT sys_role.id, sys_role.tenant_id
  FROM `system_role` sys_role
  WHERE sys_role.code = 'super_admin'
    AND sys_role.deleted = b'0'
) source_role
JOIN `system_menu` target_menu
  ON target_menu.id IN (@base_parent_id, @parts_basic_menu_id)
 AND target_menu.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` target
    WHERE target.role_id = source_role.role_id
      AND target.menu_id = target_menu.id
      AND target.tenant_id = source_role.tenant_id
      AND target.deleted = b'0'
  );

-- 租户套餐菜单范围：已有 ERP 或旧基础选项菜单的套餐，补入新统一入口及按钮。
-- menu_ids 是 varchar(4096)，接近上限的套餐跳过，避免执行脚本时被截断或报错。
UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @base_parent_id)
WHERE @base_parent_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND EXISTS (
    SELECT 1
    FROM `system_menu` old_base_menu
    WHERE (
      old_base_menu.id = @erp_parent_id
      OR old_base_menu.component IN (
        'erp/base/region/index',
        'erp/base/category/index',
        'erp/base/supplier-type/index',
        'erp/base/logistics-company/index',
        'erp/base/purchase-type/index',
        'erp/base/delivery-method/index',
        'erp/base/settle-method/index',
        'erp/base/invoice-type/index',
        'erp/base/receive-address/index',
        'erp/base/order-company/index',
        'erp/base/storage-center/index',
        'erp/base/storage-warehouse/index'
      )
    )
      AND JSON_CONTAINS(tenant_package.menu_ids, CAST(old_base_menu.id AS CHAR), '$')
  )
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_parent_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @base_options_menu_id)
WHERE @base_options_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND (
    JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_parent_id AS CHAR), '$')
    OR JSON_CONTAINS(tenant_package.menu_ids, CAST(@erp_parent_id AS CHAR), '$')
  )
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @base_options_query_menu_id)
WHERE @base_options_query_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_query_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @base_options_create_menu_id)
WHERE @base_options_create_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_create_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @base_options_update_menu_id)
WHERE @base_options_update_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_update_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @base_options_delete_menu_id)
WHERE @base_options_delete_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_options_delete_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @parts_basic_menu_id)
WHERE @parts_basic_menu_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND EXISTS (
    SELECT 1
    FROM `system_menu` owned_menu
    WHERE owned_menu.component IN (
        'erp/product/product/index',
        'erp/product/category/index',
        'erp/product/unit/index'
      )
      AND owned_menu.deleted = b'0'
      AND JSON_CONTAINS(tenant_package.menu_ids, CAST(owned_menu.id AS CHAR), '$')
  )
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@parts_basic_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @base_parent_id)
WHERE @base_parent_id IS NOT NULL
  AND tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND CHAR_LENGTH(tenant_package.menu_ids) < 4000
  AND EXISTS (
    SELECT 1
    FROM `system_menu` owned_menu
    WHERE owned_menu.component IN (
        'erp/product/product/index',
        'erp/product/category/index',
        'erp/product/unit/index'
      )
      AND owned_menu.deleted = b'0'
      AND JSON_CONTAINS(tenant_package.menu_ids, CAST(owned_menu.id AS CHAR), '$')
  )
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@base_parent_id AS CHAR), '$');

-- 档案菜单归拢：保留原菜单 ID、按钮子节点、component、permission。
UPDATE `system_menu`
SET `name` = '供应商档案',
    `parent_id` = @base_parent_id,
    `sort` = 10,
    `path` = 'supplier',
    `updater` = '1',
    `update_time` = NOW()
WHERE `component` = 'erp/purchase/supplier/index'
  AND `type` = 2
  AND `deleted` = b'0'
  AND @base_parent_id IS NOT NULL;

UPDATE `system_menu`
SET `name` = '客户档案',
    `parent_id` = @base_parent_id,
    `sort` = 20,
    `path` = 'customer',
    `updater` = '1',
    `update_time` = NOW()
WHERE `component` = 'erp/sale/customer/index'
  AND `type` = 2
  AND `deleted` = b'0'
  AND @base_parent_id IS NOT NULL;

UPDATE `system_menu`
SET `name` = '配件信息',
    `parent_id` = @parts_basic_menu_id,
    `sort` = 10,
    `path` = 'product',
    `updater` = '1',
    `update_time` = NOW()
WHERE `component` = 'erp/product/product/index'
  AND `type` = 2
  AND `deleted` = b'0'
  AND @parts_basic_menu_id IS NOT NULL;

UPDATE `system_menu`
SET `name` = '配件分类',
    `parent_id` = @parts_basic_menu_id,
    `sort` = 20,
    `path` = 'product-category',
    `updater` = '1',
    `update_time` = NOW()
WHERE `component` = 'erp/product/category/index'
  AND `type` = 2
  AND `deleted` = b'0'
  AND @parts_basic_menu_id IS NOT NULL;

UPDATE `system_menu`
SET `name` = '配件单位',
    `parent_id` = @parts_basic_menu_id,
    `sort` = 30,
    `path` = 'product-unit',
    `updater` = '1',
    `update_time` = NOW()
WHERE `component` = 'erp/product/unit/index'
  AND `type` = 2
  AND `deleted` = b'0'
  AND @parts_basic_menu_id IS NOT NULL;

UPDATE `system_menu`
SET `name` = '价格体系',
    `parent_id` = @base_parent_id,
    `sort` = 60,
    `path` = 'price-system',
    `updater` = '1',
    `update_time` = NOW()
WHERE `component` = 'erp/product/pricesystem/index'
  AND `type` = 2
  AND `deleted` = b'0'
  AND @base_parent_id IS NOT NULL;

-- 隐藏旧基础选项独立菜单。仅按组件路径精确更新，菜单和按钮权限节点不删除。
UPDATE `system_menu`
SET `visible` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `component` IN (
    'erp/base/region/index',
    'erp/base/category/index',
    'erp/base/supplier-type/index',
    'erp/base/logistics-company/index',
    'erp/base/purchase-type/index',
    'erp/base/delivery-method/index',
    'erp/base/settle-method/index',
    'erp/base/invoice-type/index',
    'erp/base/receive-address/index',
    'erp/base/order-company/index',
    'erp/base/storage-center/index',
    'erp/base/storage-warehouse/index'
  )
  AND `type` = 2
  AND `deleted` = b'0';
