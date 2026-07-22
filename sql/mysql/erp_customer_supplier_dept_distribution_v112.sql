-- ERP customer/supplier department distribution (v112).
-- Safe to rerun. This script only appends columns, relation table, field definitions
-- and button permissions; it does not delete or overwrite existing role/menu data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_customer_allow_multi_dept_v112;

DELIMITER //
CREATE PROCEDURE add_erp_customer_allow_multi_dept_v112()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_customer'
           AND COLUMN_NAME = 'allow_multi_dept'
    ) THEN
        ALTER TABLE `erp_customer`
            ADD COLUMN `allow_multi_dept` bit(1) NOT NULL DEFAULT b'0' COMMENT '允许多部门' AFTER `dept_id`;
    END IF;
END //
DELIMITER ;

CALL add_erp_customer_allow_multi_dept_v112();
DROP PROCEDURE IF EXISTS add_erp_customer_allow_multi_dept_v112;

CREATE TABLE IF NOT EXISTS `erp_customer_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `customer_id` bigint NOT NULL COMMENT '客户编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_customer_dept` (`customer_id`, `dept_id`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 客户适用部门';

INSERT IGNORE INTO `erp_customer_dept`
(`customer_id`, `dept_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `id`, `dept_id`, IFNULL(`creator`, '1'), NOW(), IFNULL(`updater`, '1'), NOW(), b'0', `tenant_id`
  FROM `erp_customer`
 WHERE `allow_multi_dept` = b'1'
   AND `dept_id` IS NOT NULL
   AND `deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_customer', 'allowMultiDept', '允许多部门', 'sale_info', 521, '1', NOW(), '1', NOW(), b'0', 1),
('erp_customer', 'deptIds', '适用部门', 'sale_info', 522, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('customer', 'allowMultiDept', '允许多部门', b'0', b'1', 521, '1', NOW(), '1', NOW(), b'0', 1),
('customer', 'deptIds', '适用部门', b'0', b'1', 522, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `required` = VALUES(`required`),
  `visible` = VALUES(`visible`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_dept_distribution_permission;

CREATE TEMPORARY TABLE tmp_erp_dept_distribution_permission (
  archive_type varchar(32) NOT NULL,
  parent_permission varchar(128) NOT NULL,
  inherit_permission varchar(128) NOT NULL,
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_dept_distribution_permission
(`archive_type`, `parent_permission`, `inherit_permission`, `button_name`, `button_permission`, `sort`)
VALUES
('supplier', 'erp:supplier:query', 'erp:supplier:update', '供应商部门分配', 'erp:supplier:dept-distribute', 8),
('customer', 'erp:customer:query', 'erp:customer:update', '客户部门分配', 'erp:customer:dept-distribute', 8);

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
FROM tmp_erp_dept_distribution_permission perm
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
FROM tmp_erp_dept_distribution_permission perm
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
  ON button_menu.permission IN ('erp:supplier:dept-distribute', 'erp:customer:dept-distribute')
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

DROP TEMPORARY TABLE IF EXISTS tmp_erp_dept_distribution_permission;
