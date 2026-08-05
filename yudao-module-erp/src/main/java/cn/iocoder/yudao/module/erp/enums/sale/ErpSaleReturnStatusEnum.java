package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 销售退货状态枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpSaleReturnStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PROCESS(10, "待审批"),
    APPROVE(20, "已审批"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpSaleReturnStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
