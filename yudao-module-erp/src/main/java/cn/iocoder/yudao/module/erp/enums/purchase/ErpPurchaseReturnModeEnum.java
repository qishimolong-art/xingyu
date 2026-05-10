package cn.iocoder.yudao.module.erp.enums.purchase;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 采购退货 - 退货模式枚举
 *
 * @author Claude
 */
@RequiredArgsConstructor
@Getter
public enum ErpPurchaseReturnModeEnum implements ArrayValuable<Integer> {

    BY_ORDER(10, "按入库单退货"),
    BY_STOCK(20, "按库存退货"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpPurchaseReturnModeEnum::getMode).toArray(Integer[]::new);

    /**
     * 模式
     */
    private final Integer mode;
    /**
     * 名字
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    /**
     * 判断给定值是否为"按入库单退货"
     */
    public static boolean isByOrder(Integer mode) {
        return BY_ORDER.getMode().equals(mode);
    }

    /**
     * 判断给定值是否为"按库存退货"
     */
    public static boolean isByStock(Integer mode) {
        return BY_STOCK.getMode().equals(mode);
    }
}
