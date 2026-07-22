package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "Admin - ERP warehouse move summary Response VO")
@Data
public class ErpWarehouseMoveSummaryRespVO {

    @Schema(description = "总单数")
    private Long totalRows;

    @Schema(description = "总项数")
    private Long totalItems;

    @Schema(description = "总移货数")
    private BigDecimal totalCount;

    @Schema(description = "总成本金额")
    private BigDecimal totalCostAmount;

    @Schema(description = "总移货金额")
    private BigDecimal totalPrice;

}
