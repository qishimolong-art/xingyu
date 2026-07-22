package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 销售手推车初审配置 Response VO")
@Data
public class ErpSaleCartFirstApproveConfigRespVO {

    @Schema(description = "是否启用初审", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean enabled;

    @Schema(description = "是否启用部门授权", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean deptAuthEnabled;

    @Schema(description = "授权部门是否包含子部门", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean includeChildDept;

    @Schema(description = "授权部门编号列表")
    private List<Long> deptIds;

    @Schema(description = "当前用户是否可执行初审", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean currentUserAllowed;

}
