package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "ERP 应收账款明细 Response VO")
@ExcelIgnoreUnannotated
@Data
public class ErpReceivableDetailRespVO {
    @ExcelProperty(value = "审批日期", index = 0)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @ExcelProperty(value = "单据类型", index = 1)
    private String docType;

    @ExcelProperty(value = "单据时间", index = 22)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime docDate;

    @ExcelProperty(value = "单号", index = 2)
    private String docNo;

    @ExcelProperty(value = "上笔余额", index = 3, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal prevBalance;

    @ExcelProperty(value = "增加应收", index = 4, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal increaseAmount;

    @Schema(description = "实际收款金额，用于账户余额计算")
    private BigDecimal receiptAmount;

    @Schema(description = "收款展示金额，收款单取优惠前总金额")
    @ExcelProperty(value = "收款金额", index = 5, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal receiptDisplayAmount;

    @ExcelProperty(value = "余额", index = 6, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;

    @ExcelProperty(value = "审核人", index = 7)
    private String auditorName;

    @ExcelProperty(value = "送货方式", index = 8)
    private String deliveryMethod;

    @ExcelProperty(value = "结算方式", index = 9)
    private String settleMethod;

    @ExcelProperty(value = "开票情况", index = 10)
    private String invoiceStatus;

    @ExcelProperty(value = "票据金额", index = 11, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal billAmount;

    @ExcelProperty(value = "票据号", index = 12)
    private String billNo;

    @ExcelProperty(value = "备注", index = 13)
    private String remark;

    @ExcelProperty(value = "凭证号", index = 14)
    private String voucherNo;

    @ExcelProperty(value = "客户名称", index = 15)
    private String customerName;

    @ExcelProperty(value = "说明", index = 16)
    private String internalNote;

    @ExcelProperty(value = "已核销金额", index = 17)
    private BigDecimal receivedWriteOffAmount = BigDecimal.ZERO;

    @ExcelProperty(value = "已收款金额", index = 18)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @ExcelProperty(value = "所属部门", index = 19)
    private String deptName;

    @ExcelProperty(value = "应收调账", index = 20, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal otherReceivableAmount;

    @ExcelProperty(value = "核销金额", index = 21, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal writeOffAmount;

    private Integer bizType;
    private Long bizId;
    private Long deptId;
    private Integer returnStatus;
    private Boolean priceAdjusted;
    private BigDecimal allocatedAmount;
    private BigDecimal writeOffBaseAmount;
    private BigDecimal changeAmount;
    private String eventKey;
}
