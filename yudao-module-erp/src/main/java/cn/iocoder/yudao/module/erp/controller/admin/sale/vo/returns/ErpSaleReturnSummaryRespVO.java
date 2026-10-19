package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleReturnSummaryRespVO {

    private BigDecimal totalCount;
    private BigDecimal totalProductPrice;
    private BigDecimal discountPrice;
    private BigDecimal totalPrice;

}
