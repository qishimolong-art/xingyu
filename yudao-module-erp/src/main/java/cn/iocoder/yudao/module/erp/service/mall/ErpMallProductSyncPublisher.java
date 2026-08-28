package cn.iocoder.yudao.module.erp.service.mall;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ERP 配件资料同步商城事件发布器。
 */
@Component
public class ErpMallProductSyncPublisher {

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishCategorySync(Long erpCategoryId) {
        publish(ErpMallProductSyncEvent.TYPE_CATEGORY, erpCategoryId);
    }

    public void publishProductSync(Long erpProductId) {
        publish(ErpMallProductSyncEvent.TYPE_PRODUCT, erpProductId);
    }

    public void publishProductStockSync(Long erpProductId) {
        publish(ErpMallProductSyncEvent.TYPE_PRODUCT_STOCK, erpProductId);
    }

    private void publish(int type, Long bizId) {
        if (bizId == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new ErpMallProductSyncEvent(type, bizId));
    }

}
