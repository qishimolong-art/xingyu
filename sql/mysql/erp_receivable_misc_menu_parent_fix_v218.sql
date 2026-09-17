-- ERP 其他应收菜单父级修复（v218）
-- 背景：
--   v216/v217 中“应收管理”父菜单定位曾使用 path = 'receivable'。
--   由于 CRM 回款管理也可能使用同一个 path，个别环境会把新“其他应收”
--   菜单挂到 CRM 回款管理下，导致 ERP 资金管理 > 应收管理中看不到。
-- 安全性：
--   - 不删除业务数据。
--   - 只修复 ErpReceivableMisc 菜单、按钮权限、租户套餐和角色菜单授权。
--   - 可重复执行；不覆盖角色已有权限。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `system_tenant_package`
  MODIFY COLUMN `menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联的菜单编号';

SET @receivable_parent_id := (
  SELECT parent_menu.`id`
  FROM `system_menu` parent_menu
  WHERE parent_menu.`deleted` = b'0'
    AND (
      parent_menu.`id` = 3100
      OR parent_menu.`component_name` = 'ErpReceivable'
      OR (parent_menu.`path` = 'receivable' AND parent_menu.`parent_id` = 2645)
      OR EXISTS (
        SELECT 1
        FROM `system_menu` receivable_child
        WHERE receivable_child.`deleted` = b'0'
          AND receivable_child.`parent_id` = parent_menu.`id`
          AND receivable_child.`component` IN (
            'erp/finance/receivable/account/index',
            'erp/finance/receivable/other-receivable/index',
            'erp/finance/receivable/other-income/index',
            'erp/finance/receivable/report/index'
          )
      )
    )
  ORDER BY (parent_menu.`id` = 3100) DESC,
           (parent_menu.`component_name` = 'ErpReceivable') DESC,
           (parent_menu.`parent_id` = 2645) DESC,
           parent_menu.`id` DESC
  LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '其他应收', '', 2, 55, @receivable_parent_id, 'misc', 'ep:money', 'erp/finance/receivable/misc/index', 'ErpReceivableMisc', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @receivable_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND (`component_name` = 'ErpReceivableMisc'
        OR `component` = 'erp/finance/receivable/misc/index')
  );

UPDATE `system_menu`
SET `name` = '其他应收',
    `permission` = '',
    `type` = 2,
    `sort` = 55,
    `parent_id` = @receivable_parent_id,
    `path` = 'misc',
    `icon` = 'ep:money',
    `component` = 'erp/finance/receivable/misc/index',
    `component_name` = 'ErpReceivableMisc',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`component_name` = 'ErpReceivableMisc'
    OR `component` = 'erp/finance/receivable/misc/index')
  AND @receivable_parent_id IS NOT NULL;

SET @receivable_misc_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `component_name` = 'ErpReceivableMisc'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT child_name, permission, 3, sort_no, @receivable_misc_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '其他应收查询' AS child_name, 'erp:receivable-misc:query' AS permission, 1 AS sort_no
  UNION ALL SELECT '其他应收创建', 'erp:receivable-misc:create', 2
  UNION ALL SELECT '其他应收修改', 'erp:receivable-misc:update', 3
  UNION ALL SELECT '其他应收删除', 'erp:receivable-misc:delete', 4
  UNION ALL SELECT '其他应收审核', 'erp:receivable-misc:update-status', 5
  UNION ALL SELECT '其他应收导出', 'erp:receivable-misc:export', 6
) source
WHERE @receivable_misc_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_menu` existing_menu
    WHERE existing_menu.`deleted` = b'0'
      AND existing_menu.`permission` = source.permission
  );

UPDATE `system_menu`
SET `parent_id` = @receivable_misc_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `permission` LIKE 'erp:receivable-misc:%'
  AND @receivable_misc_menu_id IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_receivable_misc_menu_targets_v218`;
CREATE TEMPORARY TABLE `tmp_erp_receivable_misc_menu_targets_v218` (`menu_id` bigint NOT NULL PRIMARY KEY);

INSERT IGNORE INTO `tmp_erp_receivable_misc_menu_targets_v218` (`menu_id`)
SELECT `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND (`component_name` = 'ErpReceivableMisc'
    OR `permission` LIKE 'erp:receivable-misc:%');

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_receivable_misc_package_append_v218`;
CREATE TEMPORARY TABLE `tmp_erp_receivable_misc_package_append_v218` (
  `package_id` bigint NOT NULL PRIMARY KEY,
  `missing_menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO `tmp_erp_receivable_misc_package_append_v218` (`package_id`, `missing_menu_ids`)
SELECT package_menu.`package_id`, JSON_ARRAYAGG(package_menu.`menu_id`)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.`menu_id`
  FROM `system_tenant_package` tenant_package
  JOIN `tmp_erp_receivable_misc_menu_targets_v218` target ON TRUE
  WHERE tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@receivable_parent_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` owned_child
        WHERE owned_child.`deleted` = b'0'
          AND owned_child.`parent_id` = @receivable_parent_id
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(owned_child.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.`menu_id` AS CHAR), '$')
) package_menu
GROUP BY package_menu.`package_id`;

UPDATE `system_tenant_package` tenant_package
JOIN `tmp_erp_receivable_misc_package_append_v218` package_append
  ON package_append.`package_id` = tenant_package.`id`
SET tenant_package.`menu_ids` = JSON_MERGE_PRESERVE(tenant_package.`menu_ids`, package_append.`missing_menu_ids`),
    tenant_package.`update_time` = NOW()
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND JSON_VALID(package_append.`missing_menu_ids`);

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_receivable_misc_package_append_v218`;

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT role_info.`id`, target.`menu_id`, '1', NOW(), '1', NOW(), b'0', role_info.`tenant_id`
FROM `system_role` role_info
CROSS JOIN `tmp_erp_receivable_misc_menu_targets_v218` target
WHERE role_info.`deleted` = b'0'
  AND role_info.`code` = 'super_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`deleted` = b'0'
      AND existing_role_menu.`role_id` = role_info.`id`
      AND existing_role_menu.`menu_id` = target.`menu_id`
      AND existing_role_menu.`tenant_id` = role_info.`tenant_id`
  );

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT role_menu.`role_id`, target.`menu_id`, '1', NOW(), '1', NOW(), b'0', role_menu.`tenant_id`
FROM `tmp_erp_receivable_misc_menu_targets_v218` target
JOIN `system_role_menu` role_menu ON role_menu.`deleted` = b'0'
  AND (
    role_menu.`menu_id` = @receivable_parent_id
    OR role_menu.`menu_id` IN (
      SELECT existing_menu.`id`
      FROM `system_menu` existing_menu
      WHERE existing_menu.`deleted` = b'0'
        AND existing_menu.`parent_id` = @receivable_parent_id
    )
    OR role_menu.`menu_id` IN (
      SELECT existing_button.`id`
      FROM `system_menu` existing_button
      WHERE existing_button.`deleted` = b'0'
        AND (existing_button.`permission` LIKE 'erp:receivable-account:%'
          OR existing_button.`permission` LIKE 'erp:receivable-report:%'
          OR existing_button.`permission` LIKE 'erp:receivable-other:%'
          OR existing_button.`permission` LIKE 'erp:receivable-other-income:%')
    )
  )
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_role_menu` existing_role_menu
  WHERE existing_role_menu.`deleted` = b'0'
    AND existing_role_menu.`role_id` = role_menu.`role_id`
    AND existing_role_menu.`menu_id` = target.`menu_id`
    AND existing_role_menu.`tenant_id` = role_menu.`tenant_id`
);

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_receivable_misc_menu_targets_v218`;
