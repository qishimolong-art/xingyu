package cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 表单数据权限配置保存 Request VO")
@Data
public class FormPermissionConfigSaveReqVO {

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "erp_purchase_order")
    @NotEmpty(message = "表名不能为空")
    private String formType;

    @Schema(description = "表说明", example = "ERP 采购订单")
    private String tableDesc;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    @Schema(description = "字段配置")
    @Valid
    private List<FormPermissionFieldConfigSaveReqVO> fields;

}
