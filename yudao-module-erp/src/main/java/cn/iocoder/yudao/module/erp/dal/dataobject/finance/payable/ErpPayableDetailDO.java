package cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpPayableDetailDO {

    private String docType;
    private Integer bizType;
    private Long bizId;
    private LocalDateTime docDate;
    private String docNo;
    private BigDecimal increaseAmount;
    private BigDecimal paymentAmount;
    private BigDecimal writeOffAmount;
}
