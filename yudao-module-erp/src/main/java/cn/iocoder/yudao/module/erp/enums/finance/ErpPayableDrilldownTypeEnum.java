package cn.iocoder.yudao.module.erp.enums.finance;

import java.util.Arrays;

public enum ErpPayableDrilldownTypeEnum {
    BALANCE("应付余额"),
    OPENING_BALANCE("此前应付"),
    MISC_PAYABLE("其他应付款"),
    PURCHASE_IN("采购入库"),
    PURCHASE_RETURN("采购退货"),
    PRICE_ADJUST("采购调价"),
    PAYABLE_ADJUST("应付调账"),
    PAYMENT("付款"),
    WRITE_OFF("核销");

    private final String label;

    ErpPayableDrilldownTypeEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ErpPayableDrilldownTypeEnum fromValue(String value) {
        return Arrays.stream(values()).filter(item -> item.name().equals(value)).findFirst().orElse(null);
    }
}
