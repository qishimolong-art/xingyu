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
    private Long areaId;
    private Integer customerType;
    private Long saleUserId;
    private String saleUserName;
    private Long deptId;
    private String deptName;
    private Long routeId;
    private BigDecimal baseAmount;
    private BigDecimal creditLimit;
    private BigDecimal creditBalance;
    private Integer creditTermDays;
    private BigDecimal saleOutAmount;
    private BigDecimal saleReturnAmount;
    private BigDecimal priceAdjustAmount;
    private BigDecimal receiptAmount;
    private BigDecimal writeOffAmount;
    private BigDecimal otherReceivableAmount;
    private BigDecimal receivableAmount;
    private BigDecimal receivedAmount;
    private BigDecimal unreceivedAmount;
    private String billType;
    private String receiveStatus;
    private BigDecimal receivableBalance;
    private BigDecimal preAdvanceAmount;
    private BigDecimal totalReceivable;
    private LocalDateTime lastBizTime;

}
