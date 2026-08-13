-- 阶段 1 止血：临时关闭销售手推车的表单数据权限配置。
--
-- 背景：
-- 当前销售手推车启用 `form_permission_table_config` 后，`erp_sale_cart`
-- 会被排除出普通部门数据权限规则，只剩 `form_data_permission` 白名单过滤。
-- 新增销售手推车不会实时写入白名单，导致拥有本部门数据权限的账号也查不到
-- 本部门新建的销售手推车。
--
-- 本脚本仅关闭 `erp_sale_cart` 的表单数据权限接入，让它临时回到普通
-- 部门/角色数据范围过滤；不删除字段配置，也不删除历史授权记录，便于后续
-- 第二阶段并集权限改造完成后重新启用。
--
-- 执行后生效方式：
-- 1. 推荐重启后端服务；或
-- 2. 在“系统配置 - 表单数据权限配置”页面重新保存任意配置，触发内存注册表刷新。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE `form_permission_table_config`
SET `enabled` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `form_type` = 'erp_sale_cart'
  AND `deleted` = b'0'
  AND `enabled` <> b'0';

-- 验证：enabled 为 0 表示销售手推车已退出表单数据权限过滤。
SELECT `form_type`, `table_desc`, `enabled`, `update_time`
FROM `form_permission_table_config`
WHERE `form_type` = 'erp_sale_cart'
  AND `deleted` = b'0';
