package cn.iocoder.yudao.module.erp.api.mall;

/**
 * ERP mall product price API.
 */
public interface ErpMallProductPriceApi {

    /**
     * Updates the ERP retail price behind a mall SPU and syncs the mall display price.
     *
     * @param mallSpuId mall SPU id
     * @param price retail price in cents
     */
    void updateMallSpuRetailPrice(Long mallSpuId, Integer price);

}
