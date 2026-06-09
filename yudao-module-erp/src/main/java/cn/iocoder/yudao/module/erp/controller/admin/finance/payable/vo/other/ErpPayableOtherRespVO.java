package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "ERP 其他应付 Response VO")
@Data
public class ErpPayableOtherRespVO {

    private Long id;

    private String no;

    private Integer status;

    private LocalDate bizTime;

    private Long supplierId;

    private String supplierName;

    private String supplierContact;

    private String supplierMobile;

    private String voucherNo;

    private BigDecimal settledAmount;

    private Long deptId;

    private String deptName;

    private BigDecimal payableAmount;

    private String project;

    private String sourceType;

    private Long handlerId;

    private String handlerName;

    private String remark;

    private String fileUrl;

    private String creator;

    private String creatorName;

    private LocalDateTime createTime;

    private String updater;

    private String updaterName;

    private LocalDateTime updateTime;

}
