-- ============================================================
-- ERP 菜单增补脚本（四期/五期前端菜单注册）
-- 用途：
--   1. 在【基础信息】下新增"价格体系"管理菜单 + 子权限
--   2. 升级已有"出入库明细"菜单 → 改名为"库存进出流水明细账"（五期报表页）
--   3. 补充四期已有的隐性权限点（erp:stock:adjust）
--
-- 日期：2026-05-08
-- 作者：Claude
-- 说明：
--   - 菜单 ID 使用 2950 段，避开仓库插件/其它模块的历史分配（如 2571-2610）
--   - parent_id 2564 = "基础信息"（/erp/product）；2583 = "库存管理"（/erp/stock）
--   - type：1=目录 2=菜单 3=按钮
--   - visible / keep_alive / always_show 使用 BIT(1) 1/0
--   - 部署后需登录后端重新分配权限或直接走管理员，刷新缓存
-- ============================================================

-- ----------------------------------------------------------
-- 1. 价格体系管理（四期）
-- ----------------------------------------------------------

-- 主菜单：价格体系
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2950, '价格体系', '', 2, 3, 2564, 'price-system', 'fa:tags',
 'erp/product/pricesystem/index', 'ErpPriceSystem',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 按钮：价格体系查询
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2951, '价格体系查询', 'erp:price-system:query', 3, 1, 2950, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 按钮：价格体系创建
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2952, '价格体系创建', 'erp:price-system:create', 3, 2, 2950, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 按钮：价格体系更新
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2953, '价格体系更新', 'erp:price-system:update', 3, 3, 2950, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 按钮：价格体系删除
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2954, '价格体系删除', 'erp:price-system:delete', 3, 4, 2950, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 2. 库存进出流水明细账（五期）
--    升级已有 id=2593 菜单：
--    - 原名"出入库明细" → 改为"库存进出流水明细账"
--    - 保留 path / component / component_name 不变（ErpStockRecord 与 /erp/stock/record 前端路径一致）
-- ----------------------------------------------------------

UPDATE `system_menu`
SET `name` = '库存进出流水明细账',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2593
  AND `deleted` = b'0';

-- ----------------------------------------------------------
-- 3. 补充四期隐性权限：库存手动调整
--    ErpStockController.adjustStock 使用 'erp:stock:adjust'
-- ----------------------------------------------------------

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2955, '库存手动调整', 'erp:stock:adjust', 3, 6, 2590, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 4. 授权超级管理员（role_id=1）拥有以上所有菜单
--    避免"加了菜单但管理员看不到"的常见问题
-- ----------------------------------------------------------

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES (1, 2950, '1', NOW(), '1', NOW(), b'0', 1),
       (1, 2951, '1', NOW(), '1', NOW(), b'0', 1),
       (1, 2952, '1', NOW(), '1', NOW(), b'0', 1),
       (1, 2953, '1', NOW(), '1', NOW(), b'0', 1),
       (1, 2954, '1', NOW(), '1', NOW(), b'0', 1),
       (1, 2955, '1', NOW(), '1', NOW(), b'0', 1);

-- ----------------------------------------------------------
-- 执行完成后请在应用内：
--   系统管理 → 菜单管理 → 点击刷新缓存
-- 或者重启后端服务，以便权限生效
-- ----------------------------------------------------------
