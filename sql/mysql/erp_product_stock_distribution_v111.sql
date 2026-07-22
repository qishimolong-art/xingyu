-- ERP 配件信息库存分发权限（v111）
-- 安全说明：
--   - 仅追加“库存分发”按钮权限。
--   - 不删除 system_menu / system_role_menu，不覆盖已有配件信息权限。
--   - 执行后刷新菜单缓存，并重新登录前端。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_stock_distribution_parent;

CREATE TEMPORARY TABLE tmp_erp_product_stock_distribution_parent (
  parent_id bigint NOT NULL PRIMARY KEY
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_product_stock_distribution_parent (parent_id)
SELECT MIN(candidate.parent_id) AS parent_id
FROM system_menu candidate
WHERE candidate.deleted = b'0'
  AND candidate.parent_id > 0
  AND candidate.permission IN ('erp:product:query', 'erp:product:update')
HAVING parent_id IS NOT NULL;

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '库存分发',
       'erp:product:stock-distribute',
       3,
       8,
       parent.parent_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM tmp_erp_product_stock_distribution_parent parent
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.permission = 'erp:product:stock-distribute'
      AND exists_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       stock_distribution_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM system_menu stock_distribution_menu
JOIN system_role_menu owned_role
  ON owned_role.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.id = owned_role.menu_id
 AND owned_menu.deleted = b'0'
 AND owned_menu.permission = 'erp:product:update'
WHERE stock_distribution_menu.permission = 'erp:product:stock-distribute'
  AND stock_distribution_menu.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu target
      WHERE target.role_id = owned_role.role_id
        AND target.menu_id = stock_distribution_menu.id
        AND target.tenant_id = owned_role.tenant_id
        AND target.deleted = b'0'
  );

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       stock_distribution_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM system_role super_role
JOIN system_menu stock_distribution_menu
  ON stock_distribution_menu.permission = 'erp:product:stock-distribute'
 AND stock_distribution_menu.deleted = b'0'
WHERE super_role.code = 'super_admin'
  AND super_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu target
      WHERE target.role_id = super_role.id
        AND target.menu_id = stock_distribution_menu.id
        AND target.tenant_id = super_role.tenant_id
        AND target.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_stock_distribution_parent;
