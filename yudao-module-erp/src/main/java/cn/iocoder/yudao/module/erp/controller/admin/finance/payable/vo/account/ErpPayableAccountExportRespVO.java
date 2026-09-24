package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpPayableAccountExportRespVO {

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("联系人")
    private String contact;

    @ExcelProperty("联系电话")
    private String mobile;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("经办人")
    private String handlerName;

    @ExcelProperty("采购入库金额")
    private BigDecimal purchaseInAmount;

    @ExcelProperty("采购退货金额")
    private BigDecimal purchaseReturnAmount;

    @ExcelProperty("调价金额")
    private BigDecimal priceAdjustAmount;

    @ExcelProperty("其他应付")
    private BigDecimal miscPayableAmount;

    @ExcelProperty("已付款")
    private BigDecimal paymentAmount;

    @ExcelProperty("已核销")
    private BigDecimal writeOffAmount;

    @ExcelProperty("应付余额")
    private BigDecimal balance;

    @ExcelProperty("未核销预付款")
    private BigDecimal unclearedPrepayment;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
