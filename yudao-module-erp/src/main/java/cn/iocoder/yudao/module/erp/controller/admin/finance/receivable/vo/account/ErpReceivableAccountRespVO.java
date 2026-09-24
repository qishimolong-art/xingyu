package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应收账款 Response VO")
@Data
public class ErpReceivableAccountRespVO {
    private String accountKey;
    private String deptKey;
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
    @Schema(description = "应收调账金额")
    private BigDecimal otherReceivableAmount;
    @Schema(description = "其他应收款金额，取独立其他应收合计的负数，不计入应收余额")
    private BigDecimal miscReceivableAmount;
    @Schema(description = "此前应收金额，按筛选开始时间之前的应收主账款余额统计")
    private BigDecimal openingReceivableBalance;
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
