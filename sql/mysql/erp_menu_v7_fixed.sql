-- ============================================================
-- ERP 第七期菜单增补脚本：字段必填配置（幂等版本）
-- 用途：
--   在 ERP 顶级菜单下新增"系统配置"二级目录，下挂"字段配置"菜单 + 4 个按钮权限点
--
-- 日期：2026-05-12
-- 作者：Claude
-- 说明：
--   - 菜单 ID 使用 2960 段
--   - 二级目录 parent_id=2563（"ERP 系统"）
--   - type：1=目录 2=菜单 3=按钮
--   - 幂等设计：先删除旧记录再插入，可重复执行
--   - 部署后需登录后台 → 系统管理 → 菜单管理 → 刷新缓存；或重启后端服务
-- ============================================================

-- ----------------------------------------------------------
-- 0. 清理旧记录（如果存在）
-- ----------------------------------------------------------
DELETE FROM `system_role_menu` WHERE `menu_id` IN (2960, 2961, 2962, 2963);
DELETE FROM `system_menu` WHERE `id` IN (2960, 2961, 2962, 2963);

-- ----------------------------------------------------------
-- 1. ERP 二级目录：系统配置
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2960, '系统配置', '', 1, 90, 2563, 'system', 'ep:setting', '', '',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 2. 三级菜单：字段配置
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2961, '字段配置', '', 2, 1, 2960, 'fieldconfig', 'fa:list-alt',
 'erp/system/fieldconfig/index', 'ErpFieldConfig',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 3. 按钮权限点
-- ----------------------------------------------------------
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2962, '字段配置查询', 'erp:field-config:query', 3, 1, 2961, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(2963, '字段配置更新', 'erp:field-config:update', 3, 2, 2961, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- ----------------------------------------------------------
-- 4. 授权超级管理员（role_id=1）
-- ----------------------------------------------------------
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 2960, '1', NOW(), '1', NOW(), b'0', 1),
(1, 2961, '1', NOW(), '1', NOW(), b'0', 1),
(1, 2962, '1', NOW(), '1', NOW(), b'0', 1),
(1, 2963, '1', NOW(), '1', NOW(), b'0', 1);

-- ----------------------------------------------------------
-- 执行完成后：
--   系统管理 → 菜单管理 → 点击刷新缓存
-- 或重启后端服务以使权限生效
-- ============================================================
