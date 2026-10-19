package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ErpPurchaseInSummaryRespVO {
    private BigDecimal totalCount;
    private BigDecimal totalProductPrice;
    private BigDecimal discountPrice;
    private BigDecimal totalFreight1;
    private BigDecimal totalPrice;
    private BigDecimal totalWithFreight;
}
