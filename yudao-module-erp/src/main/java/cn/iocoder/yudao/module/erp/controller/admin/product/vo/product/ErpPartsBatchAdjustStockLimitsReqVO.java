package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 配件批量调整库存上下限 Request VO")
@Data
public class ErpPartsBatchAdjustStockLimitsReqVO {

    @Schema(description = "筛选条件")
    @Valid
    private ErpPartsPriceAdjustFilterVO filterCondition;

    @Schema(description = "库存上限（null 表示不修改）")
    private Integer stockMax;

    @Schema(description = "库存下限（null 表示不修改）")
    private Integer stockMin;

    @Schema(description = "标准库存（null 表示不修改）")
    private Integer stockStandard;

    @Schema(description = "调整口令", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "调整口令不能为空")
    private String password;

}
