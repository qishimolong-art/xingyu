package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 客户部门授信保存 Request VO")
@Data
public class ErpCustomerDeptCreditSaveReqVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户编号不能为空")
    private Long id;

    @Schema(description = "部门授信配置")
    @Valid
    private List<Item> items;

    @Schema(description = "部门授信配置明细")
    @Data
    public static class Item {

        @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "102")
        @NotNull(message = "部门编号不能为空")
        private Long deptId;

        @Schema(description = "是否启用授信", example = "true")
        private Boolean creditEnabled;

        @Schema(description = "授信金额", example = "100000.00")
        @DecimalMin(value = "0", message = "授信金额不能小于 0")
        private BigDecimal creditLimit;

        @Schema(description = "授信期限（天）", example = "30")
        @Min(value = 0, message = "授信期限不能小于 0")
        private Integer creditTermDays;

        @Schema(description = "备注")
        private String remark;

    }

}
