package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockAdjustReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockBatchNoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchQuantityDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockBatchQuantityMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockTransferDirectionEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE2;

/**
 * ERP 产品库存 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockServiceImpl implements ErpStockService {

    /**
     * 允许库存为负数
     *
     * TODO 芋艿：后续做成 db 配置
     */
    private static final Boolean NEGATIVE_STOCK_COUNT_ENABLE = false;

    /**
     * 成本均价保留位数
     */
    private static final int COST_PRICE_SCALE = 6;

    /**
     * 乐观锁最大重试次数
     */
    private static final int MAX_RETRY_TIMES = 5;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpStockLockMapper stockLockMapper;
    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpStockBatchQuantityMapper stockBatchQuantityMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;

    @Lazy
    @Resource
    private ErpStockCheckService stockCheckService;

    @Override
    public ErpStockDO getStock(Long id) {
        return stockMapper.selectById(id);
    }

    @Override
    public ErpStockDO getStock(Long productId, Long warehouseId) {
        return stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
    }

    @Override
    public BigDecimal getStockCount(Long productId) {
        BigDecimal count = stockMapper.selectSumByProductId(productId);
        return count != null ? count : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getStockCount(Long productId, Long warehouseId) {
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        return stock != null && stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
    }

    @Override
    public List<ErpStockBatchNoRespVO> getAvailableBatchNoList(Long productId, Long warehouseId) {
        if (productId == null || warehouseId == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = purchaseInItemMapper.selectAvailableBatchNoList(
                productId, warehouseId, ErpAuditStatus.APPROVE.getStatus());
        return convertBatchNoRows(rows);
    }

    @Override
    public Map<String, List<ErpStockBatchNoRespVO>> getAvailableBatchNoListMap(Collection<ErpStockDO> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ErpStockDO> validStocks = new ArrayList<>(stocks.stream()
                .filter(stock -> stock.getProductId() != null && stock.getWarehouseId() != null)
                .collect(Collectors.toMap(
                        stock -> buildStockBatchNoMapKey(stock.getProductId(), stock.getWarehouseId()),
                        stock -> stock,
                        (first, second) -> first,
                        LinkedHashMap::new))
                .values());
        if (validStocks.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = purchaseInItemMapper.selectAvailableBatchNoListByProductAndWarehouseIds(
                validStocks, ErpAuditStatus.APPROVE.getStatus());
        return rows.stream()
                .collect(Collectors.groupingBy(row -> buildStockBatchNoMapKey(
                                toLong(row.get("product_id")), toLong(row.get("warehouse_id"))),
                        Collectors.mapping(this::convertBatchNoRow, Collectors.toList())));
    }

    @Override
    public Map<String, List<ErpStockBatchNoRespVO>> getStockBatchBalanceListMap(Collection<ErpStockDO> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> stockKeys = stocks.stream()
                .filter(stock -> stock.getProductId() != null && stock.getWarehouseId() != null)
                .map(stock -> buildStockBatchNoMapKey(stock.getProductId(), stock.getWarehouseId()))
                .collect(Collectors.toSet());
        if (stockKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = DataPermissionUtils.executeIgnore(
                () -> stockRecordMapper.selectBatchBalanceList(stocks));
        Map<String, List<ErpStockBatchNoRespVO>> result = rows.stream()
                .filter(row -> stockKeys.contains(buildStockBatchNoMapKey(
                        toLong(row.get("product_id")), toLong(row.get("warehouse_id")))))
                .collect(Collectors.groupingBy(row -> buildStockBatchNoMapKey(
                                toLong(row.get("product_id")), toLong(row.get("warehouse_id"))),
                        LinkedHashMap::new,
                        Collectors.mapping(this::convertBatchNoRow, Collectors.toList())));
        result.values().forEach(list -> list.sort((first, second) -> {
            if (first.getBatchNo() == null) {
                return second.getBatchNo() == null ? 0 : 1;
            }
            if (second.getBatchNo() == null) {
                return -1;
            }
            return first.getBatchNo().compareToIgnoreCase(second.getBatchNo());
        }));
        return result;
    }

    private List<ErpStockBatchNoRespVO> convertBatchNoRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ErpStockBatchNoRespVO> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            list.add(convertBatchNoRow(row));
        }
        return list;
    }

    private ErpStockBatchNoRespVO convertBatchNoRow(Map<String, Object> row) {
        return new ErpStockBatchNoRespVO()
                .setBatchNo(row.get("batch_no") != null ? String.valueOf(row.get("batch_no")) : null)
                .setAvailableCount(toBigDecimal(row.get("available_count")))
                .setFirstInTime(toLocalDateTime(row.get("first_in_time")));
    }

    private String buildStockBatchNoMapKey(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            return Long.valueOf(String.valueOf(value));
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value != null) {
            return new BigDecimal(String.valueOf(value));
        }
        return BigDecimal.ZERO;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime()).toLocalDateTime();
        }
        return null;
    }

    @Override
    public Map<Long, BigDecimal> getStockCountMap(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return stockMapper.selectSumMapByProductIds(productIds);
    }

    @Override
    public Map<Long, BigDecimal> getStockLockCountMap(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return stockLockMapper.selectList(new LambdaQueryWrapper<ErpStockLockDO>()
                        .in(ErpStockLockDO::getProductId, productIds)
                        .eq(ErpStockLockDO::getStatus, 1))
                .stream()
                .collect(Collectors.groupingBy(ErpStockLockDO::getProductId,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getLockCount() != null ? item.getLockCount() : BigDecimal.ZERO,
                                BigDecimal::add)));
    }

    @Override
    public Map<String, BigDecimal> getOccupiedCountMap(Collection<Long> productIds,
                                                       Collection<Long> warehouseIds) {
        return stockMapper.selectOccupiedCountMap(productIds, warehouseIds);
    }

    @Override
    public PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO) {
        // 1. 预先处理"货架位重复/空置"特殊条件

        // 2. 若有任何产品维度条件，先按产品过滤拿 productIds
        Collection<Long> productIdFilter = null;
        if (hasProductConditionExceptKeyword(pageReqVO)) {
            productIdFilter = DataPermissionUtils.executeIgnore(() ->
                    selectProductIdsWithoutStockShelfFilters(pageReqVO));
        }

        // 3. 查库存
        Collection<Long> warehouseIdFilter = null;
        if (pageReqVO.getDeptId() != null) {
            warehouseIdFilter = getWarehouseListByDeptFilter(pageReqVO).stream()
                    .map(ErpWarehouseDO::getId)
                    .collect(Collectors.toList());
        }
        boolean saleBizType = isSaleBizType(pageReqVO);
        ErpProductStockPermissionScope productStockScope = null;
        if (!saleBizType) {
            productStockScope = warehouseService.getCurrentUserProductStockPermissionScope();
            warehouseIdFilter = intersectWarehouseIds(warehouseIdFilter,
                    productStockScope.getVisibleWarehouseIds());
        } else {
            Collection<Long> visibleWarehouseIds = getVisibleWarehouseIdsForSaleStockPage(pageReqVO);
            warehouseIdFilter = intersectWarehouseIds(warehouseIdFilter, visibleWarehouseIds);
        }
        Collection<Long> keywordProductIdFilter = null;
        Collection<Long> keywordWarehouseIdFilter = null;
        Map<Long, Set<Long>> batchKeywordStockKeyMap = null;
        if (StringUtils.hasText(pageReqVO.getKeyword())) {
            keywordProductIdFilter = DataPermissionUtils.executeIgnore(() ->
                    productMapper.selectIdsByKeyword(pageReqVO));
            keywordWarehouseIdFilter = warehouseService.getWarehousePage(buildKeywordWarehouseReqVO(pageReqVO)).getList().stream()
                    .map(ErpWarehouseDO::getId)
                    .collect(Collectors.toList());
            if (Boolean.TRUE.equals(pageReqVO.getShowBatchNo())) {
                Collection<Long> finalVisibleWarehouseIds = warehouseIdFilter;
                batchKeywordStockKeyMap = new LinkedHashMap<>(DataPermissionUtils.executeIgnore(() ->
                        stockRecordMapper.selectStockKeyMapByBatchNoKeyword(
                                pageReqVO.getKeyword(), finalVisibleWarehouseIds)));
                List<ErpStockBatchQuantityDO> associatedBatchKeys = DataPermissionUtils.executeIgnore(() ->
                        stockBatchQuantityMapper.selectAssociatedBatchKeywordStockKeyList(
                                pageReqVO.getKeyword().trim().replaceAll("\\s+", "%"),
                                finalVisibleWarehouseIds,
                                ErpAuditStatus.PROCESS.getStatus(),
                                Arrays.asList(ErpSaleCartStatusEnum.PROCESS.getStatus(),
                                        ErpSaleCartStatusEnum.SUBMITTED.getStatus(),
                                        ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()),
                                ErpStockCheckTypeEnum.COUNT.getType(),
                                ErpStockTransferDirectionEnum.TRANSFER_OUT.getDirection(),
                                ErpStockTransferDirectionEnum.TRANSFER_IN.getDirection(),
                                Collections.singletonList(ErpAuditStatus.APPROVE.getStatus())));
                mergeBatchKeywordStockKeys(batchKeywordStockKeyMap, associatedBatchKeys);
            }
        }
        Collection<Long> finalProductIdFilter = productIdFilter;
        Collection<Long> finalWarehouseIdFilter = warehouseIdFilter;
        Collection<Long> finalKeywordProductIdFilter = keywordProductIdFilter;
        Collection<Long> finalKeywordWarehouseIdFilter = keywordWarehouseIdFilter;
        Map<Long, Set<Long>> finalBatchKeywordStockKeyMap = batchKeywordStockKeyMap;
        ErpProductStockPermissionScope finalProductStockScope = productStockScope;
        return DataPermissionUtils.executeIgnore(() ->
                finalProductStockScope == null || finalProductStockScope.isAll()
                        ? selectStockPage(pageReqVO, finalProductIdFilter, finalWarehouseIdFilter,
                        finalKeywordProductIdFilter, finalKeywordWarehouseIdFilter, finalBatchKeywordStockKeyMap)
                        : selectStockPageWithPermission(pageReqVO, finalProductIdFilter, finalWarehouseIdFilter,
                        finalKeywordProductIdFilter, finalKeywordWarehouseIdFilter, finalBatchKeywordStockKeyMap,
                        finalProductStockScope.getDepartmentWarehouseIds(),
                        finalProductStockScope.getSelfWarehouseIds(),
                        finalProductStockScope.getUserId() != null
                                ? String.valueOf(finalProductStockScope.getUserId()) : ""));
    }

    private PageResult<ErpStockDO> selectStockPage(ErpStockPageReqVO pageReqVO,
                                                   Collection<Long> productIdFilter,
                                                   Collection<Long> warehouseIdFilter,
                                                   Collection<Long> keywordProductIdFilter,
                                                   Collection<Long> keywordWarehouseIdFilter,
                                                   Map<Long, Set<Long>> batchKeywordStockKeyMap) {
        if (batchKeywordStockKeyMap == null) {
            return stockMapper.selectPage(pageReqVO, productIdFilter, warehouseIdFilter,
                    keywordProductIdFilter, keywordWarehouseIdFilter);
        }
        return stockMapper.selectPage(pageReqVO, productIdFilter, warehouseIdFilter,
                keywordProductIdFilter, keywordWarehouseIdFilter, batchKeywordStockKeyMap);
    }

    private PageResult<ErpStockDO> selectStockPageWithPermission(
            ErpStockPageReqVO pageReqVO,
            Collection<Long> productIdFilter,
            Collection<Long> warehouseIdFilter,
            Collection<Long> keywordProductIdFilter,
            Collection<Long> keywordWarehouseIdFilter,
            Map<Long, Set<Long>> batchKeywordStockKeyMap,
            Collection<Long> departmentWarehouseIds,
            Collection<Long> selfWarehouseIds,
            String selfCreator) {
        if (batchKeywordStockKeyMap == null) {
            return stockMapper.selectPage(pageReqVO, productIdFilter, warehouseIdFilter,
                    keywordProductIdFilter, keywordWarehouseIdFilter,
                    departmentWarehouseIds, selfWarehouseIds, selfCreator);
        }
        return stockMapper.selectPage(pageReqVO, productIdFilter, warehouseIdFilter,
                keywordProductIdFilter, keywordWarehouseIdFilter, batchKeywordStockKeyMap,
                departmentWarehouseIds, selfWarehouseIds, selfCreator);
    }

    private void mergeBatchKeywordStockKeys(Map<Long, Set<Long>> stockKeyMap,
                                            Collection<ErpStockBatchQuantityDO> stockKeys) {
        if (stockKeyMap == null || stockKeys == null) {
            return;
        }
        for (ErpStockBatchQuantityDO stockKey : stockKeys) {
            if (stockKey.getProductId() == null || stockKey.getWarehouseId() == null) {
                continue;
            }
            stockKeyMap.computeIfAbsent(stockKey.getProductId(), key -> new LinkedHashSet<>())
                    .add(stockKey.getWarehouseId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureStockExists(Long productId, Long warehouseId) {
        if (productId == null || warehouseId == null) {
            return;
        }
        Long warehouseDeptId = resolveWarehouseDeptId(warehouseId);
        ErpStockDO stock = selectStockIgnoreDataPermission(productId, warehouseId);
        if (stock != null) {
            if (!java.util.Objects.equals(stock.getDeptId(), warehouseDeptId)) {
                DataPermissionUtils.executeIgnore(() -> stockMapper.updateById(
                        new ErpStockDO().setId(stock.getId()).setDeptId(warehouseDeptId)));
            }
            return;
        }
        ErpStockDO zeroStock = new ErpStockDO()
                .setProductId(productId)
                .setWarehouseId(warehouseId)
                .setDeptId(warehouseDeptId)
                .setCount(BigDecimal.ZERO)
                .setLockCount(BigDecimal.ZERO)
                .setCostPrice(BigDecimal.ZERO)
                .setCostAmount(BigDecimal.ZERO);
        try {
            insertStockIgnoreDataPermission(zeroStock);
        } catch (DuplicateKeyException ignored) {
            // A concurrent transaction created the same product/warehouse row first.
        }
    }

    private Collection<Long> intersectWarehouseIds(Collection<Long> requestWarehouseIds,
                                                   Collection<Long> visibleWarehouseIds) {
        if (visibleWarehouseIds == null) {
            return requestWarehouseIds;
        }
        if (requestWarehouseIds == null) {
            return visibleWarehouseIds;
        }
        return requestWarehouseIds.stream()
                .filter(visibleWarehouseIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Collection<Long> getVisibleWarehouseIdsForSaleStockPage(ErpStockPageReqVO pageReqVO) {
        boolean allWarehousePermission = warehouseService.hasCurrentUserAllWarehousePermission();
        if (pageReqVO.getSaleDeptId() != null) {
            Collection<Long> saleDeptWarehouseIds = warehouseService.getSaleWarehouseListByDeptId(pageReqVO.getSaleDeptId())
                    .stream()
                    .map(ErpWarehouseDO::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (allWarehousePermission) {
                return saleDeptWarehouseIds;
            }
            Collection<Long> currentVisibleWarehouseIds = warehouseService.getCurrentUserVisibleSaleWarehouseList()
                    .stream()
                    .map(ErpWarehouseDO::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return intersectWarehouseIds(saleDeptWarehouseIds, currentVisibleWarehouseIds);
        }
        List<ErpWarehouseDO> visibleWarehouses = warehouseService.getCurrentUserVisibleSaleWarehouseList();
        Collection<Long> visibleWarehouseIds = visibleWarehouses.stream()
                .map(ErpWarehouseDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return visibleWarehouseIds;
    }

    private List<ErpWarehouseDO> getWarehouseListByDeptFilter(ErpStockPageReqVO pageReqVO) {
        // The final warehouse set is intersected with the current product-stock/sale visible scope above.
        // Ignore the generic department interceptor here so a distributed warehouse can still be found by owner dept.
        return DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseListByDeptId(pageReqVO.getDeptId()));
    }

    private boolean isSaleBizType(ErpStockPageReqVO pageReqVO) {
        return pageReqVO != null && "sale".equalsIgnoreCase(pageReqVO.getBizType());
    }

    @Override
    public void updateStockShelf(Collection<Long> stockIds, String shelf) {
        if (stockIds == null || stockIds.isEmpty()) {
            return;
        }
        List<Long> ids = stockIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        stockMapper.updateShelfByIds(ids, StringUtils.hasText(shelf) ? shelf.trim() : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpStockDO updateStockEditableFields(ErpStockUpdateReqVO reqVO) {
        ErpStockDO stock = stockMapper.selectById(reqVO.getId());
        if (stock == null) {
            throw new IllegalArgumentException("库存不存在");
        }
        BigDecimal count = stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
        BigDecimal costPrice;
        BigDecimal costAmount;

        switch (reqVO.getFieldName()) {
            case "shelf":
                stockMapper.updateShelfById(stock.getId(),
                        StringUtils.hasText(reqVO.getShelf()) ? reqVO.getShelf().trim() : null);
                break;
            case "purchasePrice":
                stockMapper.updatePurchasePriceById(stock.getId(), defaultZero(reqVO.getPurchasePrice()));
                break;
            case "costPrice":
                costPrice = defaultZero(reqVO.getCostPrice());
                costAmount = MoneyUtils.priceMultiply(costPrice, count);
                if (costAmount == null) {
                    costAmount = BigDecimal.ZERO;
                }
                stockMapper.updateCostById(stock.getId(), costPrice, costAmount);
                break;
            case "costAmount":
                costAmount = defaultZero(reqVO.getCostAmount());
                costPrice = count.signum() > 0
                        ? costAmount.divide(count, COST_PRICE_SCALE, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                stockMapper.updateCostById(stock.getId(), costPrice, costAmount);
                break;
            default:
                throw new IllegalArgumentException("不支持编辑的库存字段：" + reqVO.getFieldName());
        }
        return stockMapper.selectById(reqVO.getId());
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean hasProductCondition(ErpStockPageReqVO v) {
        return StringUtils.hasText(v.getKeyword())
                || StringUtils.hasText(v.getProductCode()) || StringUtils.hasText(v.getProductName())
                || StringUtils.hasText(v.getDrawingNo()) || StringUtils.hasText(v.getVehicleModel())
                || StringUtils.hasText(v.getOriginPlace()) || StringUtils.hasText(v.getBrand())
                || StringUtils.hasText(v.getFeatureCode())
                || StringUtils.hasText(v.getStandard()) || StringUtils.hasText(v.getFactoryCode())
                || StringUtils.hasText(v.getBarCode()) || StringUtils.hasText(v.getOeNumber())
                || v.getCategoryId() != null || v.getProductStatus() != null
                || v.getStockMaxMin() != null || v.getStockMaxMax() != null
                || v.getStockMinMin() != null || v.getStockMinMax() != null
                || v.getStockStandardMin() != null || v.getStockStandardMax() != null;
    }

    private boolean hasProductConditionExceptKeyword(ErpStockPageReqVO v) {
        return StringUtils.hasText(v.getProductCode()) || StringUtils.hasText(v.getProductName())
                || StringUtils.hasText(v.getDrawingNo()) || StringUtils.hasText(v.getVehicleModel())
                || StringUtils.hasText(v.getOriginPlace()) || StringUtils.hasText(v.getBrand())
                || StringUtils.hasText(v.getFeatureCode())
                || StringUtils.hasText(v.getStandard()) || StringUtils.hasText(v.getFactoryCode())
                || StringUtils.hasText(v.getBarCode()) || StringUtils.hasText(v.getOeNumber())
                || v.getCategoryId() != null || v.getProductStatus() != null
                || v.getStockMaxMin() != null || v.getStockMaxMax() != null
                || v.getStockMinMin() != null || v.getStockMinMax() != null
                || v.getStockStandardMin() != null || v.getStockStandardMax() != null;
    }

    private Collection<Long> selectProductIdsWithoutStockShelfFilters(ErpStockPageReqVO pageReqVO) {
        String shelf = pageReqVO.getShelf();
        Boolean shelfDuplicateOnly = pageReqVO.getShelfDuplicateOnly();
        Boolean shelfEmptyOnly = pageReqVO.getShelfEmptyOnly();
        pageReqVO.setShelf(null);
        pageReqVO.setShelfDuplicateOnly(null);
        pageReqVO.setShelfEmptyOnly(null);
        try {
            return productMapper.selectIdsByComplexQueryWithoutKeyword(pageReqVO, null, null);
        } finally {
            pageReqVO.setShelf(shelf);
            pageReqVO.setShelfDuplicateOnly(shelfDuplicateOnly);
            pageReqVO.setShelfEmptyOnly(shelfEmptyOnly);
        }
    }

    private cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO buildKeywordWarehouseReqVO(
            ErpStockPageReqVO reqVO) {
        cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO warehouseReqVO =
                new cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO();
        warehouseReqVO.setPageNo(1);
        warehouseReqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        warehouseReqVO.setKeyword(reqVO.getKeyword());
        return warehouseReqVO;
    }

    @Override
    public BigDecimal updateStockCountIncrement(Long productId, Long warehouseId, BigDecimal count) {
        // 1.1 查询当前库存
        ErpStockDO stock = selectStockIgnoreDataPermission(productId, warehouseId);
        if (stock == null) {
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId)
                    .setDeptId(resolveWarehouseDeptId(warehouseId))
                    .setCount(BigDecimal.ZERO);
            insertStockIgnoreDataPermission(stock);
        }
        // 1.2 校验库存是否充足
        if (!NEGATIVE_STOCK_COUNT_ENABLE && stock.getCount().add(count).compareTo(BigDecimal.ZERO) < 0) {
            throw exception(STOCK_COUNT_NEGATIVE, getProductNameIgnoreDataPermission(productId),
                    getWarehouseNameIgnoreDataPermission(warehouseId), stock.getCount(), count);
        }

        // 2. 库存变更
        Long stockId = stock.getId();
        int updateCount = DataPermissionUtils.executeIgnore(() ->
                stockMapper.updateCountIncrement(stockId, count, NEGATIVE_STOCK_COUNT_ENABLE));
        if (updateCount == 0) {
            // 此时不好去查询最新库存，所以直接抛出该提示，不提供具体库存数字
            throw exception(STOCK_COUNT_NEGATIVE2, getProductNameIgnoreDataPermission(productId),
                    getWarehouseNameIgnoreDataPermission(warehouseId));
        }

        // 3. 返回最新库存
        return stock.getCount().add(count);
    }

    @Override
    public StockUpdateResult updateStockCountAndCost(Long productId, Long warehouseId,
                                                    BigDecimal count, BigDecimal unitPrice, Integer bizType) {
        // 0. 入库必须有单价（出库允许 null，由上层按当前成本均价回填至流水）
        if (count.compareTo(BigDecimal.ZERO) > 0 && unitPrice == null) {
            throw new IllegalArgumentException("入库时 unitPrice 不能为空");
        }

        // 1. 查询当前库存；若不存在则初始化
        ErpStockDO stock = selectStockIgnoreDataPermission(productId, warehouseId);
        if (stock == null) {
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId)
                    .setDeptId(resolveWarehouseDeptId(warehouseId))
                    .setCount(BigDecimal.ZERO)
                    .setCostPrice(BigDecimal.ZERO)
                    .setCostAmount(BigDecimal.ZERO);
            insertStockIgnoreDataPermission(stock);
        }

        // 2. 乐观锁循环重试
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            BigDecimal oldCount = stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
            BigDecimal oldCost = stock.getCostPrice() != null ? stock.getCostPrice() : BigDecimal.ZERO;
            BigDecimal oldCostAmount = stock.getCostAmount() != null
                    ? stock.getCostAmount()
                    : MoneyUtils.priceMultiply(oldCost, oldCount);
            if (oldCostAmount == null) {
                oldCostAmount = BigDecimal.ZERO;
            }

            // 2.1 校验库存是否充足
            if (!NEGATIVE_STOCK_COUNT_ENABLE && oldCount.add(count).compareTo(BigDecimal.ZERO) < 0) {
                throw exception(STOCK_COUNT_NEGATIVE, getProductNameIgnoreDataPermission(productId),
                        getWarehouseNameIgnoreDataPermission(warehouseId), oldCount, count);
            }

            // 2.2 零增量：直接返回
            if (count.compareTo(BigDecimal.ZERO) == 0) {
                return new StockUpdateResult(oldCount, oldCost, oldCostAmount);
            }

            BigDecimal newCount = oldCount.add(count);
            BigDecimal newCost;
            BigDecimal newAmount;
            if (count.compareTo(BigDecimal.ZERO) > 0) {
                // 入库：移动加权平均
                if (newCount.compareTo(BigDecimal.ZERO) == 0) {
                    newCost = BigDecimal.ZERO;
                    newAmount = BigDecimal.ZERO;
                } else {
                    newAmount = oldCostAmount.add(count.multiply(unitPrice));
                    newCost = newAmount.divide(newCount, COST_PRICE_SCALE, RoundingMode.HALF_UP);
                }
            } else {
                // 出库：成本均价不变；若全部出光，归零
                if (newCount.compareTo(BigDecimal.ZERO) == 0) {
                    newCost = BigDecimal.ZERO;
                    newAmount = BigDecimal.ZERO;
                } else if (shouldDeductOutboundCostByUnitPrice(unitPrice, bizType)) {
                    newAmount = oldCostAmount.add(count.multiply(unitPrice));
                    newCost = newAmount.divide(newCount, COST_PRICE_SCALE, RoundingMode.HALF_UP);
                } else {
                    newCost = oldCost;
                    newAmount = MoneyUtils.priceMultiply(newCost, newCount);
                    if (newAmount == null) {
                        newAmount = BigDecimal.ZERO;
                    }
                }
            }

            // 2.3 结存金额
            if (newAmount == null) {
                newAmount = BigDecimal.ZERO;
            }

            // 2.4 原子更新（乐观锁）
            Long stockId = stock.getId();
            BigDecimal expectedOldCount = oldCount;
            BigDecimal targetCount = newCount;
            BigDecimal targetCost = newCost;
            BigDecimal targetAmount = newAmount;
            int updateCount = DataPermissionUtils.executeIgnore(() ->
                    stockMapper.updateCountAndCost(stockId, expectedOldCount, targetCount,
                            targetCost, targetAmount, NEGATIVE_STOCK_COUNT_ENABLE));
            if (updateCount == 1) {
                return new StockUpdateResult(newCount, newCost, newAmount);
            }

            // 2.5 冲突：重新读取再试
            ErpStockDO latest = selectStockIgnoreDataPermission(productId, warehouseId);
            if (latest != null) {
                stock = latest;
            }
        }

        // 3. 重试耗尽
        throw exception(STOCK_COUNT_NEGATIVE2, getProductNameIgnoreDataPermission(productId),
                getWarehouseNameIgnoreDataPermission(warehouseId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal adjustStock(ErpStockAdjustReqVO reqVO) {
        return stockCheckService.createAndApproveStockAdjustCheck(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStockCostAmount(Long productId, Long warehouseId,
                                      BigDecimal deltaCostAmountFull, BigDecimal sumInCount,
                                      Long bizId, String bizNo, LocalDateTime bizDate) {
        // 0. 空差额直接短路
        if (deltaCostAmountFull == null || deltaCostAmountFull.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // 1. 查库存；不存在则初始化（当前无库存也可以先写流水痕迹，但差额无处摊）
        ErpStockDO stock = selectStockIgnoreDataPermission(productId, warehouseId);
        if (stock == null) {
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId)
                    .setDeptId(resolveWarehouseDeptId(warehouseId))
                    .setCount(BigDecimal.ZERO)
                    .setCostPrice(BigDecimal.ZERO)
                    .setCostAmount(BigDecimal.ZERO);
            insertStockIgnoreDataPermission(stock);
        }

        BigDecimal effectiveDelta = BigDecimal.ZERO;
        BigDecimal newCostAmount;
        BigDecimal newCostPrice = BigDecimal.ZERO;

        // 2. 乐观锁循环重试：更新 cost_amount / cost_price（count 不变，作为乐观锁 where 条件）
        boolean success = false;
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            BigDecimal currentCount = stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
            BigDecimal currentAmount = stock.getCostAmount() != null ? stock.getCostAmount() : BigDecimal.ZERO;

            // 2.1 计算摊分比例（Q1=A 方案）：ratio = min(currentCount / sumInCount, 1)
            BigDecimal ratio;
            if (sumInCount == null || sumInCount.signum() == 0 || currentCount.signum() <= 0) {
                ratio = BigDecimal.ZERO;
            } else {
                ratio = currentCount.divide(sumInCount, COST_PRICE_SCALE, RoundingMode.HALF_UP);
                if (ratio.compareTo(BigDecimal.ONE) > 0) {
                    ratio = BigDecimal.ONE;
                }
            }

            // 2.2 有效差额（保留 2 位）
            effectiveDelta = deltaCostAmountFull.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            // 2.3 新成本金额与均价
            newCostAmount = currentAmount.add(effectiveDelta);
            if (currentCount.signum() > 0) {
                newCostPrice = newCostAmount.divide(currentCount, COST_PRICE_SCALE, RoundingMode.HALF_UP);
            } else {
                newCostPrice = BigDecimal.ZERO;
            }

            // 2.4 乐观锁更新：count / cost_amount 作为版本校验条件
            Long stockId = stock.getId();
            BigDecimal expectedCount = currentCount;
            BigDecimal expectedAmount = currentAmount;
            BigDecimal targetAmount = newCostAmount;
            BigDecimal targetCostPrice = newCostPrice;
            int affected = DataPermissionUtils.executeIgnore(() ->
                    stockMapper.updateCostAmountAndPrice(stockId, expectedCount, expectedAmount,
                            targetAmount, targetCostPrice));
            if (affected == 1) {
                success = true;
                break;
            }
            // 2.5 冲突：重读再算
            ErpStockDO latest = selectStockIgnoreDataPermission(productId, warehouseId);
            if (latest != null) {
                stock = latest;
            }
        }
        if (!success) {
            // 理论上极少达到；此处复用 STOCK_COUNT_NEGATIVE2 语义（并发冲突无法完成）
            throw exception(STOCK_COUNT_NEGATIVE2, getProductNameIgnoreDataPermission(productId),
                    getWarehouseNameIgnoreDataPermission(warehouseId));
        }

        // 3. 手工写流水（ErpStockRecordServiceImpl.createStockRecord 内部会调用 updateStockCountAndCost，
        //    但我们既不能让它改数量，也不想它重算成本均价，因此直接插入流水 DO）
        ErpStockRecordDO record = new ErpStockRecordDO()
                .setProductId(productId)
                .setWarehouseId(warehouseId)
                .setDeptId(resolveStockDeptId(stock, warehouseId))
                .setCount(BigDecimal.ZERO)
                .setTotalCount(stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO)
                .setBizType(ErpStockRecordBizTypeEnum.PURCHASE_PRICE_ADJUST.getType())
                .setBizId(bizId)
                .setBizItemId(0L)
                .setBizNo(bizNo)
                .setUnitPrice(null)
                .setTotalPrice(effectiveDelta)
                .setCostPrice(newCostPrice)
                .setCostAmount(stock.getCostAmount() != null
                        ? stock.getCostAmount().add(effectiveDelta)
                        : effectiveDelta)
                .setBizDate(bizDate != null ? bizDate : LocalDateTime.now());
        stockRecordMapper.insert(record);
    }

    @Override
    public StockUpdateResult updateStockCostPrice(Long productId, Long warehouseId, BigDecimal costPrice) {
        BigDecimal targetCostPrice = costPrice != null ? costPrice : BigDecimal.ZERO;
        ErpStockDO stock = selectStockIgnoreDataPermission(productId, warehouseId);
        if (stock == null) {
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId)
                    .setDeptId(resolveWarehouseDeptId(warehouseId))
                    .setCount(BigDecimal.ZERO)
                    .setCostPrice(BigDecimal.ZERO)
                    .setCostAmount(BigDecimal.ZERO);
            insertStockIgnoreDataPermission(stock);
        }

        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            BigDecimal currentCount = stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
            BigDecimal currentCostPrice = stock.getCostPrice() != null ? stock.getCostPrice() : BigDecimal.ZERO;
            BigDecimal newCostAmount = MoneyUtils.priceMultiply(targetCostPrice, currentCount);
            if (newCostAmount == null) {
                newCostAmount = BigDecimal.ZERO;
            }
            Long stockId = stock.getId();
            BigDecimal expectedCount = currentCount;
            BigDecimal expectedCostPrice = currentCostPrice;
            BigDecimal targetAmount = newCostAmount;
            int affected = DataPermissionUtils.executeIgnore(() ->
                    stockMapper.updateCostPriceAndAmount(stockId, expectedCount, expectedCostPrice,
                            targetCostPrice, targetAmount));
            if (affected == 1) {
                return new StockUpdateResult(currentCount, targetCostPrice, newCostAmount);
            }
            ErpStockDO latest = selectStockIgnoreDataPermission(productId, warehouseId);
            if (latest != null) {
                stock = latest;
            }
        }
        throw exception(STOCK_COUNT_NEGATIVE2, getProductNameIgnoreDataPermission(productId),
                getWarehouseNameIgnoreDataPermission(warehouseId));
    }

    private boolean shouldDeductOutboundCostByUnitPrice(BigDecimal unitPrice, Integer bizType) {
        return unitPrice != null && ErpStockRecordBizTypeEnum.PURCHASE_RETURN.getType().equals(bizType);
    }

    private ErpStockDO selectStockIgnoreDataPermission(Long productId, Long warehouseId) {
        return DataPermissionUtils.executeIgnore(() -> stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId));
    }

    private void insertStockIgnoreDataPermission(ErpStockDO stock) {
        DataPermissionUtils.executeIgnore(() -> {
            stockMapper.insert(stock);
        });
    }

    private Long resolveWarehouseDeptId(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(warehouseId);
        return warehouse != null ? warehouse.getDeptId() : null;
    }

    private Long resolveStockDeptId(ErpStockDO stock, Long warehouseId) {
        if (stock != null && stock.getDeptId() != null) {
            return stock.getDeptId();
        }
        return resolveWarehouseDeptId(warehouseId);
    }

    private String getProductNameIgnoreDataPermission(Long productId) {
        ErpProductDO product = DataPermissionUtils.executeIgnore(() -> productService.getProduct(productId));
        return product != null && StringUtils.hasText(product.getName()) ? product.getName() : String.valueOf(productId);
    }

    private String getWarehouseNameIgnoreDataPermission(Long warehouseId) {
        ErpWarehouseDO warehouse = DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouse(warehouseId));
        return warehouse != null && StringUtils.hasText(warehouse.getName()) ? warehouse.getName() : String.valueOf(warehouseId);
    }

}
