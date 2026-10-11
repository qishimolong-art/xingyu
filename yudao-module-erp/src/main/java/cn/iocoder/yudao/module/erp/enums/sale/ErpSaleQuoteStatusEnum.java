package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 报价订单状态枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpSaleQuoteStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PROCESS(10, "待下订"),
    APPROVE(20, "已下订"),
    PART_CONVERTED_CART(30, "部分转手推车"),
    CONVERTED_CART(40, "全部转手推车"),
    GENERATED_SALE_OUT(50, "已出库"),
    CANCEL(90, "已作废"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpSaleQuoteStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
