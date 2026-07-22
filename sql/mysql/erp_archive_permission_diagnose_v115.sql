-- ERP archive permission diagnose (v115)
--
-- Usage:
--   1. Replace @login_username with the account that receives "没有该操作权限".
--   2. Execute this script in the target MySQL database.
--   3. Send the result sets back for analysis if the issue remains unclear.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @login_username := 'admin';

SELECT
  '01_target_user' AS section,
  user.id AS user_id,
  user.username,
  user.nickname,
  user.status AS user_status,
  user.tenant_id
FROM system_users user
WHERE user.username = @login_username
  AND user.deleted = b'0';

SELECT
  '02_user_roles' AS section,
  user.id AS user_id,
  user.username,
  role.id AS role_id,
  role.name AS role_name,
  role.code AS role_code,
  role.status AS role_status,
  role.deleted AS role_deleted,
  role.tenant_id AS role_tenant_id
FROM system_users user
JOIN system_user_role user_role
  ON user_role.user_id = user.id
 AND user_role.deleted = b'0'
JOIN system_role role
  ON role.id = user_role.role_id
WHERE user.username = @login_username
  AND user.deleted = b'0'
ORDER BY role.id;

SELECT
  '03_archive_menus' AS section,
  menu.id,
  menu.name,
  menu.permission,
  menu.type,
  menu.parent_id,
  menu.path,
  menu.component,
  menu.status,
  menu.deleted
FROM system_menu menu
WHERE menu.deleted = b'0'
  AND (
    menu.component IN ('erp/purchase/supplier/index', 'erp/sale/customer/index')
    OR menu.permission IN ('erp:supplier:query', 'erp:customer:query')
  )
ORDER BY menu.component, menu.permission, menu.id;

SELECT
  '04_role_query_grants' AS section,
  user.id AS user_id,
  user.username,
  role.id AS role_id,
  role.name AS role_name,
  role.code AS role_code,
  menu.id AS menu_id,
  menu.name AS menu_name,
  menu.permission,
  role_menu.tenant_id AS grant_tenant_id
FROM system_users user
JOIN system_user_role user_role
  ON user_role.user_id = user.id
 AND user_role.deleted = b'0'
JOIN system_role role
  ON role.id = user_role.role_id
 AND role.deleted = b'0'
JOIN system_role_menu role_menu
  ON role_menu.role_id = role.id
 AND role_menu.deleted = b'0'
JOIN system_menu menu
  ON menu.id = role_menu.menu_id
 AND menu.deleted = b'0'
WHERE user.username = @login_username
  AND user.deleted = b'0'
  AND menu.permission IN ('erp:supplier:query', 'erp:customer:query')
ORDER BY role.id, menu.permission;

SELECT
  '05_user_denied_permissions' AS section,
  user.id AS user_id,
  user.username,
  deny.permission,
  deny.tenant_id,
  deny.deleted
FROM system_users user
JOIN system_user_permission_deny deny
  ON deny.user_id = user.id
WHERE user.username = @login_username
  AND user.deleted = b'0'
  AND deny.deleted = b'0'
  AND deny.permission IN ('erp:supplier:query', 'erp:customer:query')
ORDER BY deny.permission;

SELECT
  '06_effective_result' AS section,
  user.id AS user_id,
  user.username,
  target.permission,
  CASE
    WHEN EXISTS (
      SELECT 1
      FROM system_user_role user_role
      JOIN system_role role
        ON role.id = user_role.role_id
       AND role.deleted = b'0'
       AND role.status = 0
       AND role.code = 'super_admin'
      WHERE user_role.user_id = user.id
        AND user_role.deleted = b'0'
    ) THEN 'PASS_SUPER_ADMIN'
    WHEN EXISTS (
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
       AND menu.permission = target.permission
      WHERE user_role.user_id = user.id
        AND user_role.deleted = b'0'
    )
    AND NOT EXISTS (
      SELECT 1
      FROM system_user_permission_deny deny
      WHERE deny.user_id = user.id
        AND deny.deleted = b'0'
        AND deny.permission = target.permission
    ) THEN 'PASS_ROLE_GRANTED'
    WHEN EXISTS (
      SELECT 1
      FROM system_user_permission_deny deny
      WHERE deny.user_id = user.id
        AND deny.deleted = b'0'
        AND deny.permission = target.permission
    ) THEN 'FAIL_USER_DENIED'
    ELSE 'FAIL_NOT_GRANTED'
  END AS effective_result
FROM system_users user
JOIN (
  SELECT 'erp:supplier:query' AS permission
  UNION ALL
  SELECT 'erp:customer:query' AS permission
) target
WHERE user.username = @login_username
  AND user.deleted = b'0'
ORDER BY target.permission;
