package cn.iocoder.yudao.module.erp.enums.stock;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP stock transfer direction.
 */
@RequiredArgsConstructor
@Getter
public enum ErpStockTransferDirectionEnum implements ArrayValuable<Integer> {

    TRANSFER_OUT(10, "调拨出库"),
    TRANSFER_IN(20, "调拨入库"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpStockTransferDirectionEnum::getDirection)
            .toArray(Integer[]::new);

    private final Integer direction;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
