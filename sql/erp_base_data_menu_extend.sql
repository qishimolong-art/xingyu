-- =============================================
-- 基础数据管理 - 新增菜单
-- 采购方式、送货方式、结算方式、开票类型、收货地址、订货公司
-- =============================================

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6410, '采购方式管理', 'erp:base-data:query', 2, 5, 6400, 'purchase-type', 'ep:shopping-cart', 'erp/base/purchase-type/index', 'ErpBasePurchaseType', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6411, '送货方式管理', 'erp:base-data:query', 2, 6, 6400, 'delivery-method', 'ep:van', 'erp/base/delivery-method/index', 'ErpBaseDeliveryMethod', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6412, '结算方式管理', 'erp:base-data:query', 2, 7, 6400, 'settle-method', 'ep:money', 'erp/base/settle-method/index', 'ErpBaseSettleMethod', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6413, '开票类型管理', 'erp:base-data:query', 2, 8, 6400, 'invoice-type', 'ep:document', 'erp/base/invoice-type/index', 'ErpBaseInvoiceType', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6414, '收货地址管理', 'erp:base-data:query', 2, 9, 6400, 'receive-address', 'ep:office-building', 'erp/base/receive-address/index', 'ErpBaseReceiveAddress', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6415, '订货公司管理', 'erp:base-data:query', 2, 10, 6400, 'order-company', 'ep:coordinate', 'erp/base/order-company/index', 'ErpBaseOrderCompany', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');
