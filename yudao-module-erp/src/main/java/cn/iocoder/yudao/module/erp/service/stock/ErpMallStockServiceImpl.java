package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockOptionBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallStockSummaryBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final double EARTH_RADIUS_METERS = 6371000D;

    @Resource
    private ErpMallProductMappingMapper mallProductMappingMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpWarehouseMapper warehouseMapper;
    @Resource
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private DeptApi deptApi;

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
                .setTotalAvailableCount(BigDecimal.ZERO)
                .setTotalCount(BigDecimal.ZERO)
                .setTotalLockCount(BigDecimal.ZERO)
                .setTotalOccupiedCount(BigDecimal.ZERO)
                .setTotalPendingInCount(BigDecimal.ZERO)
                .setTotalInTransitCount(BigDecimal.ZERO)
                .setWarehouseStocks(Collections.emptyList());
        if (CollUtil.isEmpty(mappings)) {
            return summary;
        }
        Set<Long> erpProductIds = convertSet(mappings, ErpMallProductMappingDO::getErpProductId);
        List<ErpStockDO> stocks = stockMapper.selectListByProductIds(erpProductIds);
        return fillMallStockSummary(summary, stocks);
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
    public PageResult<ErpMallStockOptionBO> getMallStockOptionPage(Long spuId, Long skuId, Integer pageNo,
                                                                   Integer pageSize, BigDecimal userLongitude,
                                                                   BigDecimal userLatitude) {
        List<ErpMallProductMappingDO> mappings = getMatchedMappings(spuId, skuId);
        if (CollUtil.isEmpty(mappings)) {
            return PageResult.empty();
        }
        Set<Long> erpProductIds = convertSet(mappings, ErpMallProductMappingDO::getErpProductId);
        List<ErpStockDO> stocks = stockMapper.selectListByProductIds(erpProductIds);
        List<ErpMallStockOptionBO> options = buildOptions(stocks, userLongitude, userLatitude);
        if (CollUtil.isEmpty(options)) {
            return PageResult.empty();
        }
        options.sort(buildMallStockOptionComparator());
        int safePageNo = pageNo == null || pageNo < 1 ? DEFAULT_PAGE_NO : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int fromIndex = (safePageNo - 1) * safePageSize;
        if (fromIndex >= options.size()) {
            return PageResult.empty((long) options.size());
        }
        int toIndex = Math.min(fromIndex + safePageSize, options.size());
        return new PageResult<>(new ArrayList<>(options.subList(fromIndex, toIndex)), (long) options.size());
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
        return buildOptions(stocks, null, null);
    }

    private List<ErpMallStockOptionBO> buildOptions(List<ErpStockDO> stocks, BigDecimal userLongitude,
                                                    BigDecimal userLatitude) {
        if (CollUtil.isEmpty(stocks)) {
            return Collections.emptyList();
        }
        boolean canCalculateDistance = isValidCoordinate(userLongitude, userLatitude);
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
            if (canCalculateDistance && warehouse != null
                    && isValidCoordinate(warehouse.getLongitude(), warehouse.getLatitude())) {
                long distanceMeters = calculateDistanceMeters(userLongitude, userLatitude,
                        warehouse.getLongitude(), warehouse.getLatitude());
                option.setDistanceMeters(distanceMeters)
                        .setDistanceText(formatDistanceText(distanceMeters));
            }
            options.put(option.getStockId(), option);
        }
        return CollectionUtils.convertList(options.values(), option -> option);
    }

    private Comparator<ErpMallStockOptionBO> buildMallStockOptionComparator() {
        return Comparator
                .comparing((ErpMallStockOptionBO option) -> !Boolean.TRUE.equals(option.getAvailable()))
                .thenComparing(option -> option.getDistanceMeters() == null)
                .thenComparing(ErpMallStockOptionBO::getDistanceMeters,
                        Comparator.nullsLast(Long::compareTo))
                .thenComparing(option -> option.getWarehouseName() == null ? "" : option.getWarehouseName(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ErpMallStockOptionBO::getStockId,
                        Comparator.nullsLast(Long::compareTo));
    }

    private boolean isValidCoordinate(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return false;
        }
        if (BigDecimal.ZERO.compareTo(longitude) == 0 && BigDecimal.ZERO.compareTo(latitude) == 0) {
            return false;
        }
        return longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
                && longitude.compareTo(BigDecimal.valueOf(180)) <= 0
                && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
                && latitude.compareTo(BigDecimal.valueOf(90)) <= 0;
    }

    private long calculateDistanceMeters(BigDecimal userLongitude, BigDecimal userLatitude,
                                         BigDecimal warehouseLongitude, BigDecimal warehouseLatitude) {
        double userLonRad = Math.toRadians(userLongitude.doubleValue());
        double userLatRad = Math.toRadians(userLatitude.doubleValue());
        double warehouseLonRad = Math.toRadians(warehouseLongitude.doubleValue());
        double warehouseLatRad = Math.toRadians(warehouseLatitude.doubleValue());
        double deltaLon = warehouseLonRad - userLonRad;
        double deltaLat = warehouseLatRad - userLatRad;
        double haversine = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(userLatRad) * Math.cos(warehouseLatRad) * Math.pow(Math.sin(deltaLon / 2), 2);
        double distance = 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return Math.round(distance);
    }

    private String formatDistanceText(long distanceMeters) {
        if (distanceMeters < 1000) {
            return distanceMeters + "m";
        }
        return BigDecimal.valueOf(distanceMeters)
                .divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "km";
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

    private ErpMallStockSummaryBO fillMallStockSummary(ErpMallStockSummaryBO summary, List<ErpStockDO> stocks) {
        if (CollUtil.isEmpty(stocks)) {
            return summary;
        }
        Set<Long> warehouseIds = convertSet(stocks, ErpStockDO::getWarehouseId);
        Set<Long> productIds = convertSet(stocks, ErpStockDO::getProductId);
        Map<Long, ErpWarehouseDO> warehouseMap = convertMap(warehouseMapper.selectBatchIds(warehouseIds),
                ErpWarehouseDO::getId);
        Set<Long> deptIds = new HashSet<>();
        warehouseMap.values().stream().map(ErpWarehouseDO::getDeptId).filter(Objects::nonNull).forEach(deptIds::add);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Map<String, BigDecimal> occupiedMap = stockMapper.selectOccupiedCountMap(productIds, warehouseIds);
        Map<String, BigDecimal> pendingInMap = stockMapper.selectPendingInCountMap(productIds, warehouseIds);
        Map<String, BigDecimal> inTransitMap = purchaseOrderItemMapper.selectInTransitCountMap(productIds, warehouseIds,
                Collections.singletonList(ErpAuditStatus.APPROVE.getStatus()));
        Map<Long, ErpMallStockSummaryBO.WarehouseStock> rowMap = new LinkedHashMap<>();
        Map<Long, List<String>> shelfMap = new LinkedHashMap<>();
        for (ErpStockDO stock : stocks) {
            Long warehouseId = stock.getWarehouseId();
            ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
            Long deptId = warehouse != null ? warehouse.getDeptId() : stock.getDeptId();
            ErpMallStockSummaryBO.WarehouseStock row = rowMap.computeIfAbsent(warehouseId, key ->
                    new ErpMallStockSummaryBO.WarehouseStock()
                            .setWarehouseId(warehouseId)
                            .setWarehouseName(warehouse == null ? "" : warehouse.getName())
                            .setDeptId(deptId)
                            .setDeptName(getDeptName(deptMap, deptId))
                            .setCount(BigDecimal.ZERO)
                            .setLockCount(BigDecimal.ZERO)
                            .setOccupiedCount(BigDecimal.ZERO)
                            .setPendingInCount(BigDecimal.ZERO)
                            .setInTransitCount(BigDecimal.ZERO)
                            .setAvailableCount(BigDecimal.ZERO));
            if (stock.getShelf() != null && !stock.getShelf().trim().isEmpty()) {
                shelfMap.computeIfAbsent(warehouseId, key -> new ArrayList<>()).add(stock.getShelf().trim());
            }
            String key = buildStockSummaryMapKey(stock.getProductId(), warehouseId);
            BigDecimal count = defaultZero(stock.getCount());
            BigDecimal lockCount = defaultZero(stock.getLockCount());
            BigDecimal occupiedCount = defaultZero(occupiedMap.get(key));
            BigDecimal pendingInCount = defaultZero(pendingInMap.get(key));
            BigDecimal inTransitCount = defaultZero(inTransitMap.get(key));
            BigDecimal availableCount = getDisplayAvailableCount(stock);
            row.setCount(row.getCount().add(count))
                    .setLockCount(row.getLockCount().add(lockCount))
                    .setOccupiedCount(row.getOccupiedCount().add(occupiedCount))
                    .setPendingInCount(row.getPendingInCount().add(pendingInCount))
                    .setInTransitCount(row.getInTransitCount().add(inTransitCount))
                    .setAvailableCount(row.getAvailableCount().add(availableCount));
        }
        rowMap.forEach((warehouseId, row) -> row.setShelf(formatShelves(shelfMap.get(warehouseId))));
        List<ErpMallStockSummaryBO.WarehouseStock> rows = new ArrayList<>(rowMap.values());
        rows.sort((a, b) -> a.getWarehouseName().compareToIgnoreCase(b.getWarehouseName()));
        return summary
                .setWarehouseStocks(rows)
                .setTotalCount(sumWarehouseStock(rows, ErpMallStockSummaryBO.WarehouseStock::getCount))
                .setTotalLockCount(sumWarehouseStock(rows, ErpMallStockSummaryBO.WarehouseStock::getLockCount))
                .setTotalOccupiedCount(sumWarehouseStock(rows, ErpMallStockSummaryBO.WarehouseStock::getOccupiedCount))
                .setTotalPendingInCount(sumWarehouseStock(rows, ErpMallStockSummaryBO.WarehouseStock::getPendingInCount))
                .setTotalInTransitCount(sumWarehouseStock(rows, ErpMallStockSummaryBO.WarehouseStock::getInTransitCount))
                .setTotalAvailableCount(sumWarehouseStock(rows, ErpMallStockSummaryBO.WarehouseStock::getAvailableCount));
    }

    private String getDeptName(Map<Long, DeptRespDTO> deptMap, Long deptId) {
        DeptRespDTO dept = deptId == null ? null : deptMap.get(deptId);
        return dept == null ? "" : dept.getName();
    }

    private String formatShelves(List<String> shelves) {
        if (CollUtil.isEmpty(shelves)) {
            return "";
        }
        return shelves.stream().distinct().collect(Collectors.joining(" / "));
    }

    private BigDecimal sumWarehouseStock(List<ErpMallStockSummaryBO.WarehouseStock> rows,
                                         java.util.function.Function<ErpMallStockSummaryBO.WarehouseStock, BigDecimal> getter) {
        return rows.stream().map(getter).map(this::defaultZero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String buildStockSummaryMapKey(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }

}
