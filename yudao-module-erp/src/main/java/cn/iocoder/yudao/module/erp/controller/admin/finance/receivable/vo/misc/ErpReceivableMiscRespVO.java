package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpReceivableMiscRespVO {

    private Long id;
    private String no;
    private Integer status;
    private LocalDateTime bizTime;
    private Long customerId;
    private String customerName;
    private String customerContact;
    private String customerMobile;
    private Long accountId;
    private String accountName;
    private BigDecimal amount;
    private BigDecimal settledAmount;
    private BigDecimal balanceAmount;
    private String remark;
    private String fileUrl;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long sourceItemId;
    private Long sourceMiscId;
    private String sourceMiscNo;
    private Boolean generatedOffset;
    private Long deptId;
    private String deptName;
    private Long handlerId;
    private String handlerName;
    private String creator;
    private String creatorName;
    private LocalDateTime createTime;
    private String auditorName;
    private LocalDateTime auditTime;
    private String updater;
    private String updaterName;
    private LocalDateTime updateTime;

}
