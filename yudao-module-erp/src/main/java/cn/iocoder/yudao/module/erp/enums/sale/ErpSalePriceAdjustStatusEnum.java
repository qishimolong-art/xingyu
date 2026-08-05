package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 销售调价单状态枚举。
 */
@RequiredArgsConstructor
@Getter
public enum ErpSalePriceAdjustStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PROCESS(10, "未审核"),
    APPROVE(20, "已审核"),
    REJECT(30, "已驳回");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpSalePriceAdjustStatusEnum::getStatus)
            .toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
