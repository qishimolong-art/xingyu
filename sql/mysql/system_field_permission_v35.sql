-- System field permission v35
-- Blacklist mode: records in system_role_field_permission mean hidden fields.
-- Current field hierarchy in the role permission UI: 产品管理 > 配件信息 > 字段分组 > 字段.

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

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_product', 'code', '配件编码', 'base_info', 5, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'name', '零件名称', 'base_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'unitId', '单位', 'base_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'defaultWarehouseId', '默认仓库', 'base_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'vehicleModel', '适用车型', 'base_info', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'standard', '规格', 'base_info', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'categoryId', '类别', 'base_info', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'barCode', '条形码', 'base_info', 70, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'factoryCode', '厂家编码', 'base_info', 80, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'status', '状态', 'base_info', 90, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'remark', '备注', 'base_info', 100, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'brand', '品牌', 'base_info', 110, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'oeNumber', 'OE号', 'base_info', 120, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'originPlace', '产地', 'base_info', 130, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'purchasePrice', '采购价', 'price_info', 1, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'salePrice', '销售价', 'price_info', 2, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'minPrice', '最低价', 'price_info', 3, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'referencePrice', '参考价', 'price_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'retailPrice', '零售价', 'price_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'lastPurchasePrice', '最后采购入库价', 'price_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'grossProfitRate', '毛利率（%）', 'price_info', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'backupPrice1', '备用价1', 'price_info', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'wholesalePrice', '批发价', 'price_info', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'stockMax', '库存上限', 'extend_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'stockMin', '库存下限', 'extend_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'stockStandard', '标准库存', 'extend_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'packageQty', '包装数', 'extend_info', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'weight', '重量（kg）', 'extend_info', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_code', '列表-配件编码', 'list_col', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_name', '列表-零件名称', 'list_col', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_vehicleModel', '列表-适用车型', 'list_col', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_standard', '列表-规格', 'list_col', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_categoryName', '列表-类别', 'list_col', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_unitName', '列表-单位', 'list_col', 60, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_barCode', '列表-条形码', 'list_col', 70, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_factoryCode', '列表-厂家编码', 'list_col', 80, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_defaultWarehouseName', '列表-默认仓库', 'list_col', 90, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_retailPrice', '列表-零售价', 'list_col', 100, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_referencePrice', '列表-参考价', 'list_col', 110, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_currentStock', '列表-当前库存', 'list_col', 120, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_status', '列表-状态', 'list_col', 130, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_createTime', '列表-创建时间', 'list_col', 140, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_lockCount', '列表-占用数量', 'list_col', 150, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_purchasePrice', '列表-采购价', 'list_col', 160, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_remark', '列表-零件备注', 'list_col', 170, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_weight', '列表-重量', 'list_col', 180, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_backupPrice1', '列表-备用价1', 'list_col', 190, '1', NOW(), '1', NOW(), b'0', 1),
('erp_product', 'col_wholesalePrice', '列表-批发价', 'list_col', 200, '1', NOW(), '1', NOW(), b'0', 1)
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
