package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ErpPurchaseOrderSummaryRespVO {
    private BigDecimal totalCount;
    private BigDecimal totalProductPrice;
    private BigDecimal discountPrice;
    private BigDecimal feeAmount;
    private BigDecimal depositPrice;
    private BigDecimal totalPrice;
}
