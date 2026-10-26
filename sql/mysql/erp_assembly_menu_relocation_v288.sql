-- v288：将“模块管理”“套件组装单”迁移至“基础数据 / 组装件”。
-- MySQL 5.7 / 8.0 compatible；可重复执行；不修改业务表、页面路由、组件或按钮权限码。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @assembly_base_parent_id := 6400;
SET @assembly_group_menu_id := 900697;
SET @assembly_recipe_menu_id := (
  SELECT id FROM system_menu
  WHERE deleted = b'0' AND component = 'erp/assembly/recipe/index'
  ORDER BY id DESC LIMIT 1
);
SET @assembly_order_menu_id := (
  SELECT id FROM system_menu
  WHERE deleted = b'0' AND component = 'erp/assembly/order/index'
  ORDER BY id DESC LIMIT 1
);

-- 执行前保护：只接受当前约定的 ERP 基础数据和组装页面结构。
SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE id = 2563 AND name = 'ERP 系统' AND type = 1 AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: ERP 根菜单 2563 不存在或结构不符''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE id = @assembly_base_parent_id AND name = '基础数据' AND type = 1
     AND parent_id = 2563 AND path = 'base' AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: ERP 基础数据菜单 6400 不存在或结构不符''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @assembly_recipe_menu_id IS NOT NULL
    AND (SELECT COUNT(*) FROM system_menu
         WHERE id = @assembly_recipe_menu_id AND type = 2 AND path = 'assembly-recipe'
           AND component = 'erp/assembly/recipe/index'
           AND component_name = 'ErpAssemblyRecipe' AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 模块管理菜单不存在或路由结构不符''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @assembly_order_menu_id IS NOT NULL
    AND (SELECT COUNT(*) FROM system_menu
         WHERE id = @assembly_order_menu_id AND type = 2 AND path = 'assembly-order'
           AND component = 'erp/assembly/order/index'
           AND component_name = 'ErpAssemblyOrder' AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 套件组装单菜单不存在或路由结构不符''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1 FROM system_menu
    WHERE id = @assembly_group_menu_id
      AND NOT (name = '组装件' AND type = 1 AND parent_id = @assembly_base_parent_id
        AND path = 'assembly' AND permission = '' AND deleted = b'0')
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 菜单 ID 900697 已被其它菜单占用''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1 FROM system_menu
    WHERE parent_id = @assembly_base_parent_id AND deleted = b'0'
      AND id <> @assembly_group_menu_id
      AND (name = '组装件' OR path = 'assembly')
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 基础数据下已存在其它同名或同路径菜单''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1 FROM system_tenant_package
    WHERE deleted = b'0' AND NOT JSON_VALID(menu_ids)
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 存在 menu_ids 非法的租户套餐，已停止迁移''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

START TRANSACTION;

SELECT id
FROM system_menu
WHERE id IN (@assembly_base_parent_id, @assembly_group_menu_id,
             @assembly_recipe_menu_id, @assembly_order_menu_id)
FOR UPDATE;

INSERT INTO system_menu
(id, name, permission, type, sort, parent_id, path, icon, component, component_name,
 status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT @assembly_group_menu_id, '组装件', '', 1, 5, @assembly_base_parent_id, 'assembly',
       'ep:box', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = @assembly_group_menu_id);

UPDATE system_menu
SET name = '组装件', permission = '', type = 1, sort = 5,
    parent_id = @assembly_base_parent_id, path = 'assembly', icon = 'ep:box',
    component = '', component_name = '', status = 0, visible = b'1',
    keep_alive = b'1', always_show = b'1', updater = '1', update_time = NOW()
WHERE id = @assembly_group_menu_id AND deleted = b'0';

UPDATE system_menu
SET parent_id = @assembly_group_menu_id, sort = 1,
    updater = '1', update_time = NOW()
WHERE id = @assembly_recipe_menu_id AND deleted = b'0';

UPDATE system_menu
SET parent_id = @assembly_group_menu_id, sort = 2,
    updater = '1', update_time = NOW()
WHERE id = @assembly_order_menu_id AND deleted = b'0';

-- 已拥有任一组装页面的角色，补齐“基础数据”和“组装件”两个父级目录授权。
INSERT INTO system_role_menu
(role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT DISTINCT source.role_id, target.menu_id, '1', NOW(), '1', NOW(), b'0', source.tenant_id
FROM system_role_menu source
CROSS JOIN (
  SELECT @assembly_base_parent_id AS menu_id
  UNION ALL
  SELECT @assembly_group_menu_id AS menu_id
) target
WHERE source.menu_id IN (@assembly_recipe_menu_id, @assembly_order_menu_id)
  AND source.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM system_role_menu current_grant
    WHERE current_grant.role_id = source.role_id
      AND current_grant.menu_id = target.menu_id
      AND current_grant.tenant_id = source.tenant_id
      AND current_grant.deleted = b'0'
  );

-- 已包含任一组装页面的套餐，补齐“基础数据”和“组装件”父级目录。
UPDATE system_tenant_package tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @assembly_base_parent_id),
    tenant_package.updater = '1', tenant_package.update_time = NOW()
WHERE tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND (JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_recipe_menu_id AS CHAR), '$')
    OR JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$'))
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_base_parent_id AS CHAR), '$');

UPDATE system_tenant_package tenant_package
SET tenant_package.menu_ids = JSON_ARRAY_APPEND(tenant_package.menu_ids, '$', @assembly_group_menu_id),
    tenant_package.updater = '1', tenant_package.update_time = NOW()
WHERE tenant_package.deleted = b'0'
  AND JSON_VALID(tenant_package.menu_ids)
  AND (JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_recipe_menu_id AS CHAR), '$')
    OR JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$'))
  AND NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_group_menu_id AS CHAR), '$');

-- 提交前保护：菜单层级、按钮归属、角色和套餐父级授权必须完整。
SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE id = @assembly_group_menu_id AND name = '组装件' AND type = 1 AND sort = 5
     AND parent_id = @assembly_base_parent_id AND path = 'assembly' AND icon = 'ep:box'
     AND status = 0 AND visible = b'1' AND deleted = b'0') = 1,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 组装件父菜单后检失败''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE (id = @assembly_recipe_menu_id AND name = '模块管理'
          AND parent_id = @assembly_group_menu_id AND sort = 1
          AND path = 'assembly-recipe' AND component = 'erp/assembly/recipe/index'
          AND component_name = 'ErpAssemblyRecipe' AND deleted = b'0')
      OR (id = @assembly_order_menu_id AND name = '套件组装单'
          AND parent_id = @assembly_group_menu_id AND sort = 2
          AND path = 'assembly-order' AND component = 'erp/assembly/order/index'
          AND component_name = 'ErpAssemblyOrder' AND deleted = b'0')) = 2,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 组装页面迁移后检失败''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM system_menu
   WHERE deleted = b'0' AND permission IN (
     'erp:assembly-recipe:query', 'erp:assembly-recipe:create',
     'erp:assembly-recipe:update', 'erp:assembly-recipe:delete'
   ) AND parent_id = @assembly_recipe_menu_id) = 4
  AND
  (SELECT COUNT(*) FROM system_menu
   WHERE deleted = b'0' AND permission IN (
     'erp:assembly-order:query', 'erp:assembly-order:create',
     'erp:assembly-order:update', 'erp:assembly-order:delete',
     'erp:assembly-order:complete', 'erp:assembly-order:void'
   ) AND parent_id = @assembly_order_menu_id) = 6,
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 组装按钮权限归属后检失败''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1
    FROM system_role_menu source
    WHERE source.menu_id IN (@assembly_recipe_menu_id, @assembly_order_menu_id)
      AND source.deleted = b'0'
      AND (
        NOT EXISTS (
          SELECT 1 FROM system_role_menu parent_grant
          WHERE parent_grant.role_id = source.role_id
            AND parent_grant.menu_id = @assembly_base_parent_id
            AND parent_grant.tenant_id = source.tenant_id
            AND parent_grant.deleted = b'0'
        )
        OR NOT EXISTS (
          SELECT 1 FROM system_role_menu parent_grant
          WHERE parent_grant.role_id = source.role_id
            AND parent_grant.menu_id = @assembly_group_menu_id
            AND parent_grant.tenant_id = source.tenant_id
            AND parent_grant.deleted = b'0'
        )
      )
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 存在缺少新父级菜单权限的角色''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  NOT EXISTS(
    SELECT 1
    FROM system_tenant_package tenant_package
    WHERE tenant_package.deleted = b'0'
      AND JSON_VALID(tenant_package.menu_ids)
      AND (JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_recipe_menu_id AS CHAR), '$')
        OR JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$'))
      AND (NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_base_parent_id AS CHAR), '$')
        OR NOT JSON_CONTAINS(tenant_package.menu_ids, CAST(@assembly_group_menu_id AS CHAR), '$'))
  ),
  'SELECT 1',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''v288: 存在缺少新父级菜单的租户套餐''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

COMMIT;

SELECT id, name, type, sort, parent_id, path, icon, component, component_name,
       status, visible, keep_alive, always_show, deleted
FROM system_menu
WHERE id IN (@assembly_base_parent_id, @assembly_group_menu_id,
             @assembly_recipe_menu_id, @assembly_order_menu_id)
ORDER BY parent_id, sort, id;

SELECT role_menu.menu_id, role_menu.tenant_id,
       COUNT(DISTINCT role_menu.role_id) AS authorized_role_count
FROM system_role_menu role_menu
WHERE role_menu.menu_id IN (@assembly_base_parent_id, @assembly_group_menu_id,
                            @assembly_recipe_menu_id, @assembly_order_menu_id)
  AND role_menu.deleted = b'0'
GROUP BY role_menu.menu_id, role_menu.tenant_id
ORDER BY role_menu.tenant_id, role_menu.menu_id;

SELECT id, name,
       JSON_CONTAINS(menu_ids, CAST(@assembly_base_parent_id AS CHAR), '$') AS has_base_parent,
       JSON_CONTAINS(menu_ids, CAST(@assembly_group_menu_id AS CHAR), '$') AS has_assembly_parent,
       JSON_CONTAINS(menu_ids, CAST(@assembly_recipe_menu_id AS CHAR), '$') AS has_recipe_menu,
       JSON_CONTAINS(menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$') AS has_order_menu
FROM system_tenant_package
WHERE deleted = b'0'
  AND JSON_VALID(menu_ids)
  AND (JSON_CONTAINS(menu_ids, CAST(@assembly_recipe_menu_id AS CHAR), '$')
    OR JSON_CONTAINS(menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$'))
ORDER BY id;
