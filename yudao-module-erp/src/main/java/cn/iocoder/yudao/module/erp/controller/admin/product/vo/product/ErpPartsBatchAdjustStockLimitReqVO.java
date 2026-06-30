package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 配件批量调整库存上下限 Request VO")
@Data
public class ErpPartsBatchAdjustStockLimitReqVO {

    @Schema(description = "筛选条件")
    @Valid
    private ErpPartsPriceAdjustFilterVO filterCondition;

    @Schema(description = "销售期间开始日期")
    private LocalDateTime salePeriodStart;

    @Schema(description = "销售期间结束日期")
    private LocalDateTime salePeriodEnd;

    @Schema(description = "统计维度", example = "TOTAL_SALES_DAYS",
            allowableValues = {"TOTAL_SALES_DAYS"})
    private String salesStatType;

    @Schema(description = "库存上限天数", example = "30")
    @NotNull(message = "库存上限天数不能为空")
    private BigDecimal upperLimitDays;

    @Schema(description = "库存上限趋势系数", example = "1.000")
    @NotNull(message = "库存上限趋势系数不能为空")
    private BigDecimal upperLimitFactor;

    @Schema(description = "库存下限天数", example = "2")
    @NotNull(message = "库存下限天数不能为空")
    private BigDecimal lowerLimitDays;

    @Schema(description = "库存下限趋势系数", example = "1.000")
    @NotNull(message = "库存下限趋势系数不能为空")
    private BigDecimal lowerLimitFactor;

    @Schema(description = "标准库存天数", example = "7")
    @NotNull(message = "标准库存天数不能为空")
    private BigDecimal standardStockDays;

    @Schema(description = "标准库存趋势系数", example = "1.000")
    @NotNull(message = "标准库存趋势系数不能为空")
    private BigDecimal standardStockFactor;

    @Schema(description = "调整口令", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "调整口令不能为空")
    private String password;

}
