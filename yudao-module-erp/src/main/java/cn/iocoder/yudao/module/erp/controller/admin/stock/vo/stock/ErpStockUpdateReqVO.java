package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Admin - ERP stock editable field update Request VO")
@Data
public class ErpStockUpdateReqVO {

    @Schema(description = "Stock id", requiredMode = Schema.RequiredMode.REQUIRED, example = "17086")
    @NotNull(message = "stock id cannot be empty")
    private Long id;

    @Schema(description = "Field name", requiredMode = Schema.RequiredMode.REQUIRED, example = "costPrice")
    @NotBlank(message = "field name cannot be empty")
    @Pattern(regexp = "shelf|purchasePrice|costPrice|costAmount",
            message = "unsupported editable stock field")
    private String fieldName;

    @Schema(description = "Shelf")
    @Size(max = 64, message = "shelf length cannot exceed 64 characters")
    private String shelf;

    @Schema(description = "Purchase price")
    @DecimalMin(value = "0", message = "purchase price cannot be less than 0")
    private BigDecimal purchasePrice;

    @Schema(description = "Cost price")
    @DecimalMin(value = "0", message = "cost price cannot be less than 0")
    private BigDecimal costPrice;

    @Schema(description = "Cost amount")
    @DecimalMin(value = "0", message = "cost amount cannot be less than 0")
    private BigDecimal costAmount;

}
