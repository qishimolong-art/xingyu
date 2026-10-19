package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleOutSummaryRespVO {

    private BigDecimal totalCount;
    private BigDecimal totalProductPrice;
    private BigDecimal reductionAmount;
    private BigDecimal afterReductionAmount;
    private BigDecimal billAmount;
    private BigDecimal freight;

}
