-- ERP 银行转账新增页字段补齐（v182）
-- 1. 补齐汇率、手续费、费用项目字段。
-- 2. 补齐字段权限/字段配置定义。
-- 3. 初始化银行转账费用项目基础资料。

SET @tenant_id := 1;

DROP PROCEDURE IF EXISTS add_erp_finance_transfer_add_page_fields_v182;

DELIMITER //
CREATE PROCEDURE add_erp_finance_transfer_add_page_fields_v182()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_finance_transfer'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'erp_finance_transfer'
              AND COLUMN_NAME = 'exchange_rate'
        ) THEN
            ALTER TABLE `erp_finance_transfer`
                ADD COLUMN `exchange_rate` DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率'
                    AFTER `transfer_price`;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'erp_finance_transfer'
              AND COLUMN_NAME = 'fee_price'
        ) THEN
            ALTER TABLE `erp_finance_transfer`
                ADD COLUMN `fee_price` DECIMAL(24, 6) NOT NULL DEFAULT 0 COMMENT '手续费'
                    AFTER `exchange_rate`;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'erp_finance_transfer'
              AND COLUMN_NAME = 'fee_expense_category'
        ) THEN
            ALTER TABLE `erp_finance_transfer`
                ADD COLUMN `fee_expense_category` VARCHAR(64) NULL COMMENT '费用项目'
                    AFTER `fee_price`;
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_field_config'
    ) THEN
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
              AND COLUMN_NAME = 'field_group'
        ) THEN
            ALTER TABLE `erp_field_config`
                ADD COLUMN `field_group` varchar(64) NULL COMMENT '字段分组'
                    AFTER `sort`;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'erp_field_config'
              AND COLUMN_NAME = 'field_source'
        ) THEN
            ALTER TABLE `erp_field_config`
                ADD COLUMN `field_source` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT '字段来源'
                    AFTER `field_group`;
        END IF;
    END IF;
END //
DELIMITER ;

CALL add_erp_finance_transfer_add_page_fields_v182();
DROP PROCEDURE IF EXISTS add_erp_finance_transfer_add_page_fields_v182;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.module, seed.field_key, seed.field_label, seed.field_group, seed.sort,
       '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'erp_finance_transfer' AS module, 'voucherNo' AS field_key, '凭证号' AS field_label, 'main_form' AS field_group, 25 AS sort
    UNION ALL SELECT 'erp_finance_transfer', 'exchangeRate', '汇率', 'main_form', 65
    UNION ALL SELECT 'erp_finance_transfer', 'feePrice', '手续费', 'main_form', 75
    UNION ALL SELECT 'erp_finance_transfer', 'feeExpenseCategory', '费用项目', 'main_form', 76
    UNION ALL SELECT 'erp_finance_transfer', 'outAccountBankName', '转出开户行', 'main_form', 41
    UNION ALL SELECT 'erp_finance_transfer', 'outAccountBankAccount', '转出银行账号', 'main_form', 42
    UNION ALL SELECT 'erp_finance_transfer', 'inAccountBalance', '转入账户余额', 'main_form', 51
    UNION ALL SELECT 'erp_finance_transfer', 'inAccountBankName', '转入开户行', 'main_form', 52
    UNION ALL SELECT 'erp_finance_transfer', 'inAccountBankAccount', '转入银行账号', 'main_form', 53
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_field_definition` existing
    WHERE existing.`module` = seed.module
      AND existing.`field_key` = seed.field_key
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);

UPDATE `system_field_definition`
SET `field_label` = CASE `field_key`
        WHEN 'no' THEN '单据编号'
        WHEN 'financeUserId' THEN '经手人'
        WHEN 'outAccountBalance' THEN '转出账户余额'
        ELSE `field_label`
    END,
    `updater` = '1',
    `update_time` = NOW()
WHERE `module` = 'erp_finance_transfer'
  AND `field_key` IN ('no', 'financeUserId', 'outAccountBalance')
  AND `tenant_id` = @tenant_id
  AND `deleted` = b'0';

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT seed.module_key, seed.field_name, seed.field_label, seed.required, b'1', seed.sort, seed.field_group,
       'SYSTEM', @tenant_id, '1', NOW(), '1', NOW(), b'0'
FROM (
    SELECT 'erp_finance_transfer' AS module_key, 'voucherNo' AS field_name, '凭证号' AS field_label, b'0' AS required, 25 AS sort, 'base_info' AS field_group
    UNION ALL SELECT 'erp_finance_transfer', 'exchangeRate', '汇率', b'1', 65, 'finance_info'
    UNION ALL SELECT 'erp_finance_transfer', 'feePrice', '手续费', b'0', 75, 'finance_info'
    UNION ALL SELECT 'erp_finance_transfer', 'feeExpenseCategory', '费用项目', b'0', 76, 'finance_info'
    UNION ALL SELECT 'erp_finance_transfer', 'outAccountBankName', '转出开户行', b'0', 41, 'finance_info'
    UNION ALL SELECT 'erp_finance_transfer', 'outAccountBankAccount', '转出银行账号', b'0', 42, 'finance_info'
    UNION ALL SELECT 'erp_finance_transfer', 'inAccountBalance', '转入账户余额', b'0', 51, 'finance_info'
    UNION ALL SELECT 'erp_finance_transfer', 'inAccountBankName', '转入开户行', b'0', 52, 'finance_info'
    UNION ALL SELECT 'erp_finance_transfer', 'inAccountBankAccount', '转入银行账号', b'0', 53, 'finance_info'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_field_config` existing
    WHERE existing.`module_key` = seed.module_key
      AND existing.`field_name` = seed.field_name
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);

UPDATE `erp_field_config`
SET `field_label` = CASE `field_name`
        WHEN 'no' THEN '单据编号'
        WHEN 'financeUserId' THEN '经手人'
        WHEN 'outAccountBalance' THEN '转出账户余额'
        ELSE `field_label`
    END,
    `required` = CASE `field_name`
        WHEN 'financeUserId' THEN b'1'
        WHEN 'deptId' THEN b'1'
        ELSE `required`
    END,
    `updater` = '1',
    `update_time` = NOW()
WHERE `module_key` = 'erp_finance_transfer'
  AND `field_name` IN ('no', 'financeUserId', 'deptId', 'outAccountBalance')
  AND `tenant_id` = @tenant_id
  AND `deleted` = b'0';

INSERT INTO `erp_base_data`
(`type`, `name`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'bank_transfer_fee_expense_category' AS type, '银行手续费' AS name, 10 AS sort, '银行转账费用项目初始化' AS remark
    UNION ALL SELECT 'bank_transfer_fee_expense_category', '跨行手续费', 20, '银行转账费用项目初始化'
    UNION ALL SELECT 'bank_transfer_fee_expense_category', '其他手续费', 30, '银行转账费用项目初始化'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`name` = seed.name
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);
