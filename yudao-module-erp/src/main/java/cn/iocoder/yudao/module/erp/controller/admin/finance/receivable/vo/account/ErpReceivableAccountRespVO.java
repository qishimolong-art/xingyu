package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应收账款 Response VO")
@Data
public class ErpReceivableAccountRespVO {
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
