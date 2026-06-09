package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 账户收付款流水 Response VO")
@Data
public class ErpAccountTransactionRespVO {

    @Schema(description = "单据编号")
    private String no;

    @Schema(description = "账单类型：receipt-收款，payment-付款")
    private String billType;

    @Schema(description = "单据类型名称")
    private String billTypeName;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "交易时间")
    private LocalDateTime transactionTime;

    @Schema(description = "关联业务单据号")
    private String bizNo;

    @Schema(description = "备注")
    private String remark;

}
