-- ERP 其他应收/其他应付独立业务表（v216）
-- 背景：
--   1. 原“其他应收/其他应付”语义调整为“应收调账/应付调账”。
--   2. 本脚本新增独立主表 erp_receivable_misc / erp_payable_misc，后续“其他应收/付”指向新表。
-- 安全性：
--   - 旧表 erp_receivable_other / erp_payable_other 不重命名、不迁移数据，只定向更新中文菜单文案。
--   - SQL 不删除业务数据、不覆盖角色权限；新菜单权限仅按已有父菜单/超级管理员追加授权。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @tenant_id := 1;

CREATE TABLE IF NOT EXISTS `erp_receivable_misc` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `no` varchar(64) NOT NULL COMMENT '单据编号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 10=待审核 20=已审核',
  `biz_time` datetime NOT NULL COMMENT '业务时间',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `account_id` bigint DEFAULT NULL COMMENT '账户ID',
  `amount` decimal(24,6) NOT NULL COMMENT '金额',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `file_url` varchar(512) DEFAULT NULL COMMENT '附件URL',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `handler_id` bigint DEFAULT NULL COMMENT '经手人用户ID',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_status_biz_time` (`status`, `biz_time`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_handler_id` (`handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 其他应收单';

CREATE TABLE IF NOT EXISTS `erp_payable_misc` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `no` varchar(64) NOT NULL COMMENT '单据编号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 10=待审核 20=已审核',
  `biz_time` datetime NOT NULL COMMENT '业务时间',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `account_id` bigint DEFAULT NULL COMMENT '账户ID',
  `amount` decimal(24,6) NOT NULL COMMENT '金额',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `file_url` varchar(512) DEFAULT NULL COMMENT '附件URL',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `handler_id` bigint DEFAULT NULL COMMENT '经手人用户ID',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_status_biz_time` (`status`, `biz_time`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_handler_id` (`handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 其他应付单';

-- 旧“其他应收/其他应付”菜单只改中文名，权限码与接口保持不变。
UPDATE `system_menu`
SET `name` = REPLACE(`name`, '其他应收', '应收调账'),
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`permission` LIKE 'erp:receivable-other:%' OR `component` = 'erp/finance/receivable/other-receivable/index')
  AND `name` LIKE '%其他应收%';

UPDATE `system_menu`
SET `name` = REPLACE(`name`, '其他应付', '应付调账'),
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`permission` LIKE 'erp:payable-other:%' OR `component` = 'erp/finance/payable/other/index')
  AND `name` LIKE '%其他应付%';

SET @receivable_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `id` = 3100
      OR `component_name` = 'ErpReceivable'
      OR (`path` = 'receivable' AND `parent_id` = 2645)
      OR EXISTS (
        SELECT 1
        FROM `system_menu` receivable_child
        WHERE receivable_child.`deleted` = b'0'
          AND receivable_child.`parent_id` = `system_menu`.`id`
          AND receivable_child.`component` IN (
            'erp/finance/receivable/account/index',
            'erp/finance/receivable/other-receivable/index',
            'erp/finance/receivable/other-income/index',
            'erp/finance/receivable/report/index'
          )
      )
    )
  ORDER BY (`id` = 3100) DESC, (`component_name` = 'ErpReceivable') DESC, `id` DESC
  LIMIT 1
);
SET @payable_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (`component_name` = 'ErpPayable' OR `path` = 'payable')
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '其他应收', '', 2, 55, @receivable_parent_id, 'misc', 'ep:money', 'erp/finance/receivable/misc/index', 'ErpReceivableMisc', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @receivable_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `component_name` = 'ErpReceivableMisc');

SET @receivable_misc_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0' AND `component_name` = 'ErpReceivableMisc'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT child_name, permission, 3, sort_no, @receivable_misc_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '其他应收查询' AS child_name, 'erp:receivable-misc:query' AS permission, 1 AS sort_no
  UNION ALL SELECT '其他应收创建', 'erp:receivable-misc:create', 2
  UNION ALL SELECT '其他应收修改', 'erp:receivable-misc:update', 3
  UNION ALL SELECT '其他应收删除', 'erp:receivable-misc:delete', 4
  UNION ALL SELECT '其他应收审核', 'erp:receivable-misc:update-status', 5
  UNION ALL SELECT '其他应收导出', 'erp:receivable-misc:export', 6
) t
WHERE @receivable_misc_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` sm WHERE sm.`deleted` = b'0' AND sm.`permission` = t.permission);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '其他应付', '', 2, 55, @payable_parent_id, 'misc', 'ep:money', 'erp/finance/payable/misc/index', 'ErpPayableMisc', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @payable_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `component_name` = 'ErpPayableMisc');

SET @payable_misc_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0' AND `component_name` = 'ErpPayableMisc'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT child_name, permission, 3, sort_no, @payable_misc_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '其他应付查询' AS child_name, 'erp:payable-misc:query' AS permission, 1 AS sort_no
  UNION ALL SELECT '其他应付创建', 'erp:payable-misc:create', 2
  UNION ALL SELECT '其他应付修改', 'erp:payable-misc:update', 3
  UNION ALL SELECT '其他应付删除', 'erp:payable-misc:delete', 4
  UNION ALL SELECT '其他应付审核', 'erp:payable-misc:update-status', 5
  UNION ALL SELECT '其他应付导出', 'erp:payable-misc:export', 6
) t
WHERE @payable_misc_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` sm WHERE sm.`deleted` = b'0' AND sm.`permission` = t.permission);

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_menu_targets_v216`;
CREATE TEMPORARY TABLE `tmp_erp_misc_menu_targets_v216` (`menu_id` bigint NOT NULL PRIMARY KEY);

INSERT IGNORE INTO `tmp_erp_misc_menu_targets_v216` (`menu_id`)
SELECT `id` FROM `system_menu`
WHERE `deleted` = b'0'
  AND (`component_name` IN ('ErpReceivableMisc', 'ErpPayableMisc')
    OR `permission` LIKE 'erp:receivable-misc:%'
    OR `permission` LIKE 'erp:payable-misc:%');

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_package_append_v216`;
CREATE TEMPORARY TABLE `tmp_erp_misc_package_append_v216` (
  `package_id` bigint NOT NULL PRIMARY KEY,
  `missing_menu_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO `tmp_erp_misc_package_append_v216` (`package_id`, `missing_menu_ids`)
SELECT package_menu.`package_id`, JSON_ARRAYAGG(package_menu.`menu_id`)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.`menu_id`
  FROM `system_tenant_package` tenant_package
  JOIN `tmp_erp_misc_menu_targets_v216` target ON TRUE
  WHERE tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND CHAR_LENGTH(tenant_package.`menu_ids`) < 4000
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@receivable_parent_id AS CHAR), '$')
      OR JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@payable_parent_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` owned_child
        WHERE owned_child.`deleted` = b'0'
          AND owned_child.`parent_id` IN (@receivable_parent_id, @payable_parent_id)
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(owned_child.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.`menu_id` AS CHAR), '$')
) package_menu
GROUP BY package_menu.`package_id`;

UPDATE `system_tenant_package` tenant_package
JOIN `tmp_erp_misc_package_append_v216` package_append
  ON package_append.`package_id` = tenant_package.`id`
SET tenant_package.`menu_ids` = JSON_MERGE_PRESERVE(tenant_package.`menu_ids`, package_append.`missing_menu_ids`),
    tenant_package.`update_time` = NOW()
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND JSON_VALID(package_append.`missing_menu_ids`)
  AND CHAR_LENGTH(tenant_package.`menu_ids`) + CHAR_LENGTH(package_append.`missing_menu_ids`) < 4000;

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_package_append_v216`;

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, t.`menu_id`, '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `tmp_erp_misc_menu_targets_v216` t
JOIN `system_menu` m ON m.`id` = t.`menu_id` AND m.`deleted` = b'0'
JOIN `system_role_menu` rm ON rm.`deleted` = b'0'
  AND (
    rm.`menu_id` IN (@receivable_parent_id, @payable_parent_id)
    OR rm.`menu_id` IN (
      SELECT `id` FROM `system_menu`
      WHERE `deleted` = b'0'
        AND (`permission` LIKE 'erp:receivable-report:%' OR `permission` LIKE 'erp:payable-report:%')
    )
  )
WHERE NOT EXISTS (
  SELECT 1 FROM `system_role_menu` exists_rm
  WHERE exists_rm.`deleted` = b'0'
    AND exists_rm.`role_id` = rm.`role_id`
    AND exists_rm.`menu_id` = t.`menu_id`
    AND exists_rm.`tenant_id` = rm.`tenant_id`
);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.`id`, t.`menu_id`, '1', NOW(), '1', NOW(), b'0', r.`tenant_id`
FROM `system_role` r
CROSS JOIN `tmp_erp_misc_menu_targets_v216` t
WHERE r.`deleted` = b'0'
  AND r.`code` = 'super_admin'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` exists_rm
    WHERE exists_rm.`deleted` = b'0'
      AND exists_rm.`role_id` = r.`id`
      AND exists_rm.`menu_id` = t.`menu_id`
      AND exists_rm.`tenant_id` = r.`tenant_id`
  );

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_menu_targets_v216`;

INSERT INTO `system_field_definition` (`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT module_name, field_key, field_label, field_group, sort_no, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
  SELECT 'erp_finance_receivable_misc' AS module_name, 'no' AS field_key, '单号' AS field_label, 'base_info' AS field_group, 1 AS sort_no
  UNION ALL SELECT 'erp_finance_receivable_misc', 'bizTime', '日期', 'base_info', 2
  UNION ALL SELECT 'erp_finance_receivable_misc', 'customerId', '客户', 'base_info', 3
  UNION ALL SELECT 'erp_finance_receivable_misc', 'accountId', '账户', 'base_info', 4
  UNION ALL SELECT 'erp_finance_receivable_misc', 'deptId', '所属部门', 'base_info', 5
  UNION ALL SELECT 'erp_finance_receivable_misc', 'amount', '金额', 'base_info', 6
  UNION ALL SELECT 'erp_finance_receivable_misc', 'remark', '备注', 'base_info', 7
  UNION ALL SELECT 'erp_finance_receivable_misc', 'fileUrl', '附件', 'base_info', 8
  UNION ALL SELECT 'erp_finance_receivable_misc', 'handlerId', '经手人', 'base_info', 9
  UNION ALL SELECT 'erp_finance_payable_misc', 'no', '单号', 'base_info', 1
  UNION ALL SELECT 'erp_finance_payable_misc', 'bizTime', '日期', 'base_info', 2
  UNION ALL SELECT 'erp_finance_payable_misc', 'supplierId', '供应商', 'base_info', 3
  UNION ALL SELECT 'erp_finance_payable_misc', 'accountId', '账户', 'base_info', 4
  UNION ALL SELECT 'erp_finance_payable_misc', 'deptId', '所属部门', 'base_info', 5
  UNION ALL SELECT 'erp_finance_payable_misc', 'amount', '金额', 'base_info', 6
  UNION ALL SELECT 'erp_finance_payable_misc', 'remark', '备注', 'base_info', 7
  UNION ALL SELECT 'erp_finance_payable_misc', 'fileUrl', '附件', 'base_info', 8
  UNION ALL SELECT 'erp_finance_payable_misc', 'handlerId', '经手人', 'base_info', 9
) fields
WHERE NOT EXISTS (
  SELECT 1 FROM `system_field_definition` d
  WHERE d.`deleted` = b'0'
    AND d.`tenant_id` = @tenant_id
    AND d.`module` = fields.module_name
    AND d.`field_key` = fields.field_key
);
