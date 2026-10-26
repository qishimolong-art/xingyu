package cn.iocoder.yudao.module.erp.enums.sale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 销售业务来源类型枚举
 */
@RequiredArgsConstructor
@Getter
public enum ErpSaleBizSourceTypeEnum implements ArrayValuable<Integer> {

    LEGACY_ORDER(10, "旧销售订单"),
    QUOTE(20, "报价订单"),
    CART(30, "销售手推车"),
    PRICE_ADJUST(40, "销售调价"),
    PURCHASE_IN(50, "Purchase In"),
    MALL_ORDER(60, "商城订单"),
    SALE_RETURN(70, "销售退货"),
    WAREHOUSE_MOVE(80, "仓库移货"),
    STOCK_IN(90, "其它入库"),
    STOCK_OUT(100, "其它出库"),
    ;

    /**
     * 拣货任务中，历史由销售单直接生成的记录没有 source_type，查询时使用 0 表示该类记录。
     */
    public static final Integer DIRECT_SALE_OUT_TYPE = 0;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpSaleBizSourceTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static String getPickSourceName(Integer type) {
        if (type == null || DIRECT_SALE_OUT_TYPE.equals(type)) {
            return "销售单";
        }
        return Arrays.stream(values())
                .filter(item -> item.getType().equals(type))
                .findFirst()
                .map(ErpSaleBizSourceTypeEnum::getName)
                .orElse("其他来源");
    }

    public static boolean isPickOnly(Integer type) {
        return SALE_RETURN.getType().equals(type) || WAREHOUSE_MOVE.getType().equals(type)
                || STOCK_IN.getType().equals(type) || STOCK_OUT.getType().equals(type);
    }

}
