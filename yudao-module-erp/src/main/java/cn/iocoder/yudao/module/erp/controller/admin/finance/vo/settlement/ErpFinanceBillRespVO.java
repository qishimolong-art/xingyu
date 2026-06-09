package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 收付款统一 Response VO")
@Data
public class ErpFinanceBillRespVO {

    private Long id;
    private String no;
    private String billType;
    private String billTypeName;
    private Integer status;
    private LocalDateTime billTime;
    private Long financeUserId;
    private String financeUserName;
    private Long deptId;
    private String deptName;
    private Long customerId;
    private String customerName;
    private Long supplierId;
    private String supplierName;
    private Long accountId;
    private String accountName;
    private BigDecimal totalPrice;
    private BigDecimal discountPrice;
    private BigDecimal actualPrice;
    private String remark;
    private String creator;
    private String creatorName;
    private LocalDateTime createTime;
    private String updater;
    private String updaterName;
    private LocalDateTime updateTime;

}
