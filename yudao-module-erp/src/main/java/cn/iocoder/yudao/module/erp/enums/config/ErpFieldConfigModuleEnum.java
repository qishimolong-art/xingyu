package cn.iocoder.yudao.module.erp.enums.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 字段配置模块枚举
 */
@Getter
@AllArgsConstructor
public enum ErpFieldConfigModuleEnum {

    PURCHASE_ORDER("purchase_order", "采购订单"),
    PURCHASE_IN("purchase_in", "采购入库"),
    PURCHASE_INVOICE("purchase_invoice", "采购票据"),
    PURCHASE_RETURN("purchase_return", "采购退货"),
    PURCHASE_PRICE_ADJUST("purchase_price_adjust", "采购调价"),
    SUPPLIER("supplier", "供应商"),
    SALE_QUOTE("sale_quote", "销售报价"),
    SALE_CART("sale_cart", "销售购物车"),
    SALE_RETURN("sale_return", "销售退货");

    private final String key;
    private final String name;

    public static boolean isValid(String key) {
        return Arrays.stream(values()).anyMatch(e -> e.getKey().equals(key));
    }

    public static ErpFieldConfigModuleEnum fromKey(String key) {
        return Arrays.stream(values()).filter(e -> e.getKey().equals(key)).findFirst().orElse(null);
    }

}
