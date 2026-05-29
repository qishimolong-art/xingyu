package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应收账款明细 Response VO")
@Data
public class ErpReceivableDetailRespVO {

    private String docType;
    private LocalDateTime docDate;
    private String docNo;
    private BigDecimal prevBalance;
    private BigDecimal increaseAmount;
    private BigDecimal receiptAmount;
    private BigDecimal balance;
}
