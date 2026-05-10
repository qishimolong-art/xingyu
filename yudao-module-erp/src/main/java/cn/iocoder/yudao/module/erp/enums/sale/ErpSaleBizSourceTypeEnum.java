package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 销售业务来源类型枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpSaleBizSourceTypeEnum implements ArrayValuable<Integer> {

    LEGACY_ORDER(10, "旧销售订单"),
    QUOTE(20, "报价订单"),
    CART(30, "销售手推车"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpSaleBizSourceTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
