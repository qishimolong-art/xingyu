package cn.iocoder.yudao.module.erp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpPayableExpenseStatusEnum {

    DRAFT(0),
    PROCESS(10),
    APPROVE(20);

    private final Integer status;

}
