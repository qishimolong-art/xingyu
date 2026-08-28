package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockSummaryBO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ErpMallStockService {

    BigDecimal getMallStockSummary(Long spuId);

    Map<Long, BigDecimal> getMallSpuAvailableStockMap(Collection<Long> spuIds);

    ErpMallStockSummaryBO getMallStockSummaryDetail(Long spuId);

    Map<Long, BigDecimal> getMallSkuAvailableStockMap(Long spuId, Collection<Long> skuIds);

    List<ErpMallStockOptionBO> getMallStockOptions(Long spuId, Long skuId);

    Map<Long, ErpMallStockOptionBO> getMallStockOptionMap(Collection<Long> stockIds);

    ErpMallStockOptionBO validateMallStock(Long spuId, Long skuId, Long stockId, Integer count);

}
