-- ERP 基础数据管理菜单
-- 父菜单：基础数据（目录类型，parent_id = 2563 即 ERP 系统）
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6400, '基础数据', '', 1, 60, 2563, 'base', 'ep:collection', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 子菜单：区域管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6401, '区域管理', 'erp:base-data:query', 2, 1, 6400, 'region', 'ep:location', 'erp/base/region/index', 'ErpBaseRegion', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 子菜单：往来类别管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6402, '往来类别管理', 'erp:base-data:query', 2, 2, 6400, 'category', 'ep:folder', 'erp/base/category/index', 'ErpBaseCategory', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 子菜单：供应商类型管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6403, '供应商类型管理', 'erp:base-data:query', 2, 3, 6400, 'supplier-type', 'ep:user', 'erp/base/supplier-type/index', 'ErpBaseSupplierType', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 子菜单：物流公司管理
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6404, '物流公司管理', 'erp:base-data:query', 2, 4, 6400, 'logistics-company', 'ep:van', 'erp/base/logistics-company/index', 'ErpBaseLogisticsCompany', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 按钮权限：创建、更新、删除（挂在区域管理下，因为所有页面共享同一套权限标识）
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6405, '基础数据创建', 'erp:base-data:create', 3, 1, 6401, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6406, '基础数据更新', 'erp:base-data:update', 3, 2, 6401, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6407, '基础数据删除', 'erp:base-data:delete', 3, 3, 6401, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');
