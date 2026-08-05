package cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 表单数据权限候选表 Response VO")
@Data
public class FormPermissionCandidateTableRespVO {

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "erp_purchase_order")
    private String formType;

    @Schema(description = "表说明", example = "ERP 采购订单")
    private String tableDesc;

    @Schema(description = "是否已接入", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean configured;

}
