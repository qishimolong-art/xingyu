package cn.iocoder.yudao.module.erp.enums.purchase;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 采购退货状态枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpPurchaseReturnStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PROCESS(10, "待审核"),
    APPROVE(20, "已审核"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpPurchaseReturnStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
