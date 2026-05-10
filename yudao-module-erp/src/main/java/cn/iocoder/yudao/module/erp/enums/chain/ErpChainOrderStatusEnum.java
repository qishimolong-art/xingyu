package cn.iocoder.yudao.module.erp.enums.chain;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 连锁开单状态枚举
 *
 * @author 汽配ERP
 */
@RequiredArgsConstructor
@Getter
public enum ErpChainOrderStatusEnum implements ArrayValuable<Integer> {

    PENDING(10, "待审核"),
    APPROVED(20, "已审核"),
    COMPLETED(30, "已完成"),
    CANCELLED(90, "已取消"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpChainOrderStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
