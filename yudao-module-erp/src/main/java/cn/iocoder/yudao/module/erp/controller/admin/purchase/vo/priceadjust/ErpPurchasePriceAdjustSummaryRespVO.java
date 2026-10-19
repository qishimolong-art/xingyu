package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ErpPurchasePriceAdjustSummaryRespVO {
    private BigDecimal originalTotalPrice;
    private BigDecimal adjustedTotalPrice;
    private BigDecimal totalAdjustPrice;
    private BigDecimal paymentPrice;
}
