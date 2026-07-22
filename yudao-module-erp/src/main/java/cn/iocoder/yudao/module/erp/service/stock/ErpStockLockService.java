package cn.iocoder.yudao.module.erp.service.stock;

import java.math.BigDecimal;

/**
 * ERP 库存占用 Service 接口
 *
 * @author 汽配ERP
 */
public interface ErpStockLockService {

    /**
     * 锁定库存
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @param count 锁定数量
     * @param bizType 业务类型
     * @param bizId 业务单据ID
     * @param bizItemId 业务单据项ID
     * @param bizNo 业务单号
     */
    void lockStock(Long productId, Long warehouseId, BigDecimal count,
                   Integer bizType, Long bizId, Long bizItemId, String bizNo);

    /**
     * 释放库存（取消订单时）
     *
     * @param bizType 业务类型
     * @param bizId 业务单据ID
     */
    void unlockStock(Integer bizType, Long bizId);

    /**
     * 扣减库存（审核出库时，将锁定转为实际扣减）
     *
     * @param bizType 业务类型
     * @param bizId 业务单据ID
     */
    void deductStock(Integer bizType, Long bizId);

    void transferStockLocks(Integer bizType, Long bizId, Long productId,
                            Long fromWarehouseId, Long toWarehouseId);

    /**
     * 获取可用库存 = 实际库存 - 锁定库存
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @return 可用库存
     */
    BigDecimal getAvailableStock(Long productId, Long warehouseId);

}
