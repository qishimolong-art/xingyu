package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "ERP 其他应收新增/修改 Request VO")
@Data
public class ErpReceivableOtherSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "业务日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务日期不能为空")
    private LocalDate bizTime;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "凭证号")
    private String voucherNo;

    @Schema(description = "已结金额")
    private BigDecimal settledAmount;

    @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "部门不能为空")
    private Long deptId;

    @Schema(description = "应收金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "应收金额不能为空")
    private BigDecimal receivableAmount;

    @Schema(description = "调账项目")
    private String project;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "经手人编号")
    private Long handlerId;

    @Schema(description = "应收类型")
    private String receivableType;

    @Schema(description = "成本金额")
    private BigDecimal costAmount;

    @Schema(description = "调账原因备注")
    private String remark;

    @Schema(description = "是否纸质单据")
    private Boolean isPaperNote;

    @Schema(description = "纸质单据说明")
    private String paperNoteDesc;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "附件 URL")
    private String fileUrl;
}
