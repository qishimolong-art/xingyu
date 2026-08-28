package cn.iocoder.yudao.module.erp.controller.app.stock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - ERP 商城商品库存选项 Response VO")
@Data
public class AppErpMallStockOptionRespVO {

    @Schema(description = "库存记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long stockId;

    @Schema(description = "ERP 产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long erpProductId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long warehouseId;

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "主仓")
    private String warehouseName;

    @Schema(description = "是否可选", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean available;

    @Schema(description = "库存状态文案", requiredMode = Schema.RequiredMode.REQUIRED, example = "现货")
    private String availableStatusText;

}
