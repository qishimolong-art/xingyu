package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum ErpSalePickStatusEnum implements ArrayValuable<Integer> {

    WAITING(10, "待拣货"),
    PARTIAL(20, "部分拣货"),
    DONE(30, "已拣货");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpSalePickStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
