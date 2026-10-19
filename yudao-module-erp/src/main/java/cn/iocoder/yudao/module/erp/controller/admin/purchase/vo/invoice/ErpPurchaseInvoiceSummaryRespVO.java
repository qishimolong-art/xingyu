package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ErpPurchaseInvoiceSummaryRespVO {
    private Long invoiceCount;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxExclusiveAmount;
}
