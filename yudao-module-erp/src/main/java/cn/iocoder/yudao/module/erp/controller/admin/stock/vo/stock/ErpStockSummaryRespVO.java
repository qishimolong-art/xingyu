package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 产品库存汇总 Response VO")
@Data
@Accessors(chain = true)
public class ErpStockSummaryRespVO {

    @Schema(description = "总库存数")
    private BigDecimal totalStockCount = BigDecimal.ZERO;

    @Schema(description = "总金额")
    private BigDecimal totalCostAmount = BigDecimal.ZERO;

    @Schema(description = "备用价1金额 / 价格体系金额")
    private BigDecimal totalCurrentPriceAmount = BigDecimal.ZERO;

    @Schema(description = "总未入数")
    private BigDecimal totalPendingInCount = BigDecimal.ZERO;

    @Schema(description = "总占用数")
    private BigDecimal totalOccupiedCount = BigDecimal.ZERO;

    @Schema(description = "总在途数")
    private BigDecimal totalInTransitCount = BigDecimal.ZERO;

    @Schema(description = "总重量，单位 Kg")
    private BigDecimal totalWeight = BigDecimal.ZERO;

    @Schema(description = "总行数")
    private Long totalRows = 0L;

}
