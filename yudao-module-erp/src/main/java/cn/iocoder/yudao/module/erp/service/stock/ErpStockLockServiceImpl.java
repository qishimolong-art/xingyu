package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_LOCK_AVAILABLE_COUNT_NOT_ENOUGH;

/** ERP 库存占用 Service 实现类。 */
@Service
@Validated
public class ErpStockLockServiceImpl implements ErpStockLockService {

    @Resource
    private ErpStockLockMapper stockLockMapper;
    @Resource
    private ErpStockMapper stockMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(Long productId, Long warehouseId, BigDecimal count,
                          Integer bizType, Long bizId, Long bizItemId, String bizNo) {
        // 同一业务明细重复调用直接返回，避免重复增加 lock_count。
        if (stockLockMapper.selectActiveByBizItem(bizType, bizId, bizItemId) != null) {
            return;
        }
        // 仓库使用权限已由上游业务校验；库存归属部门可能与销售部门不同，因此库存操作忽略部门数据权限。
        // DataPermissionUtils 只关闭数据权限，不会关闭 tenant_id 租户隔离。
        ErpStockDO stock = DataPermissionUtils.executeIgnore(
                () -> stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId));
        BigDecimal availableStock = stock == null ? BigDecimal.ZERO
                : safeCount(stock.getCount()).subtract(safeCount(stock.getLockCount()));
        // 库存校验与 lock_count 增加在一条条件更新中完成，防止并发超占。
        int updateCount = stock == null ? 0 : DataPermissionUtils.executeIgnore(
                () -> stockMapper.tryIncreaseLockCount(stock.getId(), count));
        if (updateCount == 0) {
            throw exception(STOCK_LOCK_AVAILABLE_COUNT_NOT_ENOUGH,
                    productId, warehouseId, availableStock, count);
        }
        stockLockMapper.insert(ErpStockLockDO.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .lockCount(count)
                .bizType(bizType)
                .bizId(bizId)
                .bizItemId(bizItemId)
                .bizNo(bizNo)
                .status(1)
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockStock(Integer bizType, Long bizId) {
        changeActiveLocksStatus(bizType, bizId, 2);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Integer bizType, Long bizId) {
        changeActiveLocksStatus(bizType, bizId, 3);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferStockLocks(Integer bizType, Long bizId, Long productId,
                                   Long fromWarehouseId, Long toWarehouseId) {
        if (bizType == null || bizId == null || productId == null
                || fromWarehouseId == null || toWarehouseId == null
                || fromWarehouseId.equals(toWarehouseId)) {
            return;
        }
        List<ErpStockLockDO> lockList = stockLockMapper.selectListByBiz(bizType, bizId);
        for (ErpStockLockDO lock : lockList) {
            if (!productId.equals(lock.getProductId()) || !fromWarehouseId.equals(lock.getWarehouseId())) {
                continue;
            }
            if (stockLockMapper.updateWarehouseIfActive(lock.getId(), fromWarehouseId, toWarehouseId) == 0) {
                continue;
            }
            transferLockCount(lock, fromWarehouseId, toWarehouseId);
        }
    }

    @Override
    public BigDecimal getAvailableStock(Long productId, Long warehouseId) {
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        return stock == null ? BigDecimal.ZERO
                : safeCount(stock.getCount()).subtract(safeCount(stock.getLockCount()));
    }

    private void changeActiveLocksStatus(Integer bizType, Long bizId, Integer targetStatus) {
        List<ErpStockLockDO> lockList = stockLockMapper.selectListByBiz(bizType, bizId);
        for (ErpStockLockDO lock : lockList) {
            // 条件状态更新抢占处理权，重复释放、重复核销均不会再次减少 lock_count。
            if (stockLockMapper.updateStatusIfActive(lock.getId(), targetStatus) == 0) {
                continue;
            }
            ErpStockDO stock = DataPermissionUtils.executeIgnore(() -> stockMapper.selectByProductIdAndWarehouseId(
                    lock.getProductId(), lock.getWarehouseId()));
            int updateCount = stock == null ? 0 : DataPermissionUtils.executeIgnore(
                    () -> stockMapper.tryDecreaseLockCount(stock.getId(), lock.getLockCount()));
            if (updateCount == 0) {
                throw new IllegalStateException("库存锁定数异常，无法变更库存占用状态: " + lock.getId());
            }
        }
    }

    private void transferLockCount(ErpStockLockDO lock, Long fromWarehouseId, Long toWarehouseId) {
        ErpStockDO fromStock = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectByProductIdAndWarehouseId(lock.getProductId(), fromWarehouseId));
        int decreaseCount = fromStock == null ? 0 : DataPermissionUtils.executeIgnore(
                () -> stockMapper.tryDecreaseLockCount(fromStock.getId(), lock.getLockCount()));
        if (decreaseCount == 0) {
            throw new IllegalStateException("原仓库库存锁定数异常，无法迁移库存占用：" + lock.getId());
        }

        ErpStockDO toStock = DataPermissionUtils.executeIgnore(() ->
                stockMapper.selectByProductIdAndWarehouseId(lock.getProductId(), toWarehouseId));
        BigDecimal availableStock = toStock == null ? BigDecimal.ZERO
                : safeCount(toStock.getCount()).subtract(safeCount(toStock.getLockCount()));
        int increaseCount = toStock == null ? 0 : DataPermissionUtils.executeIgnore(
                () -> stockMapper.tryIncreaseLockCount(toStock.getId(), lock.getLockCount()));
        if (increaseCount == 0) {
            throw exception(STOCK_LOCK_AVAILABLE_COUNT_NOT_ENOUGH,
                    lock.getProductId(), toWarehouseId, availableStock, lock.getLockCount());
        }
    }

    private static BigDecimal safeCount(BigDecimal count) {
        return count == null ? BigDecimal.ZERO : count;
    }

}
