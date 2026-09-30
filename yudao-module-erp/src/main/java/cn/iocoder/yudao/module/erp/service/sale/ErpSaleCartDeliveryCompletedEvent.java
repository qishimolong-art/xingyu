package cn.iocoder.yudao.module.erp.service.sale;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 非跨部门销售手推车完成送货事件。 */
@Getter
@AllArgsConstructor
public class ErpSaleCartDeliveryCompletedEvent {

    private final Long saleCartId;
    private final Long deliveryUserId;

}
