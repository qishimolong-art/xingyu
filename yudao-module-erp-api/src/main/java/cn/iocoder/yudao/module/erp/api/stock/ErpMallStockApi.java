package cn.iocoder.yudao.module.erp.api.stock;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * ERP mall stock API.
 */
public interface ErpMallStockApi {

    Map<Long, BigDecimal> getMallSpuAvailableStockMap(Collection<Long> spuIds);

    Map<Long, BigDecimal> getMallSkuAvailableStockMap(Long spuId, Collection<Long> skuIds);

}
