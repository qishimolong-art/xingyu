package cn.iocoder.yudao.module.erp.enums.purchase;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 采购调价 - 调价类型枚举
 *
 * @author 汽配ERP
 */
@RequiredArgsConstructor
@Getter
public enum ErpPurchasePriceAdjustTypeEnum implements ArrayValuable<Integer> {

    BY_IN_ORDER(10, "按入库单调价"),
    BY_ITEM(20, "添加明细"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpPurchasePriceAdjustTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 名字
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    /**
     * 判断给定值是否为"按入库单调价"
     */
    public static boolean isByInOrder(Integer type) {
        return BY_IN_ORDER.getType().equals(type);
    }

    /**
     * 判断给定值是否为"添加明细"
     */
    public static boolean isByItem(Integer type) {
        return BY_ITEM.getType().equals(type);
    }
}
