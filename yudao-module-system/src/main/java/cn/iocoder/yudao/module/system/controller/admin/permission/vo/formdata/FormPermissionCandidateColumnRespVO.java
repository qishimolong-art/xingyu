package cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 表单数据权限候选字段 Response VO")
@Data
public class FormPermissionCandidateColumnRespVO {

    @Schema(description = "列名", requiredMode = Schema.RequiredMode.REQUIRED, example = "purchaser")
    private String columnName;

    @Schema(description = "字段说明", example = "采购员")
    private String columnDesc;

    @Schema(description = "数据库类型", example = "bigint")
    private String dataType;

    @Schema(description = "是否推荐勾选", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean recommended;

}
