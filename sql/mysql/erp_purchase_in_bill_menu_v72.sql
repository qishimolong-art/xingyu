-- ERP 入仓单报表菜单（v72）
-- 挂在采购管理下，只读报表，只有查询和导出权限

-- 1. 插入入仓单菜单，挂到采购管理下
SET @purchase_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = '采购管理' AND `deleted` = b'0' LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '入仓单', '', 2, 8, @purchase_menu_id, 'inbill', 'ep:document',
       'erp/purchase/inbill/index', 'ErpPurchaseInBill', 0, b'1', b'1', b'1',
       '1', NOW(), '1', NOW(), b'0'
WHERE @purchase_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `component` = 'erp/purchase/inbill/index' AND `deleted` = b'0'
  );

-- 2. 插入查询权限按钮
SET @in_bill_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `component` = 'erp/purchase/inbill/index' AND `deleted` = b'0' LIMIT 1
);

INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '入仓单查询', 'erp:stock-in-bill:query', 3, 1, @in_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @in_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-in-bill:query' AND `deleted` = b'0'
  );

-- 3. 插入导出权限按钮
INSERT INTO `system_menu` (
  `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
  `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
  `updater`, `update_time`, `deleted`
)
SELECT '入仓单导出', 'erp:stock-in-bill:export', 3, 2, @in_bill_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @in_bill_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `permission` = 'erp:stock-in-bill:export' AND `deleted` = b'0'
  );

-- 4. 授权超管
INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 1, menu.`id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` menu
WHERE (
    menu.`component` = 'erp/purchase/inbill/index'
    OR menu.`permission` IN ('erp:stock-in-bill:query', 'erp:stock-in-bill:export')
  )
  AND menu.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.`role_id` = 1 AND rm.`menu_id` = menu.`id` AND rm.`deleted` = b'0'
  );
