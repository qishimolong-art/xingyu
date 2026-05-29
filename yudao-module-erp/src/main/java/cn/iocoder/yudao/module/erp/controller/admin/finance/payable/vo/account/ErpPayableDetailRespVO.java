package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应付明细 Response VO")
@Data
public class ErpPayableDetailRespVO {

    @Schema(description = "单据类型", example = "采购入库")
    private String docType;

    @Schema(description = "单据日期")
    private LocalDateTime docDate;

    @Schema(description = "单据号", example = "CGRK202605270001")
    private String docNo;

    @Schema(description = "上笔余额")
    private BigDecimal prevBalance;

    @Schema(description = "增加金额")
    private BigDecimal increaseAmount;

    @Schema(description = "减少金额")
    private BigDecimal paymentAmount;

    @Schema(description = "余额")
    private BigDecimal balance;
}
