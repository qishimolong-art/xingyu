package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 供应商部门分配 Request VO")
@Data
public class ErpSupplierDeptDistributionSaveReqVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "供应商编号不能为空")
    private Long id;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "适用部门")
    private List<Long> deptIds;

    @Schema(description = "允许多部门", example = "true")
    private Boolean allowMultiDept;

}
