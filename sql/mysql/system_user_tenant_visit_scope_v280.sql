-- 平台用户“可访问租户范围”幂等迁移（v280）。
-- 不初始化任何用户范围，不给普通角色或租户套餐授权。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_system_user_tenant_visit_scope_v280;
DELIMITER //
CREATE PROCEDURE migrate_system_user_tenant_visit_scope_v280()
BEGIN
    DECLARE menu_id_conflicts INT DEFAULT 0;
    DECLARE permission_conflicts INT DEFAULT 0;
    DECLARE missing_columns INT DEFAULT 0;

    IF DATABASE() IS NULL OR DATABASE() = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v280: no target database selected';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'system_menu')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'system_users') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v280: required system tables are missing';
    END IF;

    SELECT COUNT(*) INTO menu_id_conflicts
    FROM system_menu
    WHERE id = 900676
      AND NOT (permission = 'system:tenant:visit-scope' AND deleted = b'0');
    SELECT COUNT(*) INTO permission_conflicts
    FROM system_menu
    WHERE permission = 'system:tenant:visit-scope'
      AND id <> 900676 AND deleted = b'0';
    IF menu_id_conflicts > 0 OR permission_conflicts > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v280: menu id 900676 or permission code is already occupied';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 100 AND deleted = b'0') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v280: parent user menu 100 is missing';
    END IF;

    CREATE TABLE IF NOT EXISTS `system_user_tenant_visit_scope` (
      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
      `user_id` BIGINT NOT NULL COMMENT '用户编号',
      `scope_type` VARCHAR(16) NOT NULL COMMENT '范围类型：ALL/CUSTOM',
      `tenant_ids` TEXT NOT NULL COMMENT '指定租户编号 JSON 集合',
      `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
      `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '所属租户',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`, `deleted`),
      KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台用户租户访问范围';

    SELECT 10 - COUNT(DISTINCT column_name) INTO missing_columns
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'system_user_tenant_visit_scope'
      AND column_name IN ('id', 'user_id', 'scope_type', 'tenant_ids', 'creator',
                          'create_time', 'updater', 'update_time', 'deleted', 'tenant_id');
    IF missing_columns <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v280: existing scope table has incompatible columns';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'system_user_tenant_visit_scope'
                     AND index_name = 'uk_tenant_user' AND non_unique = 0) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v280: unique index uk_tenant_user is missing';
    END IF;

    START TRANSACTION;
    INSERT INTO system_menu
        (id, name, permission, type, sort, parent_id, path, icon, component,
         component_name, status, visible, keep_alive, always_show,
         creator, create_time, updater, update_time, deleted)
    SELECT 900676, '配置租户访问范围', 'system:tenant:visit-scope', 3, 8, 100,
           '', '', '', NULL, 0, b'1', b'1', b'1',
           '1', NOW(), '1', NOW(), b'0'
    WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 900676);

    IF NOT EXISTS (SELECT 1 FROM system_menu
                   WHERE id = 900676 AND permission = 'system:tenant:visit-scope'
                     AND parent_id = 100 AND type = 3 AND deleted = b'0') THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v280: menu post-check failed';
    END IF;
    COMMIT;

    SELECT COUNT(*) AS scope_row_count FROM system_user_tenant_visit_scope;
    SELECT id, name, permission, parent_id, type, status, deleted
    FROM system_menu WHERE id = 900676;
END //
DELIMITER ;

CALL migrate_system_user_tenant_visit_scope_v280();
DROP PROCEDURE IF EXISTS migrate_system_user_tenant_visit_scope_v280;
