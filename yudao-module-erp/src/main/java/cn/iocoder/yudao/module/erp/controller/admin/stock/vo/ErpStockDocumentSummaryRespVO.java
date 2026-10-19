package cn.iocoder.yudao.module.erp.controller.admin.stock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpStockDocumentSummaryRespVO {

    private BigDecimal totalCount;
    private BigDecimal totalPrice;

}
