package cn.iocoder.yudao.module.erp.service.stock;

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

/**
 * ERP 库存占用 Service 实现类
 *
 * @author 汽配ERP
 */
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
        // 1. 校验可用库存
        BigDecimal availableStock = getAvailableStock(productId, warehouseId);
        if (availableStock.compareTo(count) < 0) {
            throw exception(STOCK_LOCK_AVAILABLE_COUNT_NOT_ENOUGH,
                    productId, warehouseId, availableStock, count);
        }
        // 2. 创建锁定记录
        ErpStockLockDO lockDO = ErpStockLockDO.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .lockCount(count)
                .bizType(bizType)
                .bizId(bizId)
                .bizItemId(bizItemId)
                .bizNo(bizNo)
                .status(1) // 锁定中
                .build();
        stockLockMapper.insert(lockDO);
        // 3. 更新库存表的锁定数量
        updateStockLockCount(productId, warehouseId, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockStock(Integer bizType, Long bizId) {
        List<ErpStockLockDO> lockList = stockLockMapper.selectListByBiz(bizType, bizId);
        for (ErpStockLockDO lock : lockList) {
            lock.setStatus(2); // 已释放
            stockLockMapper.updateById(lock);
            // 减少库存表的锁定数量
            updateStockLockCount(lock.getProductId(), lock.getWarehouseId(), lock.getLockCount().negate());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Integer bizType, Long bizId) {
        List<ErpStockLockDO> lockList = stockLockMapper.selectListByBiz(bizType, bizId);
        for (ErpStockLockDO lock : lockList) {
            lock.setStatus(3); // 已扣减
            stockLockMapper.updateById(lock);
            // 减少库存表的锁定数量（实际库存扣减由 ErpStockService 处理）
            updateStockLockCount(lock.getProductId(), lock.getWarehouseId(), lock.getLockCount().negate());
        }
    }

    @Override
    public BigDecimal getAvailableStock(Long productId, Long warehouseId) {
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal lockCount = stock.getLockCount() != null ? stock.getLockCount() : BigDecimal.ZERO;
        return stock.getCount().subtract(lockCount);
    }

    private void updateStockLockCount(Long productId, Long warehouseId, BigDecimal deltaCount) {
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock != null) {
            BigDecimal currentLock = stock.getLockCount() != null ? stock.getLockCount() : BigDecimal.ZERO;
            stock.setLockCount(currentLock.add(deltaCount));
            stockMapper.updateById(stock);
        }
    }

}
