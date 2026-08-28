package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockSummaryBO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MALL_STOCK_NOT_AVAILABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.MALL_STOCK_NOT_MATCH;

@Service
@Validated
public class ErpMallStockServiceImpl implements ErpMallStockService {

    private static final String AVAILABLE_STATUS_TEXT = "现货";
    private static final String ORDER_STATUS_TEXT = "订货";

    @Resource
    private ErpMallProductMappingMapper mallProductMappingMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpWarehouseMapper warehouseMapper;

    @Override
    public BigDecimal getMallStockSummary(Long spuId) {
        return getMallStockSummaryDetail(spuId).getTotalAvailableCount();
    }

    @Override
    public Map<Long, BigDecimal> getMallSpuAvailableStockMap(Collection<Long> spuIds) {
        if (CollUtil.isEmpty(spuIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = initZeroMap(spuIds);
        List<ErpMallProductMappingDO> mappings = mallProductMappingMapper.selectListByMallSpuIds(spuIds);
        if (CollUtil.isEmpty(mappings)) {
            return result;
        }
        Map<Long, Set<Long>> spuProductIdsMap = new HashMap<>();
        for (ErpMallProductMappingDO mapping : mappings) {
            if (mapping.getMallSpuId() == null || mapping.getErpProductId() == null) {
                continue;
            }
            spuProductIdsMap.computeIfAbsent(mapping.getMallSpuId(), key -> new HashSet<>())
                    .add(mapping.getErpProductId());
        }
        Map<Long, BigDecimal> productStockMap = getProductAvailableStockMap(
                spuProductIdsMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
        for (Map.Entry<Long, Set<Long>> entry : spuProductIdsMap.entrySet()) {
            result.put(entry.getKey(), sumProductAvailableStock(entry.getValue(), productStockMap));
        }
        return result;
    }

    @Override
    public ErpMallStockSummaryBO getMallStockSummaryDetail(Long spuId) {
        List<ErpMallProductMappingDO> mappings = mallProductMappingMapper.selectListByMallSpuId(spuId);
        ErpMallStockSummaryBO summary = new ErpMallStockSummaryBO()
                .setSpuId(spuId)
                .setMapped(CollUtil.isNotEmpty(mappings))
                .setMappingCount(mappings.size())
                .setTotalAvailableCount(BigDecimal.ZERO);
        if (CollUtil.isEmpty(mappings)) {
            return summary;
        }
        Set<Long> erpProductIds = convertSet(mappings, ErpMallProductMappingDO::getErpProductId);
        BigDecimal totalAvailableCount = stockMapper.selectListByProductIds(erpProductIds).stream()
                .map(this::getDisplayAvailableCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return summary.setTotalAvailableCount(totalAvailableCount);
    }

    @Override
    public Map<Long, BigDecimal> getMallSkuAvailableStockMap(Long spuId, Collection<Long> skuIds) {
        if (spuId == null || CollUtil.isEmpty(skuIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = initZeroMap(skuIds);
        Set<Long> skuIdSet = new HashSet<>(skuIds);
        List<ErpMallProductMappingDO> mappings = mallProductMappingMapper.selectListByMallSpuId(spuId).stream()
                .filter(mapping -> skuIdSet.contains(mapping.getMallSkuId()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(mappings)) {
            return result;
        }
        Map<Long, Set<Long>> skuProductIdsMap = new HashMap<>();
        for (ErpMallProductMappingDO mapping : mappings) {
            if (mapping.getMallSkuId() == null || mapping.getErpProductId() == null) {
                continue;
            }
            skuProductIdsMap.computeIfAbsent(mapping.getMallSkuId(), key -> new HashSet<>())
                    .add(mapping.getErpProductId());
        }
        Map<Long, BigDecimal> productStockMap = getProductAvailableStockMap(
                skuProductIdsMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
        for (Map.Entry<Long, Set<Long>> entry : skuProductIdsMap.entrySet()) {
            result.put(entry.getKey(), sumProductAvailableStock(entry.getValue(), productStockMap));
        }
        return result;
    }

    @Override
    public List<ErpMallStockOptionBO> getMallStockOptions(Long spuId, Long skuId) {
        List<ErpMallProductMappingDO> mappings = getMatchedMappings(spuId, skuId);
        if (CollUtil.isEmpty(mappings)) {
            return Collections.emptyList();
        }
        Set<Long> erpProductIds = convertSet(mappings, ErpMallProductMappingDO::getErpProductId);
        List<ErpStockDO> stocks = stockMapper.selectListByProductIds(erpProductIds);
        return buildOptions(stocks);
    }

    @Override
    public Map<Long, ErpMallStockOptionBO> getMallStockOptionMap(Collection<Long> stockIds) {
        if (CollUtil.isEmpty(stockIds)) {
            return Collections.emptyMap();
        }
        List<ErpStockDO> stocks = stockMapper.selectBatchIds(stockIds);
        return convertMap(buildOptions(stocks), ErpMallStockOptionBO::getStockId);
    }

    @Override
    public ErpMallStockOptionBO validateMallStock(Long spuId, Long skuId, Long stockId, Integer count) {
        if (stockId == null) {
            throw exception(MALL_STOCK_NOT_MATCH);
        }
        ErpStockDO stock = stockMapper.selectById(stockId);
        if (stock == null || !hasMatchedMapping(spuId, skuId, stock.getProductId())) {
            throw exception(MALL_STOCK_NOT_MATCH);
        }
        BigDecimal requireCount = BigDecimal.valueOf(count == null ? 0 : count);
        if (getAvailableCount(stock).compareTo(requireCount) < 0) {
            throw exception(MALL_STOCK_NOT_AVAILABLE);
        }
        return buildOptions(Collections.singletonList(stock)).get(0);
    }

    private List<ErpMallProductMappingDO> getMatchedMappings(Long spuId, Long skuId) {
        return mallProductMappingMapper.selectListByMallSpuId(spuId).stream()
                .filter(mapping -> Objects.equals(mapping.getMallSkuId(), skuId))
                .collect(Collectors.toList());
    }

    private boolean hasMatchedMapping(Long spuId, Long skuId, Long erpProductId) {
        return getMatchedMappings(spuId, skuId).stream()
                .anyMatch(mapping -> Objects.equals(mapping.getErpProductId(), erpProductId));
    }

    private Map<Long, BigDecimal> initZeroMap(Collection<Long> ids) {
        Map<Long, BigDecimal> result = new HashMap<>();
        ids.stream().filter(Objects::nonNull).forEach(id -> result.put(id, BigDecimal.ZERO));
        return result;
    }

    private Map<Long, BigDecimal> getProductAvailableStockMap(Collection<Long> erpProductIds) {
        if (CollUtil.isEmpty(erpProductIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (ErpStockDO stock : stockMapper.selectListByProductIds(erpProductIds)) {
            if (stock.getProductId() == null) {
                continue;
            }
            result.merge(stock.getProductId(), getDisplayAvailableCount(stock), BigDecimal::add);
        }
        return result;
    }

    private BigDecimal sumProductAvailableStock(Collection<Long> erpProductIds, Map<Long, BigDecimal> productStockMap) {
        if (CollUtil.isEmpty(erpProductIds)) {
            return BigDecimal.ZERO;
        }
        return erpProductIds.stream()
                .map(productId -> productStockMap.getOrDefault(productId, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ErpMallStockOptionBO> buildOptions(List<ErpStockDO> stocks) {
        if (CollUtil.isEmpty(stocks)) {
            return Collections.emptyList();
        }
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(
                warehouseMapper.selectBatchIds(convertSet(stocks, ErpStockDO::getWarehouseId)),
                ErpWarehouseDO::getId);
        Map<Long, ErpMallStockOptionBO> options = new LinkedHashMap<>();
        for (ErpStockDO stock : stocks) {
            ErpWarehouseDO warehouse = warehouseMap.get(stock.getWarehouseId());
            BigDecimal availableCount = getAvailableCount(stock);
            boolean available = availableCount.compareTo(BigDecimal.ZERO) > 0;
            ErpMallStockOptionBO option = new ErpMallStockOptionBO()
                    .setStockId(stock.getId())
                    .setErpProductId(stock.getProductId())
                    .setWarehouseId(stock.getWarehouseId())
                    .setWarehouseName(warehouse == null ? "" : warehouse.getName())
                    .setAvailable(available)
                    .setAvailableCount(availableCount)
                    .setAvailableStatusText(available ? AVAILABLE_STATUS_TEXT : ORDER_STATUS_TEXT);
            options.put(option.getStockId(), option);
        }
        return CollectionUtils.convertList(options.values(), option -> option);
    }

    private BigDecimal getAvailableCount(ErpStockDO stock) {
        BigDecimal count = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
        BigDecimal lockCount = stock.getLockCount() == null ? BigDecimal.ZERO : stock.getLockCount();
        return count.subtract(lockCount);
    }

    private BigDecimal getDisplayAvailableCount(ErpStockDO stock) {
        BigDecimal availableCount = getAvailableCount(stock);
        return availableCount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availableCount;
    }

}
