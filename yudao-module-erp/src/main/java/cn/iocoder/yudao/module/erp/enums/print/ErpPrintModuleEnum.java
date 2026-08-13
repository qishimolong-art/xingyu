package cn.iocoder.yudao.module.erp.enums.print;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpPrintModuleEnum {

    PURCHASE_ORDER("purchase_order", "采购订单");

    private final String key;
    private final String name;

    public static boolean isSupported(String key) {
        for (ErpPrintModuleEnum value : values()) {
            if (value.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

}
