-- ERP 费用支付字段对齐（v181）
-- 1. 主表增加“类型”字段 expense_biz_type。
-- 2. 补齐费用支付字段权限定义和展示名。
-- 3. 初始化费用支付专属基础资料选项。

SET @tenant_id := 1;

DROP PROCEDURE IF EXISTS add_erp_payable_expense_field_align_v181;

DELIMITER //
CREATE PROCEDURE add_erp_payable_expense_field_align_v181()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_payable_expense'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_payable_expense'
          AND COLUMN_NAME = 'expense_biz_type'
    ) THEN
        ALTER TABLE `erp_payable_expense`
            ADD COLUMN `expense_biz_type` VARCHAR(64) DEFAULT NULL COMMENT '类型'
                AFTER `voucher_no`;
    END IF;
END //
DELIMITER ;

CALL add_erp_payable_expense_field_align_v181();
DROP PROCEDURE IF EXISTS add_erp_payable_expense_field_align_v181;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.module, seed.field_key, seed.field_label, seed.field_group, seed.sort,
       '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'erp_finance_payable_expense' AS module, 'expenseBizType' AS field_key,
           '类型' AS field_label, 'expense_info' AS field_group, 55 AS sort
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
        WHEN 'no' THEN '单号'
        WHEN 'bizTime' THEN '日期'
        WHEN 'expenseType' THEN '支出类型'
        WHEN 'deptId' THEN '开单部门'
        WHEN 'handlerId' THEN '经手人'
        WHEN 'party' THEN '对象'
        WHEN 'item_itemName' THEN '明细-项目名称'
        WHEN 'item_party' THEN '明细-对象'
        ELSE `field_label`
    END,
    `updater` = '1',
    `update_time` = NOW()
WHERE `module` = 'erp_finance_payable_expense'
  AND `field_key` IN ('no', 'bizTime', 'expenseType', 'deptId', 'handlerId', 'party', 'item_itemName', 'item_party')
  AND `tenant_id` = @tenant_id
  AND `deleted` = b'0';

INSERT INTO `erp_base_data`
(`type`, `name`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'payable_expense_biz_type' AS type, '代收代付支出' AS name, 10 AS sort, '费用支付类型初始化' AS remark
    UNION ALL SELECT 'payable_expense_biz_type', '资金支出', 20, '费用支付类型初始化'
    UNION ALL SELECT 'payable_expense_biz_type', '一般费用', 30, '费用支付类型初始化'
    UNION ALL SELECT 'payable_expense_biz_type', '管理费用', 40, '费用支付类型初始化'
    UNION ALL SELECT 'payable_expense_type', '其他', 10, '费用支付支出类型初始化'
    UNION ALL SELECT 'payable_expense_doc_type', '正常单据', 10, '费用支付单据类型初始化'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`name` = seed.name
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);
