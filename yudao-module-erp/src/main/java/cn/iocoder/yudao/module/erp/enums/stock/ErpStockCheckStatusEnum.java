package cn.iocoder.yudao.module.erp.enums.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 库存盘点状态。
 */
@Getter
@AllArgsConstructor
public enum ErpStockCheckStatusEnum {

    DRAFT(0, "草稿"),
    PROCESS(10, "待审核"),
    APPROVE(20, "已审核");

    private final Integer status;
    private final String name;

}
