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
    SALE_PRICE_ADJUST("sale_price_adjust", "sale_price_adjust");

    private final String key;
    private final String name;

    public static boolean isValid(String key) {
        return Arrays.stream(values()).anyMatch(e -> e.getKey().equals(key));
    }

    public static ErpFieldConfigModuleEnum fromKey(String key) {
        return Arrays.stream(values()).filter(e -> e.getKey().equals(key)).findFirst().orElse(null);
    }

}
