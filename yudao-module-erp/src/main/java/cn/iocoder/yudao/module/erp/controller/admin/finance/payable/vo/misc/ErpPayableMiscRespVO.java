package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpPayableMiscRespVO {

    private Long id;
    private String no;
    private Integer status;
    private LocalDateTime bizTime;
    private Long supplierId;
    private String supplierName;
    private String supplierContact;
    private String supplierMobile;
    private Long accountId;
    private String accountName;
    private BigDecimal amount;
    private String remark;
    private String fileUrl;
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
