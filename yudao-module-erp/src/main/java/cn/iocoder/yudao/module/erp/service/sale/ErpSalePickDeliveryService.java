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

    void generateForSaleOut(Long saleOutId);

    PageResult<ErpSalePickTaskRespVO> getPickPage(ErpSalePickPageReqVO reqVO, boolean mobile);

    default ErpSalePickTaskRespVO getPick(Long id, boolean mobile) {
        return getPick(id, mobile, true);
    }

    ErpSalePickTaskRespVO getPick(Long id, boolean mobile, boolean includeDetail);

    PageResult<ErpSalePickDeliveryItemRespVO> getPickItemPage(Long taskId, PageParam pageParam, boolean mobile);

    PageResult<ErpSalePickDeliverySubmitRespVO> getPickSubmitPage(Long taskId, PageParam pageParam, boolean mobile);

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

    ErpSaleOutPickDeliveryDetailRespVO getSaleOutPickDeliveryDetail(Long saleOutId);

    PageResult<ErpSalePickDeliveryItemRespVO> getSaleOutPickDeliveryItemPage(Long saleOutId, PageParam pageParam);

    PageResult<ErpSaleOutPickDeliveryFileRespVO> getSaleOutPickDeliveryFilePage(Long saleOutId, Integer type,
                                                                                PageParam pageParam);

}
