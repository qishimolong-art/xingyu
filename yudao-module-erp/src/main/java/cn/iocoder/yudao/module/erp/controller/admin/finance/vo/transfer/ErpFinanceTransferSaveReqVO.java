package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 银行转账单新增/修改 Request VO")
@Data
public class ErpFinanceTransferSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "转账时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "转账时间不能为空")
    private LocalDateTime transferTime;

    @Schema(description = "转出账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "转出账户不能为空")
    private Long outAccountId;

    @Schema(description = "转入账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "转入账户不能为空")
    private Long inAccountId;

    @Schema(description = "转账金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "转账金额不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "转账金额必须大于 0")
    private BigDecimal transferPrice;

    @Schema(description = "汇率", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @DecimalMin(value = "0", inclusive = false, message = "汇率必须大于 0")
    private BigDecimal exchangeRate;

    @Schema(description = "手续费", example = "1.00")
    @DecimalMin(value = "0", message = "手续费不能小于 0")
    private BigDecimal feePrice;

    @Schema(description = "费用项目", example = "银行手续费")
    @Size(max = 64, message = "费用项目长度不能超过 64 个字符")
    private String feeExpenseCategory;

    @Schema(description = "财务人员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long financeUserId;

    @Schema(description = "所属部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long deptId;

    @Schema(description = "备注", example = "同行转账")
    @Size(max = 512, message = "备注长度不能超过 512 个字符")
    private String remark;

    @Schema(description = "附件 URL", example = "https://example.com/file.pdf")
    @Size(max = 512, message = "附件长度不能超过 512 个字符")
    private String fileUrl;

}
