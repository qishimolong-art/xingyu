-- v286: 将拣货单、送货单从“销售管理”迁移至独立“拣货配送”分组。
-- MySQL 5.7 / 8.0 compatible；可重复执行；不修改业务数据、权限码、路由或组件。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行前保护：只接受当前约定的 ERP、销售、拣货、送货菜单结构。
SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu WHERE id = 2563 AND name = 'ERP 系统' AND type = 1 AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: ERP 根菜单 2563 不存在或结构不符''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu WHERE id = 2617 AND name = '销售管理' AND parent_id = 2563 AND type = 1 AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 销售管理菜单 2617 不存在或结构不符''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE id = 32000 AND type = 2 AND path = 'pick'
     AND component = 'erp/sale/pick/index' AND component_name = 'ErpSalePick' AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 拣货单菜单 32000 不存在或路由结构不符''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE id = 32010 AND type = 2 AND path = 'delivery'
     AND component = 'erp/sale/delivery/index' AND component_name = 'ErpSaleDelivery' AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 送货单菜单 32010 不存在或路由结构不符''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1 FROM system_menu
    WHERE id = 32020
      AND NOT (name = '拣货配送' AND type = 1 AND sort = 25 AND parent_id = 2563
        AND path = 'fulfillment' AND icon = 'ep:box' AND permission = ''
        AND status = 0 AND visible = b'1' AND keep_alive = b'1'
        AND always_show = b'1' AND deleted = b'0')
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 菜单 ID 32020 已被其它菜单占用''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1 FROM system_menu
    WHERE parent_id = 2563 AND deleted = b'0' AND id <> 32020
      AND (path = 'fulfillment' OR name = '拣货配送')
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: ERP 根菜单下已存在同名或同路径菜单''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1 FROM system_tenant_package
    WHERE deleted = b'0' AND NOT JSON_VALID(menu_ids)
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 存在 menu_ids 非法的租户套餐，已停止迁移''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

START TRANSACTION;

SELECT id
FROM system_menu
WHERE id IN (2563, 2617, 32000, 32010, 32020)
FOR UPDATE;

INSERT INTO system_menu
(id, name, permission, type, sort, parent_id, path, icon, component, component_name,
 status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT 32020, '拣货配送', '', 1, 25, 2563, 'fulfillment', 'ep:box', '', '',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 32020);

UPDATE system_menu
SET name = '拣货单', parent_id = 32020, updater = '1', update_time = NOW()
WHERE id = 32000 AND deleted = b'0';

UPDATE system_menu
SET name = '送货单', parent_id = 32020, updater = '1', update_time = NOW()
WHERE id = 32010 AND deleted = b'0';

INSERT INTO system_role_menu
(role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT DISTINCT source.role_id, 32020, '1', NOW(), '1', NOW(), b'0', source.tenant_id
FROM system_role_menu source
WHERE source.menu_id IN (32000, 32010)
  AND source.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM system_role_menu current_grant
    WHERE current_grant.role_id = source.role_id
      AND current_grant.menu_id = 32020
      AND current_grant.tenant_id = source.tenant_id
      AND current_grant.deleted = b'0'
  );

UPDATE system_tenant_package tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', 32020),
    tenant_package.updater = '1',
    tenant_package.update_time = NOW()
WHERE tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND (JSON_CONTAINS(tenant_package.menu_ids, CAST(32000 AS CHAR), '$')
    OR JSON_CONTAINS(tenant_package.menu_ids, CAST(32010 AS CHAR), '$'))
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(32020 AS CHAR), '$');

-- 提交前保护：菜单树、按钮归属、角色父级权限和租户套餐父级权限必须完整。
SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE id = 32020 AND name = '拣货配送' AND type = 1 AND sort = 25
     AND parent_id = 2563 AND path = 'fulfillment' AND icon = 'ep:box'
     AND status = 0 AND visible = b'1' AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 新父菜单后检失败''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE (id = 32000 AND name = '拣货单' AND parent_id = 32020 AND path = 'pick'
          AND component = 'erp/sale/pick/index' AND component_name = 'ErpSalePick' AND deleted = b'0')
      OR (id = 32010 AND name = '送货单' AND parent_id = 32020 AND path = 'delivery'
          AND component = 'erp/sale/delivery/index' AND component_name = 'ErpSaleDelivery' AND deleted = b'0')) = 2,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 拣货单或送货单后检失败''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE (id IN (32001, 32002, 32003) AND parent_id = 32000 AND deleted = b'0')
      OR (id IN (32011, 32012, 32013) AND parent_id = 32010 AND deleted = b'0')) = 6,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 原按钮子菜单归属后检失败''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1
    FROM system_role_menu source
    WHERE source.menu_id IN (32000, 32010) AND source.deleted = b'0'
      AND NOT EXISTS (
        SELECT 1 FROM system_role_menu parent_grant
        WHERE parent_grant.role_id = source.role_id
          AND parent_grant.menu_id = 32020
          AND parent_grant.tenant_id = source.tenant_id
          AND parent_grant.deleted = b'0'
      )
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 存在缺少新父菜单权限的角色''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1
    FROM system_tenant_package tenant_package
    WHERE tenant_package.deleted = b'0'
      AND JSON_VALID(tenant_package.menu_ids)
      AND (JSON_CONTAINS(tenant_package.menu_ids, CAST(32000 AS CHAR), '$')
        OR JSON_CONTAINS(tenant_package.menu_ids, CAST(32010 AS CHAR), '$'))
      AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(32020 AS CHAR), '$')
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v286: 存在缺少新父菜单的租户套餐''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

COMMIT;

SELECT id, name, type, sort, parent_id, path, icon, component, component_name,
       status, visible, keep_alive, always_show, deleted
FROM system_menu
WHERE id IN (32020, 32000, 32010, 32001, 32002, 32003, 32011, 32012, 32013)
ORDER BY parent_id, sort, id;

SELECT menu_id, tenant_id, COUNT(DISTINCT role_id) AS authorized_role_count
FROM system_role_menu
WHERE menu_id IN (32000, 32010, 32020) AND deleted = b'0'
GROUP BY menu_id, tenant_id
ORDER BY menu_id, tenant_id;

SELECT id, name,
       JSON_CONTAINS(menu_ids, CAST(32000 AS CHAR), '$') AS has_pick_menu,
       JSON_CONTAINS(menu_ids, CAST(32010 AS CHAR), '$') AS has_delivery_menu,
       JSON_CONTAINS(menu_ids, CAST(32020 AS CHAR), '$') AS has_fulfillment_parent
FROM system_tenant_package
WHERE deleted = b'0'
  AND JSON_VALID(menu_ids)
  AND (JSON_CONTAINS(menu_ids, CAST(32000 AS CHAR), '$')
    OR JSON_CONTAINS(menu_ids, CAST(32010 AS CHAR), '$'))
ORDER BY id;
