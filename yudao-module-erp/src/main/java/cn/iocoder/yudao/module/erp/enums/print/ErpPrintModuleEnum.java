package cn.iocoder.yudao.module.erp.enums.print;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpPrintModuleEnum {

    PURCHASE_ORDER("purchase_order", "采购订单", "erp_purchase_order", "erp:purchase-order"),
    PURCHASE_IN("purchase_in", "采购入库", "purchase_in", "erp:purchase-in"),
    PURCHASE_RETURN("purchase_return", "采购退货", "purchase_return", "erp:purchase-return"),
    PURCHASE_INVOICE("purchase_invoice", "采购发票", "purchase_invoice", "erp:purchase-invoice"),
    PURCHASE_PRICE_ADJUST("purchase_price_adjust", "采购调价", "purchase_price_adjust",
            "erp:purchase-price-adjust"),
    SALE_ORDER("sale_order", "销售订单", "erp_sale_order", "erp:sale-order"),
    SALE_OUT("sale_out", "销售出库", "erp_sale_out", "erp:sale-out"),
    SALE_RETURN("sale_return", "销售退货", "erp_sale_return", "erp:sale-return"),
    SALE_QUOTE("sale_quote", "销售报价", "erp_sale_quote", "erp:sale-quote"),
    SALE_PRICE_ADJUST("sale_price_adjust", "销售调价", "erp_sale_price_adjust",
            "erp:sale-price-adjust"),
    SALE_CART("sale_cart", "销售手推车", "erp_sale_cart", "erp:sale-cart"),
    FINANCE_PAYMENT("finance_payment", "付款单", "erp_finance_payment", "erp:finance-payment"),
    FINANCE_RECEIPT("finance_receipt", "收款单", "erp_finance_receipt", "erp:finance-receipt"),
    PAYABLE_OTHER("payable_other", "其他应付", "erp_finance_payable_other", "erp:payable-other"),
    PAYABLE_EXPENSE("payable_expense", "费用支付", "erp_finance_payable_expense", "erp:payable-expense"),
    RECEIVABLE_OTHER("receivable_other", "其他应收", "erp_finance_receivable_other", "erp:receivable-other"),
    RECEIVABLE_OTHER_INCOME("receivable_other_income", "其他收入",
            "erp_finance_receivable_other_income", "erp:receivable-other-income"),
    FINANCE_TRANSFER("finance_transfer", "银行转账", "erp_finance_transfer", "erp:finance-transfer"),
    ACCOUNTING_VOUCHER("accounting_voucher", "凭证", "erp_accounting_voucher", "erp:voucher"),
    STOCK_IN("stock_in", "其它入库单", "erp_stock_in", "erp:stock-in"),
    STOCK_OUT("stock_out", "其它出库单", "erp_stock_out", "erp:stock-out"),
    STOCK_TRANSFER_OUT("stock_transfer_out", "调拨出库单", "erp_stock_transfer_out", "erp:stock-transfer-out"),
    WAREHOUSE_MOVE("warehouse_move", "仓库移货单", "erp_warehouse_move", "erp:warehouse-move"),
    STOCK_CHECK("stock_check", "库存盘点单", "erp_stock_check", "erp:stock-check");

    private final String key;
    private final String name;
    private final String fieldModuleKey;
    private final String permissionPrefix;

    public String getPrintPermission() {
        return permissionPrefix + ":print";
    }

    public String getPrintTemplatePermission() {
        return permissionPrefix + ":print-template";
    }

    public static boolean isSupported(String key) {
        return fromKey(key) != null;
    }

    public static ErpPrintModuleEnum fromKey(String key) {
        for (ErpPrintModuleEnum value : values()) {
            if (value.getKey().equals(key)) {
                return value;
            }
        }
        return null;
    }

}
