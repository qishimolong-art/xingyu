-- ERP 采购调价菜单（v15）
-- 执行时机：已执行 erp_purchase_price_adjust_v15.sql（表结构），并部署前端页面后执行。
-- 说明：
--   1. parent_id 2602 = 采购管理（见 ruoyi-vue-pro.sql:2386）。
--   2. 同级兄弟菜单：采购订单 2666 / 采购入库 2673 / 采购退货 2680 / 供应商信息 2603 / 供应商基础表 3010。
--   3. 菜单 ID 段 3020-3026（供应商基础表 3010-3012 之后的下一段 10 位 ID 空间）。
--   4. 幂等：INSERT ... ON DUPLICATE KEY UPDATE + INSERT IGNORE。
--   5. 执行后请在后台刷新菜单缓存，或重启后端服务。

-- =====================================================
-- 采购调价 (3020-3026)
-- =====================================================
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3020, '采购调价', '', 2, 6, 2602, 'priceadjust', 'fa:exchange',
 'erp/purchase/priceadjust/index', 'ErpPurchasePriceAdjust',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3021, '采购调价查询', 'erp:purchase-price-adjust:query', 3, 1, 3020, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3022, '采购调价创建', 'erp:purchase-price-adjust:create', 3, 2, 3020, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3023, '采购调价更新', 'erp:purchase-price-adjust:update', 3, 3, 3020, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3024, '采购调价删除', 'erp:purchase-price-adjust:delete', 3, 4, 3020, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3025, '采购调价审批', 'erp:purchase-price-adjust:update-status', 3, 5, 3020, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3026, '采购调价导出', 'erp:purchase-price-adjust:export', 3, 6, 3020, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

-- =====================================================
-- 授权超管 role_id = 1
-- =====================================================
INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 3020, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3021, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3022, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3023, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3024, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3025, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3026, '1', NOW(), '1', NOW(), b'0', 1);

-- 执行后：系统管理 → 菜单管理 → 刷新缓存，或重启后端服务。
