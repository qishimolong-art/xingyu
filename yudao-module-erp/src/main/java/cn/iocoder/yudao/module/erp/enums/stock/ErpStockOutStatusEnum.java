package cn.iocoder.yudao.module.erp.enums.stock;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ErpStockOutStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PROCESS(10, "待下单确认"),
    PICKING(15, "待拣货"),
    APPROVE(20, "已审批");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpStockOutStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static String getNameByStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status))
                .findFirst().map(ErpStockOutStatusEnum::getName).orElse(null);
    }

}
