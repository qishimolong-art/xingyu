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
    SUPPLIER("supplier", "供应商");

    private final String key;
    private final String name;

    public static boolean isValid(String key) {
        return Arrays.stream(values()).anyMatch(e -> e.getKey().equals(key));
    }

    public static ErpFieldConfigModuleEnum fromKey(String key) {
        return Arrays.stream(values()).filter(e -> e.getKey().equals(key)).findFirst().orElse(null);
    }

}
