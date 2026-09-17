package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 新增/编辑收款单候选业务单据 Request VO")
@Data
public class ErpFinanceReceiptFormCandidateReqVO {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long customerId;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

    @Schema(description = "单据编号", example = "XSCK20260902000001")
    private String no;

}
