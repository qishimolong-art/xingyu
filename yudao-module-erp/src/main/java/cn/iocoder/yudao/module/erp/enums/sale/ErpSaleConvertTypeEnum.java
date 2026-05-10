package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 销售单据转换类型枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpSaleConvertTypeEnum implements ArrayValuable<Integer> {

    QUOTE_TO_CART(10, "报价订单转销售手推车"),
    CART_TO_QUOTE(20, "销售手推车转报价订单"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpSaleConvertTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
