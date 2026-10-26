-- 平台用户“可访问租户范围”迁移前检查（v280）。
-- 只读脚本：不修改数据；任何 conflict_count 非 0 时不得执行迁移。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database;

SELECT 'menu_id_900676_occupied' AS check_item, COUNT(*) AS conflict_count
FROM system_menu
WHERE id = 900676
  AND NOT (permission = 'system:tenant:visit-scope' AND deleted = b'0')
UNION ALL
SELECT 'permission_code_used_by_other_menu', COUNT(*)
FROM system_menu
WHERE permission = 'system:tenant:visit-scope'
  AND id <> 900676
  AND deleted = b'0'
UNION ALL
SELECT 'user_menu_100_missing', IF(EXISTS(
    SELECT 1 FROM system_menu WHERE id = 100 AND deleted = b'0'
), 0, 1)
UNION ALL
SELECT 'visit_permission_missing', IF(EXISTS(
    SELECT 1 FROM system_menu
    WHERE permission = 'system:tenant:visit' AND status = 0 AND deleted = b'0'
), 0, 1);

SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'system_user_tenant_visit_scope'
ORDER BY ordinal_position;

SELECT index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'system_user_tenant_visit_scope'
GROUP BY index_name, non_unique
ORDER BY index_name;

SELECT COALESCE(MAX(table_rows), 0) AS estimated_existing_scope_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'system_user_tenant_visit_scope';

SELECT u.id, u.username, r.code AS role_code
FROM system_users u
JOIN system_user_role ur ON ur.user_id = u.id AND ur.deleted = b'0'
JOIN system_role r ON r.id = ur.role_id AND r.deleted = b'0' AND r.status = 0
WHERE u.tenant_id = 1 AND u.deleted = b'0' AND u.status = 0
  AND (r.code = 'super_admin' OR EXISTS (
      SELECT 1
      FROM system_role_menu rm
      JOIN system_menu m ON m.id = rm.menu_id
      WHERE rm.role_id = r.id AND rm.deleted = b'0'
        AND m.permission = 'system:tenant:visit' AND m.status = 0 AND m.deleted = b'0'
  ))
ORDER BY u.id;
