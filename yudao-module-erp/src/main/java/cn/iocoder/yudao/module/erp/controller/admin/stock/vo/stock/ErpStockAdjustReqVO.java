package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 库存手动调整 Request VO")
@Data
public class ErpStockAdjustReqVO {

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    @NotNull(message = "产品编号不能为空")
    private Long productId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "调整后库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.000")
    @NotNull(message = "调整后库存数量不能为空")
    private BigDecimal targetCount;

    @Schema(description = "调整原因（盘点调整/报损/报溢）", example = "盘点调整")
    private String reason;

    @Schema(description = "备注")
    private String remark;
}
