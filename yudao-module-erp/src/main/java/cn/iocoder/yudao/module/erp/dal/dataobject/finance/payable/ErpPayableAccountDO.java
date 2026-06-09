package cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpPayableAccountDO {

    private Long supplierId;
    private String supplierName;
    private String contact;
    private String mobile;
    private Long deptId;
    private String deptName;
    private Long handlerId;
    private String handlerName;
    private BigDecimal purchaseInAmount;
    private BigDecimal purchaseReturnAmount;
    private BigDecimal priceAdjustAmount;
    private BigDecimal otherPayableAmount;
    private BigDecimal paymentAmount;
    private BigDecimal writeOffAmount;
    private BigDecimal balance;
    private BigDecimal unclearedPrepayment;
    private LocalDateTime lastBizTime;
}
