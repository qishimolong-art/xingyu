-- ERP supplier allow-multi-dept field.
-- Safe to rerun on MySQL 5.7 / 8.0.x.

DROP PROCEDURE IF EXISTS add_erp_supplier_allow_multi_dept_v72;

DELIMITER //
CREATE PROCEDURE add_erp_supplier_allow_multi_dept_v72()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_supplier'
    ) AND NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_supplier'
           AND COLUMN_NAME = 'allow_multi_dept'
    ) THEN
        ALTER TABLE `erp_supplier`
            ADD COLUMN `allow_multi_dept` bit(1) NOT NULL DEFAULT b'0' COMMENT '允许多部门' AFTER `dept_id`;
    END IF;
END //
DELIMITER ;

CALL add_erp_supplier_allow_multi_dept_v72();
DROP PROCEDURE IF EXISTS add_erp_supplier_allow_multi_dept_v72;

CREATE TABLE IF NOT EXISTS `erp_supplier_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_supplier_dept` (`supplier_id`, `dept_id`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 供应商适用部门';

INSERT IGNORE INTO `erp_supplier_dept`
(`supplier_id`, `dept_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `id`, `dept_id`, IFNULL(`creator`, '1'), NOW(), IFNULL(`updater`, '1'), NOW(), b'0', `tenant_id`
  FROM `erp_supplier`
 WHERE `allow_multi_dept` = b'1'
   AND `dept_id` IS NOT NULL
   AND `deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_supplier', 'allowMultiDept', '允许多部门', 'basic_info', 35, '1', NOW(), '1', NOW(), b'0', 1),
('erp_supplier', 'deptIds', '适用部门', 'basic_info', 36, '1', NOW(), '1', NOW(), b'0', 1)
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
('supplier', 'allowMultiDept', '允许多部门', b'0', b'1', 35, '1', NOW(), '1', NOW(), b'0', 1),
('supplier', 'deptIds', '适用部门', b'0', b'1', 36, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `required` = VALUES(`required`),
  `visible` = VALUES(`visible`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
