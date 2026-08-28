package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.module.erp.service.stock.ErpMallStockService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * ERP mall stock API implementation.
 */
@Service
@Validated
public class ErpMallStockApiImpl implements ErpMallStockApi {

    @Resource
    private ErpMallStockService mallStockService;

    @Override
    public Map<Long, BigDecimal> getMallSpuAvailableStockMap(Collection<Long> spuIds) {
        return mallStockService.getMallSpuAvailableStockMap(spuIds);
    }

    @Override
    public Map<Long, BigDecimal> getMallSkuAvailableStockMap(Long spuId, Collection<Long> skuIds) {
        return mallStockService.getMallSkuAvailableStockMap(spuId, skuIds);
    }

}
