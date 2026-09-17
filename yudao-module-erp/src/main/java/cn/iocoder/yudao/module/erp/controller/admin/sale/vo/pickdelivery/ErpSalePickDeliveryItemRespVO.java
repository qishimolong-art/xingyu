package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 销售拣货送货明细 Response VO")
@Data
public class ErpSalePickDeliveryItemRespVO {

    @Schema(description = "编号")
    private Long id;
    private Long saleOutItemId;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productCode;
    private String productName;
    private String standard;
    private BigDecimal count;
    private String warehousePosition;
    private Integer packageQty;
    private BigDecimal pieceCount;
    private Integer pickStatus;
    private Long pickUserId;
    private String pickUserName;
    private LocalDateTime pickTime;
    private Integer deliveryStatus;
    private Long deliveryUserId;
    private String deliveryUserName;
    private LocalDateTime deliveryTime;

}
