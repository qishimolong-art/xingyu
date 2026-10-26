package cn.iocoder.yudao.module.erp.service.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErpStockInPickCompletedListener {

    private final ErpStockInService stockInService;

    @EventListener
    public void onPickCompleted(ErpStockInPickCompletedEvent event) {
        stockInService.autoApproveAfterPick(event.getStockInId(), event.getPickUserId());
    }

}
