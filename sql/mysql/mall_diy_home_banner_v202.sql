-- 首页轮播图独立管理（v202）
-- 安全说明：
--   - 仅新增 promotion_diy_home_banner 表、商城装修下的首页轮播图菜单和按钮权限。
--   - 不删除、不覆盖原 promotion_banner 表、Banner 菜单和 promotion:banner:* 权限。
--   - 不写入 system_role_menu，执行后请在角色管理中手动授权。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `promotion_diy_home_banner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '首页轮播图编号',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '标题',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '跳转链接',
  `pic_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片地址',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序，数字越大越靠前',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0开启 1关闭）',
  `memo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_promotion_diy_home_banner_home` (`tenant_id`, `deleted`, `status`, `sort`, `id`),
  KEY `idx_promotion_diy_home_banner_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '首页轮播图表';

ALTER TABLE `promotion_diy_home_banner`
  MODIFY `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '标题',
  MODIFY `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '跳转链接';

SET @mall_diy_menu_id := (
  SELECT id
  FROM `system_menu`
  WHERE deleted = b'0'
    AND name = '商城装修'
    AND path = 'diy-template'
    AND component = 'mall/promotion/diy/template/index'
  ORDER BY id DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '首页轮播图',
       '',
       2,
       3,
       @mall_diy_menu_id,
       'diy-home-banner',
       'fa6-solid:images',
       'mall/promotion/diy/home-banner/index',
       'DiyHomeBanner',
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
WHERE @mall_diy_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` exists_menu
    WHERE exists_menu.deleted = b'0'
      AND exists_menu.parent_id = @mall_diy_menu_id
      AND exists_menu.path = 'diy-home-banner'
      AND exists_menu.component = 'mall/promotion/diy/home-banner/index'
  );

SET @mall_diy_home_banner_menu_id := (
  SELECT id
  FROM `system_menu`
  WHERE deleted = b'0'
    AND parent_id = @mall_diy_menu_id
    AND path = 'diy-home-banner'
    AND component = 'mall/promotion/diy/home-banner/index'
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
       @mall_diy_home_banner_menu_id,
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
  SELECT '首页轮播图查询' AS name, 'promotion:diy-home-banner:query' AS permission, 1 AS sort
  UNION ALL SELECT '首页轮播图创建', 'promotion:diy-home-banner:create', 2
  UNION ALL SELECT '首页轮播图更新', 'promotion:diy-home-banner:update', 3
  UNION ALL SELECT '首页轮播图删除', 'promotion:diy-home-banner:delete', 4
) source
WHERE @mall_diy_home_banner_menu_id IS NOT NULL
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
    component = 'mall/promotion/diy/home-banner/index'
    OR permission IN (
      'promotion:diy-home-banner:query',
      'promotion:diy-home-banner:create',
      'promotion:diy-home-banner:update',
      'promotion:diy-home-banner:delete'
    )
  )
ORDER BY type, sort, id;
