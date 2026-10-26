-- 用户批量设置可访问租户幂等迁移（v283）。
-- 创建租户 1 专用系统自动角色，仅授予 system:tenant:visit；不分配给任何现有用户。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_system_user_tenant_visit_batch_v283;
DELIMITER //
CREATE PROCEDURE migrate_system_user_tenant_visit_batch_v283()
BEGIN
    DECLARE managed_role_id BIGINT DEFAULT NULL;
    DECLARE role_conflicts INT DEFAULT 0;

    IF DATABASE() IS NULL OR DATABASE() = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v283: no target database selected';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM system_menu
                   WHERE id = 5010 AND permission = 'system:tenant:visit'
                     AND status = 0 AND deleted = b'0') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v283: system:tenant:visit menu 5010 is missing or disabled';
    END IF;

    SELECT COUNT(*) INTO role_conflicts
    FROM system_role
    WHERE tenant_id = 1 AND deleted = b'0'
      AND ((code = 'tenant_visit_user'
            AND NOT (name = '租户访问用户（系统自动）' AND status = 0 AND type = 1))
        OR (name = '租户访问用户（系统自动）' AND code <> 'tenant_visit_user'));
    IF role_conflicts > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v283: managed tenant visit role name or code conflict';
    END IF;
    IF (SELECT COUNT(*) FROM system_role
        WHERE tenant_id = 1 AND code = 'tenant_visit_user' AND deleted = b'0') > 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v283: duplicate managed tenant visit roles';
    END IF;

    START TRANSACTION;
    INSERT INTO system_role
        (name, code, sort, data_scope, data_scope_dept_ids, status, type, remark,
         creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT '租户访问用户（系统自动）', 'tenant_visit_user', 999, 1, '', 0, 1,
           '由租户访问范围自动维护，不允许手工分配',
           '1', NOW(), '1', NOW(), b'0', 1
    WHERE NOT EXISTS (
        SELECT 1 FROM system_role
        WHERE tenant_id = 1 AND code = 'tenant_visit_user' AND deleted = b'0'
    );

    SELECT id INTO managed_role_id
    FROM system_role
    WHERE tenant_id = 1 AND code = 'tenant_visit_user' AND deleted = b'0'
    LIMIT 1;

    INSERT INTO system_role_menu
        (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT managed_role_id, 5010, '1', NOW(), '1', NOW(), b'0', 1
    WHERE NOT EXISTS (
        SELECT 1 FROM system_role_menu
        WHERE role_id = managed_role_id AND menu_id = 5010 AND deleted = b'0'
    );

    IF managed_role_id IS NULL
       OR (SELECT COUNT(*) FROM system_role_menu rm
           WHERE rm.role_id = managed_role_id AND rm.deleted = b'0') <> 1
       OR NOT EXISTS (SELECT 1 FROM system_role_menu rm
                      WHERE rm.role_id = managed_role_id AND rm.menu_id = 5010
                        AND rm.deleted = b'0') THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v283: managed role post-check failed';
    END IF;
    COMMIT;

    SELECT id, name, code, status, type, tenant_id
    FROM system_role WHERE id = managed_role_id;
    SELECT rm.role_id, rm.menu_id, m.name, m.permission
    FROM system_role_menu rm
    JOIN system_menu m ON m.id = rm.menu_id
    WHERE rm.role_id = managed_role_id AND rm.deleted = b'0';
    SELECT COUNT(*) AS existing_user_assignments
    FROM system_user_role
    WHERE role_id = managed_role_id AND deleted = b'0';
END //
DELIMITER ;

CALL migrate_system_user_tenant_visit_batch_v283();
DROP PROCEDURE IF EXISTS migrate_system_user_tenant_visit_batch_v283;
