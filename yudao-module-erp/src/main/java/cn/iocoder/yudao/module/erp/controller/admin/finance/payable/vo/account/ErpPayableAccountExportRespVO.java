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

    @ExcelProperty(value = "采购入库金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal purchaseInAmount;

    @ExcelProperty(value = "采购退货金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal purchaseReturnAmount;

    @ExcelProperty(value = "调价金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal priceAdjustAmount;

    @ExcelProperty(value = "应付调账", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal otherPayableAmount;

    @ExcelProperty(value = "其他应付", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal miscPayableAmount;

    @ExcelProperty(value = "此前应付", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal openingPayableBalance;

    @ExcelProperty(value = "已付款", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal paymentAmount;

    @ExcelProperty(value = "已核销", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal writeOffAmount;

    @ExcelProperty(value = "应付余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;

    @ExcelProperty(value = "未核销预付款", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal unclearedPrepayment;

    @ExcelProperty("最近业务时间")
    private LocalDateTime lastBizTime;
}
