package cn.iocoder.yudao.module.erp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收付款明细的核销生命周期状态。
 */
@Getter
@AllArgsConstructor
public enum ErpFinanceWriteOffStatusEnum {

    PENDING(0, "待生效"),
    EFFECTIVE(1, "已生效"),
    REVERSED(2, "已撤销");

    private final Integer status;
    private final String name;

}
