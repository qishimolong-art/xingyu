-- ERP product multi-department relation.
-- Safe to rerun on MySQL 5.7 / 8.0.x.

CREATE TABLE IF NOT EXISTS `erp_product_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `product_id` bigint NOT NULL COMMENT '配件编号',
  `dept_id` bigint NOT NULL COMMENT '部门编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_product_dept` (`product_id`, `dept_id`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 配件所属部门';

INSERT IGNORE INTO `erp_product_dept`
(`product_id`, `dept_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `id`, `dept_id`, IFNULL(`creator`, '1'), NOW(), IFNULL(`updater`, '1'), NOW(), b'0', `tenant_id`
  FROM `erp_product`
 WHERE `dept_id` IS NOT NULL
   AND `deleted` = b'0';

-- Reuse the existing erp_product.deptId field permission for the multi-select UI.
-- If an earlier draft inserted deptIds into field permissions, remove it to avoid
-- showing two "所属部门" entries in role field-permission configuration.
DELETE rfp
  FROM `system_role_field_permission` rfp
  JOIN `system_field_definition` fd ON fd.`id` = rfp.`field_id`
 WHERE fd.`module` = 'erp_product'
   AND fd.`field_key` = 'deptIds';

DELETE FROM `system_field_definition`
 WHERE `module` = 'erp_product'
   AND `field_key` = 'deptIds';

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('product', 'deptIds', '所属部门', b'0', b'1', 31, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `required` = VALUES(`required`),
  `visible` = VALUES(`visible`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
