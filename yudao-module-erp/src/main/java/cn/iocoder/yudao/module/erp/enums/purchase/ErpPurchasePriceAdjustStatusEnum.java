package cn.iocoder.yudao.module.erp.enums.purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 采购调价单状态枚举。
 */
@Getter
@AllArgsConstructor
public enum ErpPurchasePriceAdjustStatusEnum {

    DRAFT(0, "草稿"),
    PROCESS(10, "待审批"),
    APPROVE(20, "已审批"),
    REJECT(30, "已拒绝");

    private final Integer status;
    private final String name;

}
