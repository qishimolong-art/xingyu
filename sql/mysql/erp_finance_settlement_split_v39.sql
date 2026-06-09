-- ERP 资金管理菜单修正 v39
-- 恢复“往来结算”下的“收款单”“付款单”两个独立入口。
-- 业务单据在各自表单内合并展示，不再按销售退货、销售调价等分 Tab。

UPDATE `system_menu`
SET `name` = '付款单',
    `parent_id` = 31120,
    `sort` = 1,
    `path` = 'payment',
    `icon` = 'ep:caret-right',
    `component` = 'erp/finance/settlement/payment/index',
    `component_name` = 'ErpFinancePayment',
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2687;

UPDATE `system_menu`
SET `name` = '收款单',
    `parent_id` = 31120,
    `sort` = 2,
    `path` = 'receipt',
    `icon` = 'ep:expand',
    `component` = 'erp/finance/settlement/receipt/index',
    `component_name` = 'ErpFinanceReceipt',
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2694;

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 2687, '1', NOW(), '1', NOW(), b'0', 1),
(1, 2694, '1', NOW(), '1', NOW(), b'0', 1);
