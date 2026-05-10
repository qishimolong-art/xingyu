-- =============================================
-- 采购订单新增字段 - 数据库迁移脚本
-- =============================================

-- 采购订单表新增字段（采购员、部门、订货日期、采购周期、订货公司、税率）
ALTER TABLE erp_purchase_order ADD COLUMN purchaser BIGINT COMMENT '采购员（用户ID）';
ALTER TABLE erp_purchase_order ADD COLUMN dept_id BIGINT COMMENT '部门ID';
ALTER TABLE erp_purchase_order ADD COLUMN order_date DATE COMMENT '订货日期';
ALTER TABLE erp_purchase_order ADD COLUMN purchase_cycle INT COMMENT '采购周期(天)';
ALTER TABLE erp_purchase_order ADD COLUMN order_company VARCHAR(128) COMMENT '订货公司';
ALTER TABLE erp_purchase_order ADD COLUMN tax_percent DECIMAL(24,2) COMMENT '税率(%)';

-- 基础数据管理 - 新增菜单（采购方式、送货方式、结算方式、开票类型、收货地址、订货公司）
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6410, '采购方式管理', 'erp:base-data:query', 2, 5, 6400, 'purchase-type', 'ep:shopping-cart', 'erp/base/purchase-type/index', 'ErpBasePurchaseType', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6411, '送货方式管理', 'erp:base-data:query', 2, 6, 6400, 'delivery-method', 'ep:truck', 'erp/base/delivery-method/index', 'ErpBaseDeliveryMethod', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6412, '结算方式管理', 'erp:base-data:query', 2, 7, 6400, 'settle-method', 'ep:money', 'erp/base/settle-method/index', 'ErpBaseSettleMethod', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6413, '开票类型管理', 'erp:base-data:query', 2, 8, 6400, 'invoice-type', 'ep:document', 'erp/base/invoice-type/index', 'ErpBaseInvoiceType', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6414, '收货地址管理', 'erp:base-data:query', 2, 9, 6400, 'receive-address', 'ep:office-building', 'erp/base/receive-address/index', 'ErpBaseReceiveAddress', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (6415, '订货公司管理', 'erp:base-data:query', 2, 10, 6400, 'order-company', 'ep:coordinate', 'erp/base/order-company/index', 'ErpBaseOrderCompany', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');
