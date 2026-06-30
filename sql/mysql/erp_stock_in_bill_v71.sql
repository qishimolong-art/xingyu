-- ERP stock in bill report (v71)
-- Scope:
--   1. Create erp_stock_in_bill as an independent read-only report table.
--   2. Add the stock in bill menu under ERP stock management.
--   3. Add only query/export button permissions.
--   4. Grant the new menu and buttons to active super_admin roles and roles that already own ERP stock permissions.
--
-- Safety:
--   - Does not read from or depend on stock flow tables.
--   - Does not add create/update/delete/audit permissions.
--   - Does not delete or replace existing menu, role, role-menu, or tenant-package data.
--   - Re-login or clear permission cache after execution.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_stock_in_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `priority` varchar(64) DEFAULT NULL COMMENT 'priority',
  `no` varchar(64) DEFAULT NULL COMMENT 'bill no',
  `pickup_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT 'pickup flag',
  `pickup` varchar(64) DEFAULT NULL COMMENT 'pickup',
  `pickup_user_name` varchar(64) DEFAULT NULL COMMENT 'pickup user name',
  `bill_date` datetime DEFAULT NULL COMMENT 'bill date',
  `warehouse_id` bigint DEFAULT NULL COMMENT 'warehouse id',
  `source_unit_name` varchar(128) DEFAULT NULL COMMENT 'source unit name',
  `source_no` varchar(64) DEFAULT NULL COMMENT 'source bill no',
  `status` tinyint DEFAULT NULL COMMENT 'status',
  `auditor_name` varchar(64) DEFAULT NULL COMMENT 'auditor name',
  `audit_time` datetime DEFAULT NULL COMMENT 'audit time',
  `print_time` datetime DEFAULT NULL COMMENT 'print time',
  `print_count` int NOT NULL DEFAULT 0 COMMENT 'print count',
  `total_weight` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'total weight',
  `remark` varchar(1024) DEFAULT NULL COMMENT 'remark',
  `timeout_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT 'timeout flag',
  `whole_qty` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'whole quantity',
  `loose_qty` decimal(24, 6) NOT NULL DEFAULT 0.000000 COMMENT 'loose quantity',
  `creator` varchar(64) DEFAULT NULL COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT NULL COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'tenant id',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_no` (`tenant_id`, `no`),
  KEY `idx_tenant_bill_date` (`tenant_id`, `bill_date`),
  KEY `idx_tenant_warehouse` (`tenant_id`, `warehouse_id`),
  KEY `idx_tenant_source_no` (`tenant_id`, `source_no`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP stock in bill report';

SET @stock_parent_id := (
  SELECT stock_menu.`id`
  FROM `system_menu` stock_menu
  JOIN `system_menu` erp_menu
    ON erp_menu.`id` = stock_menu.`parent_id`
   AND erp_menu.`name` = 'ERP 系统'
   AND erp_menu.`type` = 1
   AND erp_menu.`deleted` = b'0'
  WHERE stock_menu.`name` = '库存管理'
    AND stock_menu.`type` = 1
    AND stock_menu.`deleted` = b'0'
  ORDER BY stock_menu.`id` DESC
  LIMIT 1
);

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
      OR owned_menu.`parent_id` IN (
        SELECT stock_child.`id`
        FROM `system_menu` stock_child
        WHERE stock_child.`parent_id` = @stock_parent_id
          AND stock_child.`deleted` = b'0'
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
  ON target_menu.`deleted` = b'0'
 AND (
   target_menu.`id` = @stock_in_bill_menu_id
   OR target_menu.`permission` IN ('erp:stock-in-bill:query', 'erp:stock-in-bill:export')
 )
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` target
  WHERE target.`role_id` = source_role.`role_id`
    AND target.`menu_id` = target_menu.`id`
    AND target.`tenant_id` = source_role.`tenant_id`
    AND target.`deleted` = b'0'
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

UPDATE `system_tenant_package` tenant_package
SET tenant_package.`menu_ids` = JSON_ARRAY_APPEND(tenant_package.`menu_ids`, '$', @stock_in_bill_menu_id)
WHERE @stock_in_bill_menu_id IS NOT NULL
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

SELECT `id`, `name`, `permission`, `parent_id`, `component`, `deleted`
FROM `system_menu`
WHERE `component` = 'erp/stock/inbill/index'
   OR `permission` IN ('erp:stock-in-bill:query', 'erp:stock-in-bill:export')
ORDER BY `type`, `sort`, `id`;
