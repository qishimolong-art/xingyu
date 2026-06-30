-- ERP field config all form seeds v72.
-- Scope: ERP form-field configuration modules that have stable create/edit/detail forms.
-- Safe to execute repeatedly. This script only inserts missing active tenant-1 rows
-- and does not overwrite user-edited labels, required flags, visible flags, groups, or sort.
--
-- Not included because they are list/report/detail-only pages, not form-field configuration:
-- stock_record, stock_in_bill, stock_out_bill, payable_account, receivable_account,
-- settlement_offset, accounting balance/income/cash-flow reports, base recycle-bin.

DROP PROCEDURE IF EXISTS add_erp_field_config_columns_v72;

DELIMITER //
CREATE PROCEDURE add_erp_field_config_columns_v72()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'visible'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `visible` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否显示'
            AFTER `required`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_group'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_group` varchar(64) NULL COMMENT 'field group'
            AFTER `sort`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'field_source'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `field_source` varchar(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'field source: SYSTEM/CUSTOM'
            AFTER `field_group`;
    END IF;
END //
DELIMITER ;

CALL add_erp_field_config_columns_v72();
DROP PROCEDURE IF EXISTS add_erp_field_config_columns_v72;

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `field_group`, `field_source`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module_key`, seed.`field_name`, seed.`field_label`, seed.`required`, b'1', seed.`sort`,
       seed.`field_group`, 'SYSTEM', '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 'erp_product_category' module_key, 'parentId' field_name, '上级分类' field_label, b'0' required, 10 sort, 'base_info' field_group UNION ALL
    SELECT 'erp_product_category', 'name', '分类名称', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_product_category', 'code', '分类编码', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_product_category', 'sort', '显示顺序', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_product_category', 'status', '状态', b'0', 50, 'status_info' UNION ALL
    SELECT 'erp_product_unit', 'name', '单位名称', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_product_unit', 'status', '单位状态', b'0', 20, 'status_info' UNION ALL
    SELECT 'erp_price_system', 'code', '价格体系编码', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_price_system', 'name', '价格体系名称', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_price_system', 'status', '状态', b'0', 30, 'status_info' UNION ALL
    SELECT 'erp_price_system', 'sort', '排序', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_price_system', 'remark', '备注', b'0', 50, 'base_info' UNION ALL

    SELECT 'erp_base_data', 'name', '名称', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_base_data', 'code', '编码', b'0', 20, 'base_info' UNION ALL
    SELECT 'erp_base_data', 'sort', '排序', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_base_data', 'status', '状态', b'0', 40, 'status_info' UNION ALL
    SELECT 'erp_base_data', 'remark', '备注', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_warehouse', 'name', '仓库名称', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_warehouse', 'warehouseCode', '仓库编码', b'0', 20, 'base_info' UNION ALL
    SELECT 'erp_warehouse', 'deptId', '所属部门', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_warehouse', 'warehouseType', '仓库类型', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_warehouse', 'status', '开启状态', b'0', 50, 'status_info' UNION ALL
    SELECT 'erp_warehouse', 'sort', '排序', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_warehouse', 'saleEnabled', '销售启用', b'0', 70, 'status_info' UNION ALL
    SELECT 'erp_warehouse', 'purchaseEnabled', '采购启用', b'0', 80, 'status_info' UNION ALL
    SELECT 'erp_warehouse', 'stockBillEnabled', '入出仓单', b'0', 90, 'status_info' UNION ALL
    SELECT 'erp_warehouse', 'scanControl', '扫码管控', b'0', 100, 'status_info' UNION ALL
    SELECT 'erp_warehouse', 'splitOrder', '是否拆单', b'0', 110, 'status_info' UNION ALL
    SELECT 'erp_warehouse', 'remark', '备注', b'0', 120, 'base_info' UNION ALL

    SELECT 'erp_sale_config', 'quoteAutoAudit', '报价自动审核', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_sale_config', 'orderAutoAudit', '订单自动审核', b'0', 20, 'base_info' UNION ALL
    SELECT 'erp_sale_config', 'outAutoAudit', '出库自动审核', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_sale_config', 'returnAutoAudit', '退货自动审核', b'0', 40, 'base_info' UNION ALL

    SELECT 'erp_stock_in', 'no', '入库单号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_stock_in', 'inTime', '入库时间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_stock_in', 'supplierId', '供应商', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_stock_in', 'remark', '备注', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_stock_in', 'fileUrl', '附件', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_stock_in', 'items', '入库产品清单', b'0', 60, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'warehouseId', '仓库名称', b'0', 70, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'productId', '产品名称', b'0', 80, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'stockCount', '库存', b'0', 90, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'productCode', '产品编码', b'0', 100, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'productBarCode', '条码', b'0', 110, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'productUnitName', '单位', b'0', 120, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'count', '数量', b'0', 130, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'productPrice', '产品单价', b'0', 140, 'detail_item' UNION ALL
    SELECT 'erp_stock_in', 'totalPrice', '金额', b'0', 150, 'detail_item' UNION ALL

    SELECT 'erp_stock_out', 'no', '出库单号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_stock_out', 'outTime', '出库时间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_stock_out', 'customerId', '客户', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_stock_out', 'remark', '备注', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_stock_out', 'fileUrl', '附件', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_stock_out', 'items', '出库产品清单', b'0', 60, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'warehouseId', '仓库名称', b'0', 70, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'productId', '产品名称', b'0', 80, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'stockCount', '库存', b'0', 90, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'productCode', '产品编码', b'0', 100, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'productBarCode', '条码', b'0', 110, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'productUnitName', '单位', b'0', 120, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'count', '数量', b'0', 130, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'productPrice', '产品单价', b'0', 140, 'detail_item' UNION ALL
    SELECT 'erp_stock_out', 'totalPrice', '金额', b'0', 150, 'detail_item' UNION ALL

    SELECT 'erp_stock_move', 'no', '调拨单号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_stock_move', 'moveTime', '调拨时间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_stock_move', 'remark', '备注', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_stock_move', 'fileUrl', '附件', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_stock_move', 'items', '调拨产品清单', b'0', 50, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'fromWarehouseId', '调出仓库', b'0', 60, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'toWarehouseId', '调入仓库', b'0', 70, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'productId', '产品名称', b'0', 80, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'stockCount', '库存', b'0', 90, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'productCode', '产品编码', b'0', 100, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'productBarCode', '条码', b'0', 110, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'productUnitName', '单位', b'0', 120, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'count', '数量', b'0', 130, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'productPrice', '产品单价', b'0', 140, 'detail_item' UNION ALL
    SELECT 'erp_stock_move', 'totalPrice', '金额', b'0', 150, 'detail_item' UNION ALL

    SELECT 'erp_stock_check', 'no', '盘点单号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_stock_check', 'checkTime', '盘点时间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_stock_check', 'remark', '备注', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_stock_check', 'fileUrl', '附件', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_stock_check', 'items', '产品清单', b'0', 50, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'warehouseId', '仓库名称', b'0', 60, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'productId', '产品名称', b'0', 70, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'stockCount', '账面库存', b'0', 80, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'actualCount', '实际库存', b'0', 90, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'count', '盈亏数量', b'0', 100, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'productPrice', '产品单价', b'0', 110, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'totalPrice', '金额', b'0', 120, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'productCode', '产品编码', b'0', 130, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'productBarCode', '条码', b'0', 140, 'detail_item' UNION ALL
    SELECT 'erp_stock_check', 'productUnitName', '单位', b'0', 150, 'detail_item' UNION ALL

    SELECT 'erp_stock', 'productName', '配件', b'1', 10, 'stock_info' UNION ALL
    SELECT 'erp_stock', 'warehouseName', '仓库', b'1', 20, 'stock_info' UNION ALL
    SELECT 'erp_stock', 'currentCount', '当前库存数', b'0', 30, 'stock_info' UNION ALL
    SELECT 'erp_stock', 'targetCount', '调整后库存数', b'1', 40, 'stock_info' UNION ALL
    SELECT 'erp_stock', 'reason', '调整原因', b'0', 50, 'stock_info' UNION ALL
    SELECT 'erp_stock', 'remark', '备注', b'0', 60, 'stock_info' UNION ALL

    SELECT 'erp_account', 'name', '名称', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_account', 'accountType', '账户类型', b'1', 20, 'finance_info' UNION ALL
    SELECT 'erp_account', 'bankName', '开户行', b'0', 30, 'finance_info' UNION ALL
    SELECT 'erp_account', 'bankAccount', '银行账号', b'0', 40, 'finance_info' UNION ALL
    SELECT 'erp_account', 'status', '状态', b'0', 50, 'status_info' UNION ALL
    SELECT 'erp_account', 'sort', '排序', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_account', 'defaultStatus', '是否默认', b'0', 70, 'status_info' UNION ALL
    SELECT 'erp_account', 'no', '编码', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_account', 'deptId', '所属部门', b'0', 90, 'base_info' UNION ALL
    SELECT 'erp_account', 'remark', '备注', b'0', 100, 'base_info' UNION ALL
    SELECT 'erp_account', 'creatorName', '创建人', b'0', 110, 'system_info' UNION ALL
    SELECT 'erp_account', 'createTime', '创建时间', b'0', 120, 'system_info' UNION ALL
    SELECT 'erp_account', 'updaterName', '修改人', b'0', 130, 'system_info' UNION ALL
    SELECT 'erp_account', 'updateTime', '修改时间', b'0', 140, 'system_info' UNION ALL

    SELECT 'erp_finance_transfer', 'no', '转账单号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_finance_transfer', 'transferTime', '转账时间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_finance_transfer', 'outAccountId', '转出账户', b'1', 30, 'finance_info' UNION ALL
    SELECT 'erp_finance_transfer', 'outAccountBalance', '账户余额', b'0', 40, 'finance_info' UNION ALL
    SELECT 'erp_finance_transfer', 'inAccountId', '转入账户', b'1', 50, 'finance_info' UNION ALL
    SELECT 'erp_finance_transfer', 'transferPrice', '转账金额', b'1', 60, 'finance_info' UNION ALL
    SELECT 'erp_finance_transfer', 'financeUserId', '财务人员', b'0', 70, 'finance_info' UNION ALL
    SELECT 'erp_finance_transfer', 'deptId', '所属部门', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_finance_transfer', 'remark', '备注', b'0', 90, 'base_info' UNION ALL
    SELECT 'erp_finance_transfer', 'fileUrl', '附件', b'0', 100, 'base_info' UNION ALL

    SELECT 'erp_finance_payment', 'no', '付款单号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_finance_payment', 'paymentTime', '付款时间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_finance_payment', 'supplierId', '供应商', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_finance_payment', 'financeUserId', '经手人', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_finance_payment', 'deptId', '所属部门', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_finance_payment', 'remark', '备注', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_finance_payment', 'fileUrl', '附件', b'0', 70, 'base_info' UNION ALL
    SELECT 'erp_finance_payment', 'items', '采购入库、退货单', b'0', 80, 'detail_item' UNION ALL
    SELECT 'erp_finance_payment', 'accountId', '付款账户', b'0', 90, 'finance_info' UNION ALL
    SELECT 'erp_finance_payment', 'totalPrice', '合计付款', b'0', 100, 'finance_info' UNION ALL
    SELECT 'erp_finance_payment', 'discountPrice', '优惠金额', b'0', 110, 'finance_info' UNION ALL
    SELECT 'erp_finance_payment', 'paymentPrice', '实际付款', b'0', 120, 'finance_info' UNION ALL
    SELECT 'erp_finance_payment', 'bizNo', '单据编号', b'0', 130, 'detail_item' UNION ALL
    SELECT 'erp_finance_payment', 'paidPrice', '已付金额', b'0', 140, 'detail_item' UNION ALL

    SELECT 'erp_finance_receipt', 'no', '收款单号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_finance_receipt', 'receiptTime', '收款时间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_finance_receipt', 'customerId', '客户', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_finance_receipt', 'financeUserId', '财务人员', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_finance_receipt', 'deptId', '所属部门', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_finance_receipt', 'accountId', '收款账户', b'0', 60, 'finance_info' UNION ALL
    SELECT 'erp_finance_receipt', 'remark', '备注', b'0', 70, 'base_info' UNION ALL
    SELECT 'erp_finance_receipt', 'fileUrl', '附件', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_finance_receipt', 'items', '销售单据', b'0', 90, 'detail_item' UNION ALL
    SELECT 'erp_finance_receipt', 'totalPrice', '合计收款', b'0', 100, 'finance_info' UNION ALL
    SELECT 'erp_finance_receipt', 'discountPrice', '优惠金额', b'0', 110, 'finance_info' UNION ALL
    SELECT 'erp_finance_receipt', 'receiptPrice', '实际收款', b'0', 120, 'finance_info' UNION ALL
    SELECT 'erp_finance_receipt', 'bizNo', '单据编号', b'0', 130, 'detail_item' UNION ALL
    SELECT 'erp_finance_receipt', 'receiptedPrice', '已收金额', b'0', 140, 'detail_item' UNION ALL

    SELECT 'erp_finance_payable_expense', 'no', '单据编号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'bizTime', '单据日期', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'settleMethod', '结算方式', b'0', 30, 'finance_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'accountId', '结算账户', b'0', 40, 'finance_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'voucherNo', '凭证号', b'0', 50, 'finance_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'expenseType', '费用类型', b'0', 60, 'finance_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'deptId', '申请部门', b'0', 70, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'handlerId', '申请人', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'party', '收款对象', b'0', 90, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'docType', '单据类型', b'0', 100, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'relatedBiz', '相关业务', b'0', 110, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'fileUrl', '附件', b'0', 120, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'remark', '备注', b'0', 130, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'totalAmount', '总金额', b'0', 140, 'finance_info' UNION ALL
    SELECT 'erp_finance_payable_expense', 'itemName', '费用项目', b'0', 150, 'detail_item' UNION ALL
    SELECT 'erp_finance_payable_expense', 'amount', '金额', b'0', 160, 'detail_item' UNION ALL
    SELECT 'erp_finance_payable_expense', 'invoiceNo', '发票号', b'0', 170, 'detail_item' UNION ALL
    SELECT 'erp_finance_payable_expense', 'bizDate', '发生日期', b'0', 180, 'detail_item' UNION ALL
    SELECT 'erp_finance_payable_expense', 'qty', '数量', b'0', 190, 'detail_item' UNION ALL
    SELECT 'erp_finance_payable_expense', 'expenseCategory', '费用分类', b'0', 200, 'detail_item' UNION ALL

    SELECT 'erp_finance_payable_other', 'no', '单据编号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'bizTime', '开单日期', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'supplierId', '供应商', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'voucherNo', '凭证号', b'0', 40, 'finance_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'deptId', '部门', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'handlerId', '经手人', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'project', '调账项目', b'0', 70, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'sourceType', '来源类型', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'settledAmount', '已结金额', b'0', 90, 'finance_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'payableAmount', '应付金额', b'1', 100, 'finance_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'fileUrl', '附件', b'0', 110, 'base_info' UNION ALL
    SELECT 'erp_finance_payable_other', 'remark', '调账原因', b'0', 120, 'base_info' UNION ALL

    SELECT 'erp_finance_receivable_other_income', 'no', '单据编号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'bizTime', '业务时间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'settleMethod', '结算方式', b'0', 30, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'accountId', '收款账户', b'0', 40, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'voucherNo', '凭证号', b'0', 50, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'incomeType', '收入类型', b'0', 60, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'deptId', '部门', b'0', 70, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'handlerId', '经手人', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'party', '往来单位', b'0', 90, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'relatedBiz', '关联业务', b'0', 100, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'docType', '单据类型', b'0', 110, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'totalAmount', '收入合计', b'0', 120, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'fileUrl', '附件', b'0', 130, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'remark', '备注', b'0', 140, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'itemName', '项目名称', b'0', 150, 'detail_item' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'amount', '收入金额', b'0', 160, 'detail_item' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'invoiceNo', '发票号', b'0', 170, 'detail_item' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'bizDate', '业务日期', b'0', 180, 'detail_item' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'qty', '数量', b'0', 190, 'detail_item' UNION ALL
    SELECT 'erp_finance_receivable_other_income', 'freightType', '运费类型', b'0', 200, 'detail_item' UNION ALL

    SELECT 'erp_finance_receivable_other', 'no', '单据编号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'bizTime', '业务日期', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'customerId', '客户', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'voucherNo', '凭证号', b'0', 40, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'deptId', '部门', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'handlerId', '经手人', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'project', '调账项目', b'0', 70, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'sourceType', '来源类型', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'receivableType', '应收类型', b'0', 90, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'sourceNo', '来源单号', b'0', 100, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'isPaperNote', '纸质单据', b'0', 110, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'paperNoteDesc', '单据说明', b'0', 120, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'settledAmount', '已结金额', b'0', 130, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'receivableAmount', '应收金额', b'1', 140, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'costAmount', '成本金额', b'0', 150, 'finance_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'fileUrl', '附件', b'0', 160, 'base_info' UNION ALL
    SELECT 'erp_finance_receivable_other', 'remark', '备注', b'0', 170, 'base_info' UNION ALL

    SELECT 'erp_accounting_subject', 'subjectCode', '科目编码', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'subjectName', '科目名称', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'shortName', '科目别名', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'subjectCategory', '科目类型', b'1', 40, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'voucherType', '凭证类型', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'parentCode', '父科目编码', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'subjectLevel', '层级', b'0', 70, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'balanceDirection', '余额方向', b'1', 80, 'finance_info' UNION ALL
    SELECT 'erp_accounting_subject', 'openingBalance', '期初余额', b'0', 90, 'finance_info' UNION ALL
    SELECT 'erp_accounting_subject', 'enable', '是否启用', b'0', 100, 'status_info' UNION ALL
    SELECT 'erp_accounting_subject', 'sort', '排序', b'0', 110, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'auxiliaryTypes', '辅助核算', b'0', 120, 'base_info' UNION ALL
    SELECT 'erp_accounting_subject', 'remark', '备注', b'0', 130, 'base_info' UNION ALL

    SELECT 'erp_accounting_voucher_word', 'code', '凭证字代码', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_word', 'name', '名称', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_word', 'enable', '是否启用', b'0', 30, 'status_info' UNION ALL
    SELECT 'erp_accounting_voucher_word', 'sort', '排序', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_word', 'remark', '备注', b'0', 50, 'base_info' UNION ALL

    SELECT 'erp_accounting_voucher', 'voucherWord', '凭证字', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'voucherNo', '凭证编号', b'0', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'voucherDate', '凭证日期', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'attachmentCount', '附件张数', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'summary', '摘要', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'remark', '备注', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'bookkeeperUserId', '记账', b'0', 70, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'cashierUserId', '出纳', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'supervisorUserId', '主管', b'0', 90, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'auditorUserId', '审核', b'0', 100, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'makerUserId', '制单', b'0', 110, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'projectId', '项目', b'0', 120, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'deptId', '部门', b'0', 130, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'customerId', '客户', b'0', 140, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'personUserId', '个人', b'0', 150, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'generateBusinessDoc', '是否产生业务单据', b'0', 160, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'bizDocType', '业务单据类型', b'0', 170, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'auditTime', '审核时间', b'0', 180, 'system_info' UNION ALL
    SELECT 'erp_accounting_voucher', 'subjectId', '会计科目', b'0', 190, 'detail_item' UNION ALL
    SELECT 'erp_accounting_voucher', 'auxiliaryName', '核算项', b'0', 200, 'detail_item' UNION ALL
    SELECT 'erp_accounting_voucher', 'debitAmount', '借方金额', b'0', 210, 'detail_item' UNION ALL
    SELECT 'erp_accounting_voucher', 'creditAmount', '贷方金额', b'0', 220, 'detail_item' UNION ALL

    SELECT 'erp_accounting_voucher_attribution', 'bizType', '业务单据类型', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'bizNo', '业务单号', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'bizDate', '业务发生日期', b'0', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'transactionParty', '交易单位', b'0', 40, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'settleMethod', '结算方式', b'0', 50, 'finance_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'shippingMethod', '运输方式', b'0', 60, 'logistics_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'bizAmount', '原始金额', b'0', 70, 'finance_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'discountAmount', '折让金额', b'0', 80, 'finance_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'receivedAmount', '实收金额', b'0', 90, 'finance_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'voucherMakeDate', '制单日期', b'0', 100, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'attributionYear', '归属年', b'1', 110, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'attributionMonth', '归属月', b'1', 120, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'receivableConfirmed', '应收确认', b'0', 130, 'status_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'invoiceIssued', '是否开票', b'0', 140, 'status_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'shipStatus', '发货状态', b'0', 150, 'status_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'summary', '摘要', b'0', 160, 'base_info' UNION ALL
    SELECT 'erp_accounting_voucher_attribution', 'remark', '备注', b'0', 170, 'base_info' UNION ALL

    SELECT 'erp_accounting_book_open', 'fiscalYear', '会计年度', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_book_open', 'period', '开账期间', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_book_open', 'startDate', '开始时间', b'1', 30, 'base_info' UNION ALL

    SELECT 'erp_accounting_pre_receipt', 'bizTime', '业务日期', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_receipt', 'partyType', '对方类型', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_receipt', 'partyName', '对方名称', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_receipt', 'accountId', '结算账户', b'1', 40, 'finance_info' UNION ALL
    SELECT 'erp_accounting_pre_receipt', 'discountAmount', '折让金额', b'0', 50, 'finance_info' UNION ALL
    SELECT 'erp_accounting_pre_receipt', 'remark', '备注', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_receipt', 'summary', '摘要', b'0', 70, 'detail_item' UNION ALL
    SELECT 'erp_accounting_pre_receipt', 'amount', '金额', b'0', 80, 'detail_item' UNION ALL

    SELECT 'erp_accounting_pre_payment', 'bizTime', '业务日期', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_payment', 'partyType', '对方类型', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_payment', 'partyName', '对方名称', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_payment', 'accountId', '结算账户', b'1', 40, 'finance_info' UNION ALL
    SELECT 'erp_accounting_pre_payment', 'remark', '备注', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_payment', 'summary', '摘要', b'0', 60, 'detail_item' UNION ALL
    SELECT 'erp_accounting_pre_payment', 'amount', '金额', b'0', 70, 'detail_item' UNION ALL

    SELECT 'erp_accounting_pre_receivable', 'bizTime', '业务日期', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_receivable', 'partyType', '对方类型', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_receivable', 'partyName', '对方名称', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_receivable', 'accountId', '结算账户', b'1', 40, 'finance_info' UNION ALL
    SELECT 'erp_accounting_pre_receivable', 'remark', '备注', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_accounting_pre_receivable', 'summary', '摘要', b'0', 60, 'detail_item' UNION ALL
    SELECT 'erp_accounting_pre_receivable', 'amount', '金额', b'0', 70, 'detail_item' UNION ALL

    SELECT 'erp_accounting_other_receivable', 'no', '单据编号', b'0', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_receivable', 'bizTime', '业务日期', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_receivable', 'partyType', '对方类型', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_receivable', 'partyName', '对方名称', b'1', 40, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_receivable', 'accountId', '结算账户', b'1', 50, 'finance_info' UNION ALL
    SELECT 'erp_accounting_other_receivable', 'remark', '备注', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_receivable', 'summary', '摘要', b'0', 70, 'detail_item' UNION ALL
    SELECT 'erp_accounting_other_receivable', 'amount', '金额', b'0', 80, 'detail_item' UNION ALL

    SELECT 'erp_accounting_other_payable', 'bizTime', '业务日期', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_payable', 'partyType', '对方类型', b'1', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_payable', 'partyName', '对方名称', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_payable', 'accountId', '结算账户', b'1', 40, 'finance_info' UNION ALL
    SELECT 'erp_accounting_other_payable', 'remark', '备注', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_accounting_other_payable', 'summary', '摘要', b'0', 60, 'detail_item' UNION ALL
    SELECT 'erp_accounting_other_payable', 'amount', '金额', b'0', 70, 'detail_item' UNION ALL

    SELECT 'erp_accounting_report_template', 'reportType', '报表类型', b'1', 10, 'base_info' UNION ALL
    SELECT 'erp_accounting_report_template', 'side', '区域', b'0', 20, 'base_info' UNION ALL
    SELECT 'erp_accounting_report_template', 'itemName', '项目名称', b'1', 30, 'base_info' UNION ALL
    SELECT 'erp_accounting_report_template', 'rowNo', '行次', b'1', 40, 'base_info' UNION ALL
    SELECT 'erp_accounting_report_template', 'parentId', '父项编号', b'0', 50, 'base_info' UNION ALL
    SELECT 'erp_accounting_report_template', 'formula', '取数公式', b'0', 60, 'base_info' UNION ALL
    SELECT 'erp_accounting_report_template', 'enable', '是否启用', b'0', 70, 'status_info' UNION ALL
    SELECT 'erp_accounting_report_template', 'sort', '排序', b'0', 80, 'base_info' UNION ALL
    SELECT 'erp_accounting_report_template', 'remark', '备注', b'0', 90, 'base_info'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_field_config` cfg
    WHERE cfg.`tenant_id` = 1
      AND cfg.`module_key` = seed.`module_key`
      AND cfg.`field_name` = seed.`field_name`
      AND cfg.`deleted` = b'0'
);

SELECT `module_key`, COUNT(*) AS `field_count`
FROM `erp_field_config`
WHERE `tenant_id` = 1
  AND `deleted` = b'0'
  AND `module_key` IN (
      'erp_product_category', 'erp_product_unit', 'erp_price_system',
      'erp_base_data', 'erp_warehouse', 'erp_sale_config',
      'erp_stock_in', 'erp_stock_out', 'erp_stock_move', 'erp_stock_check', 'erp_stock',
      'erp_account', 'erp_finance_transfer', 'erp_finance_payment', 'erp_finance_receipt',
      'erp_finance_payable_expense', 'erp_finance_payable_other',
      'erp_finance_receivable_other_income', 'erp_finance_receivable_other',
      'erp_accounting_subject', 'erp_accounting_voucher_word', 'erp_accounting_voucher',
      'erp_accounting_voucher_attribution', 'erp_accounting_book_open',
      'erp_accounting_pre_receipt', 'erp_accounting_pre_payment', 'erp_accounting_pre_receivable',
      'erp_accounting_other_receivable', 'erp_accounting_other_payable',
      'erp_accounting_report_template'
  )
GROUP BY `module_key`
ORDER BY `module_key`;

SELECT `module_key`, `field_name`, `tenant_id`, COUNT(*) AS `cnt`
FROM `erp_field_config`
WHERE `deleted` = b'0'
GROUP BY `module_key`, `field_name`, `tenant_id`
HAVING COUNT(*) > 1;
