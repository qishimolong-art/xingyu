package cn.iocoder.yudao.module.erp.enums.finance.accounting;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 凭证审核状态枚举
 *
 * 10=未审核 20=已审核 30=已反审
 */
@RequiredArgsConstructor
@Getter
public enum ErpVoucherAuditStatusEnum implements ArrayValuable<Integer> {

    PROCESS(10, "未审核"),
    APPROVE(20, "已审核"),
    REVOKED(30, "已反审"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpVoucherAuditStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
