package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpFinanceReceiptExportRespVO {

    @ExcelProperty("收款单号")
    private String no;

    @ExcelProperty("收款状态")
    private Integer status;

    @ExcelProperty("收款时间")
    private LocalDateTime receiptTime;

    @ExcelProperty("财务人员")
    private String financeUserName;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("收款账户")
    private String accountName;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("开户行")
    private String bankName;

    @ExcelProperty("来源其他应收单")
    private String sourceReceivableMiscNo;

    @ExcelProperty(value = "合计金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalPrice;

    @ExcelProperty(value = "优惠金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal discountPrice;

    @ExcelProperty(value = "实际收款金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal receiptPrice;

    @ExcelProperty(value = "已核销金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal allocatedPrice;

    @ExcelProperty(value = "未核销金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal unallocatedPrice;

    @ExcelProperty("凭证号")
    private String paymentVoucherNo;

    @ExcelProperty("会计凭证号")
    private String voucherNo;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("审核时间")
    private LocalDateTime approveTime;

    @ExcelProperty("业务类型")
    private String bizTypeName;

    @ExcelProperty("业务单号")
    private String bizNo;

    @ExcelProperty(value = "应收金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemTotalPrice;

    @ExcelProperty(value = "已收金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemReceiptedPrice;

    @ExcelProperty(value = "本次收款", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemReceiptPrice;

    @ExcelProperty("核销状态")
    private Integer itemWriteOffStatus;

    @ExcelProperty("核销时间")
    private LocalDateTime itemWriteOffTime;

    @ExcelProperty("撤销原因")
    private String itemReverseReason;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
