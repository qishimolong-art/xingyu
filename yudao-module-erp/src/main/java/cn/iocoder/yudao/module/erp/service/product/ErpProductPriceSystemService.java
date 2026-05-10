package cn.iocoder.yudao.module.erp.service.product;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * ERP 产品-价格体系关联 Service 接口
 *
 * @author Claude
 */
public interface ErpProductPriceSystemService {

    /**
     * 保存产品的价格体系价格列表（全量覆盖）
     * 先删除该产品所有关联，再批量插入非空价格
     *
     * @param productId 产品编号
     * @param priceSystemPrices key=priceSystemId, value=price（null 值表示清除，不插入）
     */
    void saveProductPrices(Long productId, Map<Long, BigDecimal> priceSystemPrices);

    /**
     * 查询某产品的所有价格体系价格
     *
     * @param productId 产品编号
     * @return key=priceSystemId, value=price
     */
    Map<Long, BigDecimal> getProductPriceMap(Long productId);

    /**
     * 批量查询多个产品在指定价格体系下的价格
     *
     * @param productIds 产品编号集合
     * @param priceSystemId 价格体系编号
     * @return key=productId, value=price
     */
    Map<Long, BigDecimal> getProductPriceMap(Collection<Long> productIds, Long priceSystemId);

    /**
     * 删除指定产品的所有价格关联（在产品被删除时调用）
     *
     * @param productId 产品编号
     */
    void deleteByProductId(Long productId);

}
