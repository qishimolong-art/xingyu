-- v286 precheck: 只读检查拣货单、送货单迁移至独立“拣货配送”分组前的菜单与权限状态。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS current_database, @@hostname AS database_host, @@version AS database_version;

SELECT id, name, type, sort, parent_id, path, icon, component, component_name,
       status, visible, keep_alive, always_show, deleted
FROM system_menu
WHERE id IN (2563, 2617, 32000, 32001, 32002, 32003, 32010, 32011, 32012, 32013, 32020)
   OR (parent_id = 2563 AND deleted = b'0')
ORDER BY parent_id, sort, id;

SELECT id, name, parent_id, path, type, deleted
FROM system_menu
WHERE id = 32020
   OR (parent_id = 2563 AND path = 'fulfillment')
   OR (parent_id = 2563 AND name = '拣货配送')
ORDER BY id;

SELECT rm.tenant_id, rm.role_id,
       MAX(rm.menu_id = 32000) AS has_pick_menu,
       MAX(rm.menu_id = 32010) AS has_delivery_menu,
       MAX(rm.menu_id = 32020) AS has_fulfillment_parent
FROM system_role_menu rm
WHERE rm.menu_id IN (32000, 32010, 32020)
  AND rm.deleted = b'0'
GROUP BY rm.tenant_id, rm.role_id
ORDER BY rm.tenant_id, rm.role_id;

SELECT id, name, status,
       JSON_VALID(menu_ids) AS menu_ids_valid,
       JSON_CONTAINS(menu_ids, CAST(32000 AS CHAR), '$') AS has_pick_menu,
       JSON_CONTAINS(menu_ids, CAST(32010 AS CHAR), '$') AS has_delivery_menu,
       JSON_CONTAINS(menu_ids, CAST(32020 AS CHAR), '$') AS has_fulfillment_parent
FROM system_tenant_package
WHERE deleted = b'0'
  AND JSON_VALID(menu_ids)
  AND (JSON_CONTAINS(menu_ids, CAST(32000 AS CHAR), '$')
    OR JSON_CONTAINS(menu_ids, CAST(32010 AS CHAR), '$'))
ORDER BY id;

SELECT id, name
FROM system_tenant_package
WHERE deleted = b'0'
  AND NOT JSON_VALID(menu_ids);
