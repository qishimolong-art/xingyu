package cn.iocoder.yudao.module.erp.service.sale;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 在最后一次拣货提交事务内自动审批销售退货，失败时整次提交一并回滚。 */
@Component
@RequiredArgsConstructor
public class ErpSaleReturnPickCompletedListener {

    private final ErpSaleReturnService saleReturnService;

    @EventListener
    public void onPickCompleted(ErpSaleReturnPickCompletedEvent event) {
        saleReturnService.autoApproveAfterPick(event.getSaleReturnId(), event.getPickUserId());
    }

}
