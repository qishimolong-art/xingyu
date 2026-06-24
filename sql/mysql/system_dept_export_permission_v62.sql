-- 部门管理导出按钮权限
-- 安全性：
-- 1. 不删除、不覆盖已有菜单或权限。
-- 2. 仅当部门管理菜单存在，且 system:dept:export 权限不存在时插入。
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
    `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
    '部门导出', 'system:dept:export', 3, 6, dept_menu.id, '', '', '',
    NULL, 0, b'1', b'1', b'1',
    '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` dept_menu
WHERE dept_menu.`permission` = ''
  AND dept_menu.`component` = 'system/dept/index'
  AND dept_menu.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu` existing
      WHERE existing.`permission` = 'system:dept:export'
        AND existing.`deleted` = b'0'
  )
LIMIT 1;
