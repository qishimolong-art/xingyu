package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockAdjustReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;

import java.math.BigDecimal;

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

    /**
     * 获得产品库存分页
     *
     * @param pageReqVO 分页查询
     * @return 库存分页
     */
    PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO);

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
    StockUpdateResult updateStockCountAndCost(Long productId, Long warehouseId, BigDecimal count, BigDecimal unitPrice);

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
     * 库存更新结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    class StockUpdateResult {
        /** 变更后总库存 */
        private BigDecimal totalCount;
        /** 变更后成本均价（如果是出库或 oldQty+inQty=0，则返回变更前的 costPrice；如果全部出光，返回 BigDecimal.ZERO） */
        private BigDecimal costPrice;
    }

}