package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售送货单 Response VO")
@Data
public class ErpSaleDeliveryOrderRespVO {

    private Long id;
    private Long saleOutId;
    private String saleOutNo;
    private Integer sourceType;
    private Long sourceId;
    private String sourceNo;
    private String displayNo;
    private Long customerId;
    private String customerName;
    private Long deptId;
    private Integer pickStatus;
    private Integer deliveryStatus;
    private Integer totalItemCount;
    private Integer pickedItemCount;
    private Integer deliveredItemCount;
    private BigDecimal totalPieceCount;
    private String pickProgress;
    private String deliveryProgress;
    private LocalDateTime latestPickTime;
    private LocalDateTime latestDeliveryTime;
    private LocalDateTime pickCompleteTime;
    private LocalDateTime deliveryCompleteTime;
    private LocalDateTime createTime;
    private List<ErpSalePickDeliveryItemRespVO> items;
    private List<ErpSalePickDeliverySubmitRespVO> submits;

}
