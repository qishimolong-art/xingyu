package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucherword;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 凭证字新增/修改 Request VO")
@Data
public class ErpVoucherWordSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "凭证字代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "记")
    @NotEmpty(message = "凭证字代码不能为空")
    private String code;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "记账凭证")
    @NotEmpty(message = "名称不能为空")
    private String name;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用不能为空")
    private Boolean enable;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "备注", example = "默认凭证字")
    private String remark;

}
