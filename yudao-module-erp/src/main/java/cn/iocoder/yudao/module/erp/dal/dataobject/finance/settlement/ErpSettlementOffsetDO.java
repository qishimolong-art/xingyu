package cn.iocoder.yudao.module.erp.dal.dataobject.finance.settlement;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSettlementOffsetDO {

    private String customerCode;
    private String subjectName;
    private Long customerId;
    private Long supplierId;
    private String contact;
    private String mobile;
    private BigDecimal receivableBalance;
    private BigDecimal payableBalance;
    private BigDecimal offsetBalance;
}
