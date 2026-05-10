package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockAdjustReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
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
    private ErpProductMapper productMapper;

    /**
     * 库存流水 Service。
     *
     * <p>注意：{@link ErpStockRecordServiceImpl} 反向注入了本类 {@link ErpStockService}，为避免 Spring
     * 循环依赖导致启动失败，这里使用 {@link Lazy} 延迟解析代理，在实际调用时才创建目标 Bean。</p>
     */
    @Resource
    @Lazy
    private ErpStockRecordService stockRecordService;

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
        return stockMapper.selectPage(pageReqVO, productIdFilter);
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
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId).setCount(BigDecimal.ZERO);
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
        // 1. 查当前库存
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(reqVO.getProductId(), reqVO.getWarehouseId());
        BigDecimal currentCount = (stock != null && stock.getCount() != null) ? stock.getCount() : BigDecimal.ZERO;
        BigDecimal targetCount = reqVO.getTargetCount();

        // 2. 计算差值：== 0 直接返回，无需落流水
        BigDecimal diff = targetCount.subtract(currentCount);
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return currentCount;
        }

        // 3. 构造调整单号与备注
        String bizNo = "ADJ" + System.currentTimeMillis();
        String recordRemark = reqVO.getReason() != null ? reqVO.getReason() : "库存调整";
        if (reqVO.getRemark() != null && !reqVO.getRemark().isEmpty()) {
            recordRemark = recordRemark + "：" + reqVO.getRemark();
        }

        // 4. 根据 diff 正负决定业务类型与单价
        Integer bizType;
        BigDecimal unitPrice;
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            // 盘盈入库：单价取产品资料的最近采购价，缺省为 0
            bizType = ErpStockRecordBizTypeEnum.CHECK_MORE_IN.getType();
            ErpProductDO product = productService.getProduct(reqVO.getProductId());
            unitPrice = (product != null && product.getLastPurchasePrice() != null)
                    ? product.getLastPurchasePrice() : BigDecimal.ZERO;
        } else {
            // 盘亏出库：单价为 null，由下游 createStockRecord 使用当前成本均价出账
            bizType = ErpStockRecordBizTypeEnum.CHECK_LESS_OUT.getType();
            unitPrice = null;
        }

        // 5. 调用一期库存流水服务（9 参构造，bizId / bizItemId 传 0 表示无单据）
        ErpStockRecordCreateReqBO bo = new ErpStockRecordCreateReqBO(
                reqVO.getProductId(),
                reqVO.getWarehouseId(),
                diff,
                bizType,
                0L,
                0L,
                bizNo,
                unitPrice,
                LocalDateTime.now()
        );
        stockRecordService.createStockRecord(bo);

        // 6. 返回调整后库存
        return targetCount;
    }

}
