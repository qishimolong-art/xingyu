-- 恢复角色管理中的表单数据权限配置入口。
-- 背景：当前产品方向为“角色按表单配置数据权限，用户管理隐藏用户级数据权限”。

UPDATE `system_menu`
SET `status` = 0,
    `visible` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'system:permission:assign-role-data-scope'
  AND `type` = 3
  AND `deleted` = b'0';

UPDATE `system_role_menu`
SET `deleted` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `menu_id` = 1064
  AND `deleted` = b'1';

UPDATE `system_tenant_package`
SET `menu_ids` = CASE
        WHEN `menu_ids` IS NULL OR `menu_ids` = '' OR `menu_ids` = '[]' THEN '[1064]'
        ELSE INSERT(`menu_ids`, LENGTH(`menu_ids`), 0, ',1064')
    END,
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND NOT (`menu_ids` = '[1064]'
       OR `menu_ids` LIKE '[1064,%'
       OR `menu_ids` LIKE '%,1064,%'
       OR `menu_ids` LIKE '%,1064]');
