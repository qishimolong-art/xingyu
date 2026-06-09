-- ERP sale quote convert-to-cart permission hotfix v51.
-- Symptom: the "convert to sale cart" row action disappears on the quote list.
-- Cause: the frontend TableAction filters the button by permission code
--        erp:sale-quote:convert-cart. If the menu button permission is missing
--        or not assigned to the role, the frontend hides it.
-- Execute this SQL, then refresh menu cache or restart the backend and log in again.

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(2976, '报价转手推车', 'erp:sale-quote:convert-cart', 3, 6, 2970, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT source.`role_id`, 2976, '1', NOW(), '1', NOW(), b'0', source.`tenant_id`
FROM `system_role_menu` source
WHERE source.`menu_id` IN (2970, 2971, 2972, 2973, 2975)
  AND source.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` target
      WHERE target.`role_id` = source.`role_id`
        AND target.`menu_id` = 2976
        AND target.`tenant_id` = source.`tenant_id`
        AND target.`deleted` = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, 2976, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` target
    WHERE target.`role_id` = 1
      AND target.`menu_id` = 2976
      AND target.`tenant_id` = 1
      AND target.`deleted` = b'0'
);
