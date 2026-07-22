package cn.iocoder.yudao.module.erp.service.sale.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class ErpCustomerCreditStatusBO {

    private Long customerId;
    private Boolean creditEnabled;
    private BigDecimal creditLimit;
    private Integer creditTermDays;
    private BigDecimal receivableBalance;
    private LocalDate earliestUnpaidDate;
    private Integer debtDays;
    private Boolean amountExceeded;
    private Boolean termExceeded;
    private Boolean blocked;
    private String blockedReason;

}
