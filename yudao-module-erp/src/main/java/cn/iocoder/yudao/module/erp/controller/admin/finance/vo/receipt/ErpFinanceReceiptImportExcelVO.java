package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpFinanceReceiptImportExcelVO {

    @ExcelProperty("收款时间")
    private String receiptTime;

    @ExcelProperty("财务人员")
    private String financeUserName;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelRequired
    @ExcelProperty("客户")
    private String customerName;

    @ExcelRequired
    @ExcelProperty("收款账户")
    private String accountName;

    @ExcelProperty("优惠金额")
    private BigDecimal discountPrice;

    @ExcelRequired
    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @ExcelRequired
    @ExcelProperty("实际收款金额")
    private BigDecimal receiptPrice;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("业务类型")
    private String bizType;

    @ExcelProperty("业务单号")
    private String bizNo;

    @ExcelProperty("已收金额")
    private BigDecimal receiptedPrice;

    @ExcelProperty("本次收款")
    private BigDecimal itemReceiptPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;

    @ExcelProperty("财务人员ID")
    private Long financeUserId;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelProperty("客户ID")
    private Long customerId;

    @ExcelProperty("收款账户ID")
    private Long accountId;

    @ExcelProperty("业务ID")
    private Long bizId;
}
