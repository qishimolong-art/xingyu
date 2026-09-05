package cn.iocoder.yudao.module.erp.enums;

/**
 * ERP 操作日志枚举
 * 目的：统一管理，也减少 Service 里各种“复杂”字符串
 *
 * @author 芋道源码
 */
public interface LogRecordConstants {

    String ERP_PURCHASE_ORDER_TYPE = "采购订单";
    String ERP_PURCHASE_IN_TYPE = "采购入库";
    String ERP_PURCHASE_RETURN_TYPE = "采购退货";
    String ERP_PURCHASE_PRICE_ADJUST_TYPE = "采购调价";
    String ERP_PURCHASE_INVOICE_TYPE = "采购票据";
    String ERP_SUPPLIER_TYPE = "供应商";
    String ERP_SALE_ORDER_TYPE = "销售订单";
    String ERP_SALE_OUT_TYPE = "销售出库";
    String ERP_SALE_RETURN_TYPE = "销售退货";
    String ERP_SALE_QUOTE_TYPE = "销售报价";
    String ERP_SALE_CART_TYPE = "销售手推车";
    String ERP_SALE_PRICE_ADJUST_TYPE = "销售调价";
    String ERP_CUSTOMER_TYPE = "客户";
    String ERP_PRODUCT_TYPE = "配件信息";
    String ERP_PRODUCT_BRAND_TYPE = "配件品牌";
    String ERP_PRODUCT_CATEGORY_TYPE = "配件分类";
    String ERP_PRODUCT_UNIT_TYPE = "配件单位";
    String ERP_ACCOUNT_TYPE = "银行账户";
    String ERP_FINANCE_RECEIPT_TYPE = "收款单";
    String ERP_FINANCE_PAYMENT_TYPE = "付款单";
    String ERP_FINANCE_TRANSFER_TYPE = "银行转账单";
    String ERP_RECEIVABLE_OTHER_TYPE = "其他应收";
    String ERP_RECEIVABLE_OTHER_INCOME_TYPE = "其他收入";
    String ERP_PAYABLE_OTHER_TYPE = "其他应付";
    String ERP_PAYABLE_EXPENSE_TYPE = "费用支付";
    String ERP_RECEIVABLE_WRITEOFF_TYPE = "应收核销";
    String ERP_PAYABLE_WRITEOFF_TYPE = "应付核销";
    String ERP_WAREHOUSE_TYPE = "仓库";
    String ERP_STOCK_IN_TYPE = "其它入库单";
    String ERP_STOCK_OUT_TYPE = "其它出库单";
    String ERP_STOCK_MOVE_TYPE = "库存调拨单";
    String ERP_WAREHOUSE_MOVE_TYPE = "仓库移货单";
    String ERP_STOCK_CHECK_TYPE = "库存盘点单";

    String ERP_CREATE_SUB_TYPE = "新增";
    String ERP_UPDATE_SUB_TYPE = "修改";
    String ERP_DELETE_SUB_TYPE = "删除";
    String ERP_IMPORT_SUB_TYPE = "导入";
    String ERP_EXPORT_SUB_TYPE = "导出";
    String ERP_APPROVE_SUB_TYPE = "审核";
    String ERP_PROCESS_SUB_TYPE = "反审核";
    String ERP_WRITEOFF_SUB_TYPE = "核销";

}
