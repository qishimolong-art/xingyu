package cn.iocoder.yudao.module.erp.service.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 销售手推车来源调拨出库审批完成事件。 */
@Getter
@AllArgsConstructor
public class ErpSaleCartTransferOutApprovedEvent {

    private final Long saleCartId;
    private final Long transferOutId;
    private final Long approveUserId;

}
