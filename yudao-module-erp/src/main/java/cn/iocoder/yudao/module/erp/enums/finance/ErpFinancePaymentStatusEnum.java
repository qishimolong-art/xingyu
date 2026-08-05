package cn.iocoder.yudao.module.erp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 付款单状态。
 */
@Getter
@AllArgsConstructor
public enum ErpFinancePaymentStatusEnum {

    DRAFT(0, "草稿"),
    PROCESS(10, "待审核"),
    APPROVE(20, "已审核"),
    REJECT(30, "已拒绝");

    private final Integer status;
    private final String name;

}
