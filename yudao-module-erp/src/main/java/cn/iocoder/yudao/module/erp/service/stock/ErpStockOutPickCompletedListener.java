package cn.iocoder.yudao.module.erp.service.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErpStockOutPickCompletedListener {

    private final ErpStockOutService stockOutService;

    @EventListener
    public void onPickCompleted(ErpStockOutPickCompletedEvent event) {
        stockOutService.autoApproveAfterPick(event.getStockOutId(), event.getPickUserId());
    }

}
