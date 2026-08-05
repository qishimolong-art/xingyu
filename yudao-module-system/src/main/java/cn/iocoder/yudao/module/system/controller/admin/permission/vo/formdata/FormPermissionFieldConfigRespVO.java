package cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 表单数据权限字段配置 Response VO")
@Data
public class FormPermissionFieldConfigRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "erp_purchase_order")
    private String formType;

    @Schema(description = "列名", requiredMode = Schema.RequiredMode.REQUIRED, example = "purchaser")
    private String columnName;

    @Schema(description = "字段说明", example = "采购员")
    private String columnDesc;

    @Schema(description = "值类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "single_id")
    private String valueType;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

}
