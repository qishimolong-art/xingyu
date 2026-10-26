package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 套件组装单保存 Request VO")
@Data
public class ErpAssemblyOrderSaveReqVO {

    private Long id;

    @NotNull(message = "组装配方不能为空")
    private Long recipeId;

    @Schema(description = "客户编号（对象）")
    private Long customerId;

    @Schema(description = "经办人编号")
    private Long handlerId;

    @NotNull(message = "组装套数不能为空")
    @Min(value = 1, message = "组装套数必须是正整数")
    private Integer setCount;

    @NotNull(message = "组装仓库不能为空")
    private Long warehouseId;

    private Long outWarehouseId;

    @Size(max = 64, message = "仓位不能超过 64 个字符")
    private String shelf;

    private LocalDateTime assemblyTime;

    @Size(max = 64, message = "成品批次号不能超过 64 个字符")
    private String finishedBatchNo;

    @DecimalMin(value = "0", message = "其他成本不能小于 0")
    @Digits(integer = 18, fraction = 6, message = "其他成本最多支持 18 位整数和 6 位小数")
    private BigDecimal otherCost;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;

        @NotNull(message = "配方明细编号不能为空")
        private Long recipeItemId;

        @Size(max = 64, message = "批次号不能超过 64 个字符")
        private String batchNo;

        @NotNull(message = "消耗数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "消耗数量必须大于 0")
        @Digits(integer = 18, fraction = 6, message = "消耗数量最多支持 18 位整数和 6 位小数")
        private BigDecimal consumeQuantity;

        @Size(max = 500, message = "明细备注不能超过 500 个字符")
        private String remark;
    }

}
