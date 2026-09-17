package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 新增/编辑付款单候选业务单据 Request VO")
@Data
public class ErpFinancePaymentFormCandidateReqVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long supplierId;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

    @Schema(description = "单据编号", example = "CGRK20260902000001")
    private String no;

}
