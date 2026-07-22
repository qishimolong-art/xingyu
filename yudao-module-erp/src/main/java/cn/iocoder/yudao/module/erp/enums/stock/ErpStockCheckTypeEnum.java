package cn.iocoder.yudao.module.erp.enums.stock;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 库存盘点 - 盘点类型枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpStockCheckTypeEnum implements ArrayValuable<Integer> {

    COUNT(1, "盘数量"),
    COST(2, "盘成本"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpStockCheckTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static Integer defaultIfNull(Integer type) {
        return type == null ? COUNT.getType() : type;
    }

    public static boolean isCount(Integer type) {
        return COUNT.getType().equals(defaultIfNull(type));
    }

    public static boolean isCost(Integer type) {
        return COST.getType().equals(type);
    }

}
