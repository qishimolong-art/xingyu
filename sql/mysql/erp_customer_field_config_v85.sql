-- ERP customer field-config seed fix v85.
-- Fixes the field-config page showing no rows for module_key = 'customer'.
-- Safe to execute repeatedly: only missing active tenant-1 rows are inserted.

DROP PROCEDURE IF EXISTS add_erp_customer_field_config_columns_v85;

DELIMITER //
CREATE PROCEDURE add_erp_customer_field_config_columns_v85()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'visible'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `visible` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否显示'
            AFTER `required`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_source'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_source` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'field source: SYSTEM/CUSTOM'
            AFTER `sort`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'physical_column'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `physical_column` varchar(64) NULL COMMENT 'custom physical column'
            AFTER `field_source`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_type'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_type` varchar(32) NULL COMMENT 'field type'
            AFTER `physical_column`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_group'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_group` varchar(64) NULL COMMENT 'field group'
            AFTER `field_type`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'component_type'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `component_type` varchar(64) NULL COMMENT 'frontend component type'
            AFTER `field_group`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'max_length'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `max_length` int NULL COMMENT 'max length'
            AFTER `component_type`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'decimal_precision'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `decimal_precision` int NULL COMMENT 'decimal precision'
            AFTER `max_length`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'decimal_scale'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `decimal_scale` int NULL COMMENT 'decimal scale'
            AFTER `decimal_precision`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'default_value'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `default_value` varchar(500) NULL COMMENT 'default value'
            AFTER `decimal_scale`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'list_visible'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `list_visible` bit(1) NOT NULL DEFAULT b'0' COMMENT 'list visible'
            AFTER `default_value`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'searchable'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `searchable` bit(1) NOT NULL DEFAULT b'0' COMMENT 'searchable'
            AFTER `list_visible`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'readonly'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `readonly` bit(1) NOT NULL DEFAULT b'0' COMMENT 'readonly'
            AFTER `searchable`;
    END IF;
END //
DELIMITER ;

CALL add_erp_customer_field_config_columns_v85();
DROP PROCEDURE IF EXISTS add_erp_customer_field_config_columns_v85;

UPDATE `erp_field_config`
   SET `field_source` = 'SYSTEM'
 WHERE (`field_source` IS NULL OR `field_source` = '')
   AND `deleted` = b'0';

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `list_visible`, `searchable`, `readonly`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module_key`, seed.`field_name`, seed.`field_label`, seed.`required`, b'1', seed.`sort`,
       seed.`field_group`, 'SYSTEM', b'0', b'0', seed.`readonly`, '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 'customer' module_key, 'code' field_name, '客户编码' field_label, b'0' required, 10 sort, 'base_info' field_group, b'0' readonly UNION ALL
    SELECT 'customer', 'manualInputCode', '手动填写', b'0', 20, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'name', '客户名称', b'1', 30, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'shortName', '简称', b'0', 40, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'relationType', '往来类别', b'1', 50, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'areaId', '区域', b'0', 60, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'customerType', '客户类型', b'0', 70, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'groupCustomer', '集团客户', b'0', 80, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'enterpriseMatchStatus', '企业匹配', b'0', 90, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'contact', '联系人', b'0', 100, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'telephone', '电话', b'0', 110, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'mobile', '手机号', b'1', 120, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'financeTelephone', '财务联系电话', b'0', 130, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'addressAreaId', '地址', b'0', 140, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'detailAddress', '详细地址', b'0', 150, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'postCode', '邮政编码', b'0', 160, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'deptId', '所属部门', b'0', 170, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'status', '状态', b'0', 180, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'remark', '备注', b'0', 190, 'base_info', b'0' UNION ALL
    SELECT 'customer', 'accountName', '账户', b'0', 210, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'settleMethod', '结算方式', b'1', 220, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'settleLocked', '结算锁定', b'0', 230, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'invoiceType', '开票类型', b'0', 240, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'minOrderAmount', '起订金额', b'0', 250, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'freightType', '运费类型', b'0', 260, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'commissionRate', '佣金比例', b'0', 270, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'commissionEnabled', '启用佣金机制', b'0', 280, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'creditLimit', '白条授信额度', b'0', 290, 'finance_info', b'0' UNION ALL
    SELECT 'customer', 'unifiedCreditCode', '统一信用代码', b'0', 310, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'taxNo', '纳税人识别号', b'0', 320, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'bankName', '开户行', b'0', 330, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'bankAccount', '开户账号', b'0', 340, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'bankAddress', '开户地址', b'0', 350, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'invoiceBankName', '开票银行', b'0', 360, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'invoiceBankAccount', '开票银行账号', b'0', 370, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'invoiceAddress', '开票地址', b'0', 380, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'invoiceTelephone', '开票电话', b'0', 390, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'invoiceCompany', '开票单位', b'0', 400, 'invoice_info', b'0' UNION ALL
    SELECT 'customer', 'saleUserId', '所属业务员', b'0', 410, 'sale_info', b'0' UNION ALL
    SELECT 'customer', 'developerUserId', '所属开发员', b'0', 420, 'sale_info', b'0' UNION ALL
    SELECT 'customer', 'priceLevel', '价格级别', b'1', 430, 'sale_info', b'0' UNION ALL
    SELECT 'customer', 'priceLocked', '价格锁定', b'0', 440, 'sale_info', b'0' UNION ALL
    SELECT 'customer', 'logistics', '是否物流', b'0', 450, 'sale_info', b'0' UNION ALL
    SELECT 'customer', 'transportMethod', '运输方式', b'0', 460, 'sale_info', b'0' UNION ALL
    SELECT 'customer', 'routeId', '线路', b'0', 470, 'sale_info', b'0' UNION ALL
    SELECT 'customer', 'freightExplainId', '运费说明', b'0', 480, 'sale_info', b'0' UNION ALL
    SELECT 'customer', 'wubiCode', '五笔码', b'0', 510, 'system_info', b'0' UNION ALL
    SELECT 'customer', 'pinyinCode', '拼音码', b'0', 520, 'system_info', b'0' UNION ALL
    SELECT 'customer', 'memberCode', '会员编码', b'0', 530, 'system_info', b'1' UNION ALL
    SELECT 'customer', 'platformCode', '平台唯一码', b'0', 540, 'system_info', b'1'
) seed
WHERE NOT EXISTS (
    SELECT 1
      FROM `erp_field_config` cfg
     WHERE cfg.`module_key` = seed.`module_key`
       AND cfg.`field_name` = seed.`field_name`
       AND cfg.`tenant_id` = 1
       AND cfg.`deleted` = b'0'
);
