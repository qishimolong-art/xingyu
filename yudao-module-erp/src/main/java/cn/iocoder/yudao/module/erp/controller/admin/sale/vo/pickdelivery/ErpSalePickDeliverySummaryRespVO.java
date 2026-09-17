package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 销售单拣货送货汇总 Response VO")
@Data
public class ErpSalePickDeliverySummaryRespVO {

    private Long orderId;
    private Integer pickStatus;
    private Integer deliveryStatus;
    private Integer totalItemCount;
    private Integer pickedItemCount;
    private Integer deliveredItemCount;
    private String pickProgress;
    private String deliveryProgress;
    private LocalDateTime latestPickTime;
    private LocalDateTime latestDeliveryTime;
    private LocalDateTime pickCompleteTime;
    private LocalDateTime deliveryCompleteTime;

}
