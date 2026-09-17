-- ERP 其他应收/其他应付菜单与权限补丁（v217）
-- 背景：
--   v216 已创建 erp_receivable_misc / erp_payable_misc 独立业务表。
--   若执行 v216 后在左侧菜单或角色权限配置表单树中仍看不到“其他应收/其他应付”，
--   通常是租户套餐 menu_ids 接近 varchar 长度上限，导致新增菜单未能进入可授权范围。
-- 安全性：
--   - 不删除业务数据。
--   - 不覆盖角色已有权限；仅追加新其他应收/付菜单和按钮权限。
--   - 兼容已执行过 v216 的环境，可重复执行。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `system_tenant_package`
  MODIFY COLUMN `menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联的菜单编号';

UPDATE `system_menu`
SET `name` = REPLACE(`name`, '其他应收', '应收调账'),
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`permission` LIKE 'erp:receivable-other:%' OR `component` = 'erp/finance/receivable/other-receivable/index')
  AND `name` LIKE '%其他应收%';

UPDATE `system_menu`
SET `name` = REPLACE(`name`, '其他应付', '应付调账'),
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND (`permission` LIKE 'erp:payable-other:%' OR `component` = 'erp/finance/payable/other/index')
  AND `name` LIKE '%其他应付%';

SET @receivable_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `id` = 3100
      OR `component_name` = 'ErpReceivable'
      OR (`path` = 'receivable' AND `parent_id` = 2645)
      OR EXISTS (
        SELECT 1
        FROM `system_menu` receivable_child
        WHERE receivable_child.`deleted` = b'0'
          AND receivable_child.`parent_id` = `system_menu`.`id`
          AND receivable_child.`component` IN (
            'erp/finance/receivable/account/index',
            'erp/finance/receivable/other-receivable/index',
            'erp/finance/receivable/other-income/index',
            'erp/finance/receivable/report/index'
          )
      )
    )
  ORDER BY (`id` = 3100) DESC, (`component_name` = 'ErpReceivable') DESC, `id` DESC
  LIMIT 1
);

SET @payable_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (`component_name` = 'ErpPayable' OR `path` = 'payable')
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '其他应收', '', 2, 55, @receivable_parent_id, 'misc', 'ep:money', 'erp/finance/receivable/misc/index', 'ErpReceivableMisc', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @receivable_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `component_name` = 'ErpReceivableMisc');

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
  AND `component_name` = 'ErpReceivableMisc'
  AND @receivable_parent_id IS NOT NULL;

SET @receivable_misc_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0' AND `component_name` = 'ErpReceivableMisc'
  ORDER BY `id`
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
) t
WHERE @receivable_misc_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` sm WHERE sm.`deleted` = b'0' AND sm.`permission` = t.permission);

UPDATE `system_menu`
SET `parent_id` = @receivable_misc_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `permission` LIKE 'erp:receivable-misc:%'
  AND @receivable_misc_menu_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '其他应付', '', 2, 55, @payable_parent_id, 'misc', 'ep:money', 'erp/finance/payable/misc/index', 'ErpPayableMisc', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @payable_parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `deleted` = b'0' AND `component_name` = 'ErpPayableMisc');

UPDATE `system_menu`
SET `name` = '其他应付',
    `permission` = '',
    `type` = 2,
    `sort` = 55,
    `parent_id` = @payable_parent_id,
    `path` = 'misc',
    `icon` = 'ep:money',
    `component` = 'erp/finance/payable/misc/index',
    `component_name` = 'ErpPayableMisc',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `component_name` = 'ErpPayableMisc'
  AND @payable_parent_id IS NOT NULL;

SET @payable_misc_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0' AND `component_name` = 'ErpPayableMisc'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT child_name, permission, 3, sort_no, @payable_misc_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM (
  SELECT '其他应付查询' AS child_name, 'erp:payable-misc:query' AS permission, 1 AS sort_no
  UNION ALL SELECT '其他应付创建', 'erp:payable-misc:create', 2
  UNION ALL SELECT '其他应付修改', 'erp:payable-misc:update', 3
  UNION ALL SELECT '其他应付删除', 'erp:payable-misc:delete', 4
  UNION ALL SELECT '其他应付审核', 'erp:payable-misc:update-status', 5
  UNION ALL SELECT '其他应付导出', 'erp:payable-misc:export', 6
) t
WHERE @payable_misc_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` sm WHERE sm.`deleted` = b'0' AND sm.`permission` = t.permission);

UPDATE `system_menu`
SET `parent_id` = @payable_misc_menu_id,
    `type` = 3,
    `status` = 0,
    `visible` = b'1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `permission` LIKE 'erp:payable-misc:%'
  AND @payable_misc_menu_id IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_menu_fix_targets_v217`;
CREATE TEMPORARY TABLE `tmp_erp_misc_menu_fix_targets_v217` (`menu_id` bigint NOT NULL PRIMARY KEY);

INSERT IGNORE INTO `tmp_erp_misc_menu_fix_targets_v217` (`menu_id`)
SELECT `id` FROM `system_menu`
WHERE `deleted` = b'0'
  AND (`component_name` IN ('ErpReceivableMisc', 'ErpPayableMisc')
    OR `permission` LIKE 'erp:receivable-misc:%'
    OR `permission` LIKE 'erp:payable-misc:%');

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_package_fix_append_v217`;
CREATE TEMPORARY TABLE `tmp_erp_misc_package_fix_append_v217` (
  `package_id` bigint NOT NULL PRIMARY KEY,
  `missing_menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO `tmp_erp_misc_package_fix_append_v217` (`package_id`, `missing_menu_ids`)
SELECT package_menu.`package_id`, JSON_ARRAYAGG(package_menu.`menu_id`)
FROM (
  SELECT DISTINCT tenant_package.`id` AS package_id, target.`menu_id`
  FROM `system_tenant_package` tenant_package
  JOIN `tmp_erp_misc_menu_fix_targets_v217` target ON TRUE
  WHERE tenant_package.`deleted` = b'0'
    AND JSON_VALID(tenant_package.`menu_ids`)
    AND (
      JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@receivable_parent_id AS CHAR), '$')
      OR JSON_CONTAINS(tenant_package.`menu_ids`, CAST(@payable_parent_id AS CHAR), '$')
      OR EXISTS (
        SELECT 1
        FROM `system_menu` owned_child
        WHERE owned_child.`deleted` = b'0'
          AND owned_child.`parent_id` IN (@receivable_parent_id, @payable_parent_id)
          AND JSON_CONTAINS(tenant_package.`menu_ids`, CAST(owned_child.`id` AS CHAR), '$')
      )
    )
    AND NOT JSON_CONTAINS(tenant_package.`menu_ids`, CAST(target.`menu_id` AS CHAR), '$')
) package_menu
GROUP BY package_menu.`package_id`;

UPDATE `system_tenant_package` tenant_package
JOIN `tmp_erp_misc_package_fix_append_v217` package_append
  ON package_append.`package_id` = tenant_package.`id`
SET tenant_package.`menu_ids` = JSON_MERGE_PRESERVE(tenant_package.`menu_ids`, package_append.`missing_menu_ids`),
    tenant_package.`update_time` = NOW()
WHERE tenant_package.`deleted` = b'0'
  AND JSON_VALID(tenant_package.`menu_ids`)
  AND JSON_VALID(package_append.`missing_menu_ids`);

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_package_fix_append_v217`;

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.`id`, t.`menu_id`, '1', NOW(), '1', NOW(), b'0', r.`tenant_id`
FROM `system_role` r
CROSS JOIN `tmp_erp_misc_menu_fix_targets_v217` t
WHERE r.`deleted` = b'0'
  AND r.`code` = 'super_admin'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` exists_rm
    WHERE exists_rm.`deleted` = b'0'
      AND exists_rm.`role_id` = r.`id`
      AND exists_rm.`menu_id` = t.`menu_id`
      AND exists_rm.`tenant_id` = r.`tenant_id`
  );

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, t.`menu_id`, '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `tmp_erp_misc_menu_fix_targets_v217` t
JOIN `system_role_menu` rm ON rm.`deleted` = b'0'
  AND (
    rm.`menu_id` IN (@receivable_parent_id, @payable_parent_id)
    OR rm.`menu_id` IN (
      SELECT `id` FROM `system_menu`
      WHERE `deleted` = b'0'
        AND (`permission` LIKE 'erp:receivable-report:%' OR `permission` LIKE 'erp:payable-report:%')
    )
  )
WHERE NOT EXISTS (
  SELECT 1 FROM `system_role_menu` exists_rm
  WHERE exists_rm.`deleted` = b'0'
    AND exists_rm.`role_id` = rm.`role_id`
    AND exists_rm.`menu_id` = t.`menu_id`
    AND exists_rm.`tenant_id` = rm.`tenant_id`
);

DROP TEMPORARY TABLE IF EXISTS `tmp_erp_misc_menu_fix_targets_v217`;
