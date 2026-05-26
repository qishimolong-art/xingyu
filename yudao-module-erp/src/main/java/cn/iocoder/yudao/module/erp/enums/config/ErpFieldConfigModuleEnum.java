package cn.iocoder.yudao.module.erp.enums.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 字段配置 - 模块标识枚举
 *
 * @author Claude
 */
@Getter
@AllArgsConstructor
public enum ErpFieldConfigModuleEnum {

    PURCHASE_ORDER("purchase_order", "采购订单"),
    PURCHASE_IN("purchase_in", "采购入库"),
    PURCHASE_RETURN("purchase_return", "采购退货"),
    PURCHASE_PRICE_ADJUST("purchase_price_adjust", "采购调价"),
    SUPPLIER("supplier", "供应商"),
    SALE_QUOTE("sale_quote", "报价订单"),
    SALE_CART("sale_cart", "销售手推车"),
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
