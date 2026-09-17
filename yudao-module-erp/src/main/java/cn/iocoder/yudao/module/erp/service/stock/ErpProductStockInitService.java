package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.service.stock.cost.ErpStockDimensionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ERP 配件库存维度初始化服务。
 */
@Service
@Validated
public class ErpProductStockInitService {

    private static final String DIRECT_WAREHOUSE_NAME = "直发仓";
    private static final int STOCK_INSERT_BATCH_SIZE = 500;

    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockDimensionService stockDimensionService;

    @Transactional(rollbackFor = Exception.class)
    public void ensureStockExists(Long productId, Long warehouseId) {
        if (productId == null || warehouseId == null) {
            return;
        }
        if (stockDimensionService.initializeDimensions(Collections.singletonList(
                new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId)))) {
            return;
        }
        Long warehouseDeptId = resolveWarehouseDeptId(warehouseId);
        ErpStockDO stock = selectStockIgnoreDataPermission(productId, warehouseId);
        if (stock != null) {
            if (!Objects.equals(stock.getDeptId(), warehouseDeptId)) {
                DataPermissionUtils.executeIgnore(() -> stockMapper.updateById(
                        new ErpStockDO().setId(stock.getId()).setDeptId(warehouseDeptId)));
            }
            return;
        }
        try {
            insertStockIgnoreDataPermission(buildZeroStock(productId, warehouseId, warehouseDeptId));
        } catch (DuplicateKeyException ignored) {
            // 并发场景下其它事务先创建了同一产品/仓库库存行，按幂等成功处理。
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureProductStockForEnabledRealWarehouses(Long productId) {
        ensureProductsStockForEnabledRealWarehouses(Collections.singletonList(productId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureProductsStockForEnabledRealWarehouses(Collection<Long> productIds) {
        List<Long> distinctProductIds = normalizeProductIds(productIds);
        if (CollUtil.isEmpty(distinctProductIds)) {
            return;
        }
        List<ErpWarehouseDO> warehouses = getEnabledRealWarehouseList();
        if (CollUtil.isEmpty(warehouses)) {
            return;
        }
        List<ErpStockDO> dimensions = buildDimensions(distinctProductIds, warehouses);
        if (stockDimensionService.initializeDimensions(dimensions)) {
            return;
        }

        List<Long> warehouseIds = warehouses.stream().map(ErpWarehouseDO::getId).collect(Collectors.toList());
        List<ErpStockDO> existingStocks = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectListByProductIdsAndWarehouseIds(distinctProductIds, warehouseIds));
        if (existingStocks == null) {
            existingStocks = Collections.emptyList();
        }
        Set<String> existingKeys = existingStocks.stream()
                .map(stock -> buildKey(stock.getProductId(), stock.getWarehouseId()))
                .collect(Collectors.toSet());
        Map<Long, Long> warehouseDeptMap = warehouses.stream()
                .collect(Collectors.toMap(ErpWarehouseDO::getId, ErpWarehouseDO::getDeptId,
                        (first, second) -> first, LinkedHashMap::new));

        List<ErpStockDO> missingStocks = new ArrayList<>();
        for (Long productId : distinctProductIds) {
            for (Long warehouseId : warehouseIds) {
                if (existingKeys.contains(buildKey(productId, warehouseId))) {
                    continue;
                }
                missingStocks.add(buildZeroStock(productId, warehouseId, warehouseDeptMap.get(warehouseId)));
            }
        }
        insertMissingStocks(missingStocks);
    }

    private List<Long> normalizeProductIds(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyList();
        }
        return productIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    private List<ErpWarehouseDO> getEnabledRealWarehouseList() {
        List<ErpWarehouseDO> warehouses = DataPermissionUtils.executeIgnore(() -> warehouseService
                .getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus()));
        if (warehouses == null) {
            return Collections.emptyList();
        }
        return warehouses.stream()
                .filter(warehouse -> warehouse != null && warehouse.getId() != null)
                .filter(warehouse -> !isDirectWarehouse(warehouse))
                .collect(Collectors.toList());
    }

    private boolean isDirectWarehouse(ErpWarehouseDO warehouse) {
        return warehouse != null && DIRECT_WAREHOUSE_NAME.equals(warehouse.getName());
    }

    private List<ErpStockDO> buildDimensions(List<Long> productIds, List<ErpWarehouseDO> warehouses) {
        List<ErpStockDO> dimensions = new ArrayList<>(productIds.size() * warehouses.size());
        for (Long productId : productIds) {
            for (ErpWarehouseDO warehouse : warehouses) {
                dimensions.add(new ErpStockDO().setProductId(productId).setWarehouseId(warehouse.getId()));
            }
        }
        return dimensions;
    }

    private void insertMissingStocks(List<ErpStockDO> missingStocks) {
        if (CollUtil.isEmpty(missingStocks)) {
            return;
        }
        stockDimensionService.assertLegacyInsertAllowed();
        for (int index = 0; index < missingStocks.size(); index += STOCK_INSERT_BATCH_SIZE) {
            List<ErpStockDO> batch = missingStocks.subList(index,
                    Math.min(index + STOCK_INSERT_BATCH_SIZE, missingStocks.size()));
            try {
                DataPermissionUtils.executeIgnore(() -> stockMapper.insertBatch(batch, STOCK_INSERT_BATCH_SIZE));
            } catch (DuplicateKeyException ignored) {
                insertOneByOneIgnoreDuplicate(batch);
            }
        }
    }

    private void insertOneByOneIgnoreDuplicate(List<ErpStockDO> batch) {
        for (ErpStockDO stock : batch) {
            try {
                insertStockIgnoreDataPermission(stock);
            } catch (DuplicateKeyException ignored) {
                // 并发重复创建同一库存维度时保持幂等。
            }
        }
    }

    private ErpStockDO selectStockIgnoreDataPermission(Long productId, Long warehouseId) {
        return DataPermissionUtils.executeIgnore(() -> stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId));
    }

    private void insertStockIgnoreDataPermission(ErpStockDO stock) {
        stockDimensionService.assertLegacyInsertAllowed();
        DataPermissionUtils.executeIgnore(() -> {
            stockMapper.insert(stock);
        });
    }

    private ErpStockDO buildZeroStock(Long productId, Long warehouseId, Long warehouseDeptId) {
        return new ErpStockDO()
                .setProductId(productId)
                .setWarehouseId(warehouseId)
                .setDeptId(warehouseDeptId)
                .setCount(BigDecimal.ZERO)
                .setLockCount(BigDecimal.ZERO)
                .setCostPrice(BigDecimal.ZERO)
                .setCostAmount(BigDecimal.ZERO);
    }

    private Long resolveWarehouseDeptId(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        ErpWarehouseDO warehouse = DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouse(warehouseId));
        return warehouse != null ? warehouse.getDeptId() : null;
    }

    private String buildKey(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }

}
