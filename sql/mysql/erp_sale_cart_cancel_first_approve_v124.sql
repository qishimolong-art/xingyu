-- 销售手推车撤销初审权限（增量、可重复执行）
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @first_approve_menu_id := (
  SELECT id FROM system_menu
  WHERE permission = 'erp:sale-cart:first-approve' AND deleted = b'0'
  ORDER BY id DESC LIMIT 1
);
SET @sale_cart_menu_id := (
  SELECT parent_id FROM system_menu WHERE id = @first_approve_menu_id LIMIT 1
);
SET @cancel_first_approve_menu_id := (
  SELECT id FROM system_menu
  WHERE permission = 'erp:sale-cart:cancel-first-approve' AND deleted = b'0'
  ORDER BY id DESC LIMIT 1
);

INSERT INTO system_menu
(name, permission, type, sort, parent_id, path, icon, component, component_name,
 status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '撤销初审', 'erp:sale-cart:cancel-first-approve', 3, 7, @sale_cart_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @sale_cart_menu_id IS NOT NULL
  AND @cancel_first_approve_menu_id IS NULL;

SET @cancel_first_approve_menu_id := (
  SELECT id FROM system_menu
  WHERE permission = 'erp:sale-cart:cancel-first-approve' AND deleted = b'0'
  ORDER BY id DESC LIMIT 1
);

-- 仅向已经拥有“初审”权限的角色追加“撤销初审”，不改写其他权限。
INSERT INTO system_role_menu
(role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT source.role_id, @cancel_first_approve_menu_id, '1', NOW(), '1', NOW(), b'0', source.tenant_id
FROM system_role_menu source
WHERE source.menu_id = @first_approve_menu_id
  AND source.deleted = b'0'
  AND @cancel_first_approve_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu target
    WHERE target.role_id = source.role_id
      AND target.menu_id = @cancel_first_approve_menu_id
      AND target.tenant_id = source.tenant_id
      AND target.deleted = b'0'
  );
