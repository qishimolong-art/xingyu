package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockAdjustReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    public PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO) {
        // 1. 预先处理"货架位重复/空置"特殊条件
        List<Long> shelfDuplicateIds = null;
        List<Long> shelfEmptyIds = null;
        if (Boolean.TRUE.equals(pageReqVO.getShelfDuplicateOnly())) {
            List<String> dupShelves = productMapper.selectDuplicateShelfValues();
            shelfDuplicateIds = dupShelves.isEmpty() ? Collections.emptyList()
                    : productMapper.selectIdsByShelfIn(dupShelves);
        }
        if (Boolean.TRUE.equals(pageReqVO.getShelfEmptyOnly())) {
            shelfEmptyIds = productMapper.selectIdsByEmptyShelf();
        }

        // 2. 若有任何产品维度条件，先按产品过滤拿 productIds
        Collection<Long> productIdFilter = null;
        if (hasProductCondition(pageReqVO)) {
            productIdFilter = productMapper.selectIdsByComplexQuery(pageReqVO, shelfDuplicateIds, shelfEmptyIds);
        }

        // 3. 查库存
        Collection<Long> warehouseIdFilter = null;
        if (pageReqVO.getDeptId() != null) {
            warehouseIdFilter = warehouseService.getWarehouseListByDeptId(pageReqVO.getDeptId()).stream()
                    .map(ErpWarehouseDO::getId)
                    .collect(Collectors.toList());
        }
        return stockMapper.selectPage(pageReqVO, productIdFilter, warehouseIdFilter);
    }

    private boolean hasProductCondition(ErpStockPageReqVO v) {
        return v.getProductCode() != null || v.getProductName() != null
                || v.getDrawingNo() != null || v.getVehicleModel() != null
                || v.getOriginPlace() != null || v.getBrand() != null
                || v.getShelf() != null || v.getFeatureCode() != null
                || v.getStandard() != null || v.getFactoryCode() != null
                || v.getBarCode() != null || v.getOeNumber() != null
                || v.getCategoryId() != null || v.getProductStatus() != null
                || v.getStockMaxMin() != null || v.getStockMaxMax() != null
                || v.getStockMinMin() != null || v.getStockMinMax() != null
                || v.getStockStandardMin() != null || v.getStockStandardMax() != null
                || Boolean.TRUE.equals(v.getShelfDuplicateOnly())
                || Boolean.TRUE.equals(v.getShelfEmptyOnly());
    }

    @Override
    public BigDecimal updateStockCountIncrement(Long productId, Long warehouseId, BigDecimal count) {
        // 1.1 查询当前库存
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock == null) {
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId)
                    .setDeptId(resolveWarehouseDeptId(warehouseId))
                    .setCount(BigDecimal.ZERO);
            stockMapper.insert(stock);
        }
        // 1.2 校验库存是否充足
        if (!NEGATIVE_STOCK_COUNT_ENABLE && stock.getCount().add(count).compareTo(BigDecimal.ZERO) < 0) {
            throw exception(STOCK_COUNT_NEGATIVE, productService.getProduct(productId).getName(),
                    warehouseService.getWarehouse(warehouseId).getName(), stock.getCount(), count);
        }

        // 2. 库存变更
        int updateCount = stockMapper.updateCountIncrement(stock.getId(), count, NEGATIVE_STOCK_COUNT_ENABLE);
        if (updateCount == 0) {
            // 此时不好去查询最新库存，所以直接抛出该提示，不提供具体库存数字
            throw exception(STOCK_COUNT_NEGATIVE2, productService.getProduct(productId).getName(),
                    warehouseService.getWarehouse(warehouseId).getName());
        }

        // 3. 返回最新库存
        return stock.getCount().add(count);
    }

    @Override
    public StockUpdateResult updateStockCountAndCost(Long productId, Long warehouseId,
                                                    BigDecimal count, BigDecimal unitPrice) {
        // 0. 入库必须有单价（出库允许 null，由上层按当前成本均价回填至流水）
        if (count.compareTo(BigDecimal.ZERO) > 0 && unitPrice == null) {
            throw new IllegalArgumentException("入库时 unitPrice 不能为空");
        }

        // 1. 查询当前库存；若不存在则初始化
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock == null) {
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId)
                    .setDeptId(resolveWarehouseDeptId(warehouseId))
                    .setCount(BigDecimal.ZERO)
                    .setCostPrice(BigDecimal.ZERO)
                    .setCostAmount(BigDecimal.ZERO);
            stockMapper.insert(stock);
        }

        // 2. 乐观锁循环重试
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            BigDecimal oldCount = stock.getCount() != null ? stock.getCount() : BigDecimal.ZERO;
            BigDecimal oldCost = stock.getCostPrice() != null ? stock.getCostPrice() : BigDecimal.ZERO;

            // 2.1 校验库存是否充足
            if (!NEGATIVE_STOCK_COUNT_ENABLE && oldCount.add(count).compareTo(BigDecimal.ZERO) < 0) {
                throw exception(STOCK_COUNT_NEGATIVE, productService.getProduct(productId).getName(),
                        warehouseService.getWarehouse(warehouseId).getName(), oldCount, count);
            }

            // 2.2 零增量：直接返回
            if (count.compareTo(BigDecimal.ZERO) == 0) {
                return new StockUpdateResult(oldCount, oldCost);
            }

            BigDecimal newCount = oldCount.add(count);
            BigDecimal newCost;
            if (count.compareTo(BigDecimal.ZERO) > 0) {
                // 入库：移动加权平均
                if (newCount.compareTo(BigDecimal.ZERO) == 0) {
                    newCost = BigDecimal.ZERO;
                } else {
                    BigDecimal totalCostAmount = oldCount.multiply(oldCost).add(count.multiply(unitPrice));
                    newCost = totalCostAmount.divide(newCount, COST_PRICE_SCALE, RoundingMode.HALF_UP);
                }
            } else {
                // 出库：成本均价不变；若全部出光，归零
                if (newCount.compareTo(BigDecimal.ZERO) == 0) {
                    newCost = BigDecimal.ZERO;
                } else {
                    newCost = oldCost;
                }
            }

            // 2.3 结存金额
            BigDecimal newAmount = MoneyUtils.priceMultiply(newCost, newCount);
            if (newAmount == null) {
                newAmount = BigDecimal.ZERO;
            }

            // 2.4 原子更新（乐观锁）
            int updateCount = stockMapper.updateCountAndCost(stock.getId(), oldCount, newCount,
                    newCost, newAmount, NEGATIVE_STOCK_COUNT_ENABLE);
            if (updateCount == 1) {
                return new StockUpdateResult(newCount, newCost);
            }

            // 2.5 冲突：重新读取再试
            ErpStockDO latest = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
            if (latest != null) {
                stock = latest;
            }
        }

        // 3. 重试耗尽
        throw exception(STOCK_COUNT_NEGATIVE2, productService.getProduct(productId).getName(),
                warehouseService.getWarehouse(warehouseId).getName());
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
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock == null) {
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId)
                    .setDeptId(resolveWarehouseDeptId(warehouseId))
                    .setCount(BigDecimal.ZERO)
                    .setCostPrice(BigDecimal.ZERO)
                    .setCostAmount(BigDecimal.ZERO);
            stockMapper.insert(stock);
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
            int affected = stockMapper.updateCostAmountAndPrice(stock.getId(), currentCount, currentAmount,
                    newCostAmount, newCostPrice);
            if (affected == 1) {
                success = true;
                break;
            }
            // 2.5 冲突：重读再算
            ErpStockDO latest = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
            if (latest != null) {
                stock = latest;
            }
        }
        if (!success) {
            // 理论上极少达到；此处复用 STOCK_COUNT_NEGATIVE2 语义（并发冲突无法完成）
            throw exception(STOCK_COUNT_NEGATIVE2, productService.getProduct(productId).getName(),
                    warehouseService.getWarehouse(warehouseId).getName());
        }

        // 3. 手工写流水（ErpStockRecordServiceImpl.createStockRecord 内部会调用 updateStockCountAndCost，
        //    但我们既不能让它改数量，也不想它重算成本均价，因此直接插入流水 DO）
        ErpStockRecordDO record = new ErpStockRecordDO()
                .setProductId(productId)
                .setWarehouseId(warehouseId)
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

    private Long resolveWarehouseDeptId(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(warehouseId);
        return warehouse != null ? warehouse.getDeptId() : null;
    }

}
