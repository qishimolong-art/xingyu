package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordSummaryVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockCheckItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.cost.ErpDualCostPostingService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * ERP 产品库存明细 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockRecordServiceImpl implements ErpStockRecordService {

    @Resource
    private ErpStockRecordMapper stockRecordMapper;

    @Resource
    private ErpDualCostPostingService dualCostPostingService;

    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockItemSnapshotSupport snapshotSupport;

    @Resource
    private ErpProductMapper productMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpStockInItemMapper stockInItemMapper;
    @Resource
    private ErpStockOutItemMapper stockOutItemMapper;
    @Resource
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Resource
    private ErpStockCheckItemMapper stockCheckItemMapper;

    @Override
    public ErpStockRecordDO getStockRecord(Long id) {
        ErpStockRecordDO stockRecord = stockRecordMapper.selectById(id);
        if (stockRecord == null) {
            return null;
        }
        fillAmount(Collections.singletonList(stockRecord));
        return stockRecord;
    }

    @Override
    public PageResult<ErpStockRecordDO> getStockRecordPage(ErpStockRecordPageReqVO pageReqVO) {
        validateBatchRequest(pageReqVO);
        return executeStockViewRead(pageReqVO, () -> {
            Collection<Long> productIdFilter = resolveProductIdFilter(pageReqVO);
            PageResult<ErpStockRecordDO> pageResult =
                    stockRecordMapper.selectPageWithProductFilter(pageReqVO, productIdFilter);
            fillAmount(pageResult.getList());
            fillBatchRunningBalance(pageReqVO, pageResult.getList());
            return pageResult;
        });
    }

    private void fillBatchRunningBalance(ErpStockRecordPageReqVO reqVO, List<ErpStockRecordDO> pageRecords) {
        if (!Boolean.TRUE.equals(reqVO.getStockView()) || pageRecords == null || pageRecords.isEmpty()) {
            return;
        }
        boolean unassignedBatch = Boolean.TRUE.equals(reqVO.getUnassignedBatch());
        boolean namedBatch = StringUtils.hasText(reqVO.getBatchNo());
        if (!unassignedBatch && !namedBatch) {
            return;
        }
        List<ErpStockRecordDO> allBatchRecords = stockRecordMapper.selectBatchRunningBalanceRecords(
                reqVO.getProductId(), reqVO.getWarehouseId(), reqVO.getBatchNo(), reqVO.getUnassignedBatch());
        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        for (ErpStockRecordDO record : allBatchRecords) {
            runningBalance = runningBalance.add(record.getCount() != null ? record.getCount() : BigDecimal.ZERO);
            runningBalanceMap.put(record.getId(), runningBalance);
        }
        for (ErpStockRecordDO record : pageRecords) {
            BigDecimal batchBalance = runningBalanceMap.get(record.getId());
            if (batchBalance == null) {
                continue;
            }
            record.setTotalCount(batchBalance);
            record.setCostAmount(record.getCostPrice() != null
                    ? record.getCostPrice().multiply(batchBalance) : BigDecimal.ZERO);
        }
    }

    @Override
    public ErpStockRecordSummaryVO getStockRecordSummary(ErpStockRecordPageReqVO reqVO) {
        validateBatchRequest(reqVO);
        return executeStockViewRead(reqVO, () -> getStockRecordSummaryInternal(reqVO));
    }

    private void validateBatchRequest(ErpStockRecordPageReqVO reqVO) {
        if (Boolean.TRUE.equals(reqVO.getUnassignedBatch()) && StringUtils.hasText(reqVO.getBatchNo())) {
            throw new IllegalArgumentException("batchNo and unassignedBatch cannot be used together");
        }
    }

    private ErpStockRecordSummaryVO getStockRecordSummaryInternal(ErpStockRecordPageReqVO reqVO) {
        ErpStockRecordSummaryVO summary = new ErpStockRecordSummaryVO();
        Collection<Long> productIdFilter = resolveProductIdFilter(reqVO);
        // 若产品维度条件命中 0 条，直接返回空汇总
        if (productIdFilter != null && productIdFilter.isEmpty()) {
            return summary;
        }
        // 查全部满足条件的流水（临时把分页大小设为不限）
        Integer origPageSize = reqVO.getPageSize();
        Integer origPageNo = reqVO.getPageNo();
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        reqVO.setPageNo(1);
        try {
            PageResult<ErpStockRecordDO> page = stockRecordMapper.selectPageWithProductFilter(reqVO, productIdFilter);
            fillAmount(page.getList());
            Map<String, ErpStockRecordDO> latestRecordMap = new HashMap<>();
            for (ErpStockRecordDO r : page.getList()) {
                BigDecimal count = r.getCount() != null ? r.getCount() : BigDecimal.ZERO;
                BigDecimal unitPrice = r.getUnitPrice() != null ? r.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal totalPrice = r.getTotalPrice() != null ? r.getTotalPrice() : BigDecimal.ZERO;
                int sign = count.compareTo(BigDecimal.ZERO);
                if (sign > 0) {
                    summary.setTotalInCount(summary.getTotalInCount().add(count));
                    BigDecimal inAmount = totalPrice.compareTo(BigDecimal.ZERO) != 0
                            ? totalPrice : count.multiply(unitPrice);
                    summary.setTotalInAmount(summary.getTotalInAmount().add(inAmount));
                } else if (sign < 0) {
                    BigDecimal abs = count.abs();
                    summary.setTotalOutCount(summary.getTotalOutCount().add(abs));
                    BigDecimal outAmount = totalPrice.compareTo(BigDecimal.ZERO) != 0
                            ? totalPrice.abs() : abs.multiply(unitPrice);
                    summary.setTotalOutAmount(summary.getTotalOutAmount().add(outAmount));
                }
                String stockKey = r.getProductId() + "_" + r.getWarehouseId();
                latestRecordMap.putIfAbsent(stockKey, r);
            }
            if (isBatchStockView(reqVO)) {
                fillBatchSummaryBalance(reqVO, summary);
            } else {
                for (ErpStockRecordDO record : latestRecordMap.values()) {
                    summary.setBalanceCount(summary.getBalanceCount().add(
                            record.getTotalCount() != null ? record.getTotalCount() : BigDecimal.ZERO));
                    summary.setBalanceAmount(summary.getBalanceAmount().add(
                            record.getCostAmount() != null ? record.getCostAmount() : BigDecimal.ZERO));
                }
            }
            summary.setRecordCount((long) page.getList().size());
        } finally {
            reqVO.setPageSize(origPageSize);
            reqVO.setPageNo(origPageNo);
        }
        return summary;
    }

    private <T> T executeStockViewRead(ErpStockRecordPageReqVO reqVO, Supplier<T> supplier) {
        if (!Boolean.TRUE.equals(reqVO.getStockView())) {
            return supplier.get();
        }
        if (reqVO.getProductId() == null || reqVO.getWarehouseId() == null) {
            throw new IllegalArgumentException("Product and warehouse are required for product stock record view");
        }
        warehouseService.validateCurrentUserStockWarehousePermission(
                Collections.singleton(reqVO.getWarehouseId()));
        return DataPermissionUtils.executeIgnore(supplier::get);
    }

    private boolean isBatchStockView(ErpStockRecordPageReqVO reqVO) {
        return Boolean.TRUE.equals(reqVO.getStockView())
                && (Boolean.TRUE.equals(reqVO.getUnassignedBatch()) || StringUtils.hasText(reqVO.getBatchNo()));
    }

    private void fillBatchSummaryBalance(ErpStockRecordPageReqVO reqVO, ErpStockRecordSummaryVO summary) {
        List<ErpStockRecordDO> records = stockRecordMapper.selectBatchRunningBalanceRecords(
                reqVO.getProductId(), reqVO.getWarehouseId(), reqVO.getBatchNo(), reqVO.getUnassignedBatch());
        BigDecimal balance = BigDecimal.ZERO;
        ErpStockRecordDO latest = null;
        for (ErpStockRecordDO record : records) {
            balance = balance.add(record.getCount() != null ? record.getCount() : BigDecimal.ZERO);
            latest = record;
        }
        summary.setBalanceCount(balance);
        summary.setBalanceAmount(latest != null && latest.getCostPrice() != null
                ? latest.getCostPrice().multiply(balance) : BigDecimal.ZERO);
    }

    private void fillAmount(List<ErpStockRecordDO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Integer, Map<Long, StockRecordAmount>> amountMapByBizType = records.stream()
                .filter(Objects::nonNull)
                .filter(record -> record.getUnitPrice() == null && record.getTotalPrice() == null)
                .filter(record -> record.getBizItemId() != null)
                .filter(record -> record.getBizType() != null)
                .map(ErpStockRecordDO::getBizType)
                .distinct()
                .collect(Collectors.toMap(bizType -> bizType, bizType -> getStockRecordAmountMap(bizType, records)));
        for (ErpStockRecordDO record : records) {
            fillAmount(record, amountMapByBizType.get(record.getBizType()));
        }
    }

    private void fillAmount(ErpStockRecordDO record, Map<Long, StockRecordAmount> amountMap) {
        BigDecimal count = record.getCount() != null ? record.getCount() : BigDecimal.ZERO;
        StockRecordAmount sourceAmount = amountMap != null ? amountMap.get(record.getBizItemId()) : null;

        BigDecimal unitPrice = record.getUnitPrice();
        if (unitPrice == null && sourceAmount != null && sourceAmount.getUnitPrice() != null) {
            unitPrice = sourceAmount.getUnitPrice();
        }

        BigDecimal totalPrice = record.getTotalPrice();
        if (totalPrice == null && sourceAmount != null && sourceAmount.getTotalPrice() != null) {
            totalPrice = sourceAmount.getTotalPrice();
            if (count.compareTo(BigDecimal.ZERO) < 0 && totalPrice.compareTo(BigDecimal.ZERO) > 0) {
                totalPrice = totalPrice.negate();
            }
        }

        if (unitPrice == null && totalPrice != null && count.compareTo(BigDecimal.ZERO) != 0) {
            unitPrice = totalPrice.abs().divide(count.abs(), 6, RoundingMode.HALF_UP);
        }
        if (totalPrice == null && unitPrice != null) {
            totalPrice = MoneyUtils.priceMultiply(unitPrice, count);
        }
        record.setUnitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO);
        record.setTotalPrice(totalPrice != null ? totalPrice : BigDecimal.ZERO);
    }

    private Map<Long, StockRecordAmount> getStockRecordAmountMap(Integer bizType, List<ErpStockRecordDO> records) {
        List<Long> bizItemIds = records.stream()
                .filter(Objects::nonNull)
                .filter(record -> bizType.equals(record.getBizType()))
                .filter(record -> record.getUnitPrice() == null && record.getTotalPrice() == null)
                .map(ErpStockRecordDO::getBizItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (bizItemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (ErpStockRecordBizTypeEnum.PURCHASE_IN.getType().equals(bizType)) {
            return purchaseInItemMapper.selectBatchIds(bizItemIds).stream()
                    .collect(Collectors.toMap(ErpPurchaseInItemDO::getId,
                            item -> new StockRecordAmount(item.getProductPrice(), item.getTotalPrice()), (a, b) -> a));
        }
        if (ErpStockRecordBizTypeEnum.PURCHASE_RETURN.getType().equals(bizType)) {
            return purchaseReturnItemMapper.selectBatchIds(bizItemIds).stream()
                    .collect(Collectors.toMap(ErpPurchaseReturnItemDO::getId,
                            item -> new StockRecordAmount(item.getProductPrice(), item.getTotalPrice()), (a, b) -> a));
        }
        if (ErpStockRecordBizTypeEnum.SALE_OUT.getType().equals(bizType)) {
            return saleOutItemMapper.selectBatchIds(bizItemIds).stream()
                    .collect(Collectors.toMap(ErpSaleOutItemDO::getId,
                            item -> new StockRecordAmount(item.getProductPrice(), item.getTotalPrice()), (a, b) -> a));
        }
        if (ErpStockRecordBizTypeEnum.SALE_RETURN.getType().equals(bizType)) {
            return saleReturnItemMapper.selectBatchIds(bizItemIds).stream()
                    .collect(Collectors.toMap(ErpSaleReturnItemDO::getId,
                            item -> new StockRecordAmount(item.getProductPrice(), item.getTotalPrice()), (a, b) -> a));
        }
        if (ErpStockRecordBizTypeEnum.OTHER_IN.getType().equals(bizType)) {
            return stockInItemMapper.selectBatchIds(bizItemIds).stream()
                    .collect(Collectors.toMap(ErpStockInItemDO::getId,
                            item -> new StockRecordAmount(item.getProductPrice(), item.getTotalPrice()), (a, b) -> a));
        }
        if (ErpStockRecordBizTypeEnum.OTHER_OUT.getType().equals(bizType)) {
            return stockOutItemMapper.selectBatchIds(bizItemIds).stream()
                    .collect(Collectors.toMap(ErpStockOutItemDO::getId,
                            item -> new StockRecordAmount(item.getProductPrice(), item.getTotalPrice()), (a, b) -> a));
        }
        if (ErpStockRecordBizTypeEnum.MOVE_IN.getType().equals(bizType)
                || ErpStockRecordBizTypeEnum.MOVE_OUT.getType().equals(bizType)) {
            return stockMoveItemMapper.selectBatchIds(bizItemIds).stream()
                    .collect(Collectors.toMap(ErpStockMoveItemDO::getId,
                            item -> new StockRecordAmount(item.getProductPrice(), item.getTotalPrice()), (a, b) -> a));
        }
        if (ErpStockRecordBizTypeEnum.CHECK_MORE_IN.getType().equals(bizType)
                || ErpStockRecordBizTypeEnum.CHECK_LESS_OUT.getType().equals(bizType)) {
            return stockCheckItemMapper.selectBatchIds(bizItemIds).stream()
                    .collect(Collectors.toMap(ErpStockCheckItemDO::getId,
                            item -> new StockRecordAmount(item.getProductPrice(), item.getTotalPrice()), (a, b) -> a));
        }
        return Collections.emptyMap();
    }

    private static class StockRecordAmount {
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;

        private StockRecordAmount(BigDecimal unitPrice, BigDecimal totalPrice) {
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }
    }

    /**
     * 若 reqVO 有产品维度条件，预查 productIds；无条件返回 null 表示不过滤
     */
    private Collection<Long> resolveProductIdFilter(ErpStockRecordPageReqVO reqVO) {
        boolean hasCondition = StringUtils.hasText(reqVO.getProductKeyword())
                || (reqVO.getProductCode() != null && !reqVO.getProductCode().isEmpty())
                || (reqVO.getProductName() != null && !reqVO.getProductName().isEmpty())
                || (reqVO.getVehicleModel() != null && !reqVO.getVehicleModel().isEmpty())
                || (reqVO.getOriginPlace() != null && !reqVO.getOriginPlace().isEmpty());
        if (!hasCondition) {
            return null;
        }
        QueryWrapper<ErpProductDO> w = new QueryWrapper<>();
        w.select("id");
        if (StringUtils.hasText(reqVO.getProductKeyword())) {
            ErpProductMapper.appendStockKeywordCondition(w.lambda(),
                    ErpProductMapper.fuzzyKeyword(reqVO.getProductKeyword()));
        }
        if (reqVO.getProductCode() != null && !reqVO.getProductCode().isEmpty()) {
            w.like("code", reqVO.getProductCode());
        }
        if (reqVO.getProductName() != null && !reqVO.getProductName().isEmpty()) {
            w.like("name", reqVO.getProductName());
        }
        if (reqVO.getVehicleModel() != null && !reqVO.getVehicleModel().isEmpty()) {
            w.like("vehicle_model", reqVO.getVehicleModel());
        }
        if (reqVO.getOriginPlace() != null && !reqVO.getOriginPlace().isEmpty()) {
            w.like("origin_place", reqVO.getOriginPlace());
        }
        List<Map<String, Object>> rows = productMapper.selectMaps(w);
        return rows.stream().map(m -> (Long) m.get("id")).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStockRecord(ErpStockRecordCreateReqBO createReqBO) {
        if (!dualCostPostingService.post(createReqBO, () -> createLegacyStockRecord(createReqBO))) {
            createLegacyStockRecord(createReqBO);
        }
    }

    private void createLegacyStockRecord(ErpStockRecordCreateReqBO createReqBO) {
        // 1. 决定业务发生日期：优先用 BO 的 bizDate，其次 LocalDateTime.now()
        LocalDateTime bizDate = createReqBO.getBizDate() != null ? createReqBO.getBizDate() : LocalDateTime.now();

        // 2. 决定本次业务单价：出库未传单价 → 取当前成本均价作为本次出库单价
        BigDecimal businessUnitPrice = createReqBO.getUnitPrice();
        if (createReqBO.getCount().compareTo(BigDecimal.ZERO) < 0 && businessUnitPrice == null) {
            ErpStockDO stock = stockService.getStock(createReqBO.getProductId(), createReqBO.getWarehouseId());
            businessUnitPrice = stock != null && stock.getCostPrice() != null ? stock.getCostPrice() : BigDecimal.ZERO;
        }

        // 3. 保留业务退款单价；新核算80的扣库成本由StockService在认证回调内按余额均价独立计算。
        ErpStockService.StockUpdateResult result = stockService.updateStockCountAndCost(
                createReqBO.getProductId(), createReqBO.getWarehouseId(),
                createReqBO.getCount(), createReqBO.getUnitPrice(), createReqBO.getBizType());

        // 4. 计算本次业务金额
        BigDecimal totalPrice = businessUnitPrice == null ? null
                : MoneyUtils.priceMultiply(businessUnitPrice, createReqBO.getCount());

        // 5. 计算结存金额
        BigDecimal costAmount = result.getCostAmount() != null ? result.getCostAmount()
                : (result.getCostPrice() == null ? BigDecimal.ZERO
                : MoneyUtils.priceMultiply(result.getCostPrice(), result.getTotalCount()));
        if (costAmount == null) {
            costAmount = BigDecimal.ZERO;
        }
        ErpProductDO product = DataPermissionUtils.executeIgnore(() -> productMapper.selectById(createReqBO.getProductId()));
        snapshotSupport.fillStockRecordSnapshot(createReqBO, product);

        // 6. 落流水
        ErpStockRecordDO stockRecord = BeanUtils.toBean(createReqBO, ErpStockRecordDO.class)
                .setBatchNo(StringUtils.hasText(createReqBO.getBatchNo()) ? createReqBO.getBatchNo().trim() : null)
                .setTotalCount(result.getTotalCount())
                .setUnitPrice(businessUnitPrice)
                .setTotalPrice(totalPrice)
                .setCostPrice(result.getCostPrice())
                .setCostAmount(costAmount)
                .setDeptId(resolveStockDeptId(createReqBO.getProductId(), createReqBO.getWarehouseId()))
                .setBizDate(bizDate);
        stockRecordMapper.insert(stockRecord);
    }

    private Long resolveStockDeptId(Long productId, Long warehouseId) {
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
        if (stock != null && stock.getDeptId() != null) {
            return stock.getDeptId();
        }
        ErpWarehouseDO warehouse = DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouse(warehouseId));
        return warehouse != null ? warehouse.getDeptId() : null;
    }

}
