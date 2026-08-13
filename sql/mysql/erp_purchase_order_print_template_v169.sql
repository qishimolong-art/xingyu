-- ERP purchase order print template and print record (v169).
-- Safe to rerun. This script creates generic print tables and appends purchase
-- order print permissions. It does not delete or overwrite existing menu, role,
-- permission, or business data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_print_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `module_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块标识',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `defaulted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `template_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板 JSON',
  `paper_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '纸张配置 JSON',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_module_default` (`module_key`, `defaulted`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_module_status` (`module_key`, `status`, `tenant_id`, `deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 打印模板';

CREATE TABLE IF NOT EXISTS `erp_print_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `module_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块标识',
  `business_id` bigint NOT NULL COMMENT '业务单据编号',
  `business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务单据号',
  `template_id` bigint NULL DEFAULT NULL COMMENT '打印模板编号',
  `printer_id` bigint NULL DEFAULT NULL COMMENT '打印人编号',
  `printer_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '打印人名称',
  `print_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打印时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_business` (`module_key`, `business_id`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_print_time` (`module_key`, `business_id`, `print_time`, `tenant_id`, `deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 打印记录';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_purchase_order_print_permission;

CREATE TEMPORARY TABLE tmp_erp_purchase_order_print_permission (
  parent_permission varchar(128) NOT NULL,
  inherit_permission varchar(128) NOT NULL,
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_purchase_order_print_permission
(`parent_permission`, `inherit_permission`, `button_name`, `button_permission`, `sort`)
VALUES
('erp:purchase-order:query', 'erp:purchase-order:query', '采购订单打印', 'erp:purchase-order:print', 7),
('erp:purchase-order:query', 'super_admin', '采购订单打印模板', 'erp:purchase-order:print-template', 8);

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
FROM tmp_erp_purchase_order_print_permission perm
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
FROM tmp_erp_purchase_order_print_permission perm
JOIN system_menu button_menu
  ON button_menu.permission = perm.button_permission
 AND button_menu.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.permission = perm.inherit_permission
 AND owned_menu.deleted = b'0'
JOIN system_role_menu owned_role
  ON owned_role.menu_id = owned_menu.id
 AND owned_role.deleted = b'0'
WHERE perm.button_permission = 'erp:purchase-order:print'
  AND NOT EXISTS (
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
  ON button_menu.permission IN ('erp:purchase-order:print', 'erp:purchase-order:print-template')
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

DROP TEMPORARY TABLE IF EXISTS tmp_erp_purchase_order_print_permission;
