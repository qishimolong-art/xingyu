package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum ErpSaleDeliveryStatusEnum implements ArrayValuable<Integer> {

    NOT_READY(5, "待拣货完成"),
    WAITING(10, "待送货"),
    PARTIAL(20, "部分送货"),
    DONE(30, "已送货");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpSaleDeliveryStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
