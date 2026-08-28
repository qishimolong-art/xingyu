package cn.iocoder.yudao.module.erp.controller.app.stock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 App - ERP 商城商品库存汇总 Response VO")
@Data
public class AppErpMallStockSummaryRespVO {

    @Schema(description = "商城 SPU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long spuId;

    @Schema(description = "总可用库存", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private BigDecimal totalAvailableCount;

}
