package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 凭证分录 Response VO")
@Data
public class ErpVoucherItemRespVO {

    @Schema(description = "分录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "凭证主表编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long voucherId;

    @Schema(description = "行号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer lineNo;

    @Schema(description = "摘要", example = "差旅费报销")
    private String summary;

    @Schema(description = "科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long subjectId;

    @Schema(description = "科目编码（冗余）", example = "1001")
    private String subjectCode;

    @Schema(description = "科目名称（冗余）", example = "现金")
    private String subjectName;

    @Schema(description = "辅助核算类型", example = "supplier")
    private String auxiliaryType;

    @Schema(description = "辅助核算 ID", example = "1024")
    private Long auxiliaryId;

    @Schema(description = "辅助核算名称（冗余）", example = "供应商A")
    private String auxiliaryName;

    @Schema(description = "借方金额", example = "100.00")
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额", example = "100.00")
    private BigDecimal creditAmount;

}
