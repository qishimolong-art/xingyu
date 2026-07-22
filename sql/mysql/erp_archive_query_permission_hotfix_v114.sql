-- ERP archive query permission hotfix (v114)
--
-- Fixes users who can open supplier/customer archive pages but receive
-- "没有该操作权限" on page load because the list APIs require:
--   - erp:supplier:query
--   - erp:customer:query
--
-- Safety:
--   - No broad DELETE/TRUNCATE.
--   - Inserts missing query button nodes and role-menu relations.
--   - Removes user-level deny rows for these two query permissions only when
--     the user's role already grants the same permission.
--   - Does not grant create/update/delete/import/export/merge permissions.
--
-- After running direct SQL, clear Redis/Spring Cache entries or restart Redis,
-- then restart backend, log out, and log in again.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_parent_id := (
  SELECT id
  FROM system_menu
  WHERE name = 'ERP 系统'
    AND type = 1
    AND parent_id = 0
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @base_parent_id := (
  SELECT id
  FROM system_menu
  WHERE name = '基础数据'
    AND type = 1
    AND parent_id = @erp_parent_id
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @supplier_menu_id := (
  SELECT id
  FROM system_menu
  WHERE component = 'erp/purchase/supplier/index'
    AND type = 2
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @customer_menu_id := (
  SELECT id
  FROM system_menu
  WHERE component = 'erp/sale/customer/index'
    AND type = 2
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @supplier_menu_id := COALESCE(@supplier_menu_id, (
  SELECT id
  FROM system_menu
  WHERE name IN ('供应商档案', '供应商信息')
    AND type = 2
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
));

SET @customer_menu_id := COALESCE(@customer_menu_id, (
  SELECT id
  FROM system_menu
  WHERE name IN ('客户档案', '客户信息')
    AND type = 2
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
));

-- Fallback: if the ERP/base parent names differ in an older database, reuse the
-- current parent of the relocated supplier/customer archive pages.
SET @base_parent_id := COALESCE(@base_parent_id, (
  SELECT parent_id
  FROM system_menu
  WHERE id IN (@supplier_menu_id, @customer_menu_id)
    AND parent_id IS NOT NULL
    AND parent_id <> 0
    AND deleted = b'0'
  GROUP BY parent_id
  ORDER BY COUNT(*) DESC, parent_id DESC
  LIMIT 1
));

-- Ensure query button nodes exist under the current archive page menus.
INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '供应商查询', 'erp:supplier:query', 3, 1, @supplier_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @supplier_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE parent_id = @supplier_menu_id
      AND permission = 'erp:supplier:query'
      AND deleted = b'0'
  );

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '客户查询', 'erp:customer:query', 3, 1, @customer_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @customer_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE parent_id = @customer_menu_id
      AND permission = 'erp:customer:query'
      AND deleted = b'0'
  );

SET @supplier_query_menu_id := (
  SELECT id
  FROM system_menu
  WHERE parent_id = @supplier_menu_id
    AND permission = 'erp:supplier:query'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

SET @customer_query_menu_id := (
  SELECT id
  FROM system_menu
  WHERE parent_id = @customer_menu_id
    AND permission = 'erp:customer:query'
    AND deleted = b'0'
  ORDER BY id DESC
  LIMIT 1
);

-- Roles that already own the archive page or any stronger archive action
-- should also own query, otherwise the page route opens but /page is denied.
INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source_role.role_id, @supplier_query_menu_id, '1', NOW(), '1', NOW(), b'0', source_role.tenant_id
FROM (
  SELECT role_menu.role_id, role_menu.tenant_id
  FROM system_role_menu role_menu
  JOIN system_menu owned_menu
    ON owned_menu.id = role_menu.menu_id
   AND owned_menu.deleted = b'0'
  WHERE role_menu.deleted = b'0'
    AND (
      owned_menu.id = @supplier_menu_id
      OR owned_menu.component = 'erp/purchase/supplier/index'
      OR owned_menu.permission IN (
        'erp:supplier:create',
        'erp:supplier:update',
        'erp:supplier:delete',
        'erp:supplier:export',
        'erp:supplier:import',
        'erp:supplier:merge',
        'erp:supplier:dept-distribute'
      )
    )
  UNION
  SELECT sys_role.id, sys_role.tenant_id
  FROM system_role sys_role
  WHERE sys_role.code = 'super_admin'
    AND sys_role.deleted = b'0'
) source_role
WHERE @supplier_query_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_role_menu target
    WHERE target.role_id = source_role.role_id
      AND target.menu_id = @supplier_query_menu_id
      AND target.tenant_id = source_role.tenant_id
      AND target.deleted = b'0'
  );

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source_role.role_id, @customer_query_menu_id, '1', NOW(), '1', NOW(), b'0', source_role.tenant_id
FROM (
  SELECT role_menu.role_id, role_menu.tenant_id
  FROM system_role_menu role_menu
  JOIN system_menu owned_menu
    ON owned_menu.id = role_menu.menu_id
   AND owned_menu.deleted = b'0'
  WHERE role_menu.deleted = b'0'
    AND (
      owned_menu.id = @customer_menu_id
      OR owned_menu.component = 'erp/sale/customer/index'
      OR owned_menu.permission IN (
        'erp:customer:create',
        'erp:customer:update',
        'erp:customer:delete',
        'erp:customer:export',
        'erp:customer:import',
        'erp:customer:merge',
        'erp:customer:dept-distribute'
      )
    )
  UNION
  SELECT sys_role.id, sys_role.tenant_id
  FROM system_role sys_role
  WHERE sys_role.code = 'super_admin'
    AND sys_role.deleted = b'0'
) source_role
WHERE @customer_query_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_role_menu target
    WHERE target.role_id = source_role.role_id
      AND target.menu_id = @customer_query_menu_id
      AND target.tenant_id = source_role.tenant_id
      AND target.deleted = b'0'
  );

-- Keep the relocated archive pages reachable for roles that already own them.
INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source_role.role_id, @base_parent_id, '1', NOW(), '1', NOW(), b'0', source_role.tenant_id
FROM (
  SELECT role_menu.role_id, role_menu.tenant_id
  FROM system_role_menu role_menu
  WHERE role_menu.deleted = b'0'
    AND role_menu.menu_id IN (@supplier_menu_id, @customer_menu_id)
  UNION
  SELECT sys_role.id, sys_role.tenant_id
  FROM system_role sys_role
  WHERE sys_role.code = 'super_admin'
    AND sys_role.deleted = b'0'
) source_role
WHERE @base_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM system_role_menu target
  WHERE target.role_id = source_role.role_id
      AND target.menu_id = @base_parent_id
      AND target.tenant_id = source_role.tenant_id
      AND target.deleted = b'0'
  );

-- If this project uses user-level denied button permissions, remove only these
-- two deny rows for users whose enabled roles already grant the same query
-- permission. This keeps user-level restrictions for unrelated permissions.
DELETE deny
FROM system_user_permission_deny deny
WHERE deny.deleted = b'0'
  AND deny.permission IN ('erp:supplier:query', 'erp:customer:query')
  AND EXISTS (
    SELECT 1
    FROM system_user_role user_role
    JOIN system_role role
      ON role.id = user_role.role_id
     AND role.deleted = b'0'
     AND role.status = 0
    JOIN system_role_menu role_menu
      ON role_menu.role_id = role.id
     AND role_menu.deleted = b'0'
    JOIN system_menu menu
      ON menu.id = role_menu.menu_id
     AND menu.deleted = b'0'
     AND menu.permission = deny.permission
    WHERE user_role.user_id = deny.user_id
      AND user_role.deleted = b'0'
  );
