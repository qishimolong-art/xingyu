package cn.iocoder.yudao.module.erp.controller.admin.report.vo.system;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ErpSystemReportSummaryRespVO {

    private Long supplierCount;
    private Long customerCount;
    private Long skuCount;
    private Long docCount;
    private Long lowStockSku;
    private Long zeroStockSku;
    private Long negativeStockSku;

    private BigDecimal purchaseAmount;
    private BigDecimal inAmount;
    private BigDecimal returnAmount;
    private BigDecimal netAmount;
    private BigDecimal purchaseCount;
    private BigDecimal inCount;
    private BigDecimal returnCount;
    private BigDecimal pendingQty;

    private BigDecimal saleAmount;
    private BigDecimal saleCount;

    private BigDecimal stockQty;
    private BigDecimal stockAmount;
    private BigDecimal occupiedQty;
    private BigDecimal availableQty;
    private BigDecimal inTransitQty;

}
