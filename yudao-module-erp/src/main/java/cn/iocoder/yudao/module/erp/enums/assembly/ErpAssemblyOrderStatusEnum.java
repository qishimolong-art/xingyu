package cn.iocoder.yudao.module.erp.enums.assembly;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 套件组装单状态。 */
@Getter
@AllArgsConstructor
public enum ErpAssemblyOrderStatusEnum {

    DRAFT(10, "草稿"),
    CONFIRMED(15, "已确认"),
    ASSEMBLING(18, "装配中"),
    COMPLETED(20, "已完成"),
    LEGACY_VOIDED(30, "历史已作废");

    private final Integer status;
    private final String name;

}
