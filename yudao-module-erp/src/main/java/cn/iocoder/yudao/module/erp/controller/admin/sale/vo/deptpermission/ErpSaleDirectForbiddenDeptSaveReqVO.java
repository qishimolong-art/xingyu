package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.deptpermission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售直接开单禁用部门保存 Request VO")
@Data
public class ErpSaleDirectForbiddenDeptSaveReqVO {

    @Schema(description = "禁止直接做销售单据的部门编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "部门编号列表不能为空")
    private List<Long> deptIds;

}
