package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.mall.ErpMallProductMappingDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.mall.ErpMallProductMappingMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpMallAutoWarehouseResultBO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商城自动分仓服务实现。
 */
@Service
public class ErpMallAutoWarehouseServiceImpl implements ErpMallAutoWarehouseService {

    private static final double EARTH_RADIUS_METERS = 6371000D;

    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpMallProductMappingMapper mallProductMappingMapper;
    @Resource
    private ErpStockMapper stockMapper;

    @Override
    public ErpMallAutoWarehouseResultBO allocate(Long deptId, BigDecimal longitude, BigDecimal latitude,
                                                 List<ErpMallAutoWarehouseResultBO.Item> items) {
        ErpMallAutoWarehouseResultBO result = new ErpMallAutoWarehouseResultBO();
        if (!isValidCoordinate(longitude, latitude)) {
            return result.setStatus(ErpMallAutoWarehouseResultBO.STATUS_LOCATION_REQUIRED);
        }
        if (CollUtil.isEmpty(items)) {
            return result.setStatus(ErpMallAutoWarehouseResultBO.STATUS_READY);
        }

        List<ErpWarehouseDO> saleWarehouses = warehouseService.getSaleWarehouseListByDeptId(deptId);
        if (saleWarehouses == null) {
            saleWarehouses = new ArrayList<>();
        }
        Map<Long, ErpWarehouseDO> locatedWarehouseMap = saleWarehouses.stream()
                .filter(warehouse -> isValidCoordinate(warehouse.getLongitude(), warehouse.getLatitude()))
                .collect(Collectors.toMap(ErpWarehouseDO::getId, warehouse -> warehouse, (left, right) -> left,
                        LinkedHashMap::new));
        if (locatedWarehouseMap.isEmpty()) {
            return buildAllItemIssues(result, items,
                    ErpMallAutoWarehouseResultBO.STATUS_WAREHOUSE_LOCATION_MISSING,
                    "当前销售部门没有已配置有效位置的可售仓库");
        }

        Set<Long> spuIds = items.stream().map(ErpMallAutoWarehouseResultBO.Item::getSpuId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollUtil.isEmpty(spuIds)) {
            return buildAllItemIssues(result, items, ErpMallAutoWarehouseResultBO.STATUS_PRODUCT_NOT_MAPPED,
                    "商品尚未映射 ERP 产品");
        }
        List<ErpMallProductMappingDO> mappings = mallProductMappingMapper.selectListByMallSpuIds(spuIds);
        Map<Long, Set<Long>> skuProductIds = new HashMap<>();
        for (ErpMallProductMappingDO mapping : mappings) {
            if (mapping.getMallSkuId() != null && mapping.getErpProductId() != null) {
                skuProductIds.computeIfAbsent(mapping.getMallSkuId(), key -> new HashSet<>())
                        .add(mapping.getErpProductId());
            }
        }
        Set<Long> productIds = skuProductIds.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        List<ErpStockDO> stocks = CollUtil.isEmpty(productIds)
                ? new ArrayList<>() : stockMapper.selectListByProductIds(productIds);
        Map<Long, List<ErpStockDO>> stockByProduct = stocks.stream()
                .filter(stock -> locatedWarehouseMap.containsKey(stock.getWarehouseId()))
                .collect(Collectors.groupingBy(ErpStockDO::getProductId));

        Map<Long, BigDecimal> consumedByStockId = new HashMap<>();
        List<ErpMallAutoWarehouseResultBO.Assignment> assignments = new ArrayList<>();
        List<ErpMallAutoWarehouseResultBO.Issue> issues = new ArrayList<>();
        boolean hasMappingIssue = false;
        for (ErpMallAutoWarehouseResultBO.Item item : items) {
            Set<Long> mappedProductIds = skuProductIds.get(item.getSkuId());
            if (CollUtil.isEmpty(mappedProductIds)) {
                hasMappingIssue = true;
                issues.add(buildIssue(item, ErpMallAutoWarehouseResultBO.STATUS_PRODUCT_NOT_MAPPED,
                        "商品尚未映射 ERP 产品"));
                continue;
            }
            BigDecimal required = BigDecimal.valueOf(item.getCount() == null ? 0 : item.getCount());
            List<Candidate> candidates = new ArrayList<>();
            for (Long productId : mappedProductIds) {
                for (ErpStockDO stock : stockByProduct.getOrDefault(productId, new ArrayList<>())) {
                    ErpWarehouseDO warehouse = locatedWarehouseMap.get(stock.getWarehouseId());
                    BigDecimal available = getAvailableCount(stock)
                            .subtract(consumedByStockId.getOrDefault(stock.getId(), BigDecimal.ZERO));
                    if (available.compareTo(required) >= 0) {
                        candidates.add(new Candidate(stock, warehouse,
                                calculateDistanceMeters(longitude, latitude,
                                        warehouse.getLongitude(), warehouse.getLatitude())));
                    }
                }
            }
            candidates.sort(Comparator.comparingLong(Candidate::distanceMeters)
                    .thenComparing(candidate -> candidate.warehouse().getSort(), Comparator.nullsLast(Long::compareTo))
                    .thenComparing(candidate -> candidate.warehouse().getId(), Comparator.nullsLast(Long::compareTo))
                    .thenComparing(candidate -> candidate.stock().getId(), Comparator.nullsLast(Long::compareTo)));
            if (candidates.isEmpty()) {
                issues.add(buildIssue(item, ErpMallAutoWarehouseResultBO.STATUS_STOCK_SHORTAGE,
                        "没有单个可售仓库可满足该商品数量"));
                continue;
            }
            Candidate selected = candidates.get(0);
            consumedByStockId.merge(selected.stock().getId(), required, BigDecimal::add);
            assignments.add(new ErpMallAutoWarehouseResultBO.Assignment()
                    .setIndex(item.getIndex()).setSpuId(item.getSpuId()).setSkuId(item.getSkuId())
                    .setCount(item.getCount()).setStockId(selected.stock().getId())
                    .setErpProductId(selected.stock().getProductId())
                    .setWarehouseId(selected.warehouse().getId())
                    .setWarehouseName(selected.warehouse().getName())
                    .setDistanceMeters(selected.distanceMeters()));
        }
        result.setAssignments(assignments).setIssues(issues);
        if (issues.isEmpty()) {
            return result.setStatus(ErpMallAutoWarehouseResultBO.STATUS_READY);
        }
        return result.setStatus(hasMappingIssue
                ? ErpMallAutoWarehouseResultBO.STATUS_PRODUCT_NOT_MAPPED
                : ErpMallAutoWarehouseResultBO.STATUS_STOCK_SHORTAGE);
    }

    private ErpMallAutoWarehouseResultBO buildAllItemIssues(ErpMallAutoWarehouseResultBO result,
                                                             List<ErpMallAutoWarehouseResultBO.Item> items,
                                                             String reason, String message) {
        List<ErpMallAutoWarehouseResultBO.Issue> issues = items.stream()
                .map(item -> buildIssue(item, reason, message)).collect(Collectors.toList());
        return result.setStatus(reason).setIssues(issues);
    }

    private ErpMallAutoWarehouseResultBO.Issue buildIssue(ErpMallAutoWarehouseResultBO.Item item,
                                                           String reason, String message) {
        return new ErpMallAutoWarehouseResultBO.Issue().setIndex(item.getIndex())
                .setSpuId(item.getSpuId()).setSkuId(item.getSkuId()).setCount(item.getCount())
                .setReason(reason).setMessage(message);
    }

    private BigDecimal getAvailableCount(ErpStockDO stock) {
        BigDecimal count = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
        BigDecimal lockCount = stock.getLockCount() == null ? BigDecimal.ZERO : stock.getLockCount();
        return count.subtract(lockCount);
    }

    private boolean isValidCoordinate(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null
                || (BigDecimal.ZERO.compareTo(longitude) == 0 && BigDecimal.ZERO.compareTo(latitude) == 0)) {
            return false;
        }
        return longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
                && longitude.compareTo(BigDecimal.valueOf(180)) <= 0
                && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
                && latitude.compareTo(BigDecimal.valueOf(90)) <= 0;
    }

    private long calculateDistanceMeters(BigDecimal longitude, BigDecimal latitude,
                                         BigDecimal warehouseLongitude, BigDecimal warehouseLatitude) {
        double lonRad = Math.toRadians(longitude.doubleValue());
        double latRad = Math.toRadians(latitude.doubleValue());
        double warehouseLonRad = Math.toRadians(warehouseLongitude.doubleValue());
        double warehouseLatRad = Math.toRadians(warehouseLatitude.doubleValue());
        double deltaLon = warehouseLonRad - lonRad;
        double deltaLat = warehouseLatRad - latRad;
        double haversine = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(latRad) * Math.cos(warehouseLatRad) * Math.pow(Math.sin(deltaLon / 2), 2);
        return Math.round(2 * EARTH_RADIUS_METERS
                * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine)));
    }

    private record Candidate(ErpStockDO stock, ErpWarehouseDO warehouse, long distanceMeters) {
    }

}
