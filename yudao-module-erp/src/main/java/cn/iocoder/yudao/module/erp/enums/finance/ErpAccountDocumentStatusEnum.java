package cn.iocoder.yudao.module.erp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 结算账户提交状态枚举
 */
@Getter
@AllArgsConstructor
public enum ErpAccountDocumentStatusEnum {

    DRAFT(0, "草稿"),
    SUBMITTED(10, "正式");

    private final Integer status;
    private final String name;

}
