-- 小程序首页公告独立表（v194）
-- 安全说明：
--   - 仅新增 mall_app_notice 表、小程序公告菜单和按钮权限。
--   - 不删除、不覆盖原 system_notice 菜单和 system:notice:* 权限。
--   - 不写入 system_role_menu，执行后请在角色管理中手动授权。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `mall_app_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告编号',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告内容',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '公告状态（0开启 1关闭）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序，数字越大越靠前',
  `start_time` datetime DEFAULT NULL COMMENT '展示开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '展示结束时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_mall_app_notice_home` (`tenant_id`, `deleted`, `status`, `sort`, `create_time`, `id`),
  KEY `idx_mall_app_notice_time` (`start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小程序公告表';

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '小程序公告',
       '',
       2,
       5,
       parent.id,
       'app-notice',
       'ep:bell',
       'system/app-notice/index',
       'SystemAppNotice',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM `system_menu` parent
WHERE parent.deleted = b'0'
  AND parent.parent_id = 1
  AND parent.path = 'messages'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` exists_menu
    WHERE exists_menu.deleted = b'0'
      AND exists_menu.parent_id = parent.id
      AND exists_menu.path = 'app-notice'
      AND exists_menu.component = 'system/app-notice/index'
  );

SET @mall_app_notice_menu_id := (
  SELECT id
  FROM `system_menu`
  WHERE deleted = b'0'
    AND path = 'app-notice'
    AND component = 'system/app-notice/index'
  ORDER BY id DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT source.name,
       source.permission,
       3,
       source.sort,
       @mall_app_notice_menu_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM (
  SELECT '小程序公告查询' AS name, 'system:app-notice:query' AS permission, 1 AS sort
  UNION ALL SELECT '小程序公告新增', 'system:app-notice:create', 2
  UNION ALL SELECT '小程序公告修改', 'system:app-notice:update', 3
  UNION ALL SELECT '小程序公告删除', 'system:app-notice:delete', 4
) source
WHERE @mall_app_notice_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` exists_menu
    WHERE exists_menu.deleted = b'0'
      AND exists_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
  );

SELECT id, name, permission, type, sort, parent_id, path, component, component_name
FROM `system_menu`
WHERE deleted = b'0'
  AND (
    component = 'system/app-notice/index'
    OR permission IN (
      'system:app-notice:query',
      'system:app-notice:create',
      'system:app-notice:update',
      'system:app-notice:delete'
    )
  )
ORDER BY type, sort, id;
