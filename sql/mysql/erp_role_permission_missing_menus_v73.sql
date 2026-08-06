-- Fix role permission configuration entries for stock out bill, stock in bill,
-- and parts price adjustment.
--
-- Scope:
--   1. Ensure the three page menus and button permissions exist.
--   2. Keep stock in bill under stock management as the canonical entry.
--   3. Append missing menu ids to tenant packages without replacing menu_ids.
--   4. Grant the menus to active super_admin roles and roles that already own
--      the corresponding parent business menu.
--
-- Safety:
--   - No DELETE.
--   - No broad overwrite of system_tenant_package.menu_ids.
--   - No blanket grant to all roles.
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

SET @stock_parent_id := (
  SELECT stock_menu.`id`
  FROM `system_menu` stock_menu
  WHERE stock_menu.`name` = '库存管理'
    AND stock_menu.`type` = 1
    AND stock_menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR stock_menu.`parent_id` = @erp_root_id)
  ORDER BY stock_menu.`id` DESC
  LIMIT 1
);

SET @purchase_parent_id := (
  SELECT purchase_menu.`id`
  FROM `system_menu` purchase_menu
  WHERE purchase_menu.`name` = '采购管理'
    AND purchase_menu.`type` = 1
    AND purchase_menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR purchase_menu.`parent_id` = @erp_root_id)
  ORDER BY purchase_menu.`id` DESC
  LIMIT 1
);

SET @system_config_parent_id := (
  SELECT system_config_menu.`id`
  FROM `system_menu` system_config_menu
  WHERE system_config_menu.`name` = '系统配置'
    AND system_config_menu.`type` = 1
    AND system_config_menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR system_config_menu.`parent_id` = @erp_root_id)
  ORDER BY system_config_menu.`id` DESC
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

SET @parts_basic_menu_id := (
  SELECT menu.`id`
  FROM `system_menu` menu
  WHERE menu.`name` = '配件基本信息'
    AND menu.`type` = 1
    AND menu.`deleted` = b'0'
    AND (@base_parent_id IS NULL OR menu.`parent_id` = @base_parent_id)
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
  SELECT system_config_menu.`id`
  FROM `system_menu` system_config_menu
  WHERE system_config_menu.`name` = '系统配置'
    AND system_config_menu.`type` = 1
    AND system_config_menu.`deleted` = b'0'
    AND (@erp_root_id IS NULL OR system_config_menu.`parent_id` = @erp_root_id)
  ORDER BY system_config_menu.`id` DESC
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '基础数据', '', 1, 5, @erp_root_id, 'base', 'ep:data-analysis',
       '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_root_id IS NOT NULL
  AND @base_parent_id IS NULL;

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

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '配件基本信息', '', 1, 30, @base_parent_id, 'parts-basic', 'fa-solid:tools',
       '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @base_parent_id IS NOT NULL
  AND @parts_basic_menu_id IS NULL;

SET @parts_basic_menu_id := (
  SELECT menu.`id`
  FROM `system_menu` menu
  WHERE menu.`name` = '配件基本信息'
    AND menu.`type` = 1
    AND menu.`deleted` = b'0'
    AND (@base_parent_id IS NULL OR menu.`parent_id` = @base_parent_id)
  ORDER BY menu.`id` DESC
  LIMIT 1
);

-- 1. Stock out bill.
INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '出仓单', '', 2, 6, @stock_parent_id, 'outbill', 'ep:document',
       'erp/stock/outbill/index', 'ErpStockOutBill',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `component` = 'erp/stock/outbill/index'
      AND `deleted` = b'0'
  );

SET @stock_out_bill_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/stock/outbill/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '出仓单',
    `parent_id` = @stock_parent_id,
    `path` = 'outbill',
    `component_name` = 'ErpStockOutBill',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @stock_parent_id IS NOT NULL
  AND `id` = @stock_out_bill_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '出仓单'
    OR `parent_id` <> @stock_parent_id
    OR `path` <> 'outbill'
    OR IFNULL(`component_name`, '') <> 'ErpStockOutBill'
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '出仓单查询', 'erp:stock-out-bill:query', 3, 1, @stock_out_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_out_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:stock-out-bill:query'
      AND `deleted` = b'0'
  );

SET @stock_out_bill_query_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:stock-out-bill:query'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '出仓单查询',
    `parent_id` = @stock_out_bill_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @stock_out_bill_menu_id IS NOT NULL
  AND `id` = @stock_out_bill_query_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '出仓单查询'
    OR `parent_id` <> @stock_out_bill_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

-- 2. Stock in bill. Keep the stock-management entry canonical.
INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '入仓单', '', 2, 7, @stock_parent_id, 'inbill', 'ep:document',
       'erp/stock/inbill/index', 'ErpStockInBill',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `component` = 'erp/stock/inbill/index'
      AND `deleted` = b'0'
  );

SET @stock_in_bill_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/stock/inbill/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `name` = '入仓单',
    `parent_id` = @stock_parent_id,
    `path` = 'inbill',
    `component_name` = 'ErpStockInBill',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @stock_parent_id IS NOT NULL
  AND `id` = @stock_in_bill_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '入仓单'
    OR `parent_id` <> @stock_parent_id
    OR `path` <> 'inbill'
    OR IFNULL(`component_name`, '') <> 'ErpStockInBill'
    OR `status` <> 0
    OR `visible` <> b'1'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '入仓单查询', 'erp:stock-in-bill:query', 3, 1, @stock_in_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_in_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:stock-in-bill:query'
      AND `deleted` = b'0'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '入仓单导出', 'erp:stock-in-bill:export', 3, 2, @stock_in_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @stock_in_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:stock-in-bill:export'
      AND `deleted` = b'0'
  );

SET @stock_in_bill_query_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:stock-in-bill:query'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @stock_in_bill_export_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:stock-in-bill:export'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @stock_in_bill_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @stock_in_bill_menu_id IS NOT NULL
  AND `permission` IN ('erp:stock-in-bill:query', 'erp:stock-in-bill:export')
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @stock_in_bill_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

-- 3. Parts price adjustment. Keep it under Base data / Parts basic info.
INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '配件价格调整', '', 2, 40, @parts_basic_menu_id, 'price-adjust', 'ep:price-tag',
       'erp/purchase/price-adjust/index', 'ErpPurchasePriceAdjust',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @parts_basic_menu_id IS NOT NULL
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
    `parent_id` = @parts_basic_menu_id,
    `sort` = 40,
    `path` = 'price-adjust',
    `component_name` = 'ErpPurchasePriceAdjust',
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_basic_menu_id IS NOT NULL
  AND `id` = @parts_price_adjust_menu_id
  AND `deleted` = b'0'
  AND (
    `name` <> '配件价格调整'
    OR `parent_id` <> @parts_basic_menu_id
    OR `sort` <> 40
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
SELECT '配件价格查询', 'erp:product:query', 3, 1, @parts_price_adjust_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:product:query'
      AND `parent_id` = @parts_price_adjust_menu_id
      AND `deleted` = b'0'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '配件价格保存', 'erp:product:update', 3, 2, @parts_price_adjust_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:product:update'
      AND `parent_id` = @parts_price_adjust_menu_id
      AND `deleted` = b'0'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '配件价格批量调整', 'erp:parts:adjust-price', 3, 3, @parts_price_adjust_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `permission` = 'erp:parts:adjust-price'
      AND `deleted` = b'0'
  );

SET @parts_price_query_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:product:query'
    AND `parent_id` = @parts_price_adjust_menu_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @parts_price_update_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:product:update'
    AND `parent_id` = @parts_price_adjust_menu_id
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @parts_price_batch_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = 'erp:parts:adjust-price'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @parts_price_adjust_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND `id` = @parts_price_batch_menu_id
  AND `deleted` = b'0'
  AND (
    `parent_id` <> @parts_price_adjust_menu_id
    OR `type` <> 3
    OR `status` <> 0
    OR `visible` <> b'1'
  );

-- 4. Tenant package menu_ids append. Only packages that already own the parent
-- business menu are expanded.
UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_out_bill_menu_id)
WHERE @stock_out_bill_menu_id IS NOT NULL
  AND @stock_parent_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND (
    JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_parent_id AS CHAR), '$')
    OR EXISTS (
      SELECT 1
      FROM `system_menu` stock_child
      WHERE stock_child.`parent_id` = @stock_parent_id
        AND stock_child.`deleted` = b'0'
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(stock_child.`id` AS CHAR), '$')
    )
  )
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_out_bill_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_out_bill_query_menu_id)
WHERE @stock_out_bill_query_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_out_bill_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_out_bill_query_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_in_bill_menu_id)
WHERE @stock_in_bill_menu_id IS NOT NULL
  AND @stock_parent_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND (
    JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_parent_id AS CHAR), '$')
    OR EXISTS (
      SELECT 1
      FROM `system_menu` stock_child
      WHERE stock_child.`parent_id` = @stock_parent_id
        AND stock_child.`deleted` = b'0'
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(stock_child.`id` AS CHAR), '$')
    )
  )
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_in_bill_query_menu_id)
WHERE @stock_in_bill_query_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_query_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_in_bill_export_menu_id)
WHERE @stock_in_bill_export_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@stock_in_bill_export_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @base_parent_id)
WHERE @base_parent_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND (
    (
      @parts_basic_menu_id IS NOT NULL
      AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_basic_menu_id AS CHAR), '$')
    )
    OR EXISTS (
      SELECT 1
      FROM `system_menu` parts_child
      WHERE parts_child.`deleted` = b'0'
        AND (
          parts_child.`parent_id` = @parts_basic_menu_id
          OR parts_child.`component` IN (
            'erp/product/product/index',
            'erp/product/category/index',
            'erp/product/unit/index',
            'erp/purchase/price-adjust/index'
          )
        )
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(parts_child.`id` AS CHAR), '$')
    )
  )
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@base_parent_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @parts_basic_menu_id)
WHERE @parts_basic_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND EXISTS (
    SELECT 1
    FROM `system_menu` parts_child
    WHERE parts_child.`deleted` = b'0'
      AND (
        parts_child.`parent_id` = @parts_basic_menu_id
        OR parts_child.`component` IN (
          'erp/product/product/index',
          'erp/product/category/index',
          'erp/product/unit/index',
          'erp/purchase/price-adjust/index'
        )
      )
      AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(parts_child.`id` AS CHAR), '$')
  )
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_basic_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @parts_price_adjust_menu_id)
WHERE @parts_price_adjust_menu_id IS NOT NULL
  AND @parts_basic_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND (
    JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_basic_menu_id AS CHAR), '$')
    OR EXISTS (
      SELECT 1
      FROM `system_menu` parts_child
      WHERE parts_child.`parent_id` = @parts_basic_menu_id
        AND parts_child.`deleted` = b'0'
        AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(parts_child.`id` AS CHAR), '$')
    )
  )
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_adjust_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @parts_price_query_menu_id)
WHERE @parts_price_query_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_adjust_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_query_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @parts_price_update_menu_id)
WHERE @parts_price_update_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_adjust_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_update_menu_id AS CHAR), '$');

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @parts_price_batch_menu_id)
WHERE @parts_price_batch_menu_id IS NOT NULL
  AND tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
  AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_adjust_menu_id AS CHAR), '$')
  AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@parts_price_batch_menu_id AS CHAR), '$');

-- 5. Role grants. Grant to super admins and roles that already own the parent
-- business menu.
INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT source_role.`role_id`, target_menu.`id`, '1', NOW(), '1', NOW(), b'0', source_role.`tenant_id`
FROM (
  SELECT role_menu.`role_id`, role_menu.`tenant_id`
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`id` = @stock_parent_id
      OR owned_menu.`parent_id` = @stock_parent_id
    )
  UNION
  SELECT role.`id`, role.`tenant_id`
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
) source_role
JOIN `system_menu` target_menu
  ON target_menu.`id` IN (
    @stock_out_bill_menu_id,
    @stock_out_bill_query_menu_id,
    @stock_in_bill_menu_id,
    @stock_in_bill_query_menu_id,
    @stock_in_bill_export_menu_id
  )
 AND target_menu.`deleted` = b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` target
  WHERE target.`role_id` = source_role.`role_id`
    AND target.`menu_id` = target_menu.`id`
    AND target.`tenant_id` = source_role.`tenant_id`
    AND target.`deleted` = b'0'
);

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT source_role.`role_id`, target_menu.`id`, '1', NOW(), '1', NOW(), b'0', source_role.`tenant_id`
FROM (
  SELECT role_menu.`role_id`, role_menu.`tenant_id`
  FROM `system_role_menu` role_menu
  JOIN `system_menu` owned_menu
    ON owned_menu.`id` = role_menu.`menu_id`
   AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`id` IN (@base_parent_id, @parts_basic_menu_id)
      OR owned_menu.`parent_id` = @parts_basic_menu_id
      OR owned_menu.`component` IN (
        'erp/product/product/index',
        'erp/product/category/index',
        'erp/product/unit/index',
        'erp/purchase/price-adjust/index'
      )
    )
  UNION
  SELECT role.`id`, role.`tenant_id`
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND role.`status` = 0
) source_role
JOIN `system_menu` target_menu
  ON target_menu.`id` IN (
    @base_parent_id,
    @parts_basic_menu_id,
    @parts_price_adjust_menu_id,
    @parts_price_query_menu_id,
    @parts_price_update_menu_id,
    @parts_price_batch_menu_id
  )
 AND target_menu.`deleted` = b'0'
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` target
  WHERE target.`role_id` = source_role.`role_id`
    AND target.`menu_id` = target_menu.`id`
    AND target.`tenant_id` = source_role.`tenant_id`
    AND target.`deleted` = b'0'
);

SELECT `id`, `name`, `permission`, `type`, `parent_id`, `component`, `status`, `visible`, `deleted`
FROM `system_menu`
WHERE `component` IN (
    'erp/stock/outbill/index',
    'erp/stock/inbill/index',
    'erp/purchase/price-adjust/index'
  )
   OR `permission` IN (
    'erp:stock-out-bill:query',
    'erp:stock-in-bill:query',
    'erp:stock-in-bill:export',
    'erp:parts:adjust-price'
  )
   OR (
    `parent_id` = @parts_price_adjust_menu_id
    AND `permission` IN ('erp:product:query', 'erp:product:update')
  )
ORDER BY `parent_id`, `type`, `sort`, `id`;
