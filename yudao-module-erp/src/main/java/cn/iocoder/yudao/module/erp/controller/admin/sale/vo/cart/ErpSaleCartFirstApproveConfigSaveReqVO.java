package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售手推车初审配置保存 Request VO")
@Data
public class ErpSaleCartFirstApproveConfigSaveReqVO {

    @Schema(description = "是否启用初审", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用初审不能为空")
    private Boolean enabled;

    @Schema(description = "是否启用部门授权", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    @NotNull(message = "是否启用部门授权不能为空")
    private Boolean deptAuthEnabled;

    @Schema(description = "授权部门是否包含子部门", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "授权部门是否包含子部门不能为空")
    private Boolean includeChildDept;

    @Schema(description = "授权部门编号列表")
    private List<Long> deptIds;

}
