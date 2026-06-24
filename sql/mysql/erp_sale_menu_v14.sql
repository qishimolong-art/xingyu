-- ERP sale module menu v14.
-- Execute after erp_sale_config_v13.sql, then refresh menu cache.
-- Idempotent: safe to re-run (INSERT ... ON DUPLICATE KEY UPDATE + INSERT IGNORE).

-- Existing sale parent: 2617 = 销售管理.
-- Existing customer: 2618, old sale order: 2638, sale out: 2652, sale return: 2659.

UPDATE `system_menu`
SET `name` = '销售单',
    `component` = 'erp/sale/out/index',
    `component_name` = 'ErpSaleOut',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2652 AND `deleted` = b'0';

-- =====================================================
-- 报价订单 (2970-2977)
-- =====================================================
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2970, '报价订单', '', 2, 5, 2617, 'quote', 'fa:file-text-o',
 'erp/sale/quote/index', 'ErpSaleQuote',
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
(2971, '报价订单查询', 'erp:sale-quote:query', 3, 1, 2970, '', '', '', NULL,
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
(2972, '报价订单创建', 'erp:sale-quote:create', 3, 2, 2970, '', '', '', NULL,
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
(2973, '报价订单更新', 'erp:sale-quote:update', 3, 3, 2970, '', '', '', NULL,
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
(2974, '报价订单删除', 'erp:sale-quote:delete', 3, 4, 2970, '', '', '', NULL,
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
(2975, '报价订单审核', 'erp:sale-quote:approve', 3, 5, 2970, '', '', '', NULL,
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
(2976, '报价转手推车', 'erp:sale-quote:convert-cart', 3, 6, 2970, '', '', '', NULL,
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
(2977, '报价订单导出', 'erp:sale-quote:export', 3, 7, 2970, '', '', '', NULL,
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
-- 销售手推车 (2980-2989, 3013)
-- =====================================================
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2980, '销售手推车', '', 2, 6, 2617, 'cart', 'fa:shopping-cart',
 'erp/sale/cart/index', 'ErpSaleCart',
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
(2981, '销售手推车查询', 'erp:sale-cart:query', 3, 1, 2980, '', '', '', NULL,
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
(2982, '销售手推车创建', 'erp:sale-cart:create', 3, 2, 2980, '', '', '', NULL,
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
(2983, '销售手推车更新', 'erp:sale-cart:update', 3, 3, 2980, '', '', '', NULL,
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
(2984, '销售手推车删除', 'erp:sale-cart:delete', 3, 4, 2980, '', '', '', NULL,
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
(2985, '销售手推车提交', 'erp:sale-cart:submit', 3, 5, 2980, '', '', '', NULL,
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
(2986, '销售手推车初审', 'erp:sale-cart:first-approve', 3, 6, 2980, '', '', '', NULL,
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
(2987, '销售手推车终审', 'erp:sale-cart:final-approve', 3, 7, 2980, '', '', '', NULL,
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
(3013, '销售手推车驳回', 'erp:sale-cart:reject', 3, 8, 2980, '', '', '', NULL,
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
(2988, '手推车转报价', 'erp:sale-cart:convert-quote', 3, 9, 2980, '', '', '', NULL,
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
(2989, '销售手推车导出', 'erp:sale-cart:export', 3, 10, 2980, '', '', '', NULL,
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
-- 销售调价 (2990-2995)
-- =====================================================
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2990, '销售调价', '', 2, 7, 2617, 'priceadjust', 'fa:exchange',
 'erp/sale/priceadjust/index', 'ErpSalePriceAdjust',
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
(2991, '销售调价查询', 'erp:sale-price-adjust:query', 3, 1, 2990, '', '', '', NULL,
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
(2992, '销售调价创建', 'erp:sale-price-adjust:create', 3, 2, 2990, '', '', '', NULL,
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
(2993, '销售调价更新', 'erp:sale-price-adjust:update', 3, 3, 2990, '', '', '', NULL,
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
(2994, '销售调价删除', 'erp:sale-price-adjust:delete', 3, 4, 2990, '', '', '', NULL,
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
(2995, '销售调价审批', 'erp:sale-price-adjust:update-status', 3, 5, 2990, '', '', '', NULL,
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
-- 销售配置 (3000-3004)
-- =====================================================
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3000, '销售配置', '', 2, 8, 2617, 'config', 'fa:cog',
 'erp/sale/config/index', 'ErpSaleConfig',
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
(3001, '销售配置查询', 'erp:sale-config:query', 3, 1, 3000, '', '', '', NULL,
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
(3002, '销售配置创建', 'erp:sale-config:create', 3, 2, 3000, '', '', '', NULL,
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
(3003, '销售配置更新', 'erp:sale-config:update', 3, 3, 3000, '', '', '', NULL,
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
(3004, '销售配置删除', 'erp:sale-config:delete', 3, 4, 3000, '', '', '', NULL,
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
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
    (1, 2970, '1', NOW(), '1', NOW(), b'0', 1), (1, 2971, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2972, '1', NOW(), '1', NOW(), b'0', 1), (1, 2973, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2974, '1', NOW(), '1', NOW(), b'0', 1), (1, 2975, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2976, '1', NOW(), '1', NOW(), b'0', 1), (1, 2977, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2980, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2981, '1', NOW(), '1', NOW(), b'0', 1), (1, 2982, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2983, '1', NOW(), '1', NOW(), b'0', 1), (1, 2984, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2985, '1', NOW(), '1', NOW(), b'0', 1), (1, 2986, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2987, '1', NOW(), '1', NOW(), b'0', 1), (1, 2988, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2989, '1', NOW(), '1', NOW(), b'0', 1), (1, 3013, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2990, '1', NOW(), '1', NOW(), b'0', 1), (1, 2991, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2992, '1', NOW(), '1', NOW(), b'0', 1), (1, 2993, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 2994, '1', NOW(), '1', NOW(), b'0', 1), (1, 2995, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 3000, '1', NOW(), '1', NOW(), b'0', 1), (1, 3001, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 3002, '1', NOW(), '1', NOW(), b'0', 1), (1, 3003, '1', NOW(), '1', NOW(), b'0', 1),
    (1, 3004, '1', NOW(), '1', NOW(), b'0', 1);

-- After execution: System Management -> Menu Management -> refresh cache, or restart backend.
