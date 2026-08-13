-- ERP customer department credit config (v168).
-- Safe to rerun. This script creates a new config table and appends one button
-- permission. It does not delete or overwrite existing menu, role, or customer data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_customer_dept_credit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `customer_id` bigint NOT NULL COMMENT '客户编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `credit_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用授信',
  `credit_limit` decimal(18,2) NULL DEFAULT NULL COMMENT '授信额度',
  `credit_term_days` int NULL DEFAULT NULL COMMENT '授信期限（天）',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_customer_dept_credit` (`customer_id`, `dept_id`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_customer_id` (`customer_id`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 客户部门授信配置';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_customer_dept_credit_permission;

CREATE TEMPORARY TABLE tmp_erp_customer_dept_credit_permission (
  parent_permission varchar(128) NOT NULL,
  inherit_permission varchar(128) NOT NULL,
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_customer_dept_credit_permission
(`parent_permission`, `inherit_permission`, `button_name`, `button_permission`, `sort`)
VALUES
('erp:customer:query', 'erp:customer:update', '客户分部门授信', 'erp:customer:dept-credit', 9);

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT perm.button_name,
       perm.button_permission,
       3,
       perm.sort,
       parent.parent_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM tmp_erp_customer_dept_credit_permission perm
JOIN system_menu parent
  ON parent.permission = perm.parent_permission
 AND parent.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM system_menu exists_menu
     WHERE exists_menu.permission = perm.button_permission
       AND exists_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       button_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM tmp_erp_customer_dept_credit_permission perm
JOIN system_menu button_menu
  ON button_menu.permission = perm.button_permission
 AND button_menu.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.permission = perm.inherit_permission
 AND owned_menu.deleted = b'0'
JOIN system_role_menu owned_role
  ON owned_role.menu_id = owned_menu.id
 AND owned_role.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM system_role_menu target
     WHERE target.role_id = owned_role.role_id
       AND target.menu_id = button_menu.id
       AND target.tenant_id = owned_role.tenant_id
       AND target.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       button_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM system_role super_role
JOIN system_menu button_menu
  ON button_menu.permission = 'erp:customer:dept-credit'
 AND button_menu.deleted = b'0'
WHERE super_role.code = 'super_admin'
  AND super_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
        FROM system_role_menu target
       WHERE target.role_id = super_role.id
         AND target.menu_id = button_menu.id
         AND target.tenant_id = super_role.tenant_id
         AND target.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_customer_dept_credit_permission;
