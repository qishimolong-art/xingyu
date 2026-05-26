package cn.iocoder.yudao.module.erp.enums.finance.accounting;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 会计科目大类枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpSubjectCategoryEnum implements ArrayValuable<Integer> {

    ASSET(1, "资产"),
    LIABILITY(2, "负债"),
    COMMON(3, "共同"),
    EQUITY(4, "权益"),
    COST(5, "成本"),
    PROFIT_LOSS(6, "损益"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpSubjectCategoryEnum::getCategory).toArray(Integer[]::new);

    private final Integer category;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
