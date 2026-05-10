package cn.iocoder.yudao.module.erp.enums.sale;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpSaleReturnModeEnum {

    LEGACY_ORDER(0, "旧销售订单退货"),
    BY_SALE_OUT(10, "按销售单退货"),
    BY_STOCK(20, "按库存退货");

    private final Integer mode;
    private final String name;

    public static boolean isBySaleOut(Integer mode) {
        return BY_SALE_OUT.getMode().equals(mode);
    }

    public static boolean isByStock(Integer mode) {
        return BY_STOCK.getMode().equals(mode);
    }

}
