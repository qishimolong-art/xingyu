package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 配件批量调整价格 Request VO")
@Data
public class ErpPartsBatchAdjustPriceReqVO {

    @Schema(description = "筛选条件")
    @Valid
    private ErpPartsPriceAdjustFilterVO filterCondition;

    @Schema(description = "目标价格类型", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "RETAIL_PRICE",
            allowableValues = {"SPARE_PRICE_1", "SPECIAL_PRICE", "REFERENCE_PRICE", "RETAIL_PRICE", "WHOLESALE_PRICE", "SHARE_PRICE", "BRANCH_PRICE", "BATCH_PRICE"})
    @NotBlank(message = "目标价格类型不能为空")
    private String targetPriceType;

    @Schema(description = "数据源价格类型", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "REFERENCE_PRICE",
            allowableValues = {"SPARE_PRICE_1", "SPECIAL_PRICE", "REFERENCE_PRICE", "RETAIL_PRICE", "WHOLESALE_PRICE", "SHARE_PRICE", "BRANCH_PRICE", "BATCH_PRICE", "LAST_PURCHASE_PRICE"})
    @NotBlank(message = "数据源价格类型不能为空")
    private String sourcePriceType;

    @Schema(description = "调整方式", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "MULTIPLY",
            allowableValues = {"ADD", "SUBTRACT", "MULTIPLY", "DIVIDE"})
    @NotBlank(message = "调整方式不能为空")
    private String adjustMethod;

    @Schema(description = "系数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.2")
    @NotNull(message = "系数不能为空")
    private BigDecimal adjustCoefficient;

    @Schema(description = "保留小数位数", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "保留小数位数不能为空")
    private Integer decimalPlaces;

}
