package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleCartSummaryRespVO {

    private BigDecimal totalProductPrice;
    private BigDecimal discountPrice;
    private BigDecimal feeAmount;
    private BigDecimal totalPrice;

}
