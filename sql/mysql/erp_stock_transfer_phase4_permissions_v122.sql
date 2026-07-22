-- ERP stock transfer phase 4 permissions (v122)
-- Scope: add missing stock transfer-out button permissions and hide the legacy stock-move menu.
-- Safety: append-only permission seeds plus one component-scoped visibility update;
--         no DELETE or overwrite-style role permission cleanup.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @transfer_out_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 2
    AND `component` = 'erp/stock/transfer-out/index'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31215, '调拨出库单审批', 'erp:stock-transfer-out:approve', 3, 5, @transfer_out_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @transfer_out_menu_id IS NOT NULL
UNION ALL
SELECT 31216, '调拨出库单导入', 'erp:stock-transfer-out:import', 3, 6, @transfer_out_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @transfer_out_menu_id IS NOT NULL
UNION ALL
SELECT 31217, '调拨出库单导出', 'erp:stock-transfer-out:export', 3, 7, @transfer_out_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @transfer_out_menu_id IS NOT NULL
UNION ALL
SELECT 31218, '调拨出库单打印', 'erp:stock-transfer-out:print', 3, 8, @transfer_out_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @transfer_out_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, `id`, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu`
WHERE `id` BETWEEN 31215 AND 31218
  AND `deleted` = b'0';

-- 原库存调拨入口停止承载新业务：仅隐藏菜单，不删除历史权限、角色授权或业务数据。
UPDATE `system_menu`
SET `visible` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `type` = 2
  AND `component` = 'erp/stock/move/index';
