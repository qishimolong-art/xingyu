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
    @ExcelProperty(value = "单据类型", index = 0)
    private String docType;

    @ExcelProperty(value = "日期", index = 1)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime docDate;

    @ExcelProperty(value = "单号", index = 2)
    private String docNo;

    @ExcelProperty(value = "上笔余额", index = 3)
    private BigDecimal prevBalance;

    @ExcelProperty(value = "增加应收", index = 4)
    private BigDecimal increaseAmount;

    @ExcelProperty(value = "其他应收", index = 5)
    private BigDecimal miscReceivableAmount = BigDecimal.ZERO;

    @ExcelProperty(value = "收款金额", index = 6)
    private BigDecimal receiptAmount;

    @ExcelProperty(value = "余额", index = 7)
    private BigDecimal balance;

    @ExcelProperty(value = "审核人", index = 8)
    private String auditorName;

    @ExcelProperty(value = "送货方式", index = 9)
    private String deliveryMethod;

    @ExcelProperty(value = "审核日期", index = 10)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @ExcelProperty(value = "结算方式", index = 11)
    private String settleMethod;

    @ExcelProperty(value = "开票情况", index = 12)
    private String invoiceStatus;

    @ExcelProperty(value = "票据金额", index = 13)
    private BigDecimal billAmount;

    @ExcelProperty(value = "票据号", index = 14)
    private String billNo;

    @ExcelProperty(value = "备注", index = 15)
    private String remark;

    @ExcelProperty(value = "凭证号", index = 16)
    private String voucherNo;

    @ExcelProperty(value = "客户名称", index = 17)
    private String customerName;

    @ExcelProperty(value = "说明", index = 18)
    private String internalNote;

    @ExcelProperty(value = "已核销金额", index = 19)
    private BigDecimal receivedWriteOffAmount = BigDecimal.ZERO;

    @ExcelProperty(value = "已收款金额", index = 20)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @ExcelProperty(value = "所属部门", index = 21)
    private String deptName;

    @ExcelProperty(value = "应收调账", index = 22)
    private BigDecimal otherReceivableAmount;

    @ExcelProperty(value = "核销金额", index = 23)
    private BigDecimal writeOffAmount;

    private Integer bizType;
    private Long bizId;
    private Long deptId;
    private Integer returnStatus;
    private Boolean priceAdjusted;
    private BigDecimal allocatedAmount;
    private BigDecimal writeOffBaseAmount;
    /** 独立其他应收展示行，不参与销售应收余额及抵销。 */
    private Boolean displayOnly;
}
