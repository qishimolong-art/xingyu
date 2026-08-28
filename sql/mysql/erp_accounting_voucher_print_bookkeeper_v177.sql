-- ERP accounting voucher print and bookkeeper date support (v177).
-- Safe to rerun. Adds a nullable column, appends print permissions, and
-- upserts field definitions without deleting role field selections.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_erp_voucher_bookkeeper_time_v177;
DELIMITER $$
CREATE PROCEDURE add_erp_voucher_bookkeeper_time_v177()
BEGIN
    IF NOT EXISTS (
        SELECT 1
         FROM information_schema.COLUMNS
         WHERE CONVERT(TABLE_SCHEMA USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_unicode_ci
           AND CONVERT(TABLE_NAME USING utf8mb4) COLLATE utf8mb4_unicode_ci = _utf8mb4'erp_voucher' COLLATE utf8mb4_unicode_ci
           AND CONVERT(COLUMN_NAME USING utf8mb4) COLLATE utf8mb4_unicode_ci = _utf8mb4'bookkeeper_time' COLLATE utf8mb4_unicode_ci
    ) THEN
        ALTER TABLE `erp_voucher`
            ADD COLUMN `bookkeeper_time` DATETIME DEFAULT NULL COMMENT '登账日期'
            AFTER `bookkeeper`;
    END IF;
END$$
DELIMITER ;
CALL add_erp_voucher_bookkeeper_time_v177();
DROP PROCEDURE IF EXISTS add_erp_voucher_bookkeeper_time_v177;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_voucher_print_permission_v177;
CREATE TEMPORARY TABLE tmp_erp_voucher_print_permission_v177 (
  parent_permission varchar(128) NOT NULL,
  inherit_permission varchar(128) NOT NULL,
  button_name varchar(64) NOT NULL,
  button_permission varchar(128) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (button_permission)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_voucher_print_permission_v177
(`parent_permission`, `inherit_permission`, `button_name`, `button_permission`, `sort`)
VALUES
('erp:voucher:query', 'erp:voucher:query', '凭证打印', 'erp:voucher:print', 7),
('erp:voucher:query', 'super_admin', '凭证打印模板', 'erp:voucher:print-template', 8);

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
FROM tmp_erp_voucher_print_permission_v177 perm
JOIN system_menu parent
  ON CONVERT(parent.permission USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONVERT(perm.parent_permission USING utf8mb4) COLLATE utf8mb4_unicode_ci
 AND parent.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1
      FROM system_menu exists_menu
     WHERE CONVERT(exists_menu.permission USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONVERT(perm.button_permission USING utf8mb4) COLLATE utf8mb4_unicode_ci
       AND exists_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       button_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM tmp_erp_voucher_print_permission_v177 perm
JOIN system_menu button_menu
  ON CONVERT(button_menu.permission USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONVERT(perm.button_permission USING utf8mb4) COLLATE utf8mb4_unicode_ci
 AND button_menu.deleted = b'0'
JOIN system_menu owned_menu
  ON CONVERT(owned_menu.permission USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONVERT(perm.inherit_permission USING utf8mb4) COLLATE utf8mb4_unicode_ci
 AND owned_menu.deleted = b'0'
JOIN system_role_menu owned_role
  ON owned_role.menu_id = owned_menu.id
 AND owned_role.deleted = b'0'
WHERE CONVERT(perm.inherit_permission USING utf8mb4) COLLATE utf8mb4_unicode_ci <> _utf8mb4'super_admin' COLLATE utf8mb4_unicode_ci
  AND NOT EXISTS (
    SELECT 1
      FROM system_role_menu target
     WHERE target.role_id = owned_role.role_id
       AND target.menu_id = button_menu.id
       AND target.tenant_id = owned_role.tenant_id
       AND target.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       button_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM tmp_erp_voucher_print_permission_v177 perm
JOIN system_menu button_menu
  ON CONVERT(button_menu.permission USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONVERT(perm.button_permission USING utf8mb4) COLLATE utf8mb4_unicode_ci
 AND button_menu.deleted = b'0'
JOIN system_role super_role
  ON CONVERT(super_role.code USING utf8mb4) COLLATE utf8mb4_unicode_ci = _utf8mb4'super_admin' COLLATE utf8mb4_unicode_ci
 AND super_role.deleted = b'0'
WHERE CONVERT(perm.inherit_permission USING utf8mb4) COLLATE utf8mb4_unicode_ci = _utf8mb4'super_admin' COLLATE utf8mb4_unicode_ci
  AND NOT EXISTS (
      SELECT 1
        FROM system_role_menu target
       WHERE target.role_id = super_role.id
         AND target.menu_id = button_menu.id
         AND target.tenant_id = super_role.tenant_id
         AND target.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_voucher_print_permission_v177;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_voucher_field_definition_v177;
CREATE TEMPORARY TABLE tmp_erp_voucher_field_definition_v177 (
  field_key varchar(128) NOT NULL,
  field_label varchar(128) NOT NULL,
  field_group varchar(64) NOT NULL,
  sort int NOT NULL,
  PRIMARY KEY (field_key)
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_voucher_field_definition_v177
(`field_key`, `field_label`, `field_group`, `sort`)
VALUES
('voucherNo', '凭证号', 'main_form', 20),
('voucherDate', '凭证日期', 'main_form', 30),
('period', '期间', 'main_form', 150),
('bookkeeperTime', '登账日期', 'main_form', 160),
('col_printAction', '列表-打印', 'list_col', 500),
('col_deleteAction', '列表-删除', 'list_col', 510),
('col_createTime', '列表-制单日期', 'list_col', 520),
('col_voucherDate', '列表-凭证日期', 'list_col', 530),
('col_period', '列表-期间', 'list_col', 540),
('col_voucherNo', '列表-凭证号', 'list_col', 550),
('col_remark', '列表-备注', 'list_col', 560),
('col_totalDebit', '列表-借方金额合计', 'list_col', 570),
('col_totalCredit', '列表-贷方金额合计', 'list_col', 580),
('col_makerUserName', '列表-制单人', 'list_col', 590),
('col_auditorUserName', '列表-审核人', 'list_col', 600),
('col_auditTime', '列表-审核日期', 'list_col', 610),
('col_bookkeeper', '列表-登账人', 'list_col', 620),
('col_bookkeeperTime', '列表-登账日期', 'list_col', 630);

UPDATE system_field_definition target
JOIN tmp_erp_voucher_field_definition_v177 source
  ON CONVERT(source.field_key USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONVERT(target.field_key USING utf8mb4) COLLATE utf8mb4_unicode_ci
 SET target.field_label = source.field_label,
     target.field_group = source.field_group,
     target.sort = source.sort,
     target.updater = '1',
     target.update_time = NOW()
WHERE CONVERT(target.module USING utf8mb4) COLLATE utf8mb4_unicode_ci = _utf8mb4'erp_accounting_voucher' COLLATE utf8mb4_unicode_ci
  AND target.tenant_id = 1
  AND target.deleted = b'0';

INSERT INTO system_field_definition
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_accounting_voucher',
       source.field_key,
       source.field_label,
       source.field_group,
       source.sort,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       1
FROM tmp_erp_voucher_field_definition_v177 source
WHERE NOT EXISTS (
    SELECT 1
      FROM system_field_definition target
     WHERE CONVERT(target.module USING utf8mb4) COLLATE utf8mb4_unicode_ci = _utf8mb4'erp_accounting_voucher' COLLATE utf8mb4_unicode_ci
       AND CONVERT(target.field_key USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONVERT(source.field_key USING utf8mb4) COLLATE utf8mb4_unicode_ci
       AND target.tenant_id = 1
       AND target.deleted = b'0'
);

DROP TEMPORARY TABLE IF EXISTS tmp_erp_voucher_field_definition_v177;
