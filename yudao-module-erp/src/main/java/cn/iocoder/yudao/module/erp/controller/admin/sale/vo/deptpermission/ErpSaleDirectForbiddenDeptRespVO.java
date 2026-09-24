package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.deptpermission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 销售直接开单禁用部门 Response VO")
@Data
public class ErpSaleDirectForbiddenDeptRespVO {

    @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long deptId;

    @Schema(description = "部门名称", example = "兴宇路通")
    private String deptName;

}
