package cn.iocoder.yudao.module.erp.enums.purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 采购票据来源类型。
 */
@Getter
@AllArgsConstructor
public enum ErpPurchaseInvoiceSourceTypeEnum {

    PURCHASE_IN(10, "采购入库"),
    PRICE_ADJUST(20, "采购调价"),
    PURCHASE_RETURN(30, "采购退货");

    private final Integer type;
    private final String name;

    public static ErpPurchaseInvoiceSourceTypeEnum valueOfType(Integer type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst().orElse(null);
    }
}
