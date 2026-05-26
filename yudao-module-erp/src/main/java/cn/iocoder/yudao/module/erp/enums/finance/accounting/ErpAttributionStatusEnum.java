package cn.iocoder.yudao.module.erp.enums.finance.accounting;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 凭证归属状态枚举
 *
 * 10=未归属 20=已归属 30=已生成凭证
 */
@RequiredArgsConstructor
@Getter
public enum ErpAttributionStatusEnum implements ArrayValuable<Integer> {

    UNATTRIBUTED(10, "未归属"),
    ATTRIBUTED(20, "已归属"),
    GENERATED(30, "已生成凭证"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpAttributionStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
