-- ERP purchase order supplier-department permission diagnose (v166)
--
-- Purpose:
--   1. Find historical purchase orders whose supplier + dept combination will
--      fail the new supplier-dept validation.
--   2. Help verify the selected user's purchase-order dept permission against
--      one supplier during manual integration testing.
--
-- Safety:
--   - Read-only script.
--   - No INSERT / UPDATE / DELETE / DDL statements.
--
-- Usage:
--   1. Set @tenant_id to a tenant id, or keep NULL to scan all tenants.
--   2. Set @login_username and @supplier_id for account-specific verification.
--   3. Execute in the target MySQL database and inspect every result set.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @tenant_id := NULL;
SET @login_username := 'admin';
SET @supplier_id := NULL;

SELECT
  '00_parameters' AS section,
  @tenant_id AS tenant_id_filter,
  @login_username AS login_username,
  @supplier_id AS supplier_id_filter,
  'Set @supplier_id to verify one supplier for the selected user.' AS note;

SELECT
  '01_historical_summary' AS section,
  'orders_with_supplier_and_dept' AS metric,
  COUNT(*) AS value
FROM erp_purchase_order orders
WHERE orders.deleted = b'0'
  AND orders.supplier_id IS NOT NULL
  AND orders.dept_id IS NOT NULL
  AND (@tenant_id IS NULL OR orders.tenant_id = @tenant_id)

UNION ALL

SELECT
  '01_historical_summary' AS section,
  'invalid_supplier_dept_orders' AS metric,
  COUNT(*) AS value
FROM erp_purchase_order orders
LEFT JOIN erp_supplier supplier
  ON supplier.id = orders.supplier_id
 AND supplier.tenant_id = orders.tenant_id
 AND supplier.deleted = b'0'
LEFT JOIN system_dept order_dept
  ON order_dept.id = orders.dept_id
 AND order_dept.tenant_id = orders.tenant_id
 AND order_dept.deleted = b'0'
LEFT JOIN (
  SELECT supplier_dept.supplier_id, supplier_dept.tenant_id, supplier_dept.dept_id
  FROM (
    SELECT supplier_scope.id AS supplier_id, supplier_scope.tenant_id, supplier_scope.dept_id
    FROM erp_supplier supplier_scope
    WHERE supplier_scope.deleted = b'0'
      AND supplier_scope.dept_id IS NOT NULL
    UNION
    SELECT relation.supplier_id, relation.tenant_id, relation.dept_id
    FROM erp_supplier_dept relation
    WHERE relation.deleted = b'0'
  ) supplier_dept
  JOIN system_dept enabled_dept
    ON enabled_dept.id = supplier_dept.dept_id
   AND enabled_dept.tenant_id = supplier_dept.tenant_id
   AND enabled_dept.deleted = b'0'
   AND enabled_dept.status = 0
) available_dept
  ON available_dept.supplier_id = orders.supplier_id
 AND available_dept.tenant_id = orders.tenant_id
 AND available_dept.dept_id = orders.dept_id
WHERE orders.deleted = b'0'
  AND orders.supplier_id IS NOT NULL
  AND orders.dept_id IS NOT NULL
  AND (@tenant_id IS NULL OR orders.tenant_id = @tenant_id)
  AND (
       supplier.id IS NULL
    OR supplier.status <> 0
    OR order_dept.id IS NULL
    OR order_dept.status <> 0
    OR available_dept.dept_id IS NULL
  )

UNION ALL

SELECT
  '01_historical_summary' AS section,
  'draft_or_incomplete_orders_missing_supplier_or_dept' AS metric,
  COUNT(*) AS value
FROM erp_purchase_order orders
WHERE orders.deleted = b'0'
  AND (orders.supplier_id IS NULL OR orders.dept_id IS NULL)
  AND (@tenant_id IS NULL OR orders.tenant_id = @tenant_id);

SELECT
  '02_invalid_order_detail' AS section,
  orders.tenant_id,
  orders.id AS order_id,
  orders.no AS order_no,
  orders.status AS order_status,
  orders.supplier_id,
  supplier.name AS supplier_name,
  supplier.status AS supplier_status,
  orders.dept_id AS order_dept_id,
  order_dept.name AS order_dept_name,
  order_dept.status AS order_dept_status,
  supplier_available.enabled_dept_count AS supplier_enabled_dept_count,
  supplier_available.enabled_depts AS supplier_enabled_depts,
  CASE
    WHEN supplier.id IS NULL THEN 'SUPPLIER_MISSING_OR_DELETED'
    WHEN supplier.status <> 0 THEN 'SUPPLIER_DISABLED'
    WHEN order_dept.id IS NULL THEN 'ORDER_DEPT_MISSING_OR_DELETED'
    WHEN order_dept.status <> 0 THEN 'ORDER_DEPT_DISABLED'
    WHEN supplier_available.enabled_dept_count IS NULL
      OR supplier_available.enabled_dept_count = 0 THEN 'SUPPLIER_HAS_NO_ENABLED_DEPT'
    WHEN available_dept.dept_id IS NULL THEN 'ORDER_DEPT_NOT_IN_ENABLED_SUPPLIER_DEPT_SCOPE'
    ELSE 'OK'
  END AS risk_reason
FROM erp_purchase_order orders
LEFT JOIN erp_supplier supplier
  ON supplier.id = orders.supplier_id
 AND supplier.tenant_id = orders.tenant_id
 AND supplier.deleted = b'0'
LEFT JOIN system_dept order_dept
  ON order_dept.id = orders.dept_id
 AND order_dept.tenant_id = orders.tenant_id
 AND order_dept.deleted = b'0'
LEFT JOIN (
  SELECT supplier_dept.supplier_id, supplier_dept.tenant_id, supplier_dept.dept_id
  FROM (
    SELECT supplier_scope.id AS supplier_id, supplier_scope.tenant_id, supplier_scope.dept_id
    FROM erp_supplier supplier_scope
    WHERE supplier_scope.deleted = b'0'
      AND supplier_scope.dept_id IS NOT NULL
    UNION
    SELECT relation.supplier_id, relation.tenant_id, relation.dept_id
    FROM erp_supplier_dept relation
    WHERE relation.deleted = b'0'
  ) supplier_dept
  JOIN system_dept enabled_dept
    ON enabled_dept.id = supplier_dept.dept_id
   AND enabled_dept.tenant_id = supplier_dept.tenant_id
   AND enabled_dept.deleted = b'0'
   AND enabled_dept.status = 0
) available_dept
  ON available_dept.supplier_id = orders.supplier_id
 AND available_dept.tenant_id = orders.tenant_id
 AND available_dept.dept_id = orders.dept_id
LEFT JOIN (
  SELECT
    supplier_dept.supplier_id,
    supplier_dept.tenant_id,
    COUNT(*) AS enabled_dept_count,
    GROUP_CONCAT(CONCAT(supplier_dept.dept_id, ':', enabled_dept.name)
      ORDER BY supplier_dept.dept_id SEPARATOR ', ') AS enabled_depts
  FROM (
    SELECT supplier_scope.id AS supplier_id, supplier_scope.tenant_id, supplier_scope.dept_id
    FROM erp_supplier supplier_scope
    WHERE supplier_scope.deleted = b'0'
      AND supplier_scope.dept_id IS NOT NULL
    UNION
    SELECT relation.supplier_id, relation.tenant_id, relation.dept_id
    FROM erp_supplier_dept relation
    WHERE relation.deleted = b'0'
  ) supplier_dept
  JOIN system_dept enabled_dept
    ON enabled_dept.id = supplier_dept.dept_id
   AND enabled_dept.tenant_id = supplier_dept.tenant_id
   AND enabled_dept.deleted = b'0'
   AND enabled_dept.status = 0
  GROUP BY supplier_dept.supplier_id, supplier_dept.tenant_id
) supplier_available
  ON supplier_available.supplier_id = orders.supplier_id
 AND supplier_available.tenant_id = orders.tenant_id
WHERE orders.deleted = b'0'
  AND orders.supplier_id IS NOT NULL
  AND orders.dept_id IS NOT NULL
  AND (@tenant_id IS NULL OR orders.tenant_id = @tenant_id)
  AND (
       supplier.id IS NULL
    OR supplier.status <> 0
    OR order_dept.id IS NULL
    OR order_dept.status <> 0
    OR available_dept.dept_id IS NULL
  )
ORDER BY orders.tenant_id, orders.id
LIMIT 200;

SELECT
  '03_supplier_enabled_dept_scope' AS section,
  supplier.id AS supplier_id,
  supplier.name AS supplier_name,
  supplier.status AS supplier_status,
  supplier_dept.dept_id,
  enabled_dept.name AS dept_name,
  enabled_dept.status AS dept_status,
  supplier.tenant_id
FROM erp_supplier supplier
JOIN (
  SELECT supplier_scope.id AS supplier_id, supplier_scope.tenant_id, supplier_scope.dept_id
  FROM erp_supplier supplier_scope
  WHERE supplier_scope.deleted = b'0'
    AND supplier_scope.dept_id IS NOT NULL
  UNION
  SELECT relation.supplier_id, relation.tenant_id, relation.dept_id
  FROM erp_supplier_dept relation
  WHERE relation.deleted = b'0'
) supplier_dept
  ON supplier_dept.supplier_id = supplier.id
 AND supplier_dept.tenant_id = supplier.tenant_id
JOIN system_dept enabled_dept
  ON enabled_dept.id = supplier_dept.dept_id
 AND enabled_dept.tenant_id = supplier_dept.tenant_id
 AND enabled_dept.deleted = b'0'
 AND enabled_dept.status = 0
WHERE supplier.deleted = b'0'
  AND (@tenant_id IS NULL OR supplier.tenant_id = @tenant_id)
  AND (@supplier_id IS NULL OR supplier.id = @supplier_id)
ORDER BY supplier.tenant_id, supplier.id, supplier_dept.dept_id
LIMIT 500;

SELECT
  '04_user_purchase_order_permission_scope_rows' AS section,
  users.id AS user_id,
  users.username,
  users.nickname,
  users.tenant_id,
  role.id AS role_id,
  role.name AS role_name,
  role.code AS role_code,
  role.status AS role_status,
  role.data_scope AS role_default_data_scope,
  role.data_scope_dept_ids AS role_default_dept_ids,
  form_scope.data_scope AS purchase_order_form_data_scope,
  form_scope.data_scope_dept_ids AS purchase_order_form_dept_ids,
  CASE
    WHEN EXISTS (
      SELECT 1
      FROM system_user_role active_user_role
      JOIN system_role active_role
        ON active_role.id = active_user_role.role_id
       AND active_role.deleted = b'0'
       AND active_role.status = 0
      JOIN system_role_form_data_scope active_scope
        ON active_scope.role_id = active_role.id
       AND active_scope.form_key = 'erp_purchase_order'
       AND active_scope.deleted = b'0'
       AND active_scope.data_scope IS NOT NULL
       AND active_scope.data_scope <> 0
      WHERE active_user_role.user_id = users.id
        AND active_user_role.deleted = b'0'
    ) THEN 'USE_FORM_SCOPE'
    ELSE 'FALLBACK_ROLE_SCOPE'
  END AS effective_source
FROM system_users users
JOIN system_user_role user_role
  ON user_role.user_id = users.id
 AND user_role.deleted = b'0'
JOIN system_role role
  ON role.id = user_role.role_id
 AND role.deleted = b'0'
LEFT JOIN system_role_form_data_scope form_scope
  ON form_scope.role_id = role.id
 AND form_scope.form_key = 'erp_purchase_order'
 AND form_scope.deleted = b'0'
WHERE users.username = @login_username
  AND users.deleted = b'0'
  AND (@tenant_id IS NULL OR users.tenant_id = @tenant_id)
ORDER BY role.id;

SELECT
  '05_user_supplier_visible_dept_audit' AS section,
  users.id AS user_id,
  users.username,
  supplier.id AS supplier_id,
  supplier.name AS supplier_name,
  supplier_dept.dept_id,
  enabled_dept.name AS dept_name,
  CASE
    WHEN @supplier_id IS NULL THEN 'SKIPPED_SET_SUPPLIER_ID'
    WHEN EXISTS (
      SELECT 1
      FROM system_user_role active_user_role
      JOIN system_role active_role
        ON active_role.id = active_user_role.role_id
       AND active_role.deleted = b'0'
       AND active_role.status = 0
      JOIN system_role_form_data_scope active_scope
        ON active_scope.role_id = active_role.id
       AND active_scope.form_key = 'erp_purchase_order'
       AND active_scope.deleted = b'0'
       AND active_scope.data_scope = 1
      WHERE active_user_role.user_id = users.id
        AND active_user_role.deleted = b'0'
    ) THEN 'VISIBLE_ALL_FORM_SCOPE'
    WHEN NOT EXISTS (
      SELECT 1
      FROM system_user_role active_user_role
      JOIN system_role active_role
        ON active_role.id = active_user_role.role_id
       AND active_role.deleted = b'0'
       AND active_role.status = 0
      JOIN system_role_form_data_scope active_scope
        ON active_scope.role_id = active_role.id
       AND active_scope.form_key = 'erp_purchase_order'
       AND active_scope.deleted = b'0'
       AND active_scope.data_scope IS NOT NULL
       AND active_scope.data_scope <> 0
      WHERE active_user_role.user_id = users.id
        AND active_user_role.deleted = b'0'
    ) AND EXISTS (
      SELECT 1
      FROM system_user_role role_user_role
      JOIN system_role role_scope
        ON role_scope.id = role_user_role.role_id
       AND role_scope.deleted = b'0'
       AND role_scope.status = 0
       AND role_scope.data_scope = 1
      WHERE role_user_role.user_id = users.id
        AND role_user_role.deleted = b'0'
    ) THEN 'VISIBLE_ALL_ROLE_SCOPE'
    WHEN EXISTS (
      SELECT 1
      FROM system_user_role active_user_role
      JOIN system_role active_role
        ON active_role.id = active_user_role.role_id
       AND active_role.deleted = b'0'
       AND active_role.status = 0
      JOIN system_role_form_data_scope active_scope
        ON active_scope.role_id = active_role.id
       AND active_scope.form_key = 'erp_purchase_order'
       AND active_scope.deleted = b'0'
       AND active_scope.data_scope = 2
      WHERE active_user_role.user_id = users.id
        AND active_user_role.deleted = b'0'
        AND JSON_CONTAINS(IF(JSON_VALID(active_scope.data_scope_dept_ids),
          active_scope.data_scope_dept_ids, '[]'), CAST(enabled_dept.id AS CHAR), '$')
    ) THEN 'VISIBLE_FORM_CUSTOM_DEPT'
    WHEN NOT EXISTS (
      SELECT 1
      FROM system_user_role active_user_role
      JOIN system_role active_role
        ON active_role.id = active_user_role.role_id
       AND active_role.deleted = b'0'
       AND active_role.status = 0
      JOIN system_role_form_data_scope active_scope
        ON active_scope.role_id = active_role.id
       AND active_scope.form_key = 'erp_purchase_order'
       AND active_scope.deleted = b'0'
       AND active_scope.data_scope IS NOT NULL
       AND active_scope.data_scope <> 0
      WHERE active_user_role.user_id = users.id
        AND active_user_role.deleted = b'0'
    ) AND EXISTS (
      SELECT 1
      FROM system_user_role role_user_role
      JOIN system_role role_scope
        ON role_scope.id = role_user_role.role_id
       AND role_scope.deleted = b'0'
       AND role_scope.status = 0
       AND role_scope.data_scope = 2
      WHERE role_user_role.user_id = users.id
        AND role_user_role.deleted = b'0'
        AND JSON_CONTAINS(IF(JSON_VALID(role_scope.data_scope_dept_ids),
          role_scope.data_scope_dept_ids, '[]'), CAST(enabled_dept.id AS CHAR), '$')
    ) THEN 'VISIBLE_ROLE_CUSTOM_DEPT'
    WHEN EXISTS (
      SELECT 1
      FROM system_user_role active_user_role
      JOIN system_role active_role
        ON active_role.id = active_user_role.role_id
       AND active_role.deleted = b'0'
       AND active_role.status = 0
      JOIN system_role_form_data_scope active_scope
        ON active_scope.role_id = active_role.id
       AND active_scope.form_key = 'erp_purchase_order'
       AND active_scope.deleted = b'0'
       AND active_scope.data_scope IN (3, 4)
      WHERE active_user_role.user_id = users.id
        AND active_user_role.deleted = b'0'
        AND EXISTS (
          SELECT 1
          FROM (
            SELECT main_user.id AS user_id, main_user.dept_id
            FROM system_users main_user
            WHERE main_user.deleted = b'0'
              AND main_user.dept_id IS NOT NULL
            UNION
            SELECT user_dept.user_id, user_dept.dept_id
            FROM system_user_dept user_dept
            WHERE user_dept.deleted = b'0'
          ) user_dept_scope
          WHERE user_dept_scope.user_id = users.id
            AND (
                 (active_scope.data_scope = 3 AND enabled_dept.id = user_dept_scope.dept_id)
              OR (active_scope.data_scope = 4 AND (
                     enabled_dept.id = user_dept_scope.dept_id
                  OR enabled_dept.parent_id = user_dept_scope.dept_id
                  OR parent_dept.id = user_dept_scope.dept_id
                  OR grand_parent_dept.id = user_dept_scope.dept_id
                 ))
            )
        )
    ) THEN 'VISIBLE_FORM_USER_DEPT_OR_CHILD_SQL_AUDIT'
    WHEN NOT EXISTS (
      SELECT 1
      FROM system_user_role active_user_role
      JOIN system_role active_role
        ON active_role.id = active_user_role.role_id
       AND active_role.deleted = b'0'
       AND active_role.status = 0
      JOIN system_role_form_data_scope active_scope
        ON active_scope.role_id = active_role.id
       AND active_scope.form_key = 'erp_purchase_order'
       AND active_scope.deleted = b'0'
       AND active_scope.data_scope IS NOT NULL
       AND active_scope.data_scope <> 0
      WHERE active_user_role.user_id = users.id
        AND active_user_role.deleted = b'0'
    ) AND EXISTS (
      SELECT 1
      FROM system_user_role role_user_role
      JOIN system_role role_scope
        ON role_scope.id = role_user_role.role_id
       AND role_scope.deleted = b'0'
       AND role_scope.status = 0
       AND role_scope.data_scope IN (3, 4)
      WHERE role_user_role.user_id = users.id
        AND role_user_role.deleted = b'0'
        AND EXISTS (
          SELECT 1
          FROM (
            SELECT main_user.id AS user_id, main_user.dept_id
            FROM system_users main_user
            WHERE main_user.deleted = b'0'
              AND main_user.dept_id IS NOT NULL
            UNION
            SELECT user_dept.user_id, user_dept.dept_id
            FROM system_user_dept user_dept
            WHERE user_dept.deleted = b'0'
          ) user_dept_scope
          WHERE user_dept_scope.user_id = users.id
            AND (
                 (role_scope.data_scope = 3 AND enabled_dept.id = user_dept_scope.dept_id)
              OR (role_scope.data_scope = 4 AND (
                     enabled_dept.id = user_dept_scope.dept_id
                  OR enabled_dept.parent_id = user_dept_scope.dept_id
                  OR parent_dept.id = user_dept_scope.dept_id
                  OR grand_parent_dept.id = user_dept_scope.dept_id
                 ))
            )
        )
    ) THEN 'VISIBLE_ROLE_USER_DEPT_OR_CHILD_SQL_AUDIT'
    ELSE 'NOT_VISIBLE_BY_SQL_AUDIT'
  END AS visibility_audit,
  'Backend permissionApi remains the final source for deep dept trees and SELF scope.' AS audit_note
FROM system_users users
JOIN erp_supplier supplier
  ON supplier.id = @supplier_id
 AND supplier.deleted = b'0'
 AND (@tenant_id IS NULL OR supplier.tenant_id = @tenant_id)
JOIN (
  SELECT supplier_scope.id AS supplier_id, supplier_scope.tenant_id, supplier_scope.dept_id
  FROM erp_supplier supplier_scope
  WHERE supplier_scope.deleted = b'0'
    AND supplier_scope.dept_id IS NOT NULL
  UNION
  SELECT relation.supplier_id, relation.tenant_id, relation.dept_id
  FROM erp_supplier_dept relation
  WHERE relation.deleted = b'0'
) supplier_dept
  ON supplier_dept.supplier_id = supplier.id
 AND supplier_dept.tenant_id = supplier.tenant_id
JOIN system_dept enabled_dept
  ON enabled_dept.id = supplier_dept.dept_id
 AND enabled_dept.tenant_id = supplier_dept.tenant_id
 AND enabled_dept.deleted = b'0'
 AND enabled_dept.status = 0
LEFT JOIN system_dept parent_dept
  ON parent_dept.id = enabled_dept.parent_id
 AND parent_dept.tenant_id = enabled_dept.tenant_id
 AND parent_dept.deleted = b'0'
LEFT JOIN system_dept grand_parent_dept
  ON grand_parent_dept.id = parent_dept.parent_id
 AND grand_parent_dept.tenant_id = parent_dept.tenant_id
 AND grand_parent_dept.deleted = b'0'
WHERE users.username = @login_username
  AND users.deleted = b'0'
  AND (@tenant_id IS NULL OR users.tenant_id = @tenant_id)
ORDER BY supplier.tenant_id, supplier.id, supplier_dept.dept_id;

SELECT
  '06_purchase_order_query_permission_chain' AS section,
  users.id AS user_id,
  users.username,
  role.id AS role_id,
  role.name AS role_name,
  menu.id AS menu_id,
  menu.name AS menu_name,
  menu.permission,
  CASE
    WHEN role.code = 'super_admin' THEN 'HAS_QUERY_PERMISSION_SUPER_ADMIN'
    WHEN menu.id IS NULL THEN 'MISSING_QUERY_PERMISSION'
    ELSE 'HAS_QUERY_PERMISSION'
  END AS permission_audit
FROM system_users users
JOIN system_user_role user_role
  ON user_role.user_id = users.id
 AND user_role.deleted = b'0'
JOIN system_role role
  ON role.id = user_role.role_id
 AND role.deleted = b'0'
 AND role.status = 0
LEFT JOIN system_role_menu role_menu
  ON role_menu.role_id = role.id
 AND role_menu.deleted = b'0'
LEFT JOIN system_menu menu
  ON menu.id = role_menu.menu_id
 AND menu.deleted = b'0'
 AND menu.permission = 'erp:purchase-order:query'
WHERE users.username = @login_username
  AND users.deleted = b'0'
  AND (@tenant_id IS NULL OR users.tenant_id = @tenant_id)
ORDER BY role.id, menu.id;
