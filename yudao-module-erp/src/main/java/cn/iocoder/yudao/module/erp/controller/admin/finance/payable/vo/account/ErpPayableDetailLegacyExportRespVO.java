package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 保留未指定 dateBasis 的旧明细导出列序。 */
@ExcelIgnoreUnannotated
@Data
public class ErpPayableDetailLegacyExportRespVO {

    @ExcelProperty("单据类型")
    private String docType;
    @ExcelProperty("单据日期")
    private LocalDateTime docDate;
    @ExcelProperty("单据号")
    private String docNo;
    @ExcelProperty("业务员")
    private String businessUserName;
    @ExcelProperty("所属部门")
    private String deptName;
    @ExcelProperty(value = "上次余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal prevBalance;
    @ExcelProperty(value = "应付款", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal increaseAmount;
    @ExcelProperty(value = "付款金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal paymentDisplayAmount;
    @ExcelProperty(value = "余额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal balance;
    @ExcelProperty("来源单号")
    private String sourceNo;
    @ExcelProperty("厂家单号")
    private String factoryOrderNo;
    @ExcelProperty("开票情况")
    private String invoiceStatus;
    @ExcelProperty("备注")
    private String remark;
    @ExcelProperty("凭证号")
    private String voucherNo;
    @ExcelProperty("确认时间")
    private LocalDateTime confirmTime;
}
