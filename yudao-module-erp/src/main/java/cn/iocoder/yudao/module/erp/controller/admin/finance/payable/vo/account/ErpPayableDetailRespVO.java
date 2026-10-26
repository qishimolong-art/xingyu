package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应付明细 Response VO")
@ExcelIgnoreUnannotated
@Data
public class ErpPayableDetailRespVO {

    @Schema(description = "审批日期")
    @ExcelProperty(value = "审批日期", index = 0)
    private LocalDateTime approveTime;

    @Schema(description = "单据类型", example = "采购入库")
    @ExcelProperty(value = "单据类型", index = 1)
    private String docType;

    @Schema(description = "业务类型")
    private Integer bizType;

    @Schema(description = "业务单据编号")
    private Long bizId;

    @Schema(description = "单据日期")
    @ExcelProperty(value = "单据时间", index = 14)
    private LocalDateTime docDate;

    @Schema(description = "单据号", example = "CGRK202605270001")
    @ExcelProperty(value = "单据号", index = 2)
    private String docNo;

    @Schema(description = "业务员")
    @ExcelProperty(value = "业务员", index = 3)
    private String businessUserName;

    @JsonIgnore
    @Schema(description = "业务员用户 ID")
    private Long businessUserId;

    @Schema(description = "采购入库单是否被调过价")
    private Boolean priceAdjusted;

    @Schema(description = "部门 ID", example = "1")
    private Long deptId;

    @Schema(description = "部门名称", example = "采购部")
    @ExcelProperty(value = "所属部门", index = 4)
    private String deptName;

    @Schema(description = "上次余额")
    @ExcelProperty(value = "上次余额", index = 5, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal prevBalance;

    @Schema(description = "应付款")
    @ExcelProperty(value = "应付款", index = 6, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal increaseAmount;

    @Schema(description = "实际付款金额，用于账户余额计算")
    private BigDecimal paymentAmount;

    @Schema(description = "付款展示金额，付款单取优惠前总金额")
    @ExcelProperty(value = "付款金额", index = 7, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal paymentDisplayAmount;

    @Schema(description = "核销金额")
    private BigDecimal writeOffAmount;

    @Schema(description = "业务单据已关联的有效付款核销金额")
    private BigDecimal allocatedAmount;

    @Schema(description = "核销状态判断的单据基准金额")
    private BigDecimal writeOffBaseAmount;

    @Schema(description = "余额")
    @ExcelProperty(value = "余额", index = 8, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;

    @Schema(description = "来源单号")
    @ExcelProperty(value = "来源单号", index = 9)
    private String sourceNo;

    @Schema(description = "来源单据类型：PURCHASE_ORDER、FINANCE_PAYMENT")
    private String sourceType;

    @Schema(description = "来源单据 ID")
    private Long sourceId;

    @Schema(description = "厂家单号")
    @ExcelProperty(value = "厂家单号", index = 10)
    private String factoryOrderNo;

    @Schema(description = "开票情况")
    @ExcelProperty(value = "开票情况", index = 11)
    private String invoiceStatus;

    @Schema(description = "备注")
    @ExcelProperty(value = "备注", index = 12)
    private String remark;

    @Schema(description = "凭证号")
    @ExcelProperty(value = "凭证号", index = 13)
    private String voucherNo;

    @JsonIgnore
    @Schema(description = "凭证来源业务类型")
    private Integer voucherSourceBizType;

    @Schema(description = "确认时间（兼容旧调用方，与审批日期相同）")
    private LocalDateTime confirmTime;

    @Schema(description = "当前下钻维度的本笔变动金额")
    private BigDecimal changeAmount;
    private String eventKey;
}
