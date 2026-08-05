package cn.iocoder.yudao.module.erp.enums.stock;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 调拨出库单状态。
 */
@RequiredArgsConstructor
@Getter
public enum ErpStockTransferOutStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PROCESS(10, "待审批"),
    APPROVE(20, "已审批"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpStockTransferOutStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
