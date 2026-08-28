package cn.iocoder.yudao.module.erp.service.mall;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;

/**
 * ERP 配件资料同步商城事件监听器。
 */
@Component
public class ErpMallProductSyncEventListener {

    @Resource
    private ErpMallProductSyncService mallProductSyncService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMallProductSyncEvent(ErpMallProductSyncEvent event) {
        if (event.getType() == ErpMallProductSyncEvent.TYPE_CATEGORY) {
            mallProductSyncService.syncCategory(event.getBizId());
        } else if (event.getType() == ErpMallProductSyncEvent.TYPE_PRODUCT) {
            mallProductSyncService.syncProduct(event.getBizId());
        } else if (event.getType() == ErpMallProductSyncEvent.TYPE_PRODUCT_STOCK) {
            mallProductSyncService.syncProductStock(event.getBizId());
        }
    }

}
