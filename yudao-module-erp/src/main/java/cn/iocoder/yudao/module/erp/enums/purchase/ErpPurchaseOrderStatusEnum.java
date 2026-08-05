package cn.iocoder.yudao.module.erp.enums.purchase;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP purchase order status.
 */
@RequiredArgsConstructor
@Getter
public enum ErpPurchaseOrderStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PROCESS(10, "未下订"),
    APPROVE(20, "已下订"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpPurchaseOrderStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
