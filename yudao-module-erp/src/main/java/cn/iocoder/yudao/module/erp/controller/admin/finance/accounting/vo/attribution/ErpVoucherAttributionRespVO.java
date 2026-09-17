package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 凭证归属 Response VO")
@Data
public class ErpVoucherAttributionRespVO {

    @Schema(description = "归属记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long id;

    @Schema(description = "业务单据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer bizType;

    @Schema(description = "业务单 ID", example = "1024")
    private Long bizId;

    @Schema(description = "业务单号", example = "CGRK202605000001")
    private String bizNo;

    @Schema(description = "业务发生日期", example = "2026-05-14 10:00:00")
    private LocalDateTime bizDate;

    @Schema(description = "原始金额", example = "100.00")
    private BigDecimal bizAmount;

    @Schema(description = "折让金额", example = "0.00")
    private BigDecimal discountAmount;

    @Schema(description = "实收金额", example = "100.00")
    private BigDecimal receivedAmount;

    @Schema(description = "制单日期（实际生成日期）", example = "2026-05-14")
    private LocalDate voucherMakeDate;

    @Schema(description = "归属年", example = "2026")
    private Integer attributionYear;

    @Schema(description = "归属月", example = "5")
    private Integer attributionMonth;

    @Schema(description = "归属状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer attributionStatus;

    @Schema(description = "生成的凭证 ID", example = "200")
    private Long voucherId;

    @Schema(description = "交易单位", example = "客户A")
    private String transactionParty;

    @Schema(description = "业务员 ID", example = "100")
    private Long handlerUserId;

    @Schema(description = "审核人 ID", example = "100")
    private Long auditorUserId;

    @Schema(description = "确认时间")
    private LocalDateTime auditTime;

    @Schema(description = "结算方式", example = "现金")
    private String settleMethod;

    @Schema(description = "部门 ID", example = "1024")
    private Long deptId;

    @Schema(description = "运输方式", example = "自提")
    private String shippingMethod;

    @Schema(description = "应收确认", example = "true")
    private Boolean receivableConfirmed;

    @Schema(description = "是否开票", example = "false")
    private Boolean invoiceIssued;

    @Schema(description = "发货状态", example = "10")
    private Integer shipStatus;

    @Schema(description = "摘要", example = "采购入库归属 5 月")
    private String summary;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    private String generationStatus;
    private java.util.List<String> generationIssues;
}
