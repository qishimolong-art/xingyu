-- ERP 其他收入字段类型与明细客户关联（v205）
-- 1. 明细表增加客户 ID，保留原 party 文本字段兼容历史数据。
-- 2. 初始化其他收入专属基础资料选项。
-- 3. 调整 settle_method 在其他收入中的期望显示顺序。

SET @tenant_id := 1;

DROP PROCEDURE IF EXISTS add_erp_receivable_other_income_customer_v205;

DELIMITER //
CREATE PROCEDURE add_erp_receivable_other_income_customer_v205()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_other_income_item'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_other_income_item'
          AND COLUMN_NAME = 'customer_id'
    ) THEN
        ALTER TABLE `erp_receivable_other_income_item`
            ADD COLUMN `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID'
                AFTER `party`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_other_income_item'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_receivable_other_income_item'
          AND INDEX_NAME = 'idx_customer_id'
    ) THEN
        ALTER TABLE `erp_receivable_other_income_item`
            ADD KEY `idx_customer_id` (`customer_id`);
    END IF;
END //
DELIMITER ;

CALL add_erp_receivable_other_income_customer_v205();
DROP PROCEDURE IF EXISTS add_erp_receivable_other_income_customer_v205;

INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'settle_method' AS type, '挂账' AS name, 'credit' AS code, 10 AS sort, '其他收入结算方式初始化' AS remark
    UNION ALL SELECT 'settle_method', '汇款', 'remittance', 20, '其他收入结算方式初始化'
    UNION ALL SELECT 'settle_method', '网上支付', 'online_payment', 30, '其他收入结算方式初始化'
    UNION ALL SELECT 'settle_method', '现金', 'cash', 40, '其他收入结算方式初始化'
    UNION ALL
    SELECT 'receivable_other_income_type' AS type, '支出' AS name, 'expense' AS code, 10 AS sort, '其他收入收入类型初始化' AS remark
    UNION ALL SELECT 'receivable_other_income_type', '成本', 'cost', 20, '其他收入收入类型初始化'
    UNION ALL SELECT 'receivable_other_income_type', '其他', 'other', 30, '其他收入收入类型初始化'
    UNION ALL SELECT 'receivable_other_income_doc_type', '正常单据', 'normal', 10, '其他收入单据类型初始化'
    UNION ALL SELECT 'receivable_other_income_doc_type', '代付款', 'payment_on_behalf', 20, '其他收入单据类型初始化'
    UNION ALL SELECT 'receivable_other_income_doc_type', '期初余额', 'opening_balance', 30, '其他收入单据类型初始化'
    UNION ALL SELECT 'receivable_other_income_item_project', '代垫运费', 'freight_advance', 10, '其他收入项目名称初始化'
    UNION ALL SELECT 'receivable_other_income_item_project', '礼金收入', 'gift_income', 20, '其他收入项目名称初始化'
    UNION ALL SELECT 'receivable_other_income_item_project', '期初存款', 'opening_deposit', 30, '其他收入项目名称初始化'
    UNION ALL SELECT 'receivable_other_income_item_project', '供应商返利', 'supplier_rebate', 40, '其他收入项目名称初始化'
    UNION ALL SELECT 'receivable_other_income_item_project', '期初现金', 'opening_cash', 50, '其他收入项目名称初始化'
    UNION ALL SELECT 'receivable_other_income_item_project', '采购减付', 'purchase_payment_reduction', 60, '其他收入项目名称初始化'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`name` = seed.name
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);

UPDATE `erp_base_data` target
JOIN (
    SELECT 'settle_method' AS type, '挂账' AS name, 10 AS sort
    UNION ALL SELECT 'settle_method', '汇款', 20
    UNION ALL SELECT 'settle_method', '网上支付', 30
    UNION ALL SELECT 'settle_method', '现金', 40
) seed ON seed.type = target.`type` AND seed.name = target.`name`
SET target.`sort` = seed.sort,
    target.`updater` = '1',
    target.`update_time` = NOW()
WHERE target.`tenant_id` = @tenant_id
  AND target.`deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.module, seed.field_key, seed.field_label, seed.field_group, seed.sort,
       '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'erp_finance_receivable_other_income' AS module, 'item_customerId' AS field_key,
           '明细-对象' AS field_label, 'detail_item' AS field_group, 190 AS sort
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
        WHEN 'party' THEN '对象'
        WHEN 'item_party' THEN '明细-对象（历史文本）'
        ELSE `field_label`
    END,
    `updater` = '1',
    `update_time` = NOW()
WHERE `module` = 'erp_finance_receivable_other_income'
  AND `field_key` IN ('party', 'item_party')
  AND `tenant_id` = @tenant_id
  AND `deleted` = b'0';
