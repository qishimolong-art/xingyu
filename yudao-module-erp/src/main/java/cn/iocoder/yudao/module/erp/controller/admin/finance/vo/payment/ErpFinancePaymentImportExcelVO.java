package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpFinancePaymentImportExcelVO {

    @ExcelRequired
    @ExcelProperty("付款时间")
    private String paymentTime;

    @ExcelProperty("财务人员ID")
    private Long financeUserId;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelRequired
    @ExcelProperty("供应商ID")
    private Long supplierId;

    @ExcelRequired
    @ExcelProperty("付款账户ID")
    private Long accountId;

    @ExcelProperty("优惠金额")
    private BigDecimal discountPrice;

    @ExcelRequired
    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @ExcelRequired
    @ExcelProperty("实际付款金额")
    private BigDecimal paymentPrice;

    @ExcelProperty("备注")
    private String remark;

    @ExcelRequired
    @ExcelProperty("业务类型")
    private Integer bizType;

    @ExcelRequired
    @ExcelProperty("业务ID")
    private Long bizId;

    @ExcelProperty("已付金额")
    private BigDecimal paidPrice;

    @ExcelRequired
    @ExcelProperty("本次付款")
    private BigDecimal itemPaymentPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
