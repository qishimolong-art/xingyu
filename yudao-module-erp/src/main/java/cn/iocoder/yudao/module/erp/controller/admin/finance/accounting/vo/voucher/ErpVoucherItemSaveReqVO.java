package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 凭证分录 新增/修改 Request VO")
@Data
public class ErpVoucherItemSaveReqVO {

    @Schema(description = "分录编号", example = "1")
    private Long id;

    @Schema(description = "行号", example = "1")
    private Integer lineNo;

    @Schema(description = "摘要", example = "差旅费报销")
    private String summary;

    @Schema(description = "科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "科目编号不能为空")
    private Long subjectId;

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

    private java.util.List<cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.Auxiliary> auxiliaries;
}
