package cn.iocoder.yudao.module.erp.service.sale;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 销售退货整单拣货完成事件；监听器与最后一次拣货提交共用事务。 */
@Getter
@AllArgsConstructor
public class ErpSaleReturnPickCompletedEvent {

    private final Long saleReturnId;
    private final Long pickUserId;

}
