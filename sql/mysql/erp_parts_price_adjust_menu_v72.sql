-- 配件价格调整菜单（v72）
-- 挂在基础数据 / 配件基本信息下，含查询/更新/批量调价权限
-- 菜单 ID：900150-900153（避开数据库已有最大 ID 900144）

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 配件价格调整菜单（900150-900153）
-- =====================================================

-- 动态查询 ERP / 基础数据 / 配件基本信息父菜单 ID
SET @erp_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = 'ERP 系统' AND `type` = 1 AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

SET @base_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = '基础数据'
    AND `type` = 1
    AND `deleted` = b'0'
    AND (@erp_parent_id IS NULL OR `parent_id` = @erp_parent_id)
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '基础数据', '', 1, 5, @erp_parent_id, 'base', 'ep:data-analysis',
  '', '',
  0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @erp_parent_id IS NOT NULL
  AND @base_parent_id IS NULL;

SET @base_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = '基础数据'
    AND `type` = 1
    AND `deleted` = b'0'
    AND (@erp_parent_id IS NULL OR `parent_id` = @erp_parent_id)
  ORDER BY `id` DESC
  LIMIT 1
);

SET @parts_basic_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = '配件基本信息'
    AND `type` = 1
    AND `deleted` = b'0'
    AND (@base_parent_id IS NULL OR `parent_id` = @base_parent_id)
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT
  '配件基本信息', '', 1, 30, @base_parent_id, 'parts-basic', 'fa-solid:tools',
  '', '',
  0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @base_parent_id IS NOT NULL
  AND @parts_basic_menu_id IS NULL;

SET @parts_basic_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = '配件基本信息'
    AND `type` = 1
    AND `deleted` = b'0'
    AND (@base_parent_id IS NULL OR `parent_id` = @base_parent_id)
  ORDER BY `id` DESC
  LIMIT 1
);

-- 插入菜单页面（仅在父菜单存在且该菜单不存在时）
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
 `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`)
SELECT 900150, '配件价格调整', '', 2, 40, @parts_basic_menu_id, 'price-adjust', 'ep:price-tag',
       'erp/purchase/price-adjust/index', 'ErpPurchasePriceAdjust',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @parts_basic_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE (`id` = 900150 OR `component` = 'erp/purchase/price-adjust/index')
      AND `deleted` = b'0'
  );

SET @parts_price_adjust_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `component` = 'erp/purchase/price-adjust/index'
    AND `deleted` = b'0'
  ORDER BY `id` DESC
  LIMIT 1
);

UPDATE `system_menu`
SET `parent_id` = @parts_basic_menu_id,
    `sort` = 40,
    `updater` = '1',
    `update_time` = NOW()
WHERE @parts_basic_menu_id IS NOT NULL
  AND `id` = @parts_price_adjust_menu_id
  AND `deleted` = b'0'
  AND (`parent_id` <> @parts_basic_menu_id OR `sort` <> 40);

-- 查询权限
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
 `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`)
SELECT 900151, '配件价格查询', 'erp:product:query', 3, 1, 900150, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @parts_price_adjust_menu_id = 900150
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900151 AND `deleted` = b'0');

-- 列表直接编辑保存权限
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
 `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`)
SELECT 900152, '配件价格保存', 'erp:product:update', 3, 2, 900150, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @parts_price_adjust_menu_id = 900150
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900152 AND `deleted` = b'0');

-- 批量调价权限
INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
 `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`)
SELECT 900153, '配件价格批量调整', 'erp:parts:adjust-price', 3, 3, 900150, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @parts_price_adjust_menu_id = 900150
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900153 AND `deleted` = b'0');

-- =====================================================
-- 授权超管 role_id = 1
-- =====================================================
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, target_menu.`id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` target_menu
WHERE target_menu.`id` IN (@base_parent_id, @parts_basic_menu_id)
  AND target_menu.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` target
    WHERE target.`role_id` = 1
      AND target.`menu_id` = target_menu.`id`
      AND target.`tenant_id` = 1
      AND target.`deleted` = b'0'
  );

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 900150, '1', NOW(), '1', NOW(), b'0', 1),
(1, 900151, '1', NOW(), '1', NOW(), b'0', 1),
(1, 900152, '1', NOW(), '1', NOW(), b'0', 1),
(1, 900153, '1', NOW(), '1', NOW(), b'0', 1);

-- =====================================================
-- 写入调整口令系统配置（默认口令 123456，请上线后修改）
-- 表名：infra_config（非 system_config）
-- =====================================================
INSERT INTO `infra_config`
(`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`)
SELECT 'erp', 2, '配件价格调整口令', 'erp.parts.adjustPassword', '123456', b'0',
       '配件价格批量调整时需要输入的验证口令，请修改为复杂口令',
       '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_config` WHERE `config_key` = 'erp.parts.adjustPassword' AND `deleted` = b'0'
);

-- 执行后：
-- 1. 系统管理 → 菜单管理 → 刷新缓存，或重启后端服务
-- 2. 登录后导航到"基础数据 → 配件基本信息 → 配件价格调整"
-- 3. 请及时修改 infra_config 表中 erp.parts.adjustPassword 的值为复杂口令
-- 4. 配置位置：基础设施 → 配置管理 → 搜索 erp.parts.adjustPassword
