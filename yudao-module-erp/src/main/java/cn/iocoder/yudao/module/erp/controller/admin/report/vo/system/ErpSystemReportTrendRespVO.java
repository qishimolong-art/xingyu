package cn.iocoder.yudao.module.erp.controller.admin.report.vo.system;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ErpSystemReportTrendRespVO {

    private String period;
    private BigDecimal purchaseAmount;
    private BigDecimal inAmount;
    private BigDecimal returnAmount;
    private BigDecimal netAmount;
    private BigDecimal purchaseCount;
    private BigDecimal inCount;
    private BigDecimal returnCount;
    private BigDecimal saleAmount;
    private BigDecimal saleCount;
    private BigDecimal outAmount;
    private BigDecimal outCount;
    private BigDecimal stockAmount;
    private BigDecimal stockQty;
    private Long docCount;

}
