package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpFinanceReceiptImportExcelVO {

    @ExcelRequired
    @ExcelProperty("收款时间")
    private String receiptTime;

    @ExcelProperty("财务人员ID")
    private Long financeUserId;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelRequired
    @ExcelProperty("客户ID")
    private Long customerId;

    @ExcelRequired
    @ExcelProperty("收款账户ID")
    private Long accountId;

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

    @ExcelRequired
    @ExcelProperty("业务类型")
    private Integer bizType;

    @ExcelRequired
    @ExcelProperty("业务ID")
    private Long bizId;

    @ExcelProperty("已收金额")
    private BigDecimal receiptedPrice;

    @ExcelRequired
    @ExcelProperty("本次收款")
    private BigDecimal itemReceiptPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
