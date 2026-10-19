package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleQuoteSummaryRespVO {

    private BigDecimal totalProductPrice;
    private BigDecimal discountPrice;
    private BigDecimal totalPrice;

}
