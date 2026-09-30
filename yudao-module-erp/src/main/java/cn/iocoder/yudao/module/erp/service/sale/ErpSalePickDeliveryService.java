package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryFileRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface ErpSalePickDeliveryService {

    /** 汇总当前销售单送货操作人，用于打印（兼容历史送货明细）。 */
    String getSaleOutDeliveryUserNames(Long saleOutId);

    void generateForSaleOut(Long saleOutId);

    void generateForSaleCart(Long saleCartId);

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

    PageResult<ErpSalePickDeliveryItemRespVO> getDeliveryItemPage(Long orderId, PageParam pageParam, boolean mobile);

    PageResult<ErpSalePickDeliverySubmitRespVO> getDeliverySubmitPage(Long orderId, PageParam pageParam, boolean mobile);

    void submitDelivery(ErpSaleDeliverySubmitReqVO reqVO);

    String uploadVoucher(byte[] content, String fileName) throws IOException;

    Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapBySaleOutIds(Collection<Long> saleOutIds);

    Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapBySaleCartIds(Collection<Long> saleCartIds);

    default ErpSalePickDeliverySummaryRespVO getSummaryBySaleCartId(Long saleCartId) {
        return saleCartId == null ? null : getSummaryMapBySaleCartIds(java.util.Collections.singleton(saleCartId))
                .get(saleCartId);
    }

    ErpSaleOutPickDeliveryDetailRespVO getSaleOutPickDeliveryDetail(Long saleOutId);

    PageResult<ErpSalePickDeliveryItemRespVO> getSaleOutPickDeliveryItemPage(Long saleOutId, PageParam pageParam);

    PageResult<ErpSaleOutPickDeliveryFileRespVO> getSaleOutPickDeliveryFilePage(Long saleOutId, Integer type,
                                                                                PageParam pageParam);

}
