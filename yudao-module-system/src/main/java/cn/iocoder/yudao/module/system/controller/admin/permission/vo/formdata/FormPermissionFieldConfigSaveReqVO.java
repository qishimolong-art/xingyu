package cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 表单数据权限字段配置保存 Request VO")
@Data
public class FormPermissionFieldConfigSaveReqVO {

    @Schema(description = "列名", requiredMode = Schema.RequiredMode.REQUIRED, example = "purchaser")
    @NotEmpty(message = "列名不能为空")
    private String columnName;

    @Schema(description = "字段说明", example = "采购员")
    private String columnDesc;

    @Schema(description = "值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "single_id")
    @NotEmpty(message = "值类型不能为空")
    private String valueType;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

}
