package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSaleDeliveryOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickTaskRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 销售单拣货送货详情 Response VO")
@Data
public class ErpSaleOutPickDeliveryDetailRespVO {

    private ErpSalePickDeliverySummaryRespVO summary;

    private ErpSaleDeliveryOrderRespVO deliveryOrder;

    private List<ErpSalePickTaskRespVO> pickTasks;

    private List<ErpSalePickDeliverySubmitRespVO> submits;

}
