package cn.iocoder.yudao.module.erp.enums.finance.accounting;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 凭证业务类型枚举（11 种）
 *
 * 用于「系统开账自动生成凭证配置」与「凭证来源业务类型」。
 */
@RequiredArgsConstructor
@Getter
public enum ErpVoucherTypeEnum implements ArrayValuable<Integer> {

    SALE(1, "销售凭证"),
    OTHER_RECEIVABLE(2, "其他应收凭证"),
    PRE_RECEIPT(3, "预收款凭证"),
    PRE_PAYMENT(4, "预付款凭证"),
    PRE_RECEIVABLE(5, "预收账款凭证"),
    STOCK_MOVE_OUT(6, "调拨出库凭证"),
    PURCHASE(7, "采购凭证"),
    OTHER_PAYABLE(8, "其他应付凭证"),
    OTHER_OUT(9, "其他出库凭证"),
    OTHER_IN(10, "其他入库凭证"),
    BANK_TRANSFER(11, "银行转账凭证"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpVoucherTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
