package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 销售手推车状态枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpSaleCartStatusEnum implements ArrayValuable<Integer> {

    PROCESS(10, "草稿"),
    SUBMITTED(20, "待初审"),
    FIRST_APPROVE(30, "初审通过"),
    FINAL_APPROVE(40, "终审通过"),
    GENERATED_SALE_OUT(50, "已生成销售单"),
    CONVERTED_QUOTE(60, "已转报价"),
    CANCEL(90, "已取消"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpSaleCartStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
