-- ERP product brand import permission patch (v194)
-- Adds the missing import button menu for product brand and grants it to roles
-- that already own related product-brand permissions.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_brand_import_permission;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_brand_import_parent;

CREATE TEMPORARY TABLE tmp_erp_product_brand_import_permission (
  name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  sort int NOT NULL,
  query_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  create_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  update_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  export_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_product_brand_import_permission
(name, permission, sort, query_perm, create_perm, update_perm, export_perm)
VALUES
('配件品牌导入', 'erp:product-brand:import', 6,
 'erp:product-brand:query', 'erp:product-brand:create',
 'erp:product-brand:update', 'erp:product-brand:export');

CREATE TEMPORARY TABLE tmp_erp_product_brand_import_parent (
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  parent_id bigint NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_product_brand_import_parent (permission, parent_id)
SELECT parent_source.permission, MIN(parent_source.parent_id) AS parent_id
FROM (
    SELECT source.permission, candidate.parent_id
    FROM tmp_erp_product_brand_import_permission source
    JOIN system_menu candidate
      ON (
          candidate.permission COLLATE utf8mb4_unicode_ci = source.query_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.create_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.update_perm COLLATE utf8mb4_unicode_ci
       OR candidate.permission COLLATE utf8mb4_unicode_ci = source.export_perm COLLATE utf8mb4_unicode_ci
      )
     AND candidate.deleted = b'0'
     AND candidate.parent_id > 0
) parent_source
GROUP BY parent_source.permission;

INSERT IGNORE INTO tmp_erp_product_brand_import_parent (permission, parent_id)
SELECT source.permission, MIN(page_menu.id) AS parent_id
FROM tmp_erp_product_brand_import_permission source
JOIN system_menu page_menu
  ON page_menu.component COLLATE utf8mb4_unicode_ci = 'erp/product/brand/index' COLLATE utf8mb4_unicode_ci
 AND page_menu.type = 2
 AND page_menu.deleted = b'0'
GROUP BY source.permission;

UPDATE system_menu m
JOIN tmp_erp_product_brand_import_permission source
  ON m.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
JOIN tmp_erp_product_brand_import_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
SET m.name = source.name,
    m.type = 3,
    m.sort = source.sort,
    m.parent_id = parent.parent_id,
    m.path = '',
    m.icon = '',
    m.component = '',
    m.component_name = NULL,
    m.status = 0,
    m.visible = b'1',
    m.keep_alive = b'1',
    m.always_show = b'1',
    m.updater = '1',
    m.update_time = NOW(),
    m.deleted = b'0'
WHERE m.deleted = b'1';

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT source.name,
       source.permission,
       3,
       source.sort,
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
FROM tmp_erp_product_brand_import_permission source
JOIN tmp_erp_product_brand_import_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
      AND exists_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       import_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM tmp_erp_product_brand_import_permission source
JOIN system_menu import_menu
  ON import_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND import_menu.deleted = b'0'
JOIN system_role_menu owned_role
  ON owned_role.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.id = owned_role.menu_id
 AND owned_menu.deleted = b'0'
 AND (
       owned_menu.permission COLLATE utf8mb4_unicode_ci = source.query_perm COLLATE utf8mb4_unicode_ci
    OR owned_menu.permission COLLATE utf8mb4_unicode_ci = source.create_perm COLLATE utf8mb4_unicode_ci
    OR owned_menu.permission COLLATE utf8mb4_unicode_ci = source.update_perm COLLATE utf8mb4_unicode_ci
    OR owned_menu.permission COLLATE utf8mb4_unicode_ci = source.export_perm COLLATE utf8mb4_unicode_ci
 )
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_menu target
    WHERE target.role_id = owned_role.role_id
      AND target.menu_id = import_menu.id
      AND target.tenant_id = owned_role.tenant_id
      AND target.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       import_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM system_role super_role
JOIN tmp_erp_product_brand_import_permission source ON 1 = 1
JOIN system_menu import_menu
  ON import_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND import_menu.deleted = b'0'
WHERE super_role.code COLLATE utf8mb4_unicode_ci = 'super_admin' COLLATE utf8mb4_unicode_ci
  AND super_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu target
      WHERE target.role_id = super_role.id
        AND target.menu_id = import_menu.id
        AND target.tenant_id = super_role.tenant_id
        AND target.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_brand_import_permission;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_product_brand_import_parent;
