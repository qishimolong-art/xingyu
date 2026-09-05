package cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleReportSummaryRespVO {

    private Long customerCount = 0L;
    private Long docCount = 0L;
    private BigDecimal saleCount = BigDecimal.ZERO;
    private BigDecimal saleAmount = BigDecimal.ZERO;
    private BigDecimal returnCount = BigDecimal.ZERO;
    private BigDecimal returnAmount = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
}
