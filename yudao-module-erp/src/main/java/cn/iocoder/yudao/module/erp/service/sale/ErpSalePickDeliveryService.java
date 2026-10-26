package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutExpressFileRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryFileRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryOperationItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ErpSalePickDeliveryService {

    /** 汇总当前销售单送货操作人，用于打印（兼容历史送货明细）。 */
    String getSaleOutDeliveryUserNames(Long saleOutId);

    void generateForSaleOut(Long saleOutId);

    void generateForSaleCart(Long saleCartId);

    void generateForSaleReturn(Long saleReturnId);

    void generateForWarehouseMove(Long warehouseMoveId);

    /**
     * 为已经直接完成的系统移货单补齐一张同步完成的拣货单。
     *
     * <p>套件组装跨仓移货需要在“完成装配”事务内立即影响库存，不能等待人工拣货；
     * 但仍需保留与普通仓库移货一致的拣货业务记录和明细。</p>
     */
    void generateCompletedForWarehouseMove(Long warehouseMoveId, Long pickUserId);

    void generateForStockIn(Long stockInId);

    void generateForStockOut(Long stockOutId);

    void rollbackUnstartedSaleReturnFulfillment(Long saleReturnId);

    void rollbackUnstartedWarehouseMoveFulfillment(Long warehouseMoveId);

    void rollbackUnstartedStockInFulfillment(Long stockInId);

    void rollbackUnstartedStockOutFulfillment(Long stockOutId);

    /**
     * 回退销售手推车尚未开始的拣货送货任务。
     *
     * <p>仅供调拨出库单“解锁手推车”使用；任务已经产生任何拣货或送货进度时拒绝回退。</p>
     */
    void rollbackUnstartedSaleCartFulfillment(Long saleCartId);

    /** 兼容旧调用名称；新流程按销售手推车整单生成。 */
    default void generateForSaleCartTransferOuts(Long saleCartId) {
        generateForSaleCart(saleCartId);
    }

    PageResult<ErpSalePickTaskRespVO> getPickPage(ErpSalePickPageReqVO reqVO, boolean mobile);

    default ErpSalePickTaskRespVO getPick(Long id, boolean mobile) {
        return getPick(id, mobile, true);
    }

    ErpSalePickTaskRespVO getPick(Long id, boolean mobile, boolean includeDetail);

    PageResult<ErpSalePickDeliveryItemRespVO> getPickItemPage(Long taskId, PageParam pageParam, boolean mobile);

    PageResult<ErpSalePickDeliverySubmitRespVO> getPickSubmitPage(Long taskId, PageParam pageParam, boolean mobile);

    PageResult<ErpSalePickDeliverySubmitItemRespVO> getSubmitItemPage(Long parentId, Long submitId,
                                                                    boolean pick, PageParam pageParam);

    void submitPick(ErpSalePickSubmitReqVO reqVO);

    PageResult<ErpSaleDeliveryOrderRespVO> getDeliveryPage(ErpSaleDeliveryPageReqVO reqVO, boolean mobile);

    default ErpSaleDeliveryOrderRespVO getDelivery(Long id, boolean mobile) {
        return getDelivery(id, mobile, true);
    }

    ErpSaleDeliveryOrderRespVO getDelivery(Long id, boolean mobile, boolean includeDetail);

    PageResult<ErpSalePickDeliveryItemRespVO> getDeliveryItemPage(Long taskId, PageParam pageParam, boolean mobile);

    PageResult<ErpSalePickDeliverySubmitRespVO> getDeliverySubmitPage(Long taskId, PageParam pageParam, boolean mobile);

    void submitDelivery(ErpSaleDeliverySubmitReqVO reqVO);

    String uploadVoucher(byte[] content, String fileName) throws IOException;

    Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapBySaleOutIds(Collection<Long> saleOutIds);

    /** 批量查询存在送货快递单的销售单编号，供列表颜色判断使用。 */
    Set<Long> getSaleOutIdsWithDeliveryFiles(Collection<Long> saleOutIds);

    /** 合并人工补传主图与送货阶段快递单，并按 URL 去重后分页。 */
    PageResult<ErpSaleOutExpressFileRespVO> getSaleOutExpressFilePage(Long saleOutId, PageParam pageParam);

    Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapBySaleCartIds(Collection<Long> saleCartIds);

    Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapBySaleReturnIds(Collection<Long> saleReturnIds);

    Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapByWarehouseMoveIds(Collection<Long> warehouseMoveIds);

    Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapByStockInIds(Collection<Long> stockInIds);

    Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapByStockOutIds(Collection<Long> stockOutIds);

    ErpSalePickSourceDetailRespVO getSourcePickDetail(Integer sourceType, Long sourceId);

    PageResult<ErpSalePickDeliveryItemRespVO> getSourcePickItemPage(
            Integer sourceType, Long sourceId, PageParam pageParam);

    PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> getSourcePickOperationItemPage(
            Integer sourceType, Long sourceId, PageParam pageParam);

    default ErpSalePickDeliverySummaryRespVO getSummaryBySaleCartId(Long saleCartId) {
        return saleCartId == null ? null : getSummaryMapBySaleCartIds(java.util.Collections.singleton(saleCartId))
                .get(saleCartId);
    }

    ErpSaleOutPickDeliveryDetailRespVO getSaleOutPickDeliveryDetail(Long saleOutId);

    PageResult<ErpSalePickDeliveryItemRespVO> getSaleOutPickDeliveryItemPage(Long saleOutId, PageParam pageParam);

    PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> getSaleOutPickDeliveryOperationItemPage(
            Long saleOutId, Integer type, PageParam pageParam);

    PageResult<ErpSaleOutPickDeliveryFileRespVO> getSaleOutPickDeliveryFilePage(Long saleOutId, Integer type,
                                                                                PageParam pageParam);

}
