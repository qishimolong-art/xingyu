-- ============================================================
-- ERP 财务核算 v26 菜单增补脚本（幂等版本）
-- 用途：
--   在 ERP 顶级菜单（parent_id=2563）下新增"财务核算"二级目录，
--   挂会计科目、系统开账、凭证列表、凭证生成、凭证字、三大报表 7 个三级菜单 + 权限点。
--
-- 日期：2026-05-14
-- 作者：Claude
-- 说明：
--   - 菜单 ID 使用 3030~3079 段（避开已用的 3010-3026）
--   - 二级目录 parent_id=2563（"ERP 系统"）
--   - type：1=目录 2=菜单 3=按钮
--   - 幂等设计：先删除旧记录再插入，可重复执行
--   - 部署后需登录后台 → 系统管理 → 菜单管理 → 刷新缓存；或重启后端服务
-- ============================================================

-- ----------------------------------------------------------
-- 0. 清理旧记录（幂等）
-- ----------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `menu_id` BETWEEN 3030 AND 3079;
DELETE FROM `system_menu` WHERE `id` BETWEEN 3030 AND 3079;

-- ----------------------------------------------------------
-- 1. ERP 二级目录：财务核算
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3030, '财务核算', '', 1, 95, 2563, 'accounting', 'fa:calculator', '', '',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 2. 会计科目（含期初余额）
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3031, '会计科目', '', 2, 1, 3030, 'subject', 'fa:list-ol',
 'erp/accounting/subject/index', 'ErpAccountingSubject',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3032, '会计科目查询', 'erp:accounting-subject:query', 3, 1, 3031, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3033, '会计科目新增', 'erp:accounting-subject:create', 3, 2, 3031, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3034, '会计科目修改', 'erp:accounting-subject:update', 3, 3, 3031, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3035, '会计科目删除', 'erp:accounting-subject:delete', 3, 4, 3031, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 3. 系统开账
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3036, '系统开账', '', 2, 2, 3030, 'book-open', 'fa:book',
 'erp/accounting/book-open/index', 'ErpBookOpen',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3037, '系统开账查询', 'erp:book-open:query', 3, 1, 3036, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3038, '系统开账新增', 'erp:book-open:create', 3, 2, 3036, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3039, '系统开账修改', 'erp:book-open:update', 3, 3, 3036, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3040, '系统开账删除', 'erp:book-open:delete', 3, 4, 3036, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 4. 凭证列表
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3041, '凭证列表', '', 2, 3, 3030, 'voucher', 'fa:file-text',
 'erp/accounting/voucher/index', 'ErpVoucher',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3042, '凭证查询', 'erp:voucher:query', 3, 1, 3041, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3043, '凭证新增', 'erp:voucher:create', 3, 2, 3041, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3044, '凭证修改', 'erp:voucher:update', 3, 3, 3041, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3045, '凭证删除', 'erp:voucher:delete', 3, 4, 3041, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3046, '凭证审核', 'erp:voucher:audit', 3, 5, 3041, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3047, '凭证反审核', 'erp:voucher:process', 3, 6, 3041, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 5. 凭证生成（跨月归属）
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3048, '凭证生成', '', 2, 4, 3030, 'voucher-attribution', 'fa:exchange',
 'erp/accounting/voucher-attribution/index', 'ErpVoucherAttribution',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3049, '凭证生成查询', 'erp:voucher-attribution:query', 3, 1, 3048, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3050, '凭证生成应用', 'erp:voucher-attribution:apply', 3, 2, 3048, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3051, '凭证生成执行', 'erp:voucher-attribution:generate', 3, 3, 3048, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 6. 凭证字字典
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3052, '凭证字', '', 2, 5, 3030, 'voucher-word', 'fa:tag',
 'erp/accounting/voucher-word/index', 'ErpVoucherWord',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3053, '凭证字查询', 'erp:voucher-word:query', 3, 1, 3052, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3054, '凭证字新增', 'erp:voucher-word:create', 3, 2, 3052, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3055, '凭证字修改', 'erp:voucher-word:update', 3, 3, 3052, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3056, '凭证字删除', 'erp:voucher-word:delete', 3, 4, 3052, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 7. 资产负债表
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3057, '资产负债表', '', 2, 6, 3030, 'balance-sheet', 'fa:balance-scale',
 'erp/accounting/balance-sheet/index', 'ErpBalanceSheet',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3058, '资产负债表查询', 'erp:balance-sheet:query', 3, 1, 3057, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3059, '资产负债表导出', 'erp:balance-sheet:export', 3, 2, 3057, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 8. 利润表
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3060, '利润表', '', 2, 7, 3030, 'income-statement', 'fa:line-chart',
 'erp/accounting/income-statement/index', 'ErpIncomeStatement',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3061, '利润表查询', 'erp:income-statement:query', 3, 1, 3060, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3062, '利润表导出', 'erp:income-statement:export', 3, 2, 3060, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 9. 现金流量表
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3063, '现金流量表', '', 2, 8, 3030, 'cash-flow', 'fa:money',
 'erp/accounting/cash-flow/index', 'ErpCashFlow',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3064, '现金流量表查询', 'erp:cash-flow:query', 3, 1, 3063, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3065, '现金流量表导出', 'erp:cash-flow:export', 3, 2, 3063, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 10. 授权超级管理员（role_id=1）
-- ----------------------------------------------------------
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 3030, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3031, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3032, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3033, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3034, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3035, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3036, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3037, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3038, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3039, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3040, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3041, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3042, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3043, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3044, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3045, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3046, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3047, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3048, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3049, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3050, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3051, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3052, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3053, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3054, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3055, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3056, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3057, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3058, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3059, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3060, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3061, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3062, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3063, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3064, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3065, '1', NOW(), '1', NOW(), b'0', 1);

-- ============================================================
-- 执行完成后：
--   系统管理 → 菜单管理 → 点击刷新缓存
-- 或重启后端服务以使权限生效
-- 预留 3066~3079 给后期扩展（月末结转/期间损益结转）
-- ============================================================
