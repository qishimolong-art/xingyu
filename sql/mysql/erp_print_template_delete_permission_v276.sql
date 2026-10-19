-- ERP 打印模板删除权限（v276）。
-- 可重复执行；只新增/校正一个独立的删除权限并授权给超级管理员。
-- 不删除模板、打印记录、菜单、角色或既有角色权限。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS apply_erp_print_template_delete_permission_v276;
DELIMITER $$

CREATE PROCEDURE apply_erp_print_template_delete_permission_v276()
BEGIN
  DECLARE erp_root_count INT DEFAULT 0;
  DECLARE system_config_count INT DEFAULT 0;
  DECLARE permission_count INT DEFAULT 0;
  DECLARE permission_menu_id BIGINT DEFAULT NULL;
  DECLARE system_config_id BIGINT DEFAULT NULL;
  DECLARE super_role_count INT DEFAULT 0;
  DECLARE granted_super_role_count INT DEFAULT 0;

  SELECT COUNT(*), MIN(root_menu.`id`)
    INTO erp_root_count, @erp_root_id
  FROM `system_menu` root_menu
  WHERE root_menu.`name` = 'ERP 系统'
    AND root_menu.`type` = 1
    AND root_menu.`deleted` = b'0';

  IF erp_root_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'v276 precheck failed: ERP 系统根菜单必须且只能有一个';
  END IF;

  SELECT COUNT(*), MIN(config_menu.`id`)
    INTO system_config_count, system_config_id
  FROM `system_menu` config_menu
  WHERE config_menu.`name` = '系统配置'
    AND config_menu.`type` = 1
    AND config_menu.`parent_id` = @erp_root_id
    AND config_menu.`deleted` = b'0';

  IF system_config_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'v276 precheck failed: ERP 系统下的系统配置菜单必须且只能有一个';
  END IF;

  SELECT COUNT(*)
    INTO permission_count
  FROM `system_menu` menu
  WHERE menu.`permission` = 'erp:print-template:delete'
    AND menu.`deleted` = b'0';

  IF permission_count > 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'v276 precheck failed: 删除打印模板权限存在重复菜单';
  END IF;

  START TRANSACTION;

  INSERT INTO `system_menu`
  (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
   `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT '删除打印模板', 'erp:print-template:delete', 3, 90, system_config_id,
         '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
  FROM DUAL
  WHERE permission_count = 0;

  SELECT menu.`id`
    INTO permission_menu_id
  FROM `system_menu` menu
  WHERE menu.`permission` = 'erp:print-template:delete'
    AND menu.`deleted` = b'0'
  ORDER BY menu.`id` DESC
  LIMIT 1;

  UPDATE `system_menu`
  SET `name` = '删除打印模板',
      `type` = 3,
      `sort` = 90,
      `parent_id` = system_config_id,
      `status` = 0,
      `visible` = b'1',
      `updater` = '1',
      `update_time` = NOW()
  WHERE `id` = permission_menu_id
    AND (`name` <> '删除打印模板'
      OR `type` <> 3
      OR `sort` <> 90
      OR `parent_id` <> system_config_id
      OR `status` <> 0
      OR `visible` <> b'1');

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT role.`id`, permission_menu_id, '1', NOW(), '1', NOW(), b'0', role.`tenant_id`
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` role_menu
      WHERE role_menu.`role_id` = role.`id`
        AND role_menu.`menu_id` = permission_menu_id
        AND role_menu.`tenant_id` = role.`tenant_id`
        AND role_menu.`deleted` = b'0'
    );

  SELECT COUNT(*)
    INTO permission_count
  FROM `system_menu` menu
  WHERE menu.`permission` = 'erp:print-template:delete'
    AND menu.`name` = '删除打印模板'
    AND menu.`type` = 3
    AND menu.`parent_id` = system_config_id
    AND menu.`status` = 0
    AND menu.`deleted` = b'0';

  SELECT COUNT(*)
    INTO super_role_count
  FROM `system_role` role
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0';

  SELECT COUNT(DISTINCT role.`id`)
    INTO granted_super_role_count
  FROM `system_role` role
  JOIN `system_role_menu` role_menu
    ON role_menu.`role_id` = role.`id`
   AND role_menu.`tenant_id` = role.`tenant_id`
   AND role_menu.`menu_id` = permission_menu_id
   AND role_menu.`deleted` = b'0'
  WHERE role.`code` = 'super_admin'
    AND role.`deleted` = b'0';

  IF permission_count <> 1 OR granted_super_role_count <> super_role_count THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'v276 postcheck failed: 权限菜单或超级管理员授权结果不符合预期';
  ELSE
    COMMIT;
  END IF;

  SELECT menu.`id`, menu.`name`, menu.`permission`, menu.`parent_id`, menu.`status`,
         super_role_count AS `super_role_count`,
         granted_super_role_count AS `granted_super_role_count`
  FROM `system_menu` menu
  WHERE menu.`id` = permission_menu_id;
END$$

DELIMITER ;
CALL apply_erp_print_template_delete_permission_v276();
DROP PROCEDURE IF EXISTS apply_erp_print_template_delete_permission_v276;
