package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售拣货单 Response VO")
@Data
public class ErpSalePickTaskRespVO {

    private Long id;
    private Long orderId;
    private Long saleOutId;
    private String saleOutNo;
    private Long customerId;
    private String customerName;
    private Long warehouseId;
    private String warehouseName;
    private Integer status;
    private Integer totalItemCount;
    private Integer pickedItemCount;
    private BigDecimal totalPieceCount;
    private String pickProgress;
    private LocalDateTime latestPickTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private List<ErpSalePickDeliveryItemRespVO> items;
    private List<ErpSalePickDeliverySubmitRespVO> submits;

}
