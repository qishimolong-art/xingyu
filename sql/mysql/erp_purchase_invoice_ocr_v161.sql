-- ERP purchase invoice OCR manual correction and error export permissions (v161)
--
-- Safety:
--   - Append-only menu permissions.
--   - No DELETE and no broad overwrite of role permissions.
--   - OCR credentials are not stored in this script.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(900216, '采购发票修正厂家单号', 'erp:purchase-invoice-ocr:update-factory-order-no', 3, 6, 900210, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(900217, '采购发票异常导出', 'erp:purchase-invoice-ocr:export', 3, 7, 900210, '', '', '', NULL,
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
SELECT DISTINCT owned_role.role_id,
       target_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM system_role_menu owned_role
JOIN system_menu owned_menu
  ON owned_menu.id = owned_role.menu_id
 AND owned_menu.deleted = b'0'
 AND owned_menu.permission COLLATE utf8mb4_unicode_ci IN (
       'erp:purchase-invoice-ocr:query' COLLATE utf8mb4_unicode_ci,
       'erp:purchase-invoice-ocr:upload' COLLATE utf8mb4_unicode_ci,
       'erp:purchase-invoice-ocr:recognize' COLLATE utf8mb4_unicode_ci,
       'erp:purchase-invoice-ocr:match' COLLATE utf8mb4_unicode_ci,
       'erp:purchase-invoice-ocr:confirm' COLLATE utf8mb4_unicode_ci
 )
JOIN system_menu target_menu
  ON target_menu.deleted = b'0'
 AND target_menu.id IN (900216, 900217)
WHERE owned_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu exists_role_menu
      WHERE exists_role_menu.role_id = owned_role.role_id
        AND exists_role_menu.menu_id = target_menu.id
        AND exists_role_menu.tenant_id = owned_role.tenant_id
        AND exists_role_menu.deleted = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       target_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM system_role super_role
JOIN system_menu target_menu
  ON target_menu.deleted = b'0'
 AND target_menu.id IN (900216, 900217)
WHERE super_role.code COLLATE utf8mb4_unicode_ci = 'super_admin' COLLATE utf8mb4_unicode_ci
  AND super_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu exists_role_menu
      WHERE exists_role_menu.role_id = super_role.id
        AND exists_role_menu.menu_id = target_menu.id
        AND exists_role_menu.tenant_id = super_role.tenant_id
        AND exists_role_menu.deleted = b'0'
  );

SELECT m.id,
       m.name,
       m.permission,
       m.parent_id,
       m.sort,
       COUNT(DISTINCT rm.role_id) AS granted_role_count
FROM system_menu m
LEFT JOIN system_role_menu rm
  ON rm.menu_id = m.id
 AND rm.deleted = b'0'
WHERE m.id IN (900216, 900217)
  AND m.deleted = b'0'
GROUP BY m.id, m.name, m.permission, m.parent_id, m.sort
ORDER BY m.id;
