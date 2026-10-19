package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpStockInBillSummaryRespVO {

    private Long documentCount;
    private BigDecimal totalCount;
    private BigDecimal pickedCount;
    private BigDecimal remainingCount;

}
