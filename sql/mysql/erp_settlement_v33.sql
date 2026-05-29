-- ERP 往来结算 v33
-- 1. 新增“往来结算”目录
-- 2. 收款/付款菜单迁移到往来结算目录，并更新组件路径到 settlement
-- 3. 新增应收冲应付报表菜单和权限

INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31120, '往来结算', '', 2, 2, 2645, 'settlement', 'ep:coin',
 '##', 'ErpFinanceSettlement',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31121, '应收冲应付', '', 2, 3, 31120, 'offset', 'ep:switch',
 'erp/finance/settlement/offset/index', 'ErpSettlementOffset',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31122, '应收冲应付查询', 'erp:settlement-offset:query', 3, 1, 31121, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

UPDATE `system_menu`
   SET `parent_id` = 31120,
       `sort` = 1,
       `path` = 'payment',
       `component` = 'erp/finance/settlement/payment/index',
       `component_name` = 'ErpFinancePayment'
 WHERE `id` = 2687;

UPDATE `system_menu`
   SET `parent_id` = 31120,
       `sort` = 2,
       `path` = 'receipt',
       `component` = 'erp/finance/settlement/receipt/index',
       `component_name` = 'ErpFinanceReceipt'
 WHERE `id` = 2694;

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31120, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31121, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31122, '1', NOW(), '1', NOW(), b'0', 1);

ALTER TABLE `erp_purchase_price_adjust`
    ADD COLUMN IF NOT EXISTS `payment_price` DECIMAL(24, 6) NOT NULL DEFAULT 0 COMMENT '已结算金额' AFTER `total_adjust_price`;
