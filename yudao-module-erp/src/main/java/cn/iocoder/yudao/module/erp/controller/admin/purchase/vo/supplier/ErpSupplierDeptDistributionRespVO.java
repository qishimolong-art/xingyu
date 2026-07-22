package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - ERP 供应商部门分配 Response VO")
@Data
public class ErpSupplierDeptDistributionRespVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "供应商名称")
    private String name;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "适用部门")
    private List<Long> deptIds;

    @Schema(description = "适用部门名称")
    private String deptNames;

    private Map<Long, String> deptNameMap;

    @Schema(description = "允许多部门", example = "true")
    private Boolean allowMultiDept;

}
