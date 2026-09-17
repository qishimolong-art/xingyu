package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应付账款 Response VO")
@Data
public class ErpPayableAccountRespVO {

    @Schema(description = "供应商 ID", example = "1")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "供应商A")
    private String supplierName;

    @Schema(description = "联系人", example = "张三")
    private String contact;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "部门 ID", example = "1")
    private Long deptId;

    @Schema(description = "部门名称", example = "采购部")
    private String deptName;

    @Schema(description = "经手人 ID", example = "1")
    private Long handlerId;

    @Schema(description = "经手人名称", example = "李四")
    private String handlerName;

    @Schema(description = "采购入库金额")
    private BigDecimal purchaseInAmount;

    @Schema(description = "采购退货金额")
    private BigDecimal purchaseReturnAmount;

    @Schema(description = "采购调价金额")
    private BigDecimal priceAdjustAmount;

    @Schema(description = "其他应付金额")
    private BigDecimal otherPayableAmount;

    @Schema(description = "独立其他应付金额")
    private BigDecimal miscPayableAmount;

    @Schema(description = "付款金额")
    private BigDecimal paymentAmount;

    @Schema(description = "核销金额")
    private BigDecimal writeOffAmount;

    @Schema(description = "应付余额")
    private BigDecimal balance;

    @Schema(description = "未核销预付款")
    private BigDecimal unclearedPrepayment;

    @Schema(description = "最近业务时间")
    private LocalDateTime lastBizTime;
}
