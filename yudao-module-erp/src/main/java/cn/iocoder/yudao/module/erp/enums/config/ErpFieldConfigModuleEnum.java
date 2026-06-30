package cn.iocoder.yudao.module.erp.enums.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP field configuration module enum.
 */
@Getter
@AllArgsConstructor
public enum ErpFieldConfigModuleEnum {

    ERP_PRODUCT("erp_product", "erp_product"),
    ERP_PRODUCT_CATEGORY("erp_product_category", "erp_product_category"),
    ERP_PRODUCT_UNIT("erp_product_unit", "erp_product_unit"),
    ERP_PRICE_SYSTEM("erp_price_system", "erp_price_system"),
    ERP_BASE_DATA("erp_base_data", "erp_base_data"),
    ERP_WAREHOUSE("erp_warehouse", "erp_warehouse"),
    PURCHASE_ORDER("purchase_order", "purchase_order"),
    PURCHASE_IN("purchase_in", "purchase_in"),
    PURCHASE_INVOICE("purchase_invoice", "purchase_invoice"),
    PURCHASE_RETURN("purchase_return", "purchase_return"),
    PURCHASE_PRICE_ADJUST("purchase_price_adjust", "purchase_price_adjust"),
    SUPPLIER("supplier", "supplier"),
    CUSTOMER("customer", "customer"),
    SALE_QUOTE("sale_quote", "sale_quote"),
    SALE_ORDER("sale_order", "sale_order"),
    SALE_CART("sale_cart", "sale_cart"),
    SALE_OUT("sale_out", "sale_out"),
    SALE_RETURN("sale_return", "sale_return"),
    SALE_PRICE_ADJUST("sale_price_adjust", "sale_price_adjust"),
    ERP_SALE_CONFIG("erp_sale_config", "erp_sale_config"),
    ERP_STOCK_IN("erp_stock_in", "erp_stock_in"),
    ERP_STOCK_OUT("erp_stock_out", "erp_stock_out"),
    ERP_STOCK_MOVE("erp_stock_move", "erp_stock_move"),
    ERP_STOCK_CHECK("erp_stock_check", "erp_stock_check"),
    ERP_STOCK("erp_stock", "erp_stock"),
    ERP_ACCOUNT("erp_account", "erp_account"),
    ERP_FINANCE_PAYMENT("erp_finance_payment", "erp_finance_payment"),
    ERP_FINANCE_RECEIPT("erp_finance_receipt", "erp_finance_receipt"),
    ERP_FINANCE_TRANSFER("erp_finance_transfer", "erp_finance_transfer"),
    ERP_FINANCE_PAYABLE_EXPENSE("erp_finance_payable_expense", "erp_finance_payable_expense"),
    ERP_FINANCE_PAYABLE_OTHER("erp_finance_payable_other", "erp_finance_payable_other"),
    ERP_FINANCE_RECEIVABLE_OTHER_INCOME("erp_finance_receivable_other_income", "erp_finance_receivable_other_income"),
    ERP_FINANCE_RECEIVABLE_OTHER("erp_finance_receivable_other", "erp_finance_receivable_other"),
    ERP_ACCOUNTING_SUBJECT("erp_accounting_subject", "erp_accounting_subject"),
    ERP_ACCOUNTING_VOUCHER_WORD("erp_accounting_voucher_word", "erp_accounting_voucher_word"),
    ERP_ACCOUNTING_VOUCHER("erp_accounting_voucher", "erp_accounting_voucher"),
    ERP_ACCOUNTING_VOUCHER_ATTRIBUTION("erp_accounting_voucher_attribution", "erp_accounting_voucher_attribution"),
    ERP_ACCOUNTING_BOOK_OPEN("erp_accounting_book_open", "erp_accounting_book_open"),
    ERP_ACCOUNTING_PRE_RECEIPT("erp_accounting_pre_receipt", "erp_accounting_pre_receipt"),
    ERP_ACCOUNTING_PRE_PAYMENT("erp_accounting_pre_payment", "erp_accounting_pre_payment"),
    ERP_ACCOUNTING_PRE_RECEIVABLE("erp_accounting_pre_receivable", "erp_accounting_pre_receivable"),
    ERP_ACCOUNTING_OTHER_RECEIVABLE("erp_accounting_other_receivable", "erp_accounting_other_receivable"),
    ERP_ACCOUNTING_OTHER_PAYABLE("erp_accounting_other_payable", "erp_accounting_other_payable"),
    ERP_ACCOUNTING_REPORT_TEMPLATE("erp_accounting_report_template", "erp_accounting_report_template");

    private final String key;
    private final String name;

    public static boolean isValid(String key) {
        return Arrays.stream(values()).anyMatch(e -> e.getKey().equals(key));
    }

    public static ErpFieldConfigModuleEnum fromKey(String key) {
        return Arrays.stream(values()).filter(e -> e.getKey().equals(key)).findFirst().orElse(null);
    }

}
