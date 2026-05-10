package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 库存进出流水 底部汇总 VO（五期）
 */
@Schema(description = "管理后台 - 库存进出流水 底部汇总 VO")
@Data
@Accessors(chain = true)
public class ErpStockRecordSummaryVO {

    @Schema(description = "总入库数")
    private BigDecimal totalInCount = BigDecimal.ZERO;
    @Schema(description = "总入库金额")
    private BigDecimal totalInAmount = BigDecimal.ZERO;
    @Schema(description = "总出库数")
    private BigDecimal totalOutCount = BigDecimal.ZERO;
    @Schema(description = "总出库成本金额")
    private BigDecimal totalOutAmount = BigDecimal.ZERO;
    @Schema(description = "记录条数")
    private Long recordCount = 0L;

}
