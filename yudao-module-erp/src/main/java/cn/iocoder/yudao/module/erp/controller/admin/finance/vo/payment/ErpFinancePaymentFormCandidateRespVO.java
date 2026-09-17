package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 新增/编辑付款单候选业务单据 Response VO")
@Data
public class ErpFinancePaymentFormCandidateRespVO {

    @Schema(description = "业务类型")
    private Integer bizType;

    @Schema(description = "业务类型名称")
    private String bizTypeName;

    @Schema(description = "业务单据编号")
    private Long bizId;

    @Schema(description = "业务单据号")
    private String bizNo;

    @Schema(description = "业务时间")
    private LocalDateTime bizTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "应付金额")
    private BigDecimal totalPrice;

    @Schema(description = "已核销金额")
    private BigDecimal allocatedPrice;

    @Schema(description = "未核销金额")
    private BigDecimal unallocatedPrice;

}
