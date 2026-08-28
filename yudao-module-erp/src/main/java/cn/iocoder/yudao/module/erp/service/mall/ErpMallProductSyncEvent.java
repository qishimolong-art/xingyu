package cn.iocoder.yudao.module.erp.service.mall;

/**
 * ERP 配件资料同步商城的领域事件。
 */
public class ErpMallProductSyncEvent {

    public static final int TYPE_CATEGORY = 1;
    public static final int TYPE_PRODUCT = 2;
    public static final int TYPE_PRODUCT_STOCK = 3;

    private final int type;
    private final Long bizId;

    public ErpMallProductSyncEvent(int type, Long bizId) {
        this.type = type;
        this.bizId = bizId;
    }

    public int getType() {
        return type;
    }

    public Long getBizId() {
        return bizId;
    }

}
