package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSalePriceAdjustSummaryRespVO {

    private BigDecimal totalOriginalPrice;
    private BigDecimal totalAdjustedPrice;
    private BigDecimal totalAdjustPrice;
    private BigDecimal taxExclusiveAdjustPrice;
    private BigDecimal taxPrice;

}
