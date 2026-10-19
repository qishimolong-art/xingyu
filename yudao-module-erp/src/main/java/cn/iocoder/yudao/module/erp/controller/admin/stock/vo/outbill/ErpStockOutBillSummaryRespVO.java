package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpStockOutBillSummaryRespVO {

    private BigDecimal totalWeight;
    private BigDecimal wholeQty;
    private BigDecimal looseQty;
    private BigDecimal totalCount;
    private BigDecimal pickedCount;

}
