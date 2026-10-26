-- v288 precheck：将组装页面迁移至“基础数据 / 组装件”前的只读检查。
-- 本脚本只查询，不修改菜单、角色、套餐或业务数据。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database, NOW() AS checked_at;

SELECT id, name, type, sort, parent_id, path, component, component_name,
       status, visible, deleted
FROM system_menu
WHERE deleted = b'0'
  AND (id IN (2563, 6400, 900697)
       OR name IN ('基础数据', '组装件', '库存管理')
       OR component IN ('erp/assembly/recipe/index', 'erp/assembly/order/index'))
ORDER BY parent_id, sort, id;

SELECT role_menu.role_id, role_menu.tenant_id,
       SUM(menu.component = 'erp/assembly/recipe/index') AS recipe_grant_count,
       SUM(menu.component = 'erp/assembly/order/index') AS order_grant_count,
       SUM(menu.id = 6400) AS base_parent_grant_count,
       SUM(menu.id = 900697) AS assembly_parent_grant_count
FROM system_role_menu role_menu
JOIN system_menu menu ON menu.id = role_menu.menu_id AND menu.deleted = b'0'
WHERE role_menu.deleted = b'0'
  AND (menu.id IN (6400, 900697)
       OR menu.component IN ('erp/assembly/recipe/index', 'erp/assembly/order/index'))
GROUP BY role_menu.role_id, role_menu.tenant_id
ORDER BY role_menu.tenant_id, role_menu.role_id;

SELECT id, name, CHAR_LENGTH(menu_ids) AS menu_ids_length,
       JSON_VALID(menu_ids) AS menu_ids_valid,
       JSON_CONTAINS(menu_ids, CAST(6400 AS CHAR), '$') AS has_base_parent,
       JSON_CONTAINS(menu_ids, CAST(900697 AS CHAR), '$') AS has_assembly_parent,
       JSON_CONTAINS(menu_ids, CAST(900685 AS CHAR), '$') AS has_recipe_menu,
       JSON_CONTAINS(menu_ids, CAST(900686 AS CHAR), '$') AS has_order_menu
FROM system_tenant_package
WHERE deleted = b'0'
  AND (NOT JSON_VALID(menu_ids)
       OR JSON_CONTAINS(menu_ids, CAST(900685 AS CHAR), '$')
       OR JSON_CONTAINS(menu_ids, CAST(900686 AS CHAR), '$'))
ORDER BY id;

SELECT role_id, menu_id, tenant_id, COUNT(*) AS active_grant_count
FROM system_role_menu
WHERE deleted = b'0'
GROUP BY role_id, menu_id, tenant_id
HAVING COUNT(*) > 1
ORDER BY tenant_id, role_id, menu_id;
