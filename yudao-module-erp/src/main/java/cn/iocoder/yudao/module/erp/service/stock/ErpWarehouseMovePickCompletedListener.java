package cn.iocoder.yudao.module.erp.service.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErpWarehouseMovePickCompletedListener {

    private final ErpWarehouseMoveService warehouseMoveService;

    @EventListener
    public void onPickCompleted(ErpWarehouseMovePickCompletedEvent event) {
        warehouseMoveService.autoApproveAfterPick(event.getWarehouseMoveId(), event.getPickUserId());
    }

}
