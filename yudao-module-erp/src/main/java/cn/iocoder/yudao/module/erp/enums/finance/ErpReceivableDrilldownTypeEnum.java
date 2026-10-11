package cn.iocoder.yudao.module.erp.enums.finance;

import java.util.Arrays;

public enum ErpReceivableDrilldownTypeEnum {
    BALANCE("余额"),
    OPENING_BALANCE("此前应收"),
    MISC_RECEIVABLE("其他应收款"),
    RECEIPT("明细收款"),
    SALE_OUT("销售增加"),
    SALE_RETURN("退货增加");

    private final String label;

    ErpReceivableDrilldownTypeEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ErpReceivableDrilldownTypeEnum fromValue(String value) {
        return Arrays.stream(values()).filter(item -> item.name().equals(value)).findFirst().orElse(null);
    }
}
