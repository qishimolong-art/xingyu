-- ERP 费用支付明细项目名称选项（v206）
-- 1. 初始化费用支付明细“项目名称”基础资料选项。
-- 2. 仅做幂等补齐，不删除、不停用、不覆盖历史基础资料。

SET @tenant_id := 1;

INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0, seed.remark,
       '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'payable_expense_item_project' AS type, '销售减收' AS name, 'sale_reduction' AS code, 10 AS sort, '费用支付项目名称初始化' AS remark
    UNION ALL SELECT 'payable_expense_item_project', '差旅费用', 'travel_expense', 20, '费用支付项目名称初始化'
    UNION ALL SELECT 'payable_expense_item_project', '通信费用', 'communication_expense', 30, '费用支付项目名称初始化'
    UNION ALL SELECT 'payable_expense_item_project', '销售产生运费', 'sale_freight', 40, '费用支付项目名称初始化'
    UNION ALL SELECT 'payable_expense_item_project', '销售退货运费', 'sale_return_freight', 50, '费用支付项目名称初始化'
    UNION ALL SELECT 'payable_expense_item_project', '调拨产生运费', 'transfer_freight', 60, '费用支付项目名称初始化'
    UNION ALL SELECT 'payable_expense_item_project', '员工工资', 'employee_salary', 70, '费用支付项目名称初始化'
    UNION ALL SELECT 'payable_expense_item_project', '员工社保', 'employee_social_security', 80, '费用支付项目名称初始化'
    UNION ALL SELECT 'payable_expense_item_project', '返利', 'rebate', 90, '费用支付项目名称初始化'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`name` = seed.name
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);
