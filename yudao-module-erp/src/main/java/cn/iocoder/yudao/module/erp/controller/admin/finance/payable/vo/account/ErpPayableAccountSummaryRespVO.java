package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "ERP 应付账款查询汇总 Response VO")
@Data
public class ErpPayableAccountSummaryRespVO {

    private Long supplierCount = 0L;
    private BigDecimal purchaseInAmount = BigDecimal.ZERO;
    private BigDecimal paymentAmount = BigDecimal.ZERO;
    private BigDecimal balance = BigDecimal.ZERO;
    private BigDecimal openingPayableBalance = BigDecimal.ZERO;
    private BigDecimal miscPayableAmount = BigDecimal.ZERO;
    private BigDecimal unclearedPrepayment = BigDecimal.ZERO;

}
