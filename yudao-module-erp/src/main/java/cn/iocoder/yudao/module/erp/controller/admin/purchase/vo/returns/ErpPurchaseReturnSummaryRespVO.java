package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ErpPurchaseReturnSummaryRespVO {
    private Long documentCount;
    private Long itemCount;
    private BigDecimal totalCount;
    private BigDecimal totalPrice;
}
