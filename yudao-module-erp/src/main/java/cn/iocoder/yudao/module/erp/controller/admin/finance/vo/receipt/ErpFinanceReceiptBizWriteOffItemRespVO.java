package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 收款单业务单据核销明细 Response VO")
@Data
@Accessors(chain = true)
public class ErpFinanceReceiptBizWriteOffItemRespVO {

    @Schema(description = "收款单明细编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long receiptItemId;

    @Schema(description = "收款单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long receiptId;

    @Schema(description = "收款单号", example = "SK202609060001")
    private String receiptNo;

    @Schema(description = "收款时间")
    private LocalDateTime receiptTime;

    @Schema(description = "收款账户", example = "招商银行")
    private String accountName;

    @Schema(description = "经办人", example = "张三")
    private String financeUserName;

    @Schema(description = "本次收款/核销金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "60.00")
    private BigDecimal receiptPrice;

    @Schema(description = "核销时间")
    private LocalDateTime writeOffTime;

    @Schema(description = "核销状态", example = "1")
    private Integer writeOffStatus;

    @Schema(description = "备注", example = "销售出库核销")
    private String remark;

}
