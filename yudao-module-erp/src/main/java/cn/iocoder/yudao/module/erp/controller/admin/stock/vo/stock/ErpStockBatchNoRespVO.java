package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 可用批次号 Response VO")
@Data
@Accessors(chain = true)
public class ErpStockBatchNoRespVO {

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String batchNo;

    @Schema(description = "可用数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal availableCount;

    @Schema(description = "首次入库时间")
    private LocalDateTime firstInTime;

}
