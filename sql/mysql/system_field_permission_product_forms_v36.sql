-- Product management form field permission definitions.
-- Scope: form fields only. List columns and search fields are intentionally excluded.

CREATE TABLE IF NOT EXISTS `system_field_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `module` VARCHAR(50) NOT NULL COMMENT 'Module key, for example erp_product',
  `field_key` VARCHAR(100) NOT NULL COMMENT 'Field key',
  `field_label` VARCHAR(100) NOT NULL COMMENT 'Field label',
  `field_group` VARCHAR(50) DEFAULT NULL COMMENT 'Field group',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort',
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_field` (`module`, `field_key`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System field definition';

CREATE TABLE IF NOT EXISTS `system_role_field_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `role_id` BIGINT NOT NULL COMMENT 'Role id',
  `field_id` BIGINT NOT NULL COMMENT 'Field definition id',
  `hidden` BIT(1) NOT NULL DEFAULT b'1' COMMENT 'Whether hidden',
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_field` (`role_id`, `field_id`, `tenant_id`, `deleted`),
  KEY `idx_field_id` (`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System role field permission';

-- Remove old list-column/search-field permissions and definitions for product management forms.
DELETE rfp
FROM `system_role_field_permission` rfp
JOIN `system_field_definition` fd ON fd.id = rfp.field_id
WHERE fd.module IN ('erp_product', 'erp_product_category', 'erp_product_unit', 'erp_price_system')
  AND (
    fd.field_group IN ('list_col', 'search_info')
    OR fd.field_key LIKE 'col\_%'
    OR fd.field_key LIKE 'search\_%'
  );

DELETE FROM `system_field_definition`
WHERE `module` IN ('erp_product', 'erp_product_category', 'erp_product_unit', 'erp_price_system')
  AND (
    `field_group` IN ('list_col', 'search_info')
    OR `field_key` LIKE 'col\_%'
    OR `field_key` LIKE 'search\_%'
  );

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_product', 'code', '配件编码', 'base_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'manualInputCode', '手动输入', 'base_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'name', '配件名称', 'base_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'unitId', '单位', 'base_info', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'defaultWarehouseId', '默认仓库', 'base_info', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'vehicleModel', '适用车型', 'base_info', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'standard', '规格', 'base_info', 70, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'categoryId', '配件分类', 'base_info', 80, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'batchNoEnabled', '是否开启批次号管理', 'base_info', 85, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'barCode', '条形码', 'base_info', 90, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'factoryCode', '厂家编码', 'base_info', 100, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'status', '状态', 'base_info', 110, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'remark', '备注', 'base_info', 120, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'purchasePrice', '采购价', 'price_info', 1, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'salePrice', '销售价', 'price_info', 2, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'minPrice', '最低价', 'price_info', 3, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'referencePrice', '参考价', 'price_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'retailPrice', '零售价', 'price_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'lastPurchasePrice', '最后采购入库价', 'price_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'grossProfitRate', '毛利率（%）', 'price_info', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'backupPrice1', '备用价1', 'price_info', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'wholesalePrice', '批发价', 'price_info', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'sharePrice', '股份价', 'price_info', 70, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'stockMax', '库存上限', 'extend_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'stockMin', '库存下限', 'extend_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'stockStandard', '标准库存', 'extend_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'packageQty', '包装数', 'extend_info', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'weight', '重量（kg）', 'extend_info', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'currentStock', '当前库存', 'stock_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'inTransitStock', '在途数量', 'stock_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'availableStock', '可用库存', 'stock_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_sharePrice', '列表-股份价', 'list_col', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_category', 'parentId', '上级分类', 'base_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_category', 'name', '分类名称', 'base_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_category', 'code', '分类编码', 'base_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_category', 'sort', '显示顺序', 'base_info', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_category', 'status', '状态', 'base_info', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_unit', 'name', '单位名称', 'base_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product_unit', 'status', '单位状态', 'base_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_price_system', 'code', '价格体系编码', 'base_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_price_system', 'name', '价格体系名称', 'base_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_price_system', 'status', '状态', 'base_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_price_system', 'sort', '排序', 'base_info', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_price_system', 'remark', '备注', 'base_info', 50, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31137, '设置角色字段权限', 'system:permission:assign-role-field-permission', 3, 8, 101, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31137, '1', NOW(), '1', NOW(), b'0', 1);
