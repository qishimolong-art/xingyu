package cn.iocoder.yudao.module.erp.enums.finance.accounting;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 凭证来源类型枚举
 *
 * 1=手动新增 2=系统自动 3=跨期归属
 */
@RequiredArgsConstructor
@Getter
public enum ErpVoucherSourceTypeEnum implements ArrayValuable<Integer> {

    MANUAL(1, "手动新增"),
    AUTO(2, "系统自动"),
    ATTRIBUTION(3, "跨期归属"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpVoucherSourceTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
