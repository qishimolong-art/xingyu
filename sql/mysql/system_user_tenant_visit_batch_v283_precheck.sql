-- 用户批量设置可访问租户迁移前检查（v283）。
-- 只读脚本：任一 conflict_count 非 0 时不得执行迁移。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database;

SELECT 'visit_menu_5010_missing_or_changed' AS check_item, IF(EXISTS(
    SELECT 1 FROM system_menu
    WHERE id = 5010 AND permission = 'system:tenant:visit'
      AND status = 0 AND deleted = b'0'
), 0, 1) AS conflict_count
UNION ALL
SELECT 'managed_role_code_conflict', COUNT(*)
FROM system_role
WHERE tenant_id = 1 AND code = 'tenant_visit_user' AND deleted = b'0'
  AND NOT (name = '租户访问用户（系统自动）' AND status = 0 AND type = 1)
UNION ALL
SELECT 'managed_role_name_conflict', COUNT(*)
FROM system_role
WHERE tenant_id = 1 AND name = '租户访问用户（系统自动）' AND deleted = b'0'
  AND code <> 'tenant_visit_user'
UNION ALL
SELECT 'managed_role_duplicate', GREATEST(COUNT(*) - 1, 0)
FROM system_role
WHERE tenant_id = 1 AND code = 'tenant_visit_user' AND deleted = b'0'
UNION ALL
SELECT 'managed_role_extra_menu_conflict', COUNT(*)
FROM system_role r
JOIN system_role_menu rm ON rm.role_id = r.id AND rm.deleted = b'0'
WHERE r.tenant_id = 1 AND r.code = 'tenant_visit_user' AND r.deleted = b'0'
  AND rm.menu_id <> 5010;

SELECT r.id, r.name, r.code, r.status, r.type, r.tenant_id,
       GROUP_CONCAT(m.permission ORDER BY m.permission) AS current_permissions
FROM system_role r
LEFT JOIN system_role_menu rm ON rm.role_id = r.id AND rm.deleted = b'0'
LEFT JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = b'0'
WHERE r.tenant_id = 1 AND r.code = 'tenant_visit_user' AND r.deleted = b'0'
GROUP BY r.id, r.name, r.code, r.status, r.type, r.tenant_id;

SELECT COUNT(*) AS existing_user_assignments
FROM system_user_role ur
JOIN system_role r ON r.id = ur.role_id
WHERE r.tenant_id = 1 AND r.code = 'tenant_visit_user'
  AND r.deleted = b'0' AND ur.deleted = b'0';
