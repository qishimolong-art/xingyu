package cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpReceivableAccountDO {

    private Long customerId;
    private String customerName;
    private String contact;
    private String mobile;
    private Integer customerType;
    private Long saleUserId;
    private String saleUserName;
    private Long deptId;
    private String deptName;
    private BigDecimal saleOutAmount;
    private BigDecimal saleReturnAmount;
    private BigDecimal priceAdjustAmount;
    private BigDecimal receiptAmount;
    private BigDecimal otherReceivableAmount;
    private BigDecimal receivableBalance;
    private BigDecimal preAdvanceAmount;
    private BigDecimal totalReceivable;
    private LocalDateTime lastBizTime;

}
