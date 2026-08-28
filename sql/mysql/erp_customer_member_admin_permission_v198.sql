-- ERP customer mini-app member authorization admin permissions (v198).
-- Safe to rerun. This script only appends button permissions under ERP customer menu.
-- It does not delete or overwrite menu, role, permission, customer, or member data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_customer_member_permission;

CREATE TEMPORARY TABLE tmp_erp_customer_member_permission (
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_customer_member_permission
(`button_name`, `button_permission`, `sort`)
VALUES
('客户小程序授权查询', 'erp:customer-member:query', 10),
('客户小程序授权创建', 'erp:customer-member:create', 11),
('客户小程序授权更新', 'erp:customer-member:update', 12),
('客户小程序授权删除', 'erp:customer-member:delete', 13);

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT perm.button_name,
       perm.button_permission,
       3,
       perm.sort,
       parent.parent_id,
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
FROM tmp_erp_customer_member_permission perm
JOIN (
    SELECT parent_id
      FROM system_menu
     WHERE permission = 'erp:customer:query'
       AND deleted = b'0'
     ORDER BY id
     LIMIT 1
) parent
WHERE NOT EXISTS (
    SELECT 1
      FROM system_menu exists_menu
     WHERE exists_menu.permission = perm.button_permission
       AND exists_menu.deleted = b'0'
);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_customer_member_permission;
