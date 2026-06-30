-- ERP user warehouse permission (v68)
-- Scope:
--   1. Create user-warehouse permission relation table.
--   2. Add warehouse permission query/update button permissions under ERP warehouse menu.
--   3. Grant the new button permissions to active super_admin roles.
--
-- Safety:
--   - Uses CREATE TABLE IF NOT EXISTS.
--   - Uses NOT EXISTS to avoid duplicate menu and role-menu rows.
--   - Does not delete or replace existing menu, role, role-menu, or user-role data.
--   - Re-login or clear permission cache after execution.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_user_warehouse_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL COMMENT 'user id',
  `warehouse_id` bigint NOT NULL COMMENT 'warehouse id',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'tenant id',
  `creator` varchar(64) DEFAULT NULL COMMENT 'creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updater` varchar(64) DEFAULT NULL COMMENT 'updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_warehouse` (`user_id`, `warehouse_id`, `tenant_id`, `deleted`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP user warehouse permission';

SET @warehouse_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `permission` = ''
    AND `component` = 'erp/stock/warehouse/index'
    AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT 'Warehouse Permission Query', 'erp:warehouse-permission:query', 3, 7, @warehouse_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:warehouse-permission:query'
      AND `deleted` = b'0'
  );

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT 'Warehouse Permission Update', 'erp:warehouse-permission:update', 3, 8, @warehouse_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @warehouse_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:warehouse-permission:update'
      AND `deleted` = b'0'
  );

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT r.`id`, m.`id`, '1', NOW(), '1', NOW(), b'0', r.`tenant_id`
FROM `system_role` r
JOIN `system_menu` m
  ON m.`permission` IN ('erp:warehouse-permission:query', 'erp:warehouse-permission:update')
 AND m.`deleted` = b'0'
WHERE r.`code` = 'crm_admin'
  AND r.`deleted` = b'0'
  AND r.`status` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` rm
    WHERE rm.`role_id` = r.`id`
      AND rm.`menu_id` = m.`id`
      AND rm.`deleted` = b'0'
  );

SELECT `permission`, `id`, `parent_id`, `deleted`
FROM `system_menu`
WHERE `permission` IN ('erp:warehouse-permission:query', 'erp:warehouse-permission:update')
ORDER BY `permission`, `id`;

SELECT COUNT(*) AS active_super_admin_grants
FROM `system_role_menu` rm
JOIN `system_role` r ON r.`id` = rm.`role_id`
JOIN `system_menu` m ON m.`id` = rm.`menu_id`
WHERE r.`code` = 'crm_admin'
  AND r.`deleted` = b'0'
  AND rm.`deleted` = b'0'
  AND m.`permission` IN ('erp:warehouse-permission:query', 'erp:warehouse-permission:update');
