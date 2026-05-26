package cn.iocoder.yudao.module.erp.enums.finance.accounting;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 会计科目凭证类型枚举
 *
 * 1=客户 2=连锁
 */
@RequiredArgsConstructor
@Getter
public enum ErpSubjectVoucherTypeEnum implements ArrayValuable<Integer> {

    CUSTOMER(1, "客户"),
    CHAIN(2, "连锁"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpSubjectVoucherTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
