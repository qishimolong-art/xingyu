package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpFinancePaymentExportRespVO {

    @ExcelProperty("付款单号")
    private String no;

    @ExcelProperty("付款状态")
    private Integer status;

    @ExcelProperty("付款时间")
    private LocalDateTime paymentTime;

    @ExcelProperty("经手人")
    private String financeUserName;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("付款账户")
    private String accountName;

    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @ExcelProperty("优惠金额")
    private BigDecimal discountPrice;

    @ExcelProperty("实际付款金额")
    private BigDecimal paymentPrice;

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

    @ExcelProperty("应付金额")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("已付金额")
    private BigDecimal itemPaidPrice;

    @ExcelProperty("本次付款")
    private BigDecimal itemPaymentPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
