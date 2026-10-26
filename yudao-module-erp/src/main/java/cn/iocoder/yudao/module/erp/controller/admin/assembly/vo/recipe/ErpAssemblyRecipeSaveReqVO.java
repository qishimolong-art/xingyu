package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 组装配方保存 Request VO")
@Data
public class ErpAssemblyRecipeSaveReqVO {

    @Schema(description = "配方编号")
    private Long id;

    @Schema(description = "成品配件编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "成品配件不能为空")
    private Long productId;

    @Schema(description = "用途：1 装配")
    private Integer purpose;

    @Schema(description = "模块编码")
    @Size(max = 64, message = "模块编码不能超过 64 个字符")
    private String moduleCode;

    @Schema(description = "模块名称")
    @Size(max = 255, message = "模块名称不能超过 255 个字符")
    private String moduleName;

    @Schema(description = "每套人工费", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每套人工费不能为空")
    @DecimalMin(value = "0", message = "每套人工费不能小于 0")
    @Digits(integer = 18, fraction = 6, message = "每套人工费最多支持 18 位整数和 6 位小数")
    private BigDecimal laborCostPerSet;

    @Schema(description = "状态：0 停用，1 启用")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

    @Valid
    @NotEmpty(message = "配方至少需要一个组成配件")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "配方明细编号")
        private Long id;

        @Schema(description = "组成配件编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "组成配件不能为空")
        private Long productId;

        @Schema(description = "每套用量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "每套用量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "每套用量必须大于 0")
        @Digits(integer = 18, fraction = 6, message = "每套用量最多支持 18 位整数和 6 位小数")
        private BigDecimal quantityPerSet;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "备注")
        @Size(max = 500, message = "明细备注不能超过 500 个字符")
        private String remark;
    }

}
