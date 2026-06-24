-- Add user-level data scope for system_users.
-- Safe to execute repeatedly on MySQL 5.7 / 8.0.
-- NULL / 0 means inherit role data scope.

DROP PROCEDURE IF EXISTS system_user_data_scope_v63_apply;

DELIMITER $$
CREATE PROCEDURE system_user_data_scope_v63_apply()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'system_users'
          AND COLUMN_NAME = 'data_scope'
    ) THEN
        ALTER TABLE `system_users`
            ADD COLUMN `data_scope` tinyint DEFAULT NULL COMMENT '用户数据范围，空表示继承角色数据范围' AFTER `status`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'system_users'
          AND COLUMN_NAME = 'data_scope_dept_ids'
    ) THEN
        ALTER TABLE `system_users`
            ADD COLUMN `data_scope_dept_ids` varchar(500) DEFAULT '[]' COMMENT '用户数据范围(指定部门数组)' AFTER `data_scope`;
    END IF;
END$$
DELIMITER ;

CALL system_user_data_scope_v63_apply();
DROP PROCEDURE IF EXISTS system_user_data_scope_v63_apply;

UPDATE `system_users`
SET `data_scope` = NULL,
    `data_scope_dept_ids` = '[]'
WHERE `deleted` = b'0'
  AND (`data_scope` IS NULL OR `data_scope` = 0);

UPDATE `system_field_definition`
SET `field_label` = '数据范围',
    `field_group` = 'main_form',
    `sort` = 50,
    `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `module` = 'system_users'
  AND `field_key` = 'dataScope'
  AND `tenant_id` = 1
  AND `deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'system_users', 'dataScope', '数据范围', 'main_form', 50, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `system_field_definition`
  WHERE `module` = 'system_users'
    AND `field_key` = 'dataScope'
    AND `tenant_id` = 1
    AND `deleted` = b'0'
);

UPDATE `system_field_definition`
SET `field_label` = '数据范围',
    `field_group` = 'list_col',
    `sort` = 155,
    `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `module` = 'system_users'
  AND `field_key` = 'col_dataScope'
  AND `tenant_id` = 1
  AND `deleted` = b'0';

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'system_users', 'col_dataScope', '数据范围', 'list_col', 155, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `system_field_definition`
  WHERE `module` = 'system_users'
    AND `field_key` = 'col_dataScope'
    AND `tenant_id` = 1
    AND `deleted` = b'0'
);
