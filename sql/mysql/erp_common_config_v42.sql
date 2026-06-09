-- ERP common configuration and safe column patch.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

CREATE TABLE IF NOT EXISTS `erp_search_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `module_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块标识',
  `field_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字段名',
  `field_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '字段标签',
  `component` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组件类型',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tenant_module_field` (`tenant_id`, `module_key`, `field_name`, `deleted`) USING BTREE,
  KEY `idx_module_key` (`tenant_id`, `module_key`, `deleted`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP 搜索字段配置';

-- 搜索字段配置复用“ERP / 系统配置 / 字段配置”菜单节点，仅补充独立后端权限点。
DELETE FROM `system_role_menu` WHERE `menu_id` IN (2964, 2965);
DELETE FROM `system_menu` WHERE `id` IN (2964, 2965);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2964, '搜索字段配置查询', 'erp:search-field-config:query', 3, 3, 2961, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(2965, '搜索字段配置更新', 'erp:search-field-config:update', 3, 4, 2961, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 2964, '1', NOW(), '1', NOW(), b'0', 1),
(1, 2965, '1', NOW(), '1', NOW(), b'0', 1);

UPDATE `system_menu`
SET `component` = 'erp/config/search/index',
    `component_name` = 'ErpSearchFieldConfig',
    `permission` = 'erp:search-field-config:query',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2961 AND `deleted` = b'0';

CREATE TABLE IF NOT EXISTS `erp_shortcut_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `module_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块标识',
  `action_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作标识',
  `key_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '快捷键编码',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tenant_module_action` (`tenant_id`, `module_key`, `action_key`, `deleted`) USING BTREE,
  KEY `idx_module_key` (`tenant_id`, `module_key`, `deleted`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'ERP 快捷键配置';

DROP PROCEDURE IF EXISTS add_erp_fee_amount_column;

DELIMITER //
CREATE PROCEDURE add_erp_fee_amount_column(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'fee_amount'
    ) THEN
        SET @addColumnSql = CONCAT('ALTER TABLE `', tableName,
                                   '` ADD COLUMN `fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''费用金额''');
        PREPARE stmt FROM @addColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_erp_fee_amount_column('erp_purchase_order');
CALL add_erp_fee_amount_column('erp_purchase_in');
CALL add_erp_fee_amount_column('erp_purchase_return');
CALL add_erp_fee_amount_column('erp_sale_order');
CALL add_erp_fee_amount_column('erp_sale_out');
CALL add_erp_fee_amount_column('erp_sale_return');
CALL add_erp_fee_amount_column('erp_sale_quote');
CALL add_erp_fee_amount_column('erp_sale_cart');

DROP PROCEDURE IF EXISTS add_erp_fee_amount_column;

DROP PROCEDURE IF EXISTS add_erp_common_dept_column;

DELIMITER //
CREATE PROCEDURE add_erp_common_dept_column(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
        ) THEN
            SET @addColumnSql = CONCAT('ALTER TABLE `', tableName,
                                       '` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''所属部门''');
            PREPARE stmt FROM @addColumnSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
        ) AND NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND INDEX_NAME = 'idx_dept_id'
        ) THEN
            SET @addIndexSql = CONCAT('CREATE INDEX `idx_dept_id` ON `', tableName, '` (`dept_id`)');
            PREPARE stmt FROM @addIndexSql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //
DELIMITER ;

-- Scope follows ErpDataPermissionConfiguration dept-column tables. This script only fills missing
-- columns/indexes and intentionally does not backfill or overwrite historical dept_id values.
CALL add_erp_common_dept_column('erp_product');
CALL add_erp_common_dept_column('erp_product_category');
CALL add_erp_common_dept_column('erp_product_price_system');
CALL add_erp_common_dept_column('erp_product_unit');
CALL add_erp_common_dept_column('erp_product_universal');
CALL add_erp_common_dept_column('erp_price_system');
CALL add_erp_common_dept_column('erp_purchase_in');
CALL add_erp_common_dept_column('erp_purchase_invoice');
CALL add_erp_common_dept_column('erp_purchase_order');
CALL add_erp_common_dept_column('erp_purchase_price_adjust');
CALL add_erp_common_dept_column('erp_purchase_return');
CALL add_erp_common_dept_column('erp_purchase_suggestion');
CALL add_erp_common_dept_column('erp_supplier');
CALL add_erp_common_dept_column('erp_supplier_contact');
CALL add_erp_common_dept_column('erp_customer');
CALL add_erp_common_dept_column('erp_customer_contact');
CALL add_erp_common_dept_column('erp_sale_cart');
CALL add_erp_common_dept_column('erp_sale_order');
CALL add_erp_common_dept_column('erp_sale_out');
CALL add_erp_common_dept_column('erp_sale_price_adjust');
CALL add_erp_common_dept_column('erp_sale_quote');
CALL add_erp_common_dept_column('erp_sale_return');
CALL add_erp_common_dept_column('erp_stock_check');
CALL add_erp_common_dept_column('erp_stock_in');
CALL add_erp_common_dept_column('erp_stock_move');
CALL add_erp_common_dept_column('erp_stock_out');
CALL add_erp_common_dept_column('erp_stock_record');
CALL add_erp_common_dept_column('erp_warehouse');
CALL add_erp_common_dept_column('erp_chain_order');
CALL add_erp_common_dept_column('erp_finance_payment');
CALL add_erp_common_dept_column('erp_finance_receipt');
CALL add_erp_common_dept_column('erp_finance_transfer');
CALL add_erp_common_dept_column('erp_other_payable');
CALL add_erp_common_dept_column('erp_payable_expense');
CALL add_erp_common_dept_column('erp_payable_expense_item');
CALL add_erp_common_dept_column('erp_payable_other');
CALL add_erp_common_dept_column('erp_pre_payment');
CALL add_erp_common_dept_column('erp_other_receivable');
CALL add_erp_common_dept_column('erp_pre_receipt');
CALL add_erp_common_dept_column('erp_pre_receivable');
CALL add_erp_common_dept_column('erp_receivable_other');
CALL add_erp_common_dept_column('erp_receivable_other_income');
CALL add_erp_common_dept_column('erp_receivable_other_income_item');
CALL add_erp_common_dept_column('erp_receivable_other_item');
CALL add_erp_common_dept_column('erp_voucher');
CALL add_erp_common_dept_column('erp_voucher_attribution');

DROP PROCEDURE IF EXISTS add_erp_common_dept_column;
