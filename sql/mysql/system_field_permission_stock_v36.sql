-- System field permission v36: ERP stock management
-- This script registers only form and detail-item fields for the first phase.
-- List columns, report columns and query fields are intentionally not registered yet.
-- Reserved future prefixes: col_ for list columns, report_ for report columns, query_ for search fields.

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
-- 库存入库
('erp_stock_in', 'no', '入库单号', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'inTime', '入库时间', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'supplierId', '供应商', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'remark', '备注', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'fileUrl', '附件', 'main_form', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_warehouseId', '仓库名称', 'detail_item', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_productId', '产品名称', 'detail_item', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_stockCount', '库存', 'detail_item', 220, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_productBarCode', '条码', 'detail_item', 230, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_productUnitName', '单位', 'detail_item', 240, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_remark', '备注', 'detail_item', 250, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_count', '数量', 'detail_item', 260, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_productPrice', '产品单价', 'detail_item', 270, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_totalPrice', '金额', 'detail_item', 280, '1', NOW(), '1', NOW(), b'0', 1),

-- 库存出库
('erp_stock_out', 'no', '出库单号', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'outTime', '出库时间', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'customerId', '客户', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'remark', '备注', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'fileUrl', '附件', 'main_form', 50, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_warehouseId', '仓库名称', 'detail_item', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_productId', '产品名称', 'detail_item', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_stockCount', '库存', 'detail_item', 220, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_productBarCode', '条码', 'detail_item', 230, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_productUnitName', '单位', 'detail_item', 240, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_remark', '备注', 'detail_item', 250, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_count', '数量', 'detail_item', 260, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_productPrice', '产品单价', 'detail_item', 270, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_totalPrice', '金额', 'detail_item', 280, '1', NOW(), '1', NOW(), b'0', 1),

-- 库存调拨
('erp_stock_move', 'no', '调拨单号', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'moveTime', '调拨时间', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'remark', '备注', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'fileUrl', '附件', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_fromWarehouseId', '调出仓库', 'detail_item', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_toWarehouseId', '调入仓库', 'detail_item', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_productId', '产品名称', 'detail_item', 220, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_stockCount', '库存', 'detail_item', 230, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_productBarCode', '条码', 'detail_item', 240, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_productUnitName', '单位', 'detail_item', 250, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_remark', '备注', 'detail_item', 260, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_count', '数量', 'detail_item', 270, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_productPrice', '产品单价', 'detail_item', 280, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_totalPrice', '金额', 'detail_item', 290, '1', NOW(), '1', NOW(), b'0', 1),

-- 库存盘点
('erp_stock_check', 'no', '盘点单号', 'main_form', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'checkTime', '盘点时间', 'main_form', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'remark', '备注', 'main_form', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'fileUrl', '附件', 'main_form', 40, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_warehouseId', '仓库名称', 'detail_item', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_productId', '产品名称', 'detail_item', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_stockCount', '账面库存', 'detail_item', 220, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_productBarCode', '条码', 'detail_item', 230, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_productUnitName', '单位', 'detail_item', 240, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_remark', '备注', 'detail_item', 250, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_actualCount', '实际库存', 'detail_item', 260, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_count', '盈亏数量', 'detail_item', 270, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_productPrice', '产品单价', 'detail_item', 280, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_totalPrice', '金额', 'detail_item', 290, '1', NOW(), '1', NOW(), b'0', 1),

-- 库存调整
('erp_stock', 'productId', '产品ID', 'stock_adjust', 400, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock', 'warehouseId', '仓库ID', 'stock_adjust', 410, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock', 'productName', '产品', 'stock_adjust', 420, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock', 'warehouseName', '仓库', 'stock_adjust', 430, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock', 'currentCount', '当前库存数', 'stock_adjust', 440, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock', 'targetCount', '调整后库存数', 'stock_adjust', 450, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock', 'reason', '调整原因', 'stock_adjust', 460, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock', 'remark', '备注', 'stock_adjust', 470, '1', NOW(), '1', NOW(), b'0', 1),

-- 仓库管理
('erp_warehouse', 'name', '仓库名称', 'base_info', 10, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'warehouseCode', '仓库编码', 'base_info', 20, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'warehouseType', '仓库类型', 'base_info', 30, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'status', '开启状态', 'enable_control', 100, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'saleEnabled', '销售启用', 'enable_control', 110, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'purchaseEnabled', '采购启用', 'enable_control', 120, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'stockBillEnabled', '入出仓单', 'enable_control', 130, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'scanControl', '扫码管控', 'operation_control', 200, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'splitOrder', '是否拆单', 'operation_control', 210, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'sort', '排序', 'system_info', 300, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'remark', '备注', 'system_info', 310, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31137, '设置角色字段权限', 'system:permission:assign-role-field-permission', 3, 8, 101, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `keep_alive` = VALUES(`keep_alive`),
  `always_show` = VALUES(`always_show`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31137, '1', NOW(), '1', NOW(), b'0', 1);

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_stock_in', 'deptId', '所属部门', 'main_form', 5, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'item_productCode', '产品编码', 'detail_item', 205, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'creatorName', '创建人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'createTime', '创建时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'updaterName', '修改人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_in', 'updateTime', '修改时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'deptId', '所属部门', 'main_form', 5, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'item_productCode', '产品编码', 'detail_item', 205, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'creatorName', '创建人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'createTime', '创建时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'updaterName', '修改人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_out', 'updateTime', '修改时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'deptId', '所属部门', 'main_form', 5, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'item_productCode', '产品编码', 'detail_item', 215, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'creatorName', '创建人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'createTime', '创建时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'updaterName', '修改人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_move', 'updateTime', '修改时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'deptId', '所属部门', 'main_form', 5, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'item_productCode', '产品编码', 'detail_item', 205, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'creatorName', '创建人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'createTime', '创建时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'updaterName', '修改人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_stock_check', 'updateTime', '修改时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'deptId', '所属部门', 'base_info', 5, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'creatorName', '创建人', 'system_info', 900, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'createTime', '创建时间', 'system_info', 910, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'updaterName', '修改人', 'system_info', 920, '1', NOW(), '1', NOW(), b'0', 1),
('erp_warehouse', 'updateTime', '修改时间', 'system_info', 930, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = '1',
  `update_time` = NOW(),
  `deleted` = b'0';
