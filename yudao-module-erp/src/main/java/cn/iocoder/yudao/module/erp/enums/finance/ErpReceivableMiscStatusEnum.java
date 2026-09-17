package cn.iocoder.yudao.module.erp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 其他应收状态。
 */
@Getter
@AllArgsConstructor
public enum ErpReceivableMiscStatusEnum {

    DRAFT(0, "草稿"),
    PROCESS(10, "待审核"),
    APPROVE(20, "已审核");

    private final Integer status;
    private final String name;

}
