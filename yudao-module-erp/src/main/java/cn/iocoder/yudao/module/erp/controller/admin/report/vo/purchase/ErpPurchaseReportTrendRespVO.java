package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchaseReportTrendRespVO {

    private String period;
    private Long docCount = 0L;
    private BigDecimal purchaseCount = BigDecimal.ZERO;
    private BigDecimal purchaseAmount = BigDecimal.ZERO;
    private BigDecimal returnCount = BigDecimal.ZERO;
    private BigDecimal returnAmount = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
}
