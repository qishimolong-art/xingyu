package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "ERP 应收账款查询汇总 Response VO")
@Data
public class ErpReceivableAccountSummaryRespVO {

    private Long comboCount = 0L;
    private Long customerCount = 0L;
    private BigDecimal receivableBalance = BigDecimal.ZERO;
    private BigDecimal openingReceivableBalance = BigDecimal.ZERO;
    private BigDecimal preAdvanceAmount = BigDecimal.ZERO;
    private BigDecimal receiptAmount = BigDecimal.ZERO;
    private BigDecimal miscReceivableAmount = BigDecimal.ZERO;
    private BigDecimal creditBalance = BigDecimal.ZERO;

}
