package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockAdjustReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockBatchNoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ERP 产品库存 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockService {

    /**
     * 获得产品库存
     *
     * @param id 编号
     * @return 库存
     */
    ErpStockDO getStock(Long id);

    /**
     * 基于产品 + 仓库，获得产品库存
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @return 产品库存
     */
    ErpStockDO getStock(Long productId, Long warehouseId);

    /**
     * 获得产品库存数量
     *
     * 如果不存在库存记录，则返回 0
     *
     * @param productId 产品编号
     * @return 产品库存数量
     */
    BigDecimal getStockCount(Long productId);

    BigDecimal getStockCount(Long productId, Long warehouseId);

    /**
     * 鑾峰緱浜у搧鍦ㄦ寚瀹氫粨搴撶殑鍙敤鎵规鍙峰垪琛?
     *
     * @param productId 浜у搧缂栧彿
     * @param warehouseId 浠撳簱缂栧彿
     * @return 鎵规鍙峰垪琛?
     */
    List<ErpStockBatchNoRespVO> getAvailableBatchNoList(Long productId, Long warehouseId);

    /**
     * 批量获得产品在指定仓库的可用批次号列表
     *
     * @param stocks 库存记录集合
     * @return key：productId_warehouseId；value：批次号列表
     */
    Map<String, List<ErpStockBatchNoRespVO>> getAvailableBatchNoListMap(Collection<ErpStockDO> stocks);

    /**
     * 按库存流水汇总产品、仓库下的批次余额，用于产品库存批次展开视图。
     *
     * @param stocks 已通过库存查看权限过滤的库存记录
     * @return key：productId_warehouseId；value：流水批次余额列表
     */
    Map<String, List<ErpStockBatchNoRespVO>> getStockBatchBalanceListMap(Collection<ErpStockDO> stocks);

    /**
     * 批量获得产品库存数量（所有仓库合计）
     *
     * @param productIds 产品编号集合
     * @return Map&lt;productId, sum(count)&gt;
     */
    Map<Long, BigDecimal> getStockCountMap(Collection<Long> productIds);

    /**
     * 批量获取产品占用数量
     *
     * @param productIds 产品编号集合
     * @return Map<productId, sum(lockCount)>
     */
    Map<Long, BigDecimal> getStockLockCountMap(Collection<Long> productIds);

    /**
     * Get dynamically occupied quantities by product and warehouse.
     *
     * @return key: productId_warehouseId, value: occupied quantity
     */
    Map<String, BigDecimal> getOccupiedCountMap(Collection<Long> productIds, Collection<Long> warehouseIds);

    /**
     * 获得产品库存分页
     *
     * @param pageReqVO 分页查询
     * @return 库存分页
     */
    PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO);

    /**
     * 获得当前查询条件下的产品库存汇总。
     *
     * @param pageReqVO 查询条件
     * @return 库存汇总
     */
    ErpStockSummaryRespVO getStockSummary(ErpStockPageReqVO pageReqVO);

    /**
     * Ensures the product/warehouse stock row exists without changing its quantity or cost.
     */
    void ensureStockExists(Long productId, Long warehouseId);

    /**
     * Update shelf locations by stock row.
     *
     * @param stockIds stock row ids
     * @param shelf shelf location, blank values clear the shelf
     */
    void updateStockShelf(Collection<Long> stockIds, String shelf);

    /**
     * Update editable stock-row fields.
     *
     * @param reqVO update request
     * @return updated stock row
     */
    ErpStockDO updateStockEditableFields(ErpStockUpdateReqVO reqVO);

    /**
     * 增量更新产品库存数量
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @param count 增量数量：正数，表示增加；负数，表示减少
     * @return 更新后的库存
     */
    BigDecimal updateStockCountIncrement(Long productId, Long warehouseId, BigDecimal count);

    /**
     * 增量更新产品库存数量 + 成本（移动加权平均）
     *
     * @param productId 产品编号
     * @param warehouseId 仓库编号
     * @param count 增量数量：正数入库，负数出库
     * @param unitPrice 本次单价；入库必填（进价），出库可为 null
     * @return StockUpdateResult(totalCount, costPrice) — 更新后的库存量与成本均价
     */
    StockUpdateResult updateStockCountAndCost(Long productId, Long warehouseId, BigDecimal count,
                                              BigDecimal unitPrice, Integer bizType);

    /**
     * 手动调整库存数量（生成一条盘点调整流水）
     *
     * <p>语义：</p>
     * <ul>
     *   <li>调整值 &gt; 当前库存：差值做盘盈入库（bizType=40），unitPrice 取产品资料 lastPurchasePrice 或 0</li>
     *   <li>调整值 &lt; 当前库存：差值做盘亏出库（bizType=42），unitPrice 传 null，由流水 Service 使用当前成本均价出账</li>
     *   <li>调整值 == 当前库存：不生成流水，直接返回当前值</li>
     * </ul>
     *
     * @param reqVO 调整请求
     * @return 调整后的库存量
     */
    BigDecimal adjustStock(ErpStockAdjustReqVO reqVO);

    /**
     * 调整库存成本金额（采购调价专用）
     *
     * <p>语义：不改库存数量，只补差 cost_amount。当前在库量 &lt; 原入库量时按比例摊分，
     * 即只摊到仍在库的那部分。当前在库超过原入库也不放大（比例上限为 1）。</p>
     *
     * <p>乐观锁重试最多 5 次；同时写入一条 PURCHASE_PRICE_ADJUST 流水。</p>
     *
     * @param productId           产品编号
     * @param warehouseId         仓库编号
     * @param deltaCostAmountFull 完整差额 Σ (newPrice - oldPrice) × count
     * @param sumInCount          本次调价涉及的入库总数量
     * @param bizId               调价单 ID
     * @param bizNo               调价单号
     * @param bizDate             调价时间
     */
    void adjustStockCostAmount(Long productId, Long warehouseId,
                               BigDecimal deltaCostAmountFull, BigDecimal sumInCount,
                               Long bizId, String bizNo, LocalDateTime bizDate);

    /**
     * 直接设置库存成本价，成本金额按当前库存数量重算。
     */
    StockUpdateResult updateStockCostPrice(Long productId, Long warehouseId, BigDecimal costPrice);

    /**
     * 库存更新结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    class StockUpdateResult {
        /** 变更后总库存 */
        private BigDecimal totalCount;
        /** 变更后成本均价（如果是出库或 oldQty+inQty=0，则返回变更前的 costPrice；如果全部出光，返回 BigDecimal.ZERO） */
        private BigDecimal costPrice;
        private BigDecimal costAmount;
    }

}
