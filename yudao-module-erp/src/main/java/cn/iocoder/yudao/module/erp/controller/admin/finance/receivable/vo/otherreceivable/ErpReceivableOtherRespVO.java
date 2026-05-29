package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ErpReceivableOtherRespVO {

    private Long id;
    private String no;
    private Integer status;
    private LocalDate bizTime;
    private Long customerId;
    private String customerName;
    private String customerContact;
    private String customerMobile;
    private String voucherNo;
    private BigDecimal settledAmount;
    private Long deptId;
    private String deptName;
    private BigDecimal receivableAmount;
    private String project;
    private String sourceType;
    private Long handlerId;
    private String handlerName;
    private String receivableType;
    private BigDecimal costAmount;
    private String remark;
    private Boolean isPaperNote;
    private String paperNoteDesc;
    private String sourceNo;
    private String fileUrl;
    private String creator;
    private String creatorName;
    private LocalDateTime createTime;
}
