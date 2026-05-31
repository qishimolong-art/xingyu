package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
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

    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @ExcelProperty("优惠金额")
    private BigDecimal discountPrice;

    @ExcelProperty("实际收款金额")
    private BigDecimal receiptPrice;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("业务类型")
    private String bizTypeName;

    @ExcelProperty("业务单号")
    private String bizNo;

    @ExcelProperty("应收金额")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("已收金额")
    private BigDecimal itemReceiptedPrice;

    @ExcelProperty("本次收款")
    private BigDecimal itemReceiptPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
